/*
 * Author: https://github.com/LiquidCake
 * MIT License
 */

package com.github.liquidcake.jsonsiever.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class TestModel {

    @JsonProperty("fString")
    private final String fString;
    @JsonProperty("fJsonNode")
    private final JsonNode fJsonNode;

    public TestModel(String fString, JsonNode fJsonNode) {
        this.fString = fString;
        this.fJsonNode = fJsonNode;
    }
}
