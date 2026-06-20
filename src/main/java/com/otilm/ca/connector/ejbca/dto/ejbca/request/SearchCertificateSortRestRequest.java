package com.otilm.ca.connector.ejbca.dto.ejbca.request;

public class SearchCertificateSortRestRequest {

    private String property;
    private String operation;

    public SearchCertificateSortRestRequest() {
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    /**
     * The set of criteria property values that are expected for SearchCertificateCriteriaRestRequest.property attribute.
     */
    public enum SortProperty {
        USERNAME,
        ISSUER_DN,
        SUBJECT_DN,
        EXTERNAL_ACCOUNT_BINDING_ID,
        END_ENTITY_PROFILE,
        CERTIFICATE_PROFILE,
        STATUS,
        TAG,
        TYPE,
        UPDATE_TIME,
        ISSUED_DATE,
        EXPIRE_DATE,
        REVOCATION_DATE;

        /**
         * Resolves the sort property using its name or returns null.
         *
         * @param property property name.
         *
         * @return SortProperty using its name or null.
         */
        public static SortProperty resolveCriteriaProperty(final String property) {
            for (SortProperty criteriaProperty : values()) {
                if (criteriaProperty.name().equalsIgnoreCase(property)) {
                    return criteriaProperty;
                }
            }
            return null;
        }
    }

    /**
     * The set of sort operation values that are expected for SearchCertificateSortRestRequest.operation attribute.
     */
    public enum SortOperation {
        ASC,
        DESC;

        /**
         * Resolves the sort operation using its name or returns null.
         *
         * @param operation operation name.
         *
         * @return SortOperation using its name or null.
         */
        public static SortOperation resolveCriteriaOperation(final String operation) {
            for (SortOperation criteriaOperation : values()) {
                if (criteriaOperation.name().equalsIgnoreCase(operation)) {
                    return criteriaOperation;
                }
            }
            return null;
        }
    }

    /**
     * Return a builder instance for this class.
     *
     * @return builder instance for this class.
     */
    public static SearchCertificateCriteriaRestRequestBuilder builder() {
        return new SearchCertificateCriteriaRestRequestBuilder();
    }

    public static class SearchCertificateCriteriaRestRequestBuilder {
        private String property;
        private String operation;

        private SearchCertificateCriteriaRestRequestBuilder() {
        }

        public SearchCertificateCriteriaRestRequestBuilder property(final String property) {
            this.property = property;
            return this;
        }

        public SearchCertificateCriteriaRestRequestBuilder operation(final String operation) {
            this.operation = operation;
            return this;
        }

        public SearchCertificateSortRestRequest build() {
            final SearchCertificateSortRestRequest request = new SearchCertificateSortRestRequest();
            request.setProperty(property);
            request.setOperation(operation);
            return request;
        }
    }

}
