package com.otilm.ca.connector.ejbca.config;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boot 4 ships each auto-configuration in its own module, and the test contexts run with Flyway disabled, so they still
 * load when the module that migrates the database at startup is missing.
 */
class ProductionAutoConfigurationTest {

    @Test
    void theApplicationMigratesTheDatabaseAtStartup() {
        List<String> candidates = ImportCandidates
                .load(AutoConfiguration.class, getClass().getClassLoader())
                .getCandidates();

        assertThat(candidates).contains("org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration");
    }
}
