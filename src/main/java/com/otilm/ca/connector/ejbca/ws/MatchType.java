package com.otilm.ca.connector.ejbca.ws;

import java.util.Arrays;

public enum MatchType {
    MATCH_TYPE_BEGINSWITH(1),
    MATCH_TYPE_CONTAINS(2),
    MATCH_TYPE_EQUALS(0);

    private final int code;

    private MatchType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static MatchType fromCode(int code) {
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(String.format("Unsupported type %s.", code)));
    }
}
