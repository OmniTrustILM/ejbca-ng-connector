package com.otilm.ca.connector.ejbca;

import com.otilm.ca.connector.ejbca.enums.JavaMigrationChecksums;
import com.otilm.core.util.DatabaseMigrationUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Simple tests for calculating checksums and validating the migration scripts integrity.
 */
public class DatabaseMigrationTest {

    @Test
    public void testCalculateChecksum_V202206231700__AttributeChanges() {
        int checksum = DatabaseMigrationUtils.calculateChecksum("src/main/java/db/migration/V202206231700__AttributeChanges.java");

        Assertions.assertEquals(JavaMigrationChecksums.V202206231700__AttributeChanges.getChecksum(), checksum);
    }

    @Test
    public void testCalculateChecksum_V202211031300__AttributeV2Changes() {
        int checksum = DatabaseMigrationUtils.calculateChecksum("src/main/java/db/migration/V202211031300__AttributeV2Changes.java");

        Assertions.assertEquals(JavaMigrationChecksums.V202211031300__AttributeV2Changes.getChecksum(), checksum);
    }

    @Test
    public void testCalculateChecksum_V202211112000__MetadataToInfoAttributeMigration() {
        int checksum = DatabaseMigrationUtils.calculateChecksum("src/main/java/db/migration/V202211112000__MetadataToInfoAttributeMigration.java");

        Assertions.assertEquals(JavaMigrationChecksums.V202211112000__MetadataToInfoAttributeMigration.getChecksum(), checksum);
    }
}
