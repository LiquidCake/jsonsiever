/*
 * Author: https://github.com/LiquidCake
 * MIT License
 */

package jsonsiever.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class TestUtils {

    public static final ObjectMapper TEST_OBJECT_MAPPER = new ObjectMapper();
    public static final ObjectWriter PRETTY_PRINTER = TEST_OBJECT_MAPPER.writerWithDefaultPrettyPrinter();

    public static JsonNode stringToJsonNode(String str) {
        try {
            return TEST_OBJECT_MAPPER.readTree(str);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String prettyPrintJsonNode(JsonNode jsonNode) {
        try {
            return PRETTY_PRINTER.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readClasspathFileAsString(String path) {
        try (InputStream inputStream = TestUtils.class.getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new RuntimeException("Failed to open file: " + path);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
