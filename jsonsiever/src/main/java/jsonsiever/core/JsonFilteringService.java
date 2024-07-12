/*
 * Author: https://github.com/LiquidCake
 * MIT License
 */

package jsonsiever.core;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.TextNode;
import jsonsiever.core.exception.BadFilterPatternException;
import jsonsiever.core.exception.JsonFilteringException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

/**
 * This service performs filtering of json data (removes particular json fields) according to specified filter pattern.
 * User should call method {@link #filterJsonFields(byte[], JsonNode) filterJsonFields()} and pass
 * to-be-filtered json data (string as byte array), as well as filter pattern json (string parsed as JsonNode).
 * <p>
 * <h2>Filter pattern</h2>
 * <p>
 * If special "include all" filter "*" is passed as a filter pattern - whole json data will be just returned as is.
 * Otherwise, filter pattern must be a json with a structure similar to passed json data. The only difference is that
 * filter pattern json may not have real values set and instead may have e.g. dummy integer value '1' set to all its fields,
 * except for object and array fields (see below).
 * <br>Fields that are present in json data but are missing from filter pattern will be filtered out.
 * <p>
 * <h3>Object fields</h3>
 * <p>
 * Value of any 'object' field (or global object) inside filter pattern may be set to "wildcard" value equal
 * to empty object ("{}"). In this case content of corresponding json data object won't be filtered, and it will be returned as is.
 * <p>Otherwise, if object inside filter pattern is not empty - corresponding object inside json data will be filtered accordingly:
 * <br>result will include only fields that are listed inside filter pattern object.
 * <p>
 * <h3>Array fields</h3>
 * <p><b>NOTE: non-wildcard filtering is supported only for homogenous arrays: all items inside array
 * (which will be objects or nested arrays) must be of exactly same type (structure).</b>
 * <p>
 * Value of any 'array' field (or global array) inside filter pattern may be set to "wildcard" value equal
 * to empty array ("[]"). In this case content of corresponding json data array won't be filtered, and will be returned as is.
 * <br>
 * <i>For array of primitives - only "wildcard" filter may be used</i>
 * <p>
 * Otherwise, if array inside filter pattern is not empty (used only for array of objects or array of arrays) -
 * corresponding array inside json data will be filtered accordingly:
 * <br>First of all, array inside filter pattern must always contain only single element.
 * This element will be used as a filter pattern for all (!) elements of array.
 * Since we work with array of objects or array of arrays - this element pattern may be an object or an array itself.
 * <p>
 * <h2>Examples:</h2>
 * <pre>
 * Remove fields from object
 * jsonData = {"name": "Kek", "age": 99, "address": {"city": "London", "street": "Baker st."}}
 * filterPattern = {"name": 1}
 * result = {"name": "Kek"}
 * <br>
 * Use wildcard filter for nested object
 * jsonData = {"name": "Kek", "age": 99, "address": {"city": "London", "street": "Baker st."}}
 * filterPattern = {"name": 1, "address": {}}
 * result = {"name": "Kek", "address": {"city": "London", "street": "Baker st."}}
 * <br>
 * Use specific filter for nested object
 * jsonData = {"name": "Kek", "age": 99, "address": {"city": "London", "street": "Baker st."}}
 * filterPattern = {"name": 1, "address": {"street": 1}}
 * result = {"name": "Kek", "address": {"street": "Baker st."}}
 * <br>
 * Use wildcard filter for array (of any type)
 * jsonData = {"shelfNum": 55, "books": [{"name": "LOTR", "genre": "fantasy"}, {"name": "Cats", "genre": "reality"}]}
 * filterPattern = {"books": []}
 * result = {"books": [{"name": "LOTR", "genre": "fantasy"}, {"name": "Cats", "genre": "reality"}]}
 * <br>
 * Use specific filter for array (of objects)
 * jsonData = {"shelfNum": 55, "books": [{"name": "LOTR", "genre": "fantasy"}, {"name": "Cats", "genre": "reality"}]}
 * filterPattern = {"books": [{"name": 1}]}
 * result = {"books": [{"name": "LOTR"}, {"name": "Cats"}]}
 * <br>
 * Use specific filter for array (of arrays)
 * jsonData = {"shelfBlocks": [
 * [{"shelfNum": 55, "code": "A"}, {"shelfNum": 56, "code": "A"}],
 * [{"shelfNum": 57, "code": "A"}, {"shelfNum": 58, "code": "A"}]
 * ]}
 * filterPattern = {"shelfBlocks": [[{"shelfNum": 1}]]}
 * result = {"shelfBlocks": [[{"shelfNum": 55}, {"shelfNum": 56}], [{"shelfNum": 57}, {"shelfNum": 58}]]}
 * </pre>
 */
