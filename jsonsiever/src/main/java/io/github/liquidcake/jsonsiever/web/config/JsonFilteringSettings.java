/*
 * Author: https://github.com/LiquidCake
 * MIT License
 */

package io.github.liquidcake.jsonsiever.web.config;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import io.github.liquidcake.jsonsiever.web.JsonFilteringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Settings file should be put in classpath under name set in {@link io.github.liquidcake.jsonsiever.web.config.JsonFilteringSettings#JSON_FILTERS_CONFIG_FILE}
 * <br>
 * Example settings file content:
 * <pre>
 * filterHeaderName: "X-json-filter-pattern"
 * clientIdHeaderName: "X-client-id"
 * endpoints:
 *   - path: "GET_/get-cats"
 *     regexpPath: false
 *     headerPatternAllowed: true
 *     filePatternAllowed: true
 *     filePatternPathPerClient:
 *       our-mobile-app: "/json-filters/GET_get-cats/our-mobile-app.json"
 *       default: "/json-filters/GET_get-cats/default.json"
 * </pre>
 * Then filter pattern files should be put in classpath under paths specified in settings file (a file per-endpoint per-client)
 * e.g. <pre>/json-filters/get_get-cats/our-mobile-app.json</pre>
 * Content of filter pattern file would be a json "mask" - e.g. to return only "name" from {"name": "Cat", age: 5}
 * you would use filter pattern {"name": 1}. More details on filter patterns in {@link io.github.liquidcake.jsonsiever.core.JsonFilteringService}
 * */
@Component
public class JsonFilteringSettings {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public final static String JSON_FILTERS_CONFIG_FILE = "json-filtering-settings.yml";

    private final JsonFilteringUtils jsonFilteringUtils;

    private JsonSiever jsonsiever;

    public JsonFilteringSettings(JsonFilteringUtils jsonFilteringUtils) {
        this.jsonFilteringUtils = jsonFilteringUtils;
    }

    @PostConstruct
    public void initProperties() {
        Optional<JsonSiever> parsedSettingsOpt =
                jsonFilteringUtils.readClasspathYamlFileAs(JSON_FILTERS_CONFIG_FILE, JsonSiever.class);

        if (parsedSettingsOpt.isEmpty()) {
            jsonsiever = new JsonSiever();

            return;
        }

        jsonsiever = parsedSettingsOpt.get();

        if (jsonsiever.endpoints != null) {
            for (Endpoint endpoint : jsonsiever.endpoints) {
                if (endpoint.isRegexpPath() && endpoint.getPath() != null) {
                    endpoint.pattern = Pattern.compile(endpoint.getPath(), Pattern.CASE_INSENSITIVE);
                }

                if (endpoint.getFilePatternPathPerClient() != null) {
                    for (Map.Entry<String, String> entry : endpoint.getFilePatternPathPerClient().entrySet()) {
                        String clientId = entry.getKey();
                        String filePath = entry.getValue();

                        Optional<JsonNode> parsedJsonFilterPatternOpt = jsonFilteringUtils.readClasspathFileAsJsonNode(filePath);

                        if (parsedJsonFilterPatternOpt.isPresent()) {
                            endpoint.filePatternPerClient.put(clientId, parsedJsonFilterPatternOpt.get());

                            log.info("Loaded json filter pattern for endpoint {} client {}", endpoint.path, clientId);
                        } else {
                            log.warn("Failed to load json filter pattern for endpoint {} client {}", endpoint.path, clientId);
                        }
                    }
                }
            }
        }
    }

    public JsonSiever getJsonsiever() {
        return jsonsiever;
    }

    public static class JsonSiever {

        private String filterHeaderName;
        private String clientIdHeaderName;
        private List<Endpoint> endpoints;

        public String getFilterHeaderName() {
            return filterHeaderName;
        }

        public void setFilterHeaderName(String filterHeaderName) {
            this.filterHeaderName = filterHeaderName;
        }

        public String getClientIdHeaderName() {
            return clientIdHeaderName;
        }

        public void setClientIdHeaderName(String clientIdHeaderName) {
            this.clientIdHeaderName = clientIdHeaderName;
        }

        public List<Endpoint> getEndpoints() {
            return endpoints;
        }

        public void setEndpoints(List<Endpoint> endpoints) {
            this.endpoints = endpoints;
        }
    }

    public static class Endpoint {
        private String path;
        private boolean regexpPath;
        private boolean headerPatternAllowed;
        private boolean filePatternAllowed;
        private Map<String, String> filePatternPathPerClient;

        //additional properties
        private Pattern pattern;
        private final Map<String, JsonNode> filePatternPerClient = new HashMap<>();

        public boolean pathMatches(String path) {
            if (pattern == null) {
                return false;
            }

            Matcher matcher = pattern.matcher(path);
            return matcher.find();
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public boolean isRegexpPath() {
            return regexpPath;
        }

        public void setRegexpPath(boolean regexpPath) {
            this.regexpPath = regexpPath;
        }

        public boolean isHeaderPatternAllowed() {
            return headerPatternAllowed;
        }

        public void setHeaderPatternAllowed(boolean headerPatternAllowed) {
            this.headerPatternAllowed = headerPatternAllowed;
        }

        public boolean isFilePatternAllowed() {
            return filePatternAllowed;
        }

        public void setFilePatternAllowed(boolean filePatternAllowed) {
            this.filePatternAllowed = filePatternAllowed;
        }

        public Map<String, String> getFilePatternPathPerClient() {
            return filePatternPathPerClient;
        }

        public void setFilePatternPathPerClient(Map<String, String> filePatternPathPerClient) {
            this.filePatternPathPerClient = filePatternPathPerClient;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public Map<String, JsonNode> getFilePatternPerClient() {
            return filePatternPerClient;
        }
    }
}
