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
 * Migration script for the Attributes changes.
 * Prerequisite for the successful migration is to have the AttributeDefinition stored in the database.
 * If the relaxed version of the AttributeDefinition is stored, the migration will fail, including missing
 * type, name, uuid, label.
 */
public class V202211031300__AttributeV2Changes extends BaseJavaMigration {

    private static final String AUTHORITY_INSTANCE_TABLE_NAME = "authority_instance";

    private static final String ATTRIBUTE_COLUMN_NAME = "attributes";
    private static final String CREDENTIAL_COLUMN_NAME = "credential_data";
    private static final String UNIQUE_IDENTIFIER = "id";

    @Override
    public Integer getChecksum() {
        return JavaMigrationChecksums.V202211031300__AttributeV2Changes.getChecksum();
    }

    public void migrate(Context context) throws Exception {
        try (Statement select = context.getConnection().createStatement()) {
            applyAuthorityInstanceMigration(context);
        }
    }

    private void applyAuthorityInstanceMigration(Context context) throws Exception {
        try (Statement select = context.getConnection().createStatement()) {
            try (ResultSet rows = select.executeQuery("SELECT id, attributes FROM authority_instance ORDER BY id")) {
                List<String> migrationCommands = V2AttributeMigrationUtils.getMigrationCommands(rows, AUTHORITY_INSTANCE_TABLE_NAME, ATTRIBUTE_COLUMN_NAME, UNIQUE_IDENTIFIER);
                ResultSet credentialRows = select.executeQuery("SELECT id, credential_data FROM authority_instance ORDER BY id");
                List<String> credentialMigrationCommands = V2AttributeMigrationUtils.getMigrationCommands(credentialRows, AUTHORITY_INSTANCE_TABLE_NAME, CREDENTIAL_COLUMN_NAME, UNIQUE_IDENTIFIER);
                executeCommands(select, migrationCommands);
                executeCommands(select, credentialMigrationCommands);
            }
        }
    }


    private void executeCommands(Statement select, List<String> commands) throws SQLException {
        for(String command: commands) {
            select.execute(command);
        }
    }
}