public class JsonFilteringService {
    private final Logger log = LoggerFactory.getLogger(JsonFilteringService.class);

    public static final String FILTER_PATTERN_INCLUDE_ALL = "*";

    private static final JsonNode ABSENT_NODE = new TextNode("JSONSIEVER_ABSENT_NODE");
    private static final JsonNode WILDCARD_NODE = new TextNode("JSONSIEVER_WILDCARD_NODE");
    private static final Set<JsonToken> UNSUPPORTED_JSON_TOKENS = Set.of(
            JsonToken.VALUE_EMBEDDED_OBJECT
    );

    private final JsonFactory jsonFactory;

    public JsonFilteringService(JsonFactory jsonFactory) {
        this.jsonFactory = jsonFactory;
    }

    /**
     * Method filters passed json data (removes particular json fields) according to passed filter pattern
     *
     * @param jsonData                  valid json (as bytes array) to be filtered
     * @param filterPatternJsonRootNode valid json (json parsed to JsonNode tree) with fields structure that correspond
     *                                  to passed json data (but probably without real values set).
     *                                  <p>Special cases:
     *                                  <ul>
     *                                  <li>"include all" special filter "*" - json data is not filtered and returned as is</li>
     *                                  <li>global "wildcard" value (empty object "{}" or empty array "[]") - json data is not filtered and returned as is</li>
     *                                  </ul>
     * @return valid json (as bytes array) that may be equal to passed json data (if nothing is filtered), or is similar but
     * with some fields removed based on filter pattern
     */
    public byte[] filterJsonFields(byte[] jsonData, JsonNode filterPatternJsonRootNode) throws JsonFilteringException {
        if (jsonData == null) {
            throw new IllegalArgumentException("Json data bytes array is null");
        }

        if (jsonData.length == 0 || filterPatternJsonRootNode == null) {
            return jsonData;
        }

        boolean filterPatternIsEmptyString = filterPatternJsonRootNode.isTextual()
                && filterPatternJsonRootNode.isEmpty();
        boolean filterPatternIsIncludeAllString = filterPatternJsonRootNode.isTextual()
                && FILTER_PATTERN_INCLUDE_ALL.equals(filterPatternJsonRootNode.asText());
        boolean filterPatternIsEmptyContainer =
                (filterPatternJsonRootNode.isObject() || filterPatternJsonRootNode.isArray())
                        && filterPatternJsonRootNode.isEmpty();

        if (filterPatternIsEmptyString || filterPatternIsIncludeAllString || filterPatternIsEmptyContainer) {
            return jsonData;
        }

        return doFilterJsonFields(jsonData, filterPatternJsonRootNode);
    }

    private byte[] doFilterJsonFields(byte[] jsonData, JsonNode filterPatternJsonRootNode)
            throws JsonFilteringException {
        ByteArrayOutputStream resultOutputStream = new ByteArrayOutputStream();
        JsonGenerator outputGenerator;
        JsonParser dataParser;

        try {
            outputGenerator = jsonFactory.createGenerator(resultOutputStream, JsonEncoding.UTF8);
            dataParser = jsonFactory.createParser(jsonData);
        } catch (IOException e) {
            throw new JsonFilteringException("Failed to start JSON processing", e);
        }

        try {
            JsonToken firstToken = dataParser.nextToken();

            assertTokenIsValid(firstToken);

            switch (firstToken) {
                case START_OBJECT: {
                    assertFilterPatternNodeCorrectness(filterPatternJsonRootNode, JsonNodeType.OBJECT);
                    //output START_OBJECT
                    outputToken(firstToken, dataParser, outputGenerator);

                    processObjectNode(dataParser, outputGenerator, filterPatternJsonRootNode);
                    break;
                }
                case START_ARRAY: {
                    assertFilterPatternNodeCorrectness(filterPatternJsonRootNode, JsonNodeType.ARRAY);
                    //output START_ARRAY
                    outputToken(firstToken, dataParser, outputGenerator);

                    processArrayNode(dataParser, outputGenerator, filterPatternJsonRootNode);
                    break;
                }
                default: {
                    //if json data only contains (single) value token - just return it
                    outputToken(firstToken, dataParser, outputGenerator);
                }
            }
        } catch (Exception e) {
            JsonLocation lastProcessedLocation = dataParser.currentLocation();

            log.error("Exception during JSON filtration. Last processed token is approximately at line '{}' column '{}'",
                    lastProcessedLocation.getLineNr(),
                    lastProcessedLocation.getColumnNr(), e);

            throw new JsonFilteringException(e);

        } finally {
            try {
                dataParser.close();
            } catch (IOException e) {
                log.error("Failed to close JsonParser", e);
            }

            try {
                outputGenerator.close();
            } catch (IOException e) {
                log.error("Failed to close JsonGenerator", e);
            }
        }

        return resultOutputStream.toByteArray();
    }

