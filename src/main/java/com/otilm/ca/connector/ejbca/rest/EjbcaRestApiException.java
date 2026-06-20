package com.otilm.ca.connector.ejbca.rest;

import com.otilm.ca.connector.ejbca.dto.ejbca.response.ExceptionErrorRestResponse;
import org.springframework.http.HttpStatus;

public class EjbcaRestApiException extends Exception {

    private ExceptionErrorRestResponse error;
    private HttpStatus httpStatus;

    public EjbcaRestApiException(String message, HttpStatus httpStatus, ExceptionErrorRestResponse error) {
        super(message);
        this.httpStatus = httpStatus;
        this.error = error;
    }

    public ExceptionErrorRestResponse getError() {
        return error;
    }

    public void setError(ExceptionErrorRestResponse error) {
        this.error = error;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
}
