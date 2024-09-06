package com.dissertation.openAPITestGeneration.automatioForRestAPIs.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ApiExecutorService {

    private final RestTemplate restTemplate = new RestTemplate();

    public ResponseEntity<String> executeApi(String url, String method, Map<String, String> headers, String body) {
        // Ensure the URL is absolute by checking and adding scheme if missing
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;  // Default to http; change to https if needed
        }

        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach(httpHeaders::set);

        HttpEntity<String> requestEntity = new HttpEntity<>(body, httpHeaders);

        HttpMethod httpMethod = getHttpMethod(method);
        if (httpMethod == null) {
            throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        return restTemplate.exchange(url, httpMethod, requestEntity, String.class);
    }

    private HttpMethod getHttpMethod(String method) {
        switch (method.toUpperCase()) {
            case "GET":
                return HttpMethod.GET;
            case "POST":
                return HttpMethod.POST;
            case "PUT":
                return HttpMethod.PUT;
            case "DELETE":
                return HttpMethod.DELETE;
            case "PATCH":
                return HttpMethod.PATCH;
            case "OPTIONS":
                return HttpMethod.OPTIONS;
            case "HEAD":
                return HttpMethod.HEAD;
            default:
                return null; // Unsupported HTTP method
        }
    }
}
