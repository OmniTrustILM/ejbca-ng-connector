package com.otilm.ca.connector.ejbca.enums;

import com.otilm.api.exception.ValidationError;
import com.otilm.api.exception.ValidationException;

import java.util.Arrays;

public enum UsernameGenMethod {
    RANDOM("Random"),
    CN("CN part of the DN");

    private String method;

    private UsernameGenMethod(String method) {
        this.method = method;
    }
    public String getCode() {
        return this.method;
    }

    public static UsernameGenMethod findByCode(String method) {
        return (UsernameGenMethod) Arrays.stream(values()).filter((k) -> {
            return k.method.equals(method);
        }).findFirst().orElseThrow(() -> {
            return new ValidationException(ValidationError.create("Unknown method {}", new Object[]{method}));
        });
    }

}
