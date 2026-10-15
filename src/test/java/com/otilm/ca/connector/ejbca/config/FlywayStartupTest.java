package com.otilm.ca.connector.ejbca.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.otilm.ca.connector.ejbca.Goldens;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The shipped schema exists only through Flyway, so the migrations have to run when the application starts. The suite's
 * database switches Flyway off, which this test undoes on a database of its own. The suite's application.yml replaces
 * the shipped one, so the shipped history table and schema are restated here.
 * <p>
 * The history the migrations leave is pinned in a golden recorded on Spring Boot 3.5, checksums included: a database
 * migrated by an earlier release validates only while the checksums Flyway computes stay the same. {@link Goldens} says
 * how the golden is recorded.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.table=ejbca_schema_history",
        "spring.flyway.schemas=ejbca",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.url=jdbc:h2:mem:flyway;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;"
                + "INIT=CREATE SCHEMA IF NOT EXISTS ejbca"})
class FlywayStartupTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void appliesEveryMigrationAtStartup() throws Exception {
        List<Map<String, Object>> history = jdbc
                .queryForList("select version, type, script, checksum from ejbca.ejbca_schema_history"
                        + " where version is not null order by installed_rank");

        Goldens
                .assertMatches("schema-history.json",
                        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(history));
    }
}
