package com.dissertation.openAPITestGeneration.automatioForRestAPIs.controller;

import com.dissertation.openAPITestGeneration.automatioForRestAPIs.models.ApiRequest;
import com.dissertation.openAPITestGeneration.automatioForRestAPIs.service.ApiExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/executor")
public class ApiExecutorController {

    @Autowired
    private ApiExecutorService apiExecutorService;

    @PostMapping("/execute")
    public ResponseEntity<String> executeApi(@RequestBody ApiRequest apiRequest) {
        String url = apiRequest.getBaseUrl() + apiRequest.getEndpoint();

        ResponseEntity<String> response = apiExecutorService.executeApi(
                url,
                apiRequest.getMethod(),
                apiRequest.getHeaders(),
                apiRequest.getBody()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        return ResponseEntity.status(response.getStatusCode())
                .headers(headers)
                .body(response.getBody());
    }
}
