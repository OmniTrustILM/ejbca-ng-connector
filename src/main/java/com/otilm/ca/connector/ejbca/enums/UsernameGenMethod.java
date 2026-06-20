package com.otilm.ca.connector.ejbca.enums;

import com.otilm.api.exception.ValidationError;
import com.otilm.api.exception.ValidationException;

import java.util.Arrays;

public enum UsernameGenMethod {
    RANDOM("Random"),
    CN("CN part of the DN");

    private final String code;

    UsernameGenMethod(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    public static UsernameGenMethod findByCode(String code) {
        return Arrays.stream(values())
                .filter(k -> k.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new ValidationException(ValidationError.create("Unknown method {}", new Object[]{code})));
    }

}
