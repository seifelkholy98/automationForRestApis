package com.dissertation.openAPITestGeneration.automatioForRestAPIs.util;

import io.swagger.v3.oas.models.Operation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TestCaseGenerator {

    public List<String> generateDependentTestCases(List<Map.Entry<String, Operation>> endpoints) {
        List<String> testCases = new ArrayList<>();
        Map<String, Map.Entry<String, Operation>> operationMap = endpoints.stream()
                .collect(Collectors.toMap(entry -> entry.getValue().getOperationId(), entry -> entry));

        for (Map.Entry<String, Operation> entry : endpoints) {
            if (isCreateOperation(entry)) {
                String createPrompt = generatePrompt(entry);
                testCases.add(createPrompt);

                List<Map.Entry<String, Operation>> dependentOperations = findDependentOperations(entry, operationMap);
                for (Map.Entry<String, Operation> dependentOperation : dependentOperations) {
                    String dependentDetails = generatePrompt(dependentOperation);
                    String interdependentPrompt = generatePrompt(entry, dependentDetails);
                    testCases.add(interdependentPrompt);
                }
            } else {
                if (isStandaloneOperation(entry, operationMap)) {
                    String standalonePrompt = generatePrompt(entry);
                    testCases.add(standalonePrompt);
                }
            }
        }
        return testCases;
    }

    private boolean isCreateOperation(Map.Entry<String, Operation> entry) {
        return "POST".equalsIgnoreCase(entry.getValue().getOperationId())
                && entry.getValue().getSummary().toLowerCase().contains("create");
    }

    private boolean isStandaloneOperation(Map.Entry<String, Operation> entry, Map<String, Map.Entry<String, Operation>> operationMap) {
        String resourceId = extractResourceId(entry.getKey());

        return operationMap.values().stream()
                .noneMatch(op -> isDependentOperation(op, entry.getValue(), resourceId));
    }

    private List<Map.Entry<String, Operation>> findDependentOperations(Map.Entry<String, Operation> createOperation, Map<String, Map.Entry<String, Operation>> operationMap) {
        List<Map.Entry<String, Operation>> dependentOperations = new ArrayList<>();
        String resourceId = extractResourceId(createOperation.getKey());

        for (Map.Entry<String, Map.Entry<String, Operation>> entry : operationMap.entrySet()) {
            if (isDependentOperation(createOperation, entry.getValue().getValue(), resourceId)) {
                dependentOperations.add(entry.getValue());
            }
        }

        return dependentOperations;
    }

    private boolean isDependentOperation(Map.Entry<String, Operation> createOperation, Operation operation, String resourceId) {
        if (operation == null || resourceId == null) {
            return false;
        }

        String operationPath = createOperation.getKey();
        boolean isDependent = false;

        if (operationPath.contains(resourceId)) {
            String method = operation.getOperationId();
            if ("GET".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
                isDependent = true;
            }
        }
        return isDependent;
    }

    private String extractResourceId(String path) {
        int startIndex = path.lastIndexOf("{");
        int endIndex = path.lastIndexOf("}");
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return path.substring(startIndex, endIndex + 1);
        }
        return null;
    }

    public String generatePrompt(Map.Entry<String, Operation> endpoint) {
        String parameters = formatParameters(endpoint.getValue().getParameters());
        String responses = formatResponses(endpoint.getValue().getResponses());

        return "You are a test case generation assistant. Generate functional test cases for the following API endpoint.\n\n" +
                "Please structure the response according to the following criteria:\n\n" +
                "Test Case (numbering goes here):\n" +
                "1. **Description**: Provide a brief description of the test case.\n" +
                "2. **Steps**: List the steps to be taken in the test case. Each step should be numbered and clearly explained.\n" +
                "   - Step 1: Describe the first action.\n" +
                "   - Step 2: Describe the second action.\n" +
                "   - Step 3: Continue with subsequent steps as needed.\n" +
                "3. **Keywords (assume this is test data)**: Include relevant test data such as input values, expected conditions, or configurations.\n" +
                "   - Example: Email: example@email.com, Password: examplePassword\n" +
                "4. **Expected Response**: Describe the expected outcome if the steps are performed correctly.\n" +
                "   - Example: The operation should be successful.\n" +
                "5. **Endpoint Extension**: Specify the part of the API endpoint that will be used.\n" +
                "6. **Method Name**: Indicate the HTTP method (e.g., GET, POST) used for this API endpoint.\n" +
                "7. **Parameters**: List any parameters required by the endpoint.\n" +
                "8. **Categories**: Specify the category (e.g., Functional, Security, Performance).\n" +
                "9. **Tags**: Include relevant tags that describe the test case.\n\n" +
                "Endpoint: " + endpoint.getKey() + "\n" +
                "Operation ID: " + (endpoint.getValue().getOperationId() != null ? endpoint.getValue().getOperationId() : "None") + "\n" +
                "Summary: " + (endpoint.getValue().getSummary() != null ? endpoint.getValue().getSummary() : "None") + "\n" +
                "Parameters: " + parameters + "\n" +
                "Responses: " + responses + "\n\n" +
                "Please include test cases for the following categories:\n" +
                "1. Functional\n" +
                "2. Security\n" +
                "3. Performance\n\n" +
                "Additionally, tag each test case with relevant keywords. Please Try to ensure that the testcases provided tests the functionality and Provide Thorough steps but are also different and targets different aspects don't generate the same logic for the test cases as before";
    }

    public String generatePrompt(Map.Entry<String, Operation> createOperation, String dependentDetails) {
        String parameters = formatParameters(createOperation.getValue().getParameters());
        String responses = formatResponses(createOperation.getValue().getResponses());

        return "You are a test case generation assistant. Generate functional test cases for the following API endpoint.\n\n" +
                "Please structure the response according to the following criteria:\n\n" +
                "Test Case (numbering goes here):\n" +
                "1. **Description**: Provide a brief description of the test case.\n" +
                "2. **Steps**: List the steps to be taken in the test case. Each step should be numbered and clearly explained.\n" +
                "   - Step 1: Describe the first action.\n" +
                "   - Step 2: Describe the second action.\n" +
                "   - Step 3: Continue with subsequent steps as needed.\n" +
                "3. **Keywords (assume this is test data)**: Include relevant test data such as input values, expected conditions, or configurations.\n" +
                "   - Example: Email: example@email.com, Password: examplePassword\n" +
                "4. **Expected Response**: Describe the expected outcome if the steps are performed correctly.\n" +
                "   - Example: The operation should be successful.\n" +
                "5. **Endpoint Extension**: Specify the part of the API endpoint that will be used.\n" +
                "6. **Method Name**: Indicate the HTTP method (e.g., GET, POST) used for this API endpoint.\n" +
                "7. **Parameters**: List any parameters required by the endpoint.\n" +
                "8. **Categories**: Specify the category (e.g., Functional, Security, Performance).\n" +
                "9. **Tags**: Include relevant tags that describe the test case.\n\n" +
                "Endpoint: " + createOperation.getKey() + "\n" +
                "Operation ID: " + (createOperation.getValue().getOperationId() != null ? createOperation.getValue().getOperationId() : "None") + "\n" +
                "Summary: " + (createOperation.getValue().getSummary() != null ? createOperation.getValue().getSummary() : "None") + "\n" +
                "Parameters: " + parameters + "\n" +
                "Responses: " + responses + "\n\n" +
                "Dependent Operation Details:\n" + dependentDetails + "\n\n" +
                "Please include test cases for the following categories:\n" +
                "1. Functional\n" +
                "2. Security\n" +
                "3. Performance\n\n" +
                "Additionally, tag each test case with relevant keywords.";
    }

    private String formatParameters(List<io.swagger.v3.oas.models.parameters.Parameter> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "None";
        }
        return parameters.stream()
                .map(param -> {
                    String schema = formatSchema(param.getSchema());
                    return String.format("{\"name\": \"%s\", \"in\": \"%s\", \"description\": \"%s\", \"required\": %s, \"schema\": %s}",
                            param.getName(),
                            param.getIn(),
                            param.getDescription() != null ? param.getDescription() : "None",
                            param.getRequired(),
                            schema);
                })
                .collect(Collectors.joining(", "));
    }

    private String formatSchema(io.swagger.v3.oas.models.media.Schema<?> schema) {
        if (schema == null) {
            return "None";
        }
        return String.format("{\"type\": \"%s\", \"format\": \"%s\"}",
                schema.getType(),
                schema.getFormat() != null ? schema.getFormat() : "None");
    }

    private String formatResponses(io.swagger.v3.oas.models.responses.ApiResponses responses) {
        if (responses == null || responses.isEmpty()) {
            return "None";
        }
        return responses.entrySet().stream()
                .map(entry -> {
                    String statusCode = entry.getKey();
                    io.swagger.v3.oas.models.responses.ApiResponse response = entry.getValue();
                    return String.format("{\"%s\": {\"description\": \"%s\", \"content\": \"%s\"}}",
                            statusCode,
                            response.getDescription() != null ? response.getDescription() : "None",
                            response.getContent() != null ? response.getContent().toString() : "None");
                })
                .collect(Collectors.joining(", "));
    }

    public String formatTestCases(List<String> responses) {
        StringBuilder formattedTestCases = new StringBuilder();
        formattedTestCases.append("[\n");
        for (String response : responses) {
            formattedTestCases.append(response).append(",\n");
        }
        if (formattedTestCases.length() > 2) {
            formattedTestCases.setLength(formattedTestCases.length() - 2);
        }
        formattedTestCases.append("\n]");
        return formattedTestCases.toString();
    }
}