    private void processObjectNode(JsonParser dataParser, JsonGenerator outputGenerator,
                                   JsonNode objectFilterPatternNode) throws JsonFilteringException, IOException {
        //consider empty object filter pattern to be a wildcard - so whole object should be returned as is
        if (!objectFilterPatternNode.equals(ABSENT_NODE) && objectFilterPatternNode.isEmpty()) {
            objectFilterPatternNode = WILDCARD_NODE;
        }

        JsonToken nextToken;

        while ((nextToken = dataParser.nextToken()) != JsonToken.END_OBJECT) {
            assertTokenIsValid(nextToken);

            String fieldName = dataParser.currentName();

            switch (nextToken) {
                case FIELD_NAME: {
                    //don't need to process field name token
                    continue;
                }
                case START_OBJECT: {
                    if (shouldIncludeObjectField(objectFilterPatternNode, fieldName)) {
                        //output START_OBJECT
                        outputToken(nextToken, fieldName, dataParser, outputGenerator);
                    }

                    JsonNode nestedObjectFilterPatternNode =
                            getFilterPatternNodeForObjectNestedContainer(objectFilterPatternNode, fieldName);

                    assertFilterPatternNodeCorrectness(nestedObjectFilterPatternNode, JsonNodeType.OBJECT);

                    processObjectNode(dataParser, outputGenerator, nestedObjectFilterPatternNode);
                    break;
                }
                case START_ARRAY: {
                    if (shouldIncludeObjectField(objectFilterPatternNode, fieldName)) {
                        //output START_ARRAY
                        outputToken(nextToken, fieldName, dataParser, outputGenerator);
                    }

                    JsonNode arrayFilterPatternNode =
                            getFilterPatternNodeForObjectNestedContainer(objectFilterPatternNode, fieldName);

                    assertFilterPatternNodeCorrectness(arrayFilterPatternNode, JsonNodeType.ARRAY);

                    processArrayNode(dataParser, outputGenerator, arrayFilterPatternNode);
                    break;
                }
                default: {
                    if (shouldIncludeObjectField(objectFilterPatternNode, fieldName)) {
                        outputToken(nextToken, fieldName, dataParser, outputGenerator);
                    }
                }
            }
        }

        if (!ABSENT_NODE.equals(objectFilterPatternNode)) {
            //output END_OBJECT
            outputToken(nextToken, dataParser, outputGenerator);
        }
    }

