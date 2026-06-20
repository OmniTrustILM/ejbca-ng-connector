package com.otilm.ca.connector.ejbca.dto.ejbca.request;

import java.util.EnumSet;

public class SearchCertificateCriteriaRestRequest {

    private String property;
    private String value;
    private String operation;

    public SearchCertificateCriteriaRestRequest() {
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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
    public enum CriteriaProperty {
        QUERY,
        EXTERNAL_ACCOUNT_BINDING_ID,
        END_ENTITY_PROFILE,
        CERTIFICATE_PROFILE,
        CA,
        STATUS,
        ISSUED_DATE,
        EXPIRE_DATE,
        REVOCATION_DATE;

        /**
         * Resolves the CriteriaProperty using its name or returns null.
         *
         * @param property property name.
         *
         * @return CriteriaProperty using its name or null.
         */
        public static CriteriaProperty resolveCriteriaProperty(final String property) {
            for (CriteriaProperty criteriaProperty : values()) {
                if (criteriaProperty.name().equalsIgnoreCase(property)) {
                    return criteriaProperty;
                }
            }
            return null;
        }

        /**
         * The subset of criteria properties that expect String input for SearchCertificateCriteriaRestRequest.value.
         *
         * @return subset of criteria properties.
         */
        public static EnumSet<CriteriaProperty> STRING_PROPERTIES() {
            return EnumSet.of(QUERY, STATUS, EXTERNAL_ACCOUNT_BINDING_ID);
        }

        /**
         * The subset of criteria properties that expect Date input for SearchCertificateCriteriaRestRequest.value.
         *
         * @return subset of criteria properties.
         */
        public static EnumSet<CriteriaProperty> DATE_PROPERTIES() {
            return EnumSet.of(ISSUED_DATE, EXPIRE_DATE, REVOCATION_DATE);
        }
    }

    /**
     * The set of criteria operation values that are expected for SearchCertificateCriteriaRestRequest.operation attribute.
     */
    public enum CriteriaOperation {
        EQUAL,
        LIKE,
        AFTER,
        BEFORE;

        /**
         * Resolves the CriteriaOperation using its name or returns null.
         *
         * @param operation operation name.
         *
         * @return CriteriaOperation using its name or null.
         */
        public static CriteriaOperation resolveCriteriaOperation(final String operation) {
            for (CriteriaOperation criteriaOperation : values()) {
                if (criteriaOperation.name().equalsIgnoreCase(operation)) {
                    return criteriaOperation;
                }
            }
            return null;
        }

        /**
         * The subset of criteria operations that are allowed for String input in SearchCertificateCriteriaRestRequest.value.
         *
         * @return subset of criteria operations.
         */
        public static EnumSet<CriteriaOperation> STRING_OPERATIONS() {
            return EnumSet.of(EQUAL, LIKE);
        }

        /**
         * The subset of criteria operations that are allowed for Date input in SearchCertificateCriteriaRestRequest.value.
         *
         * @return subset of criteria operations.
         */
        public static EnumSet<CriteriaOperation> DATE_OPERATIONS() {
            return EnumSet.of(AFTER, BEFORE);
        }
    }

    /**
     * The set of certificate status values that are expected for SearchCertificateCriteriaRestRequest.value attribute in case SearchCertificateCriteriaRestRequest.property = 'STATUS'.
     */
    public enum CertificateStatus {
        CERT_ACTIVE,
        CERT_REVOKED,
        REVOCATION_REASON_UNSPECIFIED,
        REVOCATION_REASON_KEYCOMPROMISE,
        REVOCATION_REASON_CACOMPROMISE,
        REVOCATION_REASON_AFFILIATIONCHANGED,
        REVOCATION_REASON_SUPERSEDED,
        REVOCATION_REASON_CESSATIONOFOPERATION,
        REVOCATION_REASON_CERTIFICATEHOLD,
        REVOCATION_REASON_REMOVEFROMCRL,
        REVOCATION_REASON_PRIVILEGESWITHDRAWN,
        REVOCATION_REASON_AACOMPROMISE;

        /**
         * Resolves the CertificateStatus using its name or returns null.
         *
         * @param name status name.
         *
         * @return CertificateStatus using its name or null.
         */
        public static CertificateStatus resolveCertificateStatusByName(final String name) {
            for (CertificateStatus certificateStatus : values()) {
                if (certificateStatus.name().equalsIgnoreCase(name)) {
                    return certificateStatus;
                }
            }
            return null;
        }

        /**
         * The subset of revocation reasons that are allowed for input in SearchCertificateCriteriaRestRequest.value.
         *
         * @return subset of criteria operations.
         */
        public static EnumSet<CertificateStatus> REVOCATION_REASONS() {
            return EnumSet.of(
                    REVOCATION_REASON_UNSPECIFIED,
                    REVOCATION_REASON_KEYCOMPROMISE,
                    REVOCATION_REASON_CACOMPROMISE,
                    REVOCATION_REASON_AFFILIATIONCHANGED,
                    REVOCATION_REASON_SUPERSEDED,
                    REVOCATION_REASON_CESSATIONOFOPERATION,
                    REVOCATION_REASON_CERTIFICATEHOLD,
                    REVOCATION_REASON_REMOVEFROMCRL,
                    REVOCATION_REASON_PRIVILEGESWITHDRAWN,
                    REVOCATION_REASON_AACOMPROMISE
            );
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
        private String value;
        private String operation;
        private int identifier;

        private SearchCertificateCriteriaRestRequestBuilder() {
        }

        public SearchCertificateCriteriaRestRequestBuilder property(final String property) {
            this.property = property;
            return this;
        }

        public SearchCertificateCriteriaRestRequestBuilder value(final String value) {
            this.value = value;
            return this;
        }

        public SearchCertificateCriteriaRestRequestBuilder operation(final String operation) {
            this.operation = operation;
            return this;
        }

        public SearchCertificateCriteriaRestRequestBuilder identifier(final int identifier) {
            this.identifier = identifier;
            return this;
        }

        public SearchCertificateCriteriaRestRequest build() {
            final SearchCertificateCriteriaRestRequest searchCertificateCriteriaRestRequest = new SearchCertificateCriteriaRestRequest();
            searchCertificateCriteriaRestRequest.setProperty(property);
            searchCertificateCriteriaRestRequest.setValue(value);
            searchCertificateCriteriaRestRequest.setOperation(operation);
            return searchCertificateCriteriaRestRequest;
        }
    }

}
