/*
 * Author: https://github.com/LiquidCake
 * MIT License
 */

package jsonsiever.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Component
public class JsonFilteringUtils {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ObjectMapper objectMapper;

    public JsonFilteringUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<JsonNode> readClasspathFileAsJsonNode(String path) {
        try (InputStream inputStream = this.getClass().getResourceAsStream(path)) {
            if (inputStream == null) {
                log.warn("Failed to open json filter pattern file in classpath: {}", path);

                return Optional.empty();
            }

            return Optional.ofNullable(objectMapper.readTree(inputStream));
        } catch (IOException e) {
            log.warn("Failed to parse json filter pattern file in classpath: {}", path, e);

            return Optional.empty();
        }
    }

    public <T> Optional<T> readClasspathYamlFileAs(String path, Class<?> clazz) {
        Yaml yaml = new Yaml(new Constructor(clazz, new LoaderOptions()));

        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                log.warn("Failed to open json filtering configuration file: {}", path);

                return Optional.empty();
            }

            return Optional.ofNullable(yaml.load(inputStream));
        } catch (Exception e) {
            log.warn("Error while loading json filtering configuration file: {}", path, e);

            return Optional.empty();
        }
    }
}
