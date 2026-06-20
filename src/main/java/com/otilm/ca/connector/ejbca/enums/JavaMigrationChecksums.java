package com.otilm.ca.connector.ejbca.enums;

/**
 * Stores the checksum of a Java-based migration.
 */
@SuppressWarnings("java:S115")
public enum JavaMigrationChecksums {
    V202206231700__AttributeChanges(1236550637, true),
    V202211031300__AttributeV2Changes(-44253029, true),
    V202211112000__MetadataToInfoAttributeMigration(37094549, true);

    private final int checksum;
    private final boolean isAltered;

    JavaMigrationChecksums(int checksum) {
        this(checksum, false);
    }

    JavaMigrationChecksums(int checksum, boolean isAltered) {
        this.checksum = checksum;
        this.isAltered = isAltered;
    }

    public int getChecksum() {
        return checksum;
    }

    public boolean isAltered() {
        return isAltered;
    }
}
