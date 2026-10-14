package com.otilm.ca.connector.ejbca;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private Goldens() {
    }

    public static String read(String name) throws IOException {
        return Files.readString(DIR.resolve(name));
    }

    /** JSON is compared with extra fields refused and array order ignored; anything else must match exactly. */
    public static void assertMatches(String name, String actual) throws Exception {
        if (RECORD) {
            Files.createDirectories(DIR);
            Files.writeString(DIR.resolve(name), actual);
            return;
        }
        String expected = read(name);
        if (name.endsWith(".json")) {
            JSONAssert.assertEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
        } else {
            assertThat(actual).isEqualTo(expected);
        }
    }
}
