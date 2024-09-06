package com.dissertation.openAPITestGeneration.automatioForRestAPIs.models;

import java.util.List;

public class TestCase {
    private String endpoint;
    private String method;
    private String parameters; // Added to capture parameters
    private String expectedResponse; // Added to capture expected response
    private List<TestCaseDetails> testCaseDetails;
    private List<String> categories;
    private List<String> tags;

    public TestCase() {}

    public TestCase(String endpoint, String method, String parameters, String expectedResponse,
                    List<TestCaseDetails> testCaseDetails, List<String> categories, List<String> tags) {
        this.endpoint = endpoint;
        this.method = method;
        this.parameters = parameters;
        this.expectedResponse = expectedResponse;
        this.testCaseDetails = testCaseDetails;
        this.categories = categories;
        this.tags = tags;
    }

    // Getters and setters
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }

    public String getExpectedResponse() { return expectedResponse; }
    public void setExpectedResponse(String expectedResponse) { this.expectedResponse = expectedResponse; }

    public List<TestCaseDetails> getTestCaseDetails() { return testCaseDetails; }
    public void setTestCaseDetails(List<TestCaseDetails> testCaseDetails) { this.testCaseDetails = testCaseDetails; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    // Inner class for TestCaseDetails
    public static class TestCaseDetails {
        private String description;
        private List<String> keywords;
        private List<String> steps ;

        public TestCaseDetails() {}

        public TestCaseDetails(String description, List<String> keywords,List<String> steps) {
            this.description = description;
            this.keywords = keywords;
            this.steps = steps ;
        }

        // Getters and setters
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) { this.keywords = keywords; }

        public void setSteps(List<String> steps)
        {  this.steps = steps ;
        }

        public List<String> getSteps() {
            return steps ;
        }
    }
}
