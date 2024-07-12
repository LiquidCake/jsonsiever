/*
 * Author: https://github.com/LiquidCake
 * MIT License
 */

package jsonsiever;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jsonsiever.core.exception.JsonFilteringException;
import jsonsiever.util.TestModel;
import jsonsiever.util.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static jsonsiever.core.JsonFilteringService.FILTER_PATTERN_INCLUDE_ALL;

public class BasicTest extends BaseTest {

    /**
     * Check that json with just basic value (string, int, empty string, null value) is processed correctly
     */
    @Test
    public void test_rootBasicType() throws Exception {
        //CASE: string
        String mockData = "\"just string\"";
        JsonNode filterPatternJson = new TextNode(FILTER_PATTERN_INCLUDE_ALL);

        byte[] filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), filterPatternJson);
        String filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        JsonNode parsedExpected = TestUtils.stringToJsonNode(mockData);
        JsonNode parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(TestUtils.prettyPrintJsonNode(parsedExpected), TestUtils.prettyPrintJsonNode(parsedResult));

        //CASE: int
        mockData = "123";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), filterPatternJson);
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode(mockData);
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(TestUtils.prettyPrintJsonNode(parsedExpected), TestUtils.prettyPrintJsonNode(parsedResult));

        //CASE: empty string
        mockData = " ";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), filterPatternJson);
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode(mockData);
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(TestUtils.prettyPrintJsonNode(parsedExpected), TestUtils.prettyPrintJsonNode(parsedResult));

        //CASE: null value
        mockData = "null";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), filterPatternJson);
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode(mockData);
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(TestUtils.prettyPrintJsonNode(parsedExpected), TestUtils.prettyPrintJsonNode(parsedResult));
    }

    /**
     * Json data or filter pattern is null
     */
    @Test
    public void test_nullArguments() throws Exception {
        try {
            jsonFilteringService.filterJsonFields(null, TestUtils.stringToJsonNode("{}"));
            Assertions.fail("should throw");
        } catch (Exception e) {
            Assertions.assertInstanceOf(IllegalArgumentException.class, e);
        }

        String mockData = "{\"fInt\": 55}";

        byte[] filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), null);
        String filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);
        Assertions.assertEquals(mockData, filteredJson);
    }

    /**
     * Include all and empty filters
     */
    @Test
    public void test_includeAllAndEmptyFilters() throws Exception {
        String mockData = "{\"fInt\": 55}";
        byte[] filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TextNode.valueOf(FILTER_PATTERN_INCLUDE_ALL));
        String filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);
        Assertions.assertEquals(mockData, filteredJson);

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode("{}"));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);
        Assertions.assertEquals(mockData, filteredJson);

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode("[]"));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);
        Assertions.assertEquals(mockData, filteredJson);
    }

    /**
     * Check that array in json root is processed correctly
     */
    @Test
    public void test_rootArray() throws Exception {
        String mockData = TestUtils.readClasspathFileAsString("/mock/data/BasicTest/test_rootArray.json");
        String filterPatternJson = "[]";

        byte[] filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        String filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        JsonNode parsedExpected = TestUtils.stringToJsonNode(mockData);
        JsonNode parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(TestUtils.prettyPrintJsonNode(parsedExpected), TestUtils.prettyPrintJsonNode(parsedResult));
    }

    /**
     * Filter out some object while keeping another one
     */
    @Test
    public void test_siblingObjectSkipped() throws Exception {
        testFilterApplicationWithFileMocks(
                "/mock/data/BasicTest/test_siblingObjectSkipped.json",
                "/mock/patterns/BasicTest/test_siblingObjectSkipped.json",
                "/mock/assertions/BasicTest/test_siblingObjectSkipped.json"
        );
    }

    /**
     * Filter out some object while returning its sibling with wildcard
     */
    @Test
    public void test_siblingObjectFiltered() throws Exception {
        testFilterApplicationWithFileMocks(
                "/mock/data/BasicTest/test_siblingObjectFiltered.json",
                "/mock/patterns/BasicTest/test_siblingObjectFiltered.json",
                "/mock/assertions/BasicTest/test_siblingObjectFiltered.json"
        );
    }

    /**
     * Filter out some array while keeping another one
     */
    @Test
    public void test_siblingArraySkipped() throws Exception {
        testFilterApplicationWithFileMocks(
                "/mock/data/BasicTest/test_siblingArraySkipped.json",
                "/mock/patterns/BasicTest/test_siblingArraySkipped.json",
                "/mock/assertions/BasicTest/test_siblingArraySkipped.json"
        );
    }

    /**
     * Check all object fields are filtered out
     */
    @Test
    public void test_allObjectFieldsAreFilteredOut() throws Exception {
        testFilterApplicationWithFileMocks(
                "/mock/data/BasicTest/test_allObjectFieldsAreFilteredOut.json",
                "/mock/patterns/BasicTest/test_allObjectFieldsAreFilteredOut.json",
                "/mock/assertions/BasicTest/test_allObjectFieldsAreFilteredOut.json"
        );
    }

    /**
     * If filter pattern for object element of array is passed as wildcard -
     * result is the same as whole array was filtered with wildcard
     */
    @Test
    public void test_arrayOfObjectsWithElementWildcardFilter() throws Exception {
        testFilterApplicationWithFileMocks(
                "/mock/data/BasicTest/test_arrayOfObjectsWithElementWildcardFilter.json",
                "/mock/patterns/BasicTest/test_arrayOfObjectsWithElementWildcardFilter.json",
                "/mock/assertions/BasicTest/test_arrayOfObjectsWithElementWildcardFilter.json"
        );
    }

    /**
     * If filter pattern for nested array element of array is passed as wildcard -
     * result is the same as whole array was filtered with wildcard
     */
    @Test
    public void test_arrayOfArraysWithElementWildcardFilter() throws Exception {
        testFilterApplicationWithFileMocks(
                "/mock/data/BasicTest/test_arrayOfArraysWithElementWildcardFilter.json",
                "/mock/patterns/BasicTest/test_arrayOfArraysWithElementWildcardFilter.json",
                "/mock/assertions/BasicTest/test_arrayOfArraysWithElementWildcardFilter.json"
        );
    }

    /**
     * Passing "array of objects" filter for array of primitives - should throw
     */
    @Test
    public void test_badFilterForArrayOfPrimitives() throws Exception {
        String mockData = TestUtils.readClasspathFileAsString("/mock/data/BasicTest/test_badFilterForArrayOfPrimitives.json");
        String filterPatternJson = TestUtils.readClasspathFileAsString("/mock/patterns/BasicTest/test_badFilterForArrayOfPrimitives.json");

        try {
            jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
            Assertions.fail("should throw");
        } catch (JsonFilteringException e) {
            Assertions.assertTrue(
                    e.getMessage().contains("Bad filter pattern node for array of primitives")
            );
        }
    }

    /**
     * Type of filter node must be validated to correspond to type of json data node
     */
    @Test
    public void test_typesCheck() throws Exception {
        //CASE: correct pattern
        String mockData = TestUtils.readClasspathFileAsString("/mock/data/BasicTest/test_typesCheck.json");
        String filterPatternJson = TestUtils.readClasspathFileAsString("/mock/patterns/BasicTest/test_typesCheck_correct_pattern.json");

        //no exception thrown
        jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));

        //CASE: bad object pattern
        filterPatternJson = TestUtils.readClasspathFileAsString("/mock/patterns/BasicTest/test_typesCheck_bad_object_pattern.json");

        try {
            jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
            Assertions.fail("should throw");
        } catch (JsonFilteringException e) {
            Assertions.assertTrue(
                    e.getMessage().contains("Filter pattern node is not of type OBJECT")
            );
        }

        //CASE: bad array pattern
        filterPatternJson = TestUtils.readClasspathFileAsString("/mock/patterns/BasicTest/test_typesCheck_bad_array_pattern.json");

        try {
            jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
            Assertions.fail("should throw");
        } catch (JsonFilteringException e) {
            Assertions.assertTrue(
                    e.getMessage().contains("Filter pattern node is not of type ARRAY")
            );
        }
    }

    /**
     * Type of filter node for array element must be validated to correspond to type of json data array element
     */
    @Test
    public void test_arrayElementTypesCheck() throws Exception {
        //CASE: correct pattern
        String mockData = TestUtils.readClasspathFileAsString("/mock/data/BasicTest/test_arrayElementTypesCheck.json");
        String filterPatternJson = TestUtils.readClasspathFileAsString("/mock/patterns/BasicTest/test_arrayElementTypesCheck_correct_pattern.json");

        //no exception thrown
        jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));

        //CASE: bad object element pattern
        filterPatternJson = TestUtils.readClasspathFileAsString("/mock/patterns/BasicTest/test_arrayElementTypesCheck_bad_object_element_pattern.json");

        try {
            jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
            Assertions.fail("should throw");
        } catch (JsonFilteringException e) {
            Assertions.assertTrue(
                    e.getMessage().contains("Filter pattern node is not of type OBJECT")
            );
        }

        //CASE: bad nested array pattern
        filterPatternJson = TestUtils.readClasspathFileAsString("/mock/patterns/BasicTest/test_arrayElementTypesCheck_bad_nested_array_element_pattern.json");

        try {
            jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
            Assertions.fail("should throw");
        } catch (JsonFilteringException e) {
            Assertions.assertTrue(
                    e.getMessage().contains("Filter pattern node is not of type ARRAY")
            );
        }
    }

    /**
     * All array elements must be of single type, this must be validated
     */
    @Test
    public void test_arrayElementsOfDifferentType() throws Exception {
        //CASE: array contains only objects
        String mockData = TestUtils.readClasspathFileAsString("/mock/data/BasicTest/test_arrayElementsOfDifferentType_all_object_elements.json");
        String filterPatternJson = TestUtils.readClasspathFileAsString("/mock/patterns/BasicTest/test_arrayElementsOfDifferentType.json");

        //no exception thrown
        jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));

        //CASE: array contains object and primitive elements
        mockData = TestUtils.readClasspathFileAsString("/mock/data/BasicTest/test_arrayElementsOfDifferentType_object_and_primitive.json");
        filterPatternJson = TestUtils.readClasspathFileAsString("/mock/patterns/BasicTest/test_arrayElementsOfDifferentType_object_and_primitive.json");

        try {
            jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
            Assertions.fail("should throw");
        } catch (JsonFilteringException e) {
            Assertions.assertTrue(
                    e.getMessage().contains("Array must contain elements of a single type")
            );
        }

        //CASE: array contains object and array elements
        mockData = TestUtils.readClasspathFileAsString("/mock/data/BasicTest/test_arrayElementsOfDifferentType_object_and_array.json");

        try {
            jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
            Assertions.fail("should throw");
        } catch (JsonFilteringException e) {
            Assertions.assertTrue(
                    e.getMessage().contains("Array must contain elements of a single type")
            );
        }
    }

    /**
     * Array of elements with different types is allowed in case of wildcard filtering
     */
    @Test
    public void test_arrayElementsOfDifferentType_wildcardFilter() throws Exception {
        testFilterApplicationWithFileMocks(
                "/mock/data/BasicTest/test_arrayElementsOfDifferentType_wildcardFilter.json",
                "/mock/patterns/BasicTest/test_arrayElementsOfDifferentType_wildcardFilter.json",
                //same as mock data
                "/mock/data/BasicTest/test_arrayElementsOfDifferentType_wildcardFilter.json"
        );
    }

    /**
     * Array may contain null elements
     */
    @Test
    public void test_arrayContainsNullElements() throws Exception {
        //CASE: array of nulls with wildcard filter
        String mockData = "{\"fArray\": [null, null]}";
        String filterPatternJson = "{\"fArray\": []}";

        byte[] filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        String filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        JsonNode parsedExpected = TestUtils.stringToJsonNode(mockData);
        JsonNode parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(TestUtils.prettyPrintJsonNode(parsedExpected), TestUtils.prettyPrintJsonNode(parsedResult));

        //CASE: array of nulls with object element filter
        mockData = "{\"fArray\": [null, null]}";
        filterPatternJson = "{\"fArray\": [{\"fInt\": 1}]}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode(mockData);
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(TestUtils.prettyPrintJsonNode(parsedExpected), TestUtils.prettyPrintJsonNode(parsedResult));

        //CASE: array of nulls with nested array element filter
        mockData = "{\"fArray\": [null, null]}";
        filterPatternJson = "{\"fArray\": [[{\"fInt\": 1}]]}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode(mockData);
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(TestUtils.prettyPrintJsonNode(parsedExpected), TestUtils.prettyPrintJsonNode(parsedResult));

        //CASE: array of primitives
        mockData = "{\"fArray\": [null, 1]}";
        filterPatternJson = "{\"fArray\": []}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode(mockData);
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(TestUtils.prettyPrintJsonNode(parsedExpected), TestUtils.prettyPrintJsonNode(parsedResult));

        //CASE: array of primitives (reverse)
        mockData = "{\"fArray\": [1, null]}";
        filterPatternJson = "{\"fArray\": []}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode(mockData);
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(TestUtils.prettyPrintJsonNode(parsedExpected), TestUtils.prettyPrintJsonNode(parsedResult));

        //CASE: array of objects
        mockData = "{\"fArray\": [null, {\"fInt\": 55}]}";
        filterPatternJson = "{\"fArray\": [{\"fInt\": 1}]}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode(mockData);
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(TestUtils.prettyPrintJsonNode(parsedExpected), TestUtils.prettyPrintJsonNode(parsedResult));

        //CASE: array of objects (reverse)
        mockData = "{\"fArray\": [{\"fInt\": 55}, null]}";
        filterPatternJson = "{\"fArray\": [{\"fInt\": 1}]}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode(mockData);
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(TestUtils.prettyPrintJsonNode(parsedExpected), TestUtils.prettyPrintJsonNode(parsedResult));

        //CASE: array of arrays
        mockData = "{\"fArray\": [null, [1,2]]}";
        filterPatternJson = "{\"fArray\": []}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode(mockData);
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(TestUtils.prettyPrintJsonNode(parsedExpected), TestUtils.prettyPrintJsonNode(parsedResult));

        //CASE: array of arrays (reverse)
        mockData = "{\"fArray\": [[1,2], null]}";
        filterPatternJson = "{\"fArray\": []}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode(mockData);
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(TestUtils.prettyPrintJsonNode(parsedExpected), TestUtils.prettyPrintJsonNode(parsedResult));
    }

    /**
     * Check filtering for serialized POJO with JsonNode field
     */
    @Test
    public void test_serializedPojoWithJsonNodeField() throws Exception {
        TestModel modelWithJsonNodeField = new TestModel("someString", TestUtils.stringToJsonNode("{\"fInt\": 55}"));
        String mockData = TestUtils.TEST_OBJECT_MAPPER.writeValueAsString(modelWithJsonNodeField);
        String filterPatternJson = "{\"fString\": 1, \"fJsonNode\": {\"fInt\": 1}}";

        byte[] filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        String filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        JsonNode parsedExpected = TestUtils.stringToJsonNode(mockData);
        JsonNode parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(TestUtils.prettyPrintJsonNode(parsedExpected), TestUtils.prettyPrintJsonNode(parsedResult));
    }

    /**
     * Check that value, object and array field with null value is processed correctly
     */
    @Test
    public void test_filteredFieldIsNull() throws Exception {
        //CASE: object field - wildcard
        String mockData = "{\"fString\": null, \"fObject\": null}";
        String filterPatternJson = "{\"fString\": 1, \"fObject\": {}}";

        byte[] filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        String filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        JsonNode parsedExpected = TestUtils.stringToJsonNode(mockData);
        JsonNode parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(TestUtils.prettyPrintJsonNode(parsedExpected), TestUtils.prettyPrintJsonNode(parsedResult));

        //CASE: specific object fields filter
        mockData = "{\"fString\": null, \"fObject\": null}";
        filterPatternJson = "{\"fString\": 1, \"fObject\": {\"fInt\": 1}}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode(mockData);
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(TestUtils.prettyPrintJsonNode(parsedExpected), TestUtils.prettyPrintJsonNode(parsedResult));

        //CASE: array field - wildcard
        mockData = "{\"fArray\": null}";
        filterPatternJson = "{\"fArray\": []}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode(mockData);
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(TestUtils.prettyPrintJsonNode(parsedExpected), TestUtils.prettyPrintJsonNode(parsedResult));

        //CASE: specific array element filter
        mockData = "{\"fArray\": null}";
        filterPatternJson = "{\"fArray\": [{\"fInt\": 1}]}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode(mockData);
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(TestUtils.prettyPrintJsonNode(parsedExpected), TestUtils.prettyPrintJsonNode(parsedResult));
    }

    /**
     * (THIS IS INVALID JSON SO BEHAVIOUR IS NOT TO BE GUARANTIED)
     * <p>
     * NO PRETTY PRINTER IS USED FOR ASSERTIONS TO AVOID ITS INFLUENCE
     * <p>
     * Duplicated field names should be filtered in a normal way:
     * primitive fields are removed or returned
     * object fields are removed or filtered using same filter pattern object
     * array fields are removed or filtered with wildcard using same filter pattern array
     */
    @Test
    public void test_duplicateFieldName() throws Exception {
        //CASE: primitive fields returned as is
        String mockData = "{\"fInt\": 55, \"fInt\": 66, \"fBoolean\": true}";
        String filterPatternJson = "{\"fInt\": 1, \"fInt\": 1}";

        byte[] filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        String filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        JsonNode parsedExpected = TestUtils.stringToJsonNode("{\"fInt\": 55, \"fInt\": 66}");
        JsonNode parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(parsedExpected, parsedResult);

        //CASE: primitive fields removed
        mockData = "{\"fInt\": 55, \"fInt\": 66, \"fBoolean\": true}";
        filterPatternJson = "{\"fBoolean\": 1}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode("{\"fBoolean\": true}");
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(parsedExpected, parsedResult);

        //CASE: object field filtered correctly
        mockData = "{\"fObject\": {\"fInt\": 55}, \"fObject\": {\"fInt\": 66}, \"fBoolean\": true}";
        filterPatternJson = "{\"fObject\": {\"fInt\": 1}}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode("{\"fObject\": {\"fInt\": 55}, \"fObject\": {\"fInt\": 66}}");
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(parsedExpected, parsedResult);

        //CASE: object field filtered correctly - different objects
        mockData = "{\"fObject\": {\"fInt\": 55}, \"fObject\": {\"fIntXXX\": 66}, \"fBoolean\": true}";
        filterPatternJson = "{\"fObject\": {\"fInt\": 1}}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode("{\"fObject\": {\"fInt\": 55}, \"fObject\": {}}");
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(parsedExpected, parsedResult);

        //CASE: object field filtered correctly - wildcard
        mockData = "{\"fObject\": {\"fInt\": 55}, \"fObject\": {\"fInt\": 66}, \"fBoolean\": true}";
        filterPatternJson = "{\"fObject\": {}}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode("{\"fObject\": {\"fInt\": 55}, \"fObject\": {\"fInt\": 66}}");
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(parsedExpected, parsedResult);

        //CASE: object fields removed
        mockData = "{\"fObject\": {\"fInt\": 55}, \"fObject\": {\"fIntXXX\": 66}, \"fBoolean\": true}";
        filterPatternJson = "{\"fBoolean\": 1}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode("{\"fBoolean\": true}");
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(parsedExpected, parsedResult);

        //CASE: array fields (objects) filtered correctly - wildcard
        mockData = "{\"fArray\": [1,2], \"fArray\": [{\"fInt\": 55}], \"fBoolean\": true}";
        filterPatternJson = "{\"fArray\": []}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode("{\"fArray\": [1,2], \"fArray\": [{\"fInt\": 55}]}");
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(parsedExpected, parsedResult);

        //CASE: array fields (arrays) filtered correctly - wildcard
        mockData = "{\"fArray\": [[1,2]], \"fArray\": [[3,4]], \"fBoolean\": true}";
        filterPatternJson = "{\"fArray\": []}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode("{\"fArray\": [[1,2]], \"fArray\": [[3,4]]}");
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(parsedExpected, parsedResult);

        //CASE: array fields (arrays of different types) filtered correctly - wildcard
        mockData = "{\"fArray\": [[{\"fInt\": 55}]], \"fBoolean\": true, \"fArray\": [[1,2]]}";
        filterPatternJson = "{\"fArray\": []}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode("{\"fArray\": [[{\"fInt\": 55}]], \"fArray\": [[1,2]]}");
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(parsedExpected, parsedResult);

        //CASE: array removed
        mockData = "{\"fArray\": [1,2], \"fArray\": [{\"fInt\": 55}], \"fBoolean\": true}";
        filterPatternJson = "{\"fBoolean\": 1}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode("{\"fBoolean\": true}");
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(parsedExpected, parsedResult);

        //CASE: array of objects - is correctly filtered using object pattern
        mockData = "{\"fArray\": [{\"fInt\": 55}], \"fArray\": [{\"fIntXXX\": 66}], \"fBoolean\": true}";
        filterPatternJson = "{\"fArray\": [{\"fInt\": 1}]}";

        filteredJsonBytes = jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
        filteredJson = new String(filteredJsonBytes, StandardCharsets.UTF_8);

        parsedExpected = TestUtils.stringToJsonNode("{\"fArray\": [{\"fInt\": 55}], \"fArray\": [{}]}");
        parsedResult = TestUtils.stringToJsonNode(filteredJson);

        Assertions.assertEquals(parsedExpected, parsedResult);

        //CASE: bad filter for array of primitives
        mockData = "{\"fArray\": [{\"fInt\": 55}], \"fArray\": [1,2], \"fBoolean\": true}";
        filterPatternJson = "{\"fArray\": [{\"fInt\": 1}]}";

        try {
            jsonFilteringService.filterJsonFields(mockData.getBytes(), TestUtils.stringToJsonNode(filterPatternJson));
            Assertions.fail("should throw");
        } catch (JsonFilteringException e) {
            Assertions.assertTrue(
                    e.getMessage().contains("Bad filter pattern node for array of primitives")
            );
        }
    }
}
