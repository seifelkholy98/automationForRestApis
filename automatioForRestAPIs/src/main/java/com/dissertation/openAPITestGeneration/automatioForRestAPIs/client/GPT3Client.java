package com.dissertation.openAPITestGeneration.automatioForRestAPIs.client;

import jakarta.annotation.PostConstruct;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.google.common.util.concurrent.RateLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class GPT3Client {

    @Value("${gpt3.api.key}")
    private String apiKey;

    private RateLimiter rateLimiter;
    private CloseableHttpClient httpClient;
    private final ExecutorService executorService = Executors.newFixedThreadPool(50); // Increased thread pool size

    private Timer gpt3ResponseTime;
    private Counter gpt3ErrorCounter;

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-3.5-turbo";

    private final MeterRegistry meterRegistry;

    public GPT3Client(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    private void init() {
        // Initialize HTTP connection pooling
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200); // Increased max total connections
        connectionManager.setDefaultMaxPerRoute(50); // Increased max connections per route

        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        // Adjust the rate limiter to match your actual API quota
        rateLimiter = RateLimiter.create(50.0 / 60.0); // Adjusted for more requests per minute

        // Initialize metrics
        gpt3ResponseTime = meterRegistry.timer("gpt3.response.time");
        gpt3ErrorCounter = meterRegistry.counter("gpt3.errors");

        // Register thread pool metrics
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;
        Gauge.builder("gpt3.thread.pool.active.count", threadPoolExecutor::getActiveCount)
                .description("Number of active threads in the thread pool")
                .register(meterRegistry);

        Gauge.builder("gpt3.thread.pool.queue.size", () -> threadPoolExecutor.getQueue().size())
                .description("Number of tasks in the queue")
                .register(meterRegistry);

        Gauge.builder("gpt3.thread.pool.completed.count", threadPoolExecutor::getCompletedTaskCount)
                .description("Number of completed tasks in the thread pool")
                .register(meterRegistry);
    }

    public List<String> generateTestCasesBatch(List<String> endpointDescriptions) {
        List<CompletableFuture<String>> futures = endpointDescriptions.stream()
                .map(description -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return generateTestCasesWithRetries(description, 3, 2, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, executorService))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    public String generateTestCases(String endpointDescription) throws Exception {
        rateLimiter.acquire();
        return gpt3ResponseTime.recordCallable(() -> callChatGPT(endpointDescription));
    }

    public String generateTestCasesWithRetries(String endpointDescription, int retries, long backoff, TimeUnit unit) throws Exception {
        for (int i = 0; i < retries; i++) {
            try {
                return generateTestCases(endpointDescription);
            } catch (Exception e) {
                if (e.getMessage().contains("quota") || e.getMessage().contains("Rate limit exceeded")) {
                    throw e; // Stop retrying if it's a quota or rate limit issue
                }
                if (i == retries - 1) {
                    throw e;
                }
                long waitTime = unit.toMillis(backoff);
                System.out.println("Retrying in " + waitTime + " milliseconds...");
                Thread.sleep(waitTime);
                backoff *= 2; // Exponential backoff
            }
        }
        throw new Exception("Failed to get response from GPT-3 after retries");
    }

    private String callChatGPT(String message) throws Exception {
        HttpPost post = new HttpPost(API_URL);
        post.setHeader("Authorization", "Bearer " + apiKey);
        post.setHeader("Content-Type", "application/json");

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", MODEL);
        jsonBody.put("temperature", 0.2); // Low temperature for consistency
        jsonBody.put("top_p", 1.0); // Full focus on the most likely results

        JSONArray messagesArray = new JSONArray();
        JSONObject messageObject = new JSONObject();
        messageObject.put("role", "user");
        messageObject.put("content", message);
        messagesArray.put(messageObject);

        jsonBody.put("messages", messagesArray);
        post.setEntity(new StringEntity(jsonBody.toString(), "UTF-8"));

        // Log the request payload for debugging
        System.out.println("Request JSON: " + jsonBody.toString());

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                return parseGPTResponse(EntityUtils.toString(response.getEntity()));
            } else {
                // Log the full response for debugging
                String responseContent = EntityUtils.toString(response.getEntity());
                System.out.println("Error Response: " + responseContent);
                gpt3ErrorCounter.increment();
                throw new Exception("Error calling GPT-3 API: " + statusCode + " - " + response.getStatusLine().getReasonPhrase() + " - " + responseContent);
            }
        }
    }


    private String parseGPTResponse(String responseBody) {
        JSONObject jsonObject = new JSONObject(responseBody);
        JSONArray choicesArray = jsonObject.getJSONArray("choices");

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < choicesArray.length(); i++) {
            result.append(choicesArray.getJSONObject(i).getJSONObject("message").getString("content"));
        }
        return result.toString().trim();
    }
}
