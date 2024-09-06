package com.dissertation.openAPITestGeneration.automatioForRestAPIs.controller;

import com.dissertation.openAPITestGeneration.automatioForRestAPIs.models.TestCase;
import com.dissertation.openAPITestGeneration.automatioForRestAPIs.service.TestCaseGenerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

@RestController
public class TestCaseController {

    @Autowired
    private TestCaseGenerationService testCaseGenerationService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Generate a list of test cases asynchronously
            Future<List<TestCase>> futureTestCases = testCaseGenerationService.generateTestCases(file);

            // Get the result (blocking until the test cases are generated)
            List<TestCase> testCases = futureTestCases.get();

            // Write test cases to a JSON file for download
            String outputFilePath = "output/test_cases.json";
            writeTestCasesToFile(testCases, outputFilePath);

            // Convert the list of test cases to JSON
            String testCasesJson = objectMapper.writeValueAsString(testCases);

            // Prepare the response
            response.put("message", "File uploaded successfully. Test cases generated.");
            response.put("testCases", testCasesJson);
            response.put("downloadLink", "/api/download?path=" + URLEncoder.encode(outputFilePath, StandardCharsets.UTF_8.toString()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Failed to process file: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private void writeTestCasesToFile(List<TestCase> testCases, String filePath) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), testCases);
    }

    private String encodeValue(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