    private void processArrayNode(JsonParser dataParser, JsonGenerator outputGenerator,
                                  JsonNode arrayElementFilterPatternNode) throws JsonFilteringException, IOException {
        //consider empty array filter pattern to be a wildcard - so whole array should be returned as is
        if (!arrayElementFilterPatternNode.equals(ABSENT_NODE) && arrayElementFilterPatternNode.isEmpty()) {
            arrayElementFilterPatternNode = WILDCARD_NODE;
        }

        SimpleJsonNodeType onlyAllowedArrayElementsType = null;

        JsonToken nextToken;

        while ((nextToken = dataParser.nextToken()) != JsonToken.END_ARRAY) {
            assertTokenIsValid(nextToken);

            String fieldName = dataParser.currentName();
            String fieldValueStr = dataParser.getText();

            //for wildcard or absent filter pattern - allow processing array without even checking if it is homogenous
            if (!WILDCARD_NODE.equals(arrayElementFilterPatternNode)
                    && !ABSENT_NODE.equals(arrayElementFilterPatternNode)) {
                onlyAllowedArrayElementsType =
                        verifyArrayElementType(nextToken, onlyAllowedArrayElementsType, fieldValueStr);
            }

            switch (nextToken) {
                case START_OBJECT: {
                    if (!ABSENT_NODE.equals(arrayElementFilterPatternNode)) {
                        //output START_OBJECT
                        outputToken(nextToken, fieldName, dataParser, outputGenerator);
                    }

                    JsonNode objectFilterPatternNode =
                            getFilterPatternNodeForArrayNestedContainer(arrayElementFilterPatternNode);

                    assertFilterPatternNodeCorrectness(objectFilterPatternNode, JsonNodeType.OBJECT);

                    processObjectNode(dataParser, outputGenerator, objectFilterPatternNode);
                    break;
                }
                case START_ARRAY: {
                    if (!ABSENT_NODE.equals(arrayElementFilterPatternNode)) {
                        //output START_ARRAY
                        outputToken(nextToken, fieldName, dataParser, outputGenerator);
                    }

                    JsonNode arrayFilterPatternNode =
                            getFilterPatternNodeForArrayNestedContainer(arrayElementFilterPatternNode);

                    assertFilterPatternNodeCorrectness(arrayFilterPatternNode, JsonNodeType.ARRAY);

                    processArrayNode(dataParser, outputGenerator, arrayFilterPatternNode);
                    break;
                }
                default: {
                    assertFilterPatternNodeIsValidForArrayOfPrimitives(nextToken, arrayElementFilterPatternNode);

                    if (!ABSENT_NODE.equals(arrayElementFilterPatternNode)) {
                        outputToken(nextToken, dataParser, outputGenerator);
                    }
                }
            }
        }

        if (!ABSENT_NODE.equals(arrayElementFilterPatternNode)) {
            //output END_ARRAY
            outputToken(nextToken, dataParser, outputGenerator);
        }
    }

    //nested container means 'object inside object' or 'array inside object'
    private JsonNode getFilterPatternNodeForObjectNestedContainer(JsonNode objectFilterPatternNode, String fieldName) {
        if (WILDCARD_NODE.equals(objectFilterPatternNode)) {
            return WILDCARD_NODE;
        } else if (ABSENT_NODE.equals(objectFilterPatternNode)) {
            return ABSENT_NODE;
        } else {
            return objectFilterPatternNode.has(fieldName) ? objectFilterPatternNode.get(fieldName) : ABSENT_NODE;
        }
    }

    //nested container means 'object inside array' or 'array inside array'
    private JsonNode getFilterPatternNodeForArrayNestedContainer(JsonNode arrayElementFilterPatternNode) {
        if (WILDCARD_NODE.equals(arrayElementFilterPatternNode)) {
            return WILDCARD_NODE;
        } else if (ABSENT_NODE.equals(arrayElementFilterPatternNode)) {
            return ABSENT_NODE;
        } else {
            return getFilterPatternForArrayItems(arrayElementFilterPatternNode);
        }
    }

    private SimpleJsonNodeType verifyArrayElementType(
            JsonToken token, SimpleJsonNodeType onlyAllowedArrayElementsType, Object fieldValue
    ) throws JsonFilteringException {
        SimpleJsonNodeType arrayElementType = getArrayElementType(token);

        //if not yet initialized (now checking just 1st array element)
        if (onlyAllowedArrayElementsType == null) {
            return arrayElementType;
        }

        //if all values were null until now, and now we got a proper value - change it
        if (SimpleJsonNodeType.NULL.equals(onlyAllowedArrayElementsType)
                && !SimpleJsonNodeType.NULL.equals(arrayElementType)) {
            return arrayElementType;
        }

        if (!arrayElementType.equals(onlyAllowedArrayElementsType)
                && !arrayElementType.equals(SimpleJsonNodeType.NULL)) {
            throw new JsonFilteringException(
                    String.format("Array must contain elements of a single type (OBJECT, ARRAY, VALUE). " +
                            "%s expected. Offending symbol: %s", onlyAllowedArrayElementsType, fieldValue));
        }

        return onlyAllowedArrayElementsType;
    }

