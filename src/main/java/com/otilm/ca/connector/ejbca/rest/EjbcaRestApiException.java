package com.otilm.ca.connector.ejbca.rest;

import com.otilm.ca.connector.ejbca.dto.ejbca.response.ExceptionErrorRestResponse;
import org.springframework.http.HttpStatus;

public class EjbcaRestApiException extends Exception {

    private static final long serialVersionUID = 1L;

    // ExceptionErrorRestResponse is not Serializable; marked transient to satisfy S1948
    private final transient ExceptionErrorRestResponse error;
    private final HttpStatus httpStatus;

    public EjbcaRestApiException(String message, HttpStatus httpStatus, ExceptionErrorRestResponse error) {
        super(message);
        this.httpStatus = httpStatus;
        this.error = error;
    }

    public ExceptionErrorRestResponse getError() {
        return error;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
