package com.dissertation.openAPITestGeneration.automatioForRestAPIs.controller;

import com.dissertation.openAPITestGeneration.automatioForRestAPIs.models.TestCase;
import com.dissertation.openAPITestGeneration.automatioForRestAPIs.service.TestCaseGenerationService;
import com.dissertation.openAPITestGeneration.automatioForRestAPIs.util.FileWriterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.Future;

@RestController
public class FileUploadController {

    @Autowired
    private TestCaseGenerationService testCaseGenerationService;

    @Autowired
    private FileWriterUtil fileWriterUtil;

    @PostMapping("/api/upload")
    public ResponseEntity<FileSystemResource> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Generate a list of test cases
            Future<List<TestCase>> futureTestCases = testCaseGenerationService.generateTestCases(file);
            List<TestCase> testCases = futureTestCases.get();
            // Define the output file path
            String outputFilePath = "output/test_cases.json"; // Update with actual path if needed

            // Write test cases to a JSON file using FileWriterUtil
            fileWriterUtil.writeJsonToFile(testCases, outputFilePath);

            // Prepare the response with the downloadable file
            FileSystemResource resource = new FileSystemResource(outputFilePath);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=test_cases.json");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (Exception e) {
            // Return a bad request response in case of errors
            return ResponseEntity.badRequest().build();
        }
    }
}
