package com.otilm.ca.connector.ejbca.dto.ejbca.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExceptionErrorRestResponse {

    @JsonProperty("error_code")
    private int errorCode;
    @JsonProperty("error_message")
    private String errorMessage;

    public ExceptionErrorRestResponse() {}

    private ExceptionErrorRestResponse(final int errorCode, final String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
