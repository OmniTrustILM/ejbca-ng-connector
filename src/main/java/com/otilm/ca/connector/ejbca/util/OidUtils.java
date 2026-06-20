package com.otilm.ca.connector.ejbca.util;

import com.otilm.api.exception.ValidationException;
import org.apache.commons.lang3.StringUtils;

public class OidUtils {

    private OidUtils() {
        // utility class
    }

    // Validation method for OID
    public static void validateOidFormat(String oid) {
        // OID should be a series of integers separated by dots
        if (StringUtils.isBlank(oid)) {
            throw new ValidationException("OID cannot be empty");
        }
        String[] parts = oid.split("\\.");
        if (parts.length == 0) {
            throw new ValidationException("OID cannot be empty");
        }
        for (String part : parts) {
            if (!part.matches("\\d+")) {
                throw new ValidationException("OID should be a series of integers separated by dots");
            }
        }
    }

}
