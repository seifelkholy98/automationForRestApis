package com.dissertation.openAPITestGeneration.automatioForRestAPIs.service;

import com.dissertation.openAPITestGeneration.automatioForRestAPIs.client.GPT3Client;
import com.dissertation.openAPITestGeneration.automatioForRestAPIs.models.TestCase;
import com.dissertation.openAPITestGeneration.automatioForRestAPIs.parser.OpenAPIDocumentParser;
import com.dissertation.openAPITestGeneration.automatioForRestAPIs.util.TestCaseGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TestCaseGenerationService {
    private static final Logger logger = LoggerFactory.getLogger(TestCaseGenerationService.class);

    @Autowired
    private OpenAPIDocumentParser openAPIDocumentParser;

    @Autowired
    private TestCaseGenerator testCaseGenerator;

    @Autowired
    private GPT3Client gpt3Client;

    private Jedis redisClient;
    private ObjectMapper objectMapper;

    private final Counter fileProcessingCounter;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Timer fileProcessingTimer;
    private final Counter testCaseGenerationSuccessCounter;
    private final Counter testCaseGenerationFailureCounter;
    private final DistributionSummary cacheHitMissRatio;
    private final Counter endpointGenerationCounter;
    private final Gauge uptimeGauge;
    private final long startTime;

    @Autowired
    public TestCaseGenerationService(ResourceLoader resourceLoader, MeterRegistry meterRegistry) throws IOException {
        // Initialize Redis client
        redisClient = new Jedis("localhost", 6379);

        // Initialize ObjectMapper for JSON serialization/deserialization
        objectMapper = new ObjectMapper();

        // Initialize metrics
        endpointGenerationCounter = meterRegistry.counter("endpoints.generated.count");
        fileProcessingCounter = meterRegistry.counter("file.processing.count");
        cacheHitCounter = meterRegistry.counter("cache.hits");
        cacheMissCounter = meterRegistry.counter("cache.misses");
        fileProcessingTimer = meterRegistry.timer("file.processing.time");
        testCaseGenerationSuccessCounter = meterRegistry.counter("testcase.generation.success.count");
        testCaseGenerationFailureCounter = meterRegistry.counter("testcase.generation.failure.count");
        cacheHitMissRatio = DistributionSummary.builder("cache.hit.miss.ratio")
                .description("Ratio of cache hits to misses")
                .register(meterRegistry);

        startTime = System.currentTimeMillis();
        uptimeGauge = Gauge.builder("application.uptime.seconds", this::getUptime)
                .description("Application uptime in seconds")
                .register(meterRegistry);
    }

    @Async
    public CompletableFuture<List<TestCase>> generateTestCases(MultipartFile file) throws Exception {
        fileProcessingCounter.increment();
        long start = System.nanoTime();
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try {
            validateFile(file);
            File convFile = convertToFile(file);
            logger.info("File successfully converted: {}", convFile.getPath());

            OpenAPI openAPI = openAPIDocumentParser.parseDocument(convFile.getPath());
            if (openAPI == null) {
                throw new Exception("Parsed OpenAPI document is null. Please ensure the file is a valid OpenAPI document.");
            }

            List<Map.Entry<String, Operation>> endpoints = openAPIDocumentParser.extractEndpoints(openAPI);
            String cacheKey = generateCacheKey(file.getOriginalFilename(), endpoints);

            endpointGenerationCounter.increment(endpoints.size());
            // Check Redis cache
            String cachedTestCases = redisClient.get(cacheKey);
            if (cachedTestCases != null && !cachedTestCases.equals("[]")) {
                List<TestCase> deserializedTestCases = deserializeTestCases(cachedTestCases);
                if (deserializedTestCases != null && !deserializedTestCases.isEmpty()) {
                    cacheHitCounter.increment();
                    cacheHitMissRatio.record(1);
                    logger.info("Test cases found in Redis cache for key: {}", cacheKey);
                    return CompletableFuture.completedFuture(deserializedTestCases);
                }
            }

            cacheMissCounter.increment();
            cacheHitMissRatio.record(0);

            // Generate interdependent test cases using parallel streams for efficiency
            List<String> dependentTestCaseDescriptions = testCaseGenerator.generateDependentTestCases(endpoints);
            List<String> testCaseDescriptions = gpt3Client.generateTestCasesBatch(dependentTestCaseDescriptions);

            // Parallel processing of test case descriptions
            List<TestCase> testCases = processTestCaseDescriptions(testCaseDescriptions, executor);

            // Track successful test case generation
            testCaseGenerationSuccessCounter.increment(testCases.size());

            // Cache the test cases in Redis
            redisClient.setex(cacheKey, 3600, serializeTestCases(testCases)); // Cache for 1 hour

            return CompletableFuture.completedFuture(testCases);
        } catch (Exception e) {
            // Track failure in test case generation
            testCaseGenerationFailureCounter.increment();
            throw e;
        } finally {
            executor.shutdown();
            long end = System.nanoTime();
            fileProcessingTimer.record(end - start, TimeUnit.NANOSECONDS);
        }
    }

    private double getUptime() {
        return (System.currentTimeMillis() - startTime) / 1000.0;
    }

    private List<TestCase> processTestCaseDescriptions(List<String> testCaseDescriptions, ExecutorService executor) {
        return testCaseDescriptions.parallelStream()
                .filter(Objects::nonNull)
                .flatMap(testCaseDescription -> Arrays.stream(testCaseDescription.split("(?=Test Case \\d+:)")))
                .map(testCase -> CompletableFuture.supplyAsync(() -> parseTestCase(testCase), executor))
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private TestCase parseTestCase(String testCaseDescription) {
        if (testCaseDescription == null || testCaseDescription.trim().isEmpty()) {
            logger.warn("Empty or null testCaseDescription passed to parseTestCase.");
            return null;
        }

        TestCase currentTestCase = new TestCase();
        TestCase.TestCaseDetails currentDetails = new TestCase.TestCaseDetails();
        currentTestCase.setTestCaseDetails(Collections.singletonList(currentDetails));

        String[] lines = testCaseDescription.split("\n");
        List<String> steps = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("1. **Description**:")) {
                currentDetails.setDescription(extractContentAfterColon(line));
            } else if (line.startsWith("2. **Steps**:")) {
                continue;
            } else if (line.startsWith("- Step")) {
                steps.add(line.replace("- Step ", "").replace(":", "").trim());
            } else if (line.startsWith("3. **Keywords**:")) {
                currentDetails.setKeywords(Arrays.asList(extractContentAfterColon(line).split(",\\s*")));
            } else if (line.startsWith("4. **Expected Response**:")) {
                currentTestCase.setExpectedResponse(extractContentAfterColon(line));
            } else if (line.startsWith("5. **Endpoint Extension**:")) {
                currentTestCase.setEndpoint(extractContentAfterColon(line));
            } else if (line.startsWith("6. **Method Name**:")) {
                currentTestCase.setMethod(extractContentAfterColon(line));
            } else if (line.startsWith("7. **Parameters**:")) {
                currentTestCase.setParameters(extractContentAfterColon(line));
            } else if (line.startsWith("8. **Categories**:")) {
                currentTestCase.setCategories(Arrays.asList(extractContentAfterColon(line).split(",\\s*")));
            } else if (line.startsWith("9. **Tags**:")) {
                currentTestCase.setTags(Arrays.asList(extractContentAfterColon(line).split(",\\s*")));
            }
        }

        currentDetails.setSteps(steps);

        return validateTestCase(currentTestCase) ? currentTestCase : null;
    }

    private boolean validateTestCase(TestCase testCase) {
        return testCase.getEndpoint() != null && !testCase.getEndpoint().isEmpty()
                && testCase.getMethod() != null && !testCase.getMethod().isEmpty();
    }

    private String extractContentAfterColon(String line) {
        int colonIndex = line.indexOf(":");
        return colonIndex == -1 ? "" : line.substring(colonIndex + 1).trim();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
    }

    private File convertToFile(MultipartFile file) throws IOException {
        File convFile = new File(System.getProperty("java.io.tmpdir") + System.currentTimeMillis() + "-" + file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }

    private String generateCacheKey(String fileName, List<Map.Entry<String, Operation>> endpoints) {
        return fileName + ":" + endpoints.stream()
                .map(entry -> entry.getKey() + "-" + entry.getValue().getOperationId())
                .collect(Collectors.joining(","));
    }

    private String serializeTestCases(List<TestCase> testCases) {
        try {
            return objectMapper.writeValueAsString(testCases);
        } catch (IOException e) {
            logger.error("Failed to serialize test cases", e);
            return null;
        }
    }

    private List<TestCase> deserializeTestCases(String cachedTestCases) {
        try {
            return objectMapper.readValue(cachedTestCases, new TypeReference<List<TestCase>>() {});
        } catch (IOException e) {
            logger.error("Failed to deserialize test cases", e);
            return Collections.emptyList();
        }
    }
}
