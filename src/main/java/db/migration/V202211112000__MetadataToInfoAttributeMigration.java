package db.migration;

import com.otilm.ca.connector.ejbca.enums.JavaMigrationChecksums;
import com.otilm.core.util.V2AttributeMigrationUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Migration script for the metadata changes.
 * Prerequisite for the successful migration is to have the AttributeDefinition stored in the database.
 * If the relaxed version of the AttributeDefinition is stored, the migration will fail, including missing
 * type, name, uuid, label.
 */
public class V202211112000__MetadataToInfoAttributeMigration extends BaseJavaMigration {

    private static final String DISCOVERY_HISTORY_TABLE_NAME = "discovery_history";
    private static final String DISCOVERY_CERTIFICATE_TABLE_NAME = "discovery_certificate";

    private static final String META_COLUMN_NAME = "meta";
    private static final String UNIQUE_IDENTIFIER = "id";

    @Override
    public Integer getChecksum() {
        return JavaMigrationChecksums.V202211112000__MetadataToInfoAttributeMigration.getChecksum();
    }

    public void migrate(Context context) throws Exception {
        try (Statement select = context.getConnection().createStatement()) {
            applyMetadataMigration(context);
        }
    }

    private void applyMetadataMigration(Context context) throws Exception {
        try (Statement select = context.getConnection().createStatement()) {
            try (ResultSet rows = select.executeQuery("SELECT id, meta FROM discovery_history ORDER BY id")) {
                List<String> migrationCommands = V2AttributeMigrationUtils.getMetadataMigrationCommands(rows, DISCOVERY_HISTORY_TABLE_NAME, META_COLUMN_NAME, UNIQUE_IDENTIFIER);
                ResultSet certificateRows = select.executeQuery("SELECT id, meta FROM discovery_certificate ORDER BY id");
                List<String> certificateMigrationCommands = V2AttributeMigrationUtils.getMetadataMigrationCommands(certificateRows, DISCOVERY_CERTIFICATE_TABLE_NAME, META_COLUMN_NAME, UNIQUE_IDENTIFIER);
                executeCommands(select, migrationCommands);
                executeCommands(select, certificateMigrationCommands);
            }
        }
    }


    private void executeCommands(Statement select, List<String> commands) throws SQLException {
        for (String command : commands) {
            select.execute(command);
        }
    }
}