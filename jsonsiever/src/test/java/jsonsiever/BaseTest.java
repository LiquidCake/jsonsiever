/*
 * Author: https://github.com/LiquidCake
 * MIT License
 */

package jsonsiever;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jsonsiever.core.JsonFilteringService;
import jsonsiever.core.exception.JsonFilteringException;
import org.junit.jupiter.api.Assertions;

import java.nio.charset.StandardCharsets;

import static jsonsiever.util.TestUtils.*;

public abstract class BaseTest {

    protected final JsonFilteringService jsonFilteringService =
            new JsonFilteringService(new ObjectMapper().getFactory());

    protected void testFilterApplicationWithFileMocks(String mockDataFilePath,
                                                      String filterPatternJsonFilePath,
                                                      String assertionJsonFilePath) throws JsonFilteringException {
        String mockData = readClasspathFileAsString(mockDataFilePath);
        String filterPatternJson = readClasspathFileAsString(filterPatternJsonFilePath);

        byte[] filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), stringToJsonNode(filterPatternJson));
        String filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        JsonNode parsedExpected = stringToJsonNode(readClasspathFileAsString(assertionJsonFilePath));
        JsonNode parsedResult = stringToJsonNode(filteredJson);

        Assertions.assertEquals(prettyPrintJsonNode(parsedExpected), prettyPrintJsonNode(parsedResult));
    }
}