    private SimpleJsonNodeType getArrayElementType(JsonToken nextToken) {
        switch (nextToken) {
            case START_OBJECT:
                return SimpleJsonNodeType.OBJECT;
            case START_ARRAY:
                return SimpleJsonNodeType.ARRAY;
            case VALUE_NULL:
                return SimpleJsonNodeType.NULL;
            default:
                return SimpleJsonNodeType.VALUE;
        }
    }

    private JsonNode getFilterPatternForArrayItems(JsonNode arrayElementFilterPatternNode) {
        //we expect all items of this array (objects or arrays) to have the same structure
        //so 1st element of array is expected to contain filter pattern for this structure
        return arrayElementFilterPatternNode.get(0);
    }

    private boolean shouldIncludeObjectField(JsonNode objectFilterPatternNode, String fieldName) {
        if (ABSENT_NODE.equals(objectFilterPatternNode)) {
            return false;
        }

        return WILDCARD_NODE.equals(objectFilterPatternNode) || objectFilterPatternNode.has(fieldName);
    }

    private void outputToken(JsonToken token, JsonParser jsonParser, JsonGenerator outputGenerator) throws IOException {
        outputToken(token, null, jsonParser, outputGenerator);
    }

    private void outputToken(JsonToken token, String fieldName,
                             JsonParser jsonParser, JsonGenerator outputGenerator) throws IOException {
        if (fieldName != null) {
            outputGenerator.writeFieldName(fieldName);
        }

        switch (token) {
            case START_OBJECT: {
                outputGenerator.writeStartObject();
                break;
            }
            case END_OBJECT: {
                outputGenerator.writeEndObject();
                break;
            }
            case START_ARRAY: {
                outputGenerator.writeStartArray();
                break;
            }
            case END_ARRAY: {
                outputGenerator.writeEndArray();
                break;
            }
            case VALUE_STRING: {
                outputGenerator.writeString(jsonParser.getValueAsString());
                break;
            }
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT: {
                outputGenerator.writeNumber(jsonParser.getValueAsString());
                break;
            }
            case VALUE_TRUE: {
                outputGenerator.writeBoolean(true);
                break;
            }
            case VALUE_FALSE: {
                outputGenerator.writeBoolean(false);
                break;
            }
            case VALUE_NULL: {
                outputGenerator.writeNull();
                break;
            }
        }
    }

    private void assertFilterPatternNodeCorrectness(JsonNode filterPatternNode, JsonNodeType expectedType)
            throws BadFilterPatternException {
        if (WILDCARD_NODE.equals(filterPatternNode) || ABSENT_NODE.equals(filterPatternNode)) {
            return;
        }

        if (filterPatternNode == null) {
            throw new BadFilterPatternException("Filter pattern node is null. Expected type: " + expectedType);
        }

        if (!expectedType.equals(filterPatternNode.getNodeType())) {
            throw new BadFilterPatternException(
                    String.format("Filter pattern node is not of type %s: %s", expectedType, filterPatternNode));
        }

        if (JsonNodeType.ARRAY.equals(expectedType) && filterPatternNode.size() > 1) {
            throw new BadFilterPatternException("Filter pattern node for ARRAY must contain exactly one element - " +
                    "as a pattern for all array elements (or just be empty as wildcard)");
        }
    }

    private void assertFilterPatternNodeIsValidForArrayOfPrimitives(JsonToken nextToken, JsonNode arrayElementFilterPatternNode)
            throws BadFilterPatternException {
        if (JsonToken.VALUE_NULL.equals(nextToken)) {
            return;
        }

        if (!WILDCARD_NODE.equals(arrayElementFilterPatternNode) && !ABSENT_NODE.equals(arrayElementFilterPatternNode)) {
            throw new BadFilterPatternException(String.format("Bad filter pattern node for array of primitives: %s",
                    arrayElementFilterPatternNode));
        }
    }

    private void assertTokenIsValid(JsonToken token) throws JsonFilteringException {
        if (token == JsonToken.NOT_AVAILABLE) {
            //we are always expecting input json data to be completely loaded and available
            throw new JsonFilteringException("Got NOT_AVAILABLE token. This should not happen");
        }

        if (UNSUPPORTED_JSON_TOKENS.contains(token)) {
            throw new JsonFilteringException(token + " json token is not supported");
        }
    }
}
