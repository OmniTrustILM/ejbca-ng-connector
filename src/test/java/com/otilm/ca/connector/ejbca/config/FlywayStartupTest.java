package com.otilm.ca.connector.ejbca.config;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The shipped schema exists only through Flyway, so the migrations have to run when the application starts. The suite's
 * database switches Flyway off, which this test undoes on a database of its own. The suite's application.yml replaces
 * the shipped one, so the shipped history table and schema are restated here.
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
    void appliesEveryMigrationAtStartup() {
        List<String> applied = jdbc
                .queryForList("select version from ejbca.ejbca_schema_history"
                        + " where success = true and version is not null order by version", String.class);

        assertThat(applied)
                .containsExactly("202203280920", "202206090840", "202206231700", "202211031300", "202211112000");
    }
}
