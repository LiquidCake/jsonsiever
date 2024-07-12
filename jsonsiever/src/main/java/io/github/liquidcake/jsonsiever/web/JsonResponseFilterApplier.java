/*
 * Author: https://github.com/LiquidCake
 * MIT License
 */

package io.github.liquidcake.jsonsiever.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import io.github.liquidcake.jsonsiever.core.JsonFilteringService;
import io.github.liquidcake.jsonsiever.web.config.JsonFilteringSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JsonResponseFilterApplier {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String DEFAULT_CLIENT_ID = "default";

    private final ObjectMapper objectMapper;
    private final JsonFilteringService jsonFilteringService;
    private final JsonFilteringSettings jsonFilteringSettings;
    private final DynamicJsonFiltersCacheWrapper dynamicJsonFiltersCacheWrapper;

    public JsonResponseFilterApplier(ObjectMapper objectMapper,
                                     JsonFilteringService jsonFilteringService,
                                     JsonFilteringSettings jsonFilteringSettings,
                                     DynamicJsonFiltersCacheWrapper dynamicJsonFiltersCacheWrapper) {
        this.objectMapper = objectMapper;
        this.jsonFilteringService = jsonFilteringService;
        this.jsonFilteringSettings = jsonFilteringSettings;
        this.dynamicJsonFiltersCacheWrapper = dynamicJsonFiltersCacheWrapper;
    }

    public Optional<JsonFilteringSettings.Endpoint> findEndpointFilteringSettings(String requestPathKey) {
        if (jsonFilteringSettings.getJsonsiever().getEndpoints() != null) {
            for (JsonFilteringSettings.Endpoint endpoint : jsonFilteringSettings.getJsonsiever().getEndpoints()) {
                //if all filtering patterns disabled - skip endpoint
                if (endpoint.isHeaderPatternAllowed() || endpoint.isFilePatternAllowed()) {
                    if (endpoint.isRegexpPath()) {
                        if (endpoint.pathMatches(requestPathKey)) {
                            return Optional.of(endpoint);
                        }
                    } else {
                        if (requestPathKey.equalsIgnoreCase(endpoint.getPath())) {
                            return Optional.of(endpoint);
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    public byte[] applyJsonFilterToResponseBody(
            byte[] jsonData, JsonFilteringSettings.Endpoint endpoint, HttpServletRequest request, String requestPathKey) {
        Optional<JsonNode> jsonFilterPatternOpt = Optional.empty();

        if (endpoint.isHeaderPatternAllowed()) {
            String filterPatternHeaderValue = request.getHeader(
                    jsonFilteringSettings.getJsonsiever().getFilterHeaderName()
            );
            jsonFilterPatternOpt = getFilterPatternFromHeader(requestPathKey, filterPatternHeaderValue);
        }

        if (jsonFilterPatternOpt.isEmpty() && endpoint.isFilePatternAllowed()) {
            String clientIdHeaderValue = request.getHeader(
                    jsonFilteringSettings.getJsonsiever().getClientIdHeaderName()
            );
            jsonFilterPatternOpt = getFilterPatternFromFile(endpoint, clientIdHeaderValue);
        }

        if (jsonFilterPatternOpt.isEmpty()) {
            log.info("Failed to get json filter pattern for request '{}'", requestPathKey);

            return jsonData;
        }

        try {
            return jsonFilteringService.filterJsonFields(jsonData, jsonFilterPatternOpt.get());
        } catch (Exception e) {
            log.warn("Failed to apply json filter to response body, returning original body", e);

            return jsonData;
        }
    }

    private Optional<JsonNode> getFilterPatternFromFile(
            JsonFilteringSettings.Endpoint endpoint, String clientIdHeaderValue) {

        Optional<JsonNode> filterPatternForClientOpt = Optional.empty();

        if (clientIdHeaderValue != null && !clientIdHeaderValue.trim().isEmpty()) {
            filterPatternForClientOpt = Optional.ofNullable(
                    endpoint.getFilePatternPerClient().get(clientIdHeaderValue)
            );
        }

        if (filterPatternForClientOpt.isEmpty()) {
            filterPatternForClientOpt = Optional.ofNullable(
                    endpoint.getFilePatternPerClient().get(DEFAULT_CLIENT_ID)
            );
        }

        return filterPatternForClientOpt;
    }

    private Optional<JsonNode> getFilterPatternFromHeader(String requestPathKey, String filterPatternHeaderValue) {
        if (filterPatternHeaderValue == null || filterPatternHeaderValue.trim().isEmpty()) {
            return Optional.empty();
        }

        JsonNode previouslyParsedFilterPattern = dynamicJsonFiltersCacheWrapper.get(filterPatternHeaderValue);

        if (previouslyParsedFilterPattern != null) {
            return Optional.of(previouslyParsedFilterPattern);
        }

        try {
            final JsonNode parsedFilterPattern = objectMapper.readTree(filterPatternHeaderValue);

            if (parsedFilterPattern == null) {
                log.warn("Failed to parse header with json filter pattern value for request {} (parsed as null)",
                        requestPathKey);
                if (log.isDebugEnabled()) {
                    log.debug("Request {}, Header: '{}'", requestPathKey, filterPatternHeaderValue);
                }

                return Optional.empty();
            }

            //value may be present or absent in cache at any different moments without any strict conditions -
            // consumer will just use value if it is already present and create/store its own copy if value is (seems to be) absent.
            // No concurrency issues between get/put will happen since 2 values for same key will always be equal and will be used readonly
            dynamicJsonFiltersCacheWrapper.putIfAbsent(filterPatternHeaderValue, parsedFilterPattern);

            return Optional.of(parsedFilterPattern);

        } catch (Exception e) {
            log.warn("Failed to parse header with json filter pattern value for request {}", requestPathKey);
            if (log.isDebugEnabled()) {
                log.debug("Request {}, Header: '{}'", requestPathKey, filterPatternHeaderValue, e);
            }

            return Optional.empty();
        }
    }
}
