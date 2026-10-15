package com.otilm.ca.connector.ejbca;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The golden files under {@code src/test/resources/wire}. They were recorded on Spring Boot 3.5 as the baseline for the
 * move to 4.1. Re-record an intended change with {@code -Dwire.record=true} and review the golden diff in the pull
 * request.
 */
public final class Goldens {

    private static final Path DIR = Path.of("src/test/resources/wire");
    private static final boolean RECORD = Boolean.getBoolean("wire.record");
    private static final ObjectMapper JSON = new ObjectMapper();

    private Goldens() {
    }

    public static String read(String name) throws IOException {
        return Files.readString(DIR.resolve(name));
    }

    /** JSON is compared with extra fields refused and array order ignored; anything else must match exactly. */
    public static void assertMatches(String name, String actual) throws Exception {
        if (RECORD) {
            Files.createDirectories(DIR);
            Files.writeString(DIR.resolve(name), name.endsWith(".json") ? prettyPrinted(actual) : actual);
            return;
        }
        String expected = read(name);
        if (name.endsWith(".json")) {
            JSONAssert.assertEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
        } else {
            assertThat(actual).isEqualTo(expected);
        }
    }

    /** Pretty-printed and sorted, so a re-recorded golden reviews as a line diff of what actually changed. */
    private static String prettyPrinted(String json) throws IOException {
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(sorted(JSON.readTree(json))) + "\n";
    }

    /**
     * The comparison ignores the order of properties and of array elements, and both vary between recordings: Jackson 3
     * writes properties alphabetically, and the connector lists its endpoints in hash order.
     */
    private static JsonNode sorted(JsonNode node) {
        if (node.isArray()) {
            List<JsonNode> elements = new ArrayList<>();
            node.forEach(element -> elements.add(sorted(element)));
            elements.sort(Comparator.comparing(JsonNode::toString));
            return JSON.createArrayNode().addAll(elements);
        }
        if (node.isObject()) {
            ObjectNode object = JSON.createObjectNode();
            node
                    .properties()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(property -> object.set(property.getKey(), sorted(property.getValue())));
            return object;
        }
        return node;
    }
}
