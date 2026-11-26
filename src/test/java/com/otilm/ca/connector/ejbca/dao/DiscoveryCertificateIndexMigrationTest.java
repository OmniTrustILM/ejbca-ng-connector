package com.otilm.ca.connector.ejbca.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiscoveryCertificateIndexMigrationTest {

    private static final String RUN_INDEX = "discovery_certificate_discovery_id_id_idx";
    private static final String HAND_MADE_INDEX = "discovery_certificate_discovery_id_idx";
    private static final String LAST_VERSION_WITHOUT_THE_INDEX = "202211112000";

    @Test
    void indexesEachRunInIdOrder() throws SQLException {
        String url = database("fresh");

        migrate(url, MigrationVersion.LATEST);

        assertEquals(Map.of(RUN_INDEX, List.of("discovery_id", "id")), indexesOnDiscoveryId(url));
    }

    @Test
    void replacesTheSingleColumnIndexCreatedByHand() throws SQLException {
        String url = database("indexed-by-hand");
        migrate(url, MigrationVersion.fromVersion(LAST_VERSION_WITHOUT_THE_INDEX));
        execute(url, "CREATE INDEX " + HAND_MADE_INDEX + " ON discovery_certificate (discovery_id)");

        migrate(url, MigrationVersion.LATEST);

        assertEquals(Map.of(RUN_INDEX, List.of("discovery_id", "id")), indexesOnDiscoveryId(url));
    }

    private static String database(String name) {
        return "jdbc:h2:mem:" + name + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    }

    private static void migrate(String url, MigrationVersion target) {
        Flyway
                .configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .target(target)
                .load()
                .migrate();
    }

    private static void execute(String url, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    /** Each index that covers {@code discovery_id}, with its columns in index order. */
    private static Map<String, List<String>> indexesOnDiscoveryId(String url) throws SQLException {
        String sql = "SELECT index_name, column_name FROM information_schema.index_columns"
                + " WHERE table_name = 'discovery_certificate' AND index_name IN"
                + " (SELECT index_name FROM information_schema.index_columns"
                + " WHERE table_name = 'discovery_certificate' AND column_name = 'discovery_id')"
                + " ORDER BY index_name, ordinal_position";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            Map<String, List<String>> indexes = new LinkedHashMap<>();
            while (rows.next()) {
                indexes.computeIfAbsent(rows.getString(1), name -> new ArrayList<>()).add(rows.getString(2));
            }
            return indexes;
        }
    }
}
