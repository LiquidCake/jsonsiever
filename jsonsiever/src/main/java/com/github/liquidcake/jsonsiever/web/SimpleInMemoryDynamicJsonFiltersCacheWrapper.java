/*
 * Author: https://github.com/LiquidCake
 * MIT License
 */

package com.github.liquidcake.jsonsiever.web;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a simple in-memory cache that does not have expiration or size control.
 * Substitute this bean with your own implementation if needed (e.g. create a simple wrapper around Redis)
 */
public class SimpleInMemoryDynamicJsonFiltersCacheWrapper implements DynamicJsonFiltersCacheWrapper {
    //concurrent map provides basic concurrency safety and this enough for us
    private final Map<String, JsonNode> parsedFilterPatternsFromHeaderCache = new ConcurrentHashMap<>();

    @Override
    public JsonNode get(String key) {
        return parsedFilterPatternsFromHeaderCache.get(key);
    }

    @Override
    public void putIfAbsent(String filterPatternHeaderValue, JsonNode parsedFilterPattern) {
        parsedFilterPatternsFromHeaderCache.putIfAbsent(filterPatternHeaderValue, parsedFilterPattern);
    }
}
