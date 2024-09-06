package com.dissertation.openAPITestGeneration.automatioForRestAPIs.util;

import com.dissertation.openAPITestGeneration.automatioForRestAPIs.models.TestCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Component
public class FileWriterUtil {

    /**
     * Writes a list of TestCase objects to a JSON file.
     *
     * @param testCases the list of test cases to write
     * @param filePath the file path where the JSON file should be written
     * @throws IOException if there is an issue writing to the file
     */
    public void writeJsonToFile(List<TestCase> testCases, String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.writeValue(new File(filePath), testCases);
    }
}
