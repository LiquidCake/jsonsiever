/*
 * Author: https://github.com/LiquidCake
 * MIT License
 */

package com.github.liquidcake.jsonsiever.web;

import com.fasterxml.jackson.databind.JsonNode;

public interface DynamicJsonFiltersCacheWrapper {

    JsonNode get(String key);

    void putIfAbsent(String filterPatternHeaderValue, JsonNode parsedFilterPattern);
}