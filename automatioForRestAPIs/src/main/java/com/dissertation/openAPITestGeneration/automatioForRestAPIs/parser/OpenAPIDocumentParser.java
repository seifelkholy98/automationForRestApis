package com.dissertation.openAPITestGeneration.automatioForRestAPIs.parser;

import io.swagger.parser.OpenAPIParser;
import io.swagger.parser.SwaggerParser;
import io.swagger.v3.parser.converter.SwaggerConverter;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OpenAPIDocumentParser {
    private static final Logger logger = LoggerFactory.getLogger(OpenAPIDocumentParser.class);

    /**
     * Parses the OpenAPI document from the given file path.
     *
     * @param filePath the path to the OpenAPI document file
     * @return the parsed OpenAPI object
     */
    public OpenAPI parseDocument(String filePath) {
        logger.info("Parsing OpenAPI document from file: {}", filePath);

        SwaggerParser swaggerParser = new SwaggerParser();
        io.swagger.models.Swagger swagger = swaggerParser.read(filePath);
        if (swagger != null) {
            logger.info("Swagger 2.0 document detected, converting to OpenAPI 3.0");
            SwaggerConverter converter = new SwaggerConverter();
            SwaggerParseResult result = converter.readLocation(filePath, null, null);
            if (result.getMessages() != null && !result.getMessages().isEmpty()) {
                logger.warn("Warnings while parsing OpenAPI document: {}", result.getMessages());
            }
            return result.getOpenAPI();
        } else {
            OpenAPIParser openAPIParser = new OpenAPIParser();
            SwaggerParseResult result = openAPIParser.readLocation(filePath, null, null);
            if (result.getMessages() != null && !result.getMessages().isEmpty()) {
                logger.warn("Warnings while parsing OpenAPI document: {}", result.getMessages());
            }
            return result.getOpenAPI();
        }
    }

    /**
     * Extracts endpoints from the OpenAPI object.
     *
     * @param openAPI the parsed OpenAPI object
     * @return a list of endpoints
     */
    public List<Map.Entry<String, Operation>> extractEndpoints(OpenAPI openAPI) {
        List<Map.Entry<String, Operation>> endpoints = new ArrayList<>();
        for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
            String path = entry.getKey();
            PathItem pathItem = entry.getValue();
            if (pathItem.getGet() != null) {
                endpoints.add(Map.entry(path, pathItem.getGet()));
            }
            if (pathItem.getPost() != null) {
                endpoints.add(Map.entry(path, pathItem.getPost()));
            }
            if (pathItem.getPut() != null) {
                endpoints.add(Map.entry(path, pathItem.getPut()));
            }
            if (pathItem.getDelete() != null) {
                endpoints.add(Map.entry(path, pathItem.getDelete()));
            }
        }
        return endpoints;
    }
}