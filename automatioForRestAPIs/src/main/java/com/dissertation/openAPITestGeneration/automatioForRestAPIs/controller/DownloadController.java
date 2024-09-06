package com.dissertation.openAPITestGeneration.automatioForRestAPIs.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
public class DownloadController {

    @GetMapping("/api/download")
    public ResponseEntity<FileSystemResource> downloadFile(@RequestParam("path") String encodedPath) {
        try {
            // Decode the file path to handle special characters and spaces
            String decodedPath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString());

            // Create a file object using the decoded path
            File file = new File(decodedPath);

            // Check if the file exists
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Prepare the file resource for the response
            FileSystemResource resource = new FileSystemResource(file);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());

            // Return the file in the response body with headers set for downloading
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (Exception e) {
            // Handle any errors that occur during file download
            return ResponseEntity.status(500).build(); // Internal server error
        }
    }
}
