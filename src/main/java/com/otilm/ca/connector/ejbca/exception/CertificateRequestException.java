package com.otilm.ca.connector.ejbca.exception;

public class CertificateRequestException extends RuntimeException {

        public CertificateRequestException(String message) {
            super(message);
        }

        public CertificateRequestException(String message, Throwable cause) {
            super(message, cause);
        }

}
