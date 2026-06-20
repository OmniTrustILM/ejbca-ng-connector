package com.otilm.ca.connector.ejbca.enums;

/**
 * Stores the checksum of a Java-based migration.
 */
public enum JavaMigrationChecksums {
    V202206231700__AttributeChanges(1044247081),
    V202211031300__AttributeV2Changes(1069351362),
    V202211112000__MetadataToInfoAttributeMigration(-1824741875);
    private final int checksum;

    JavaMigrationChecksums(int checksum) {
        this.checksum = checksum;
    }

    public int getChecksum() {
        return checksum;
    }
}