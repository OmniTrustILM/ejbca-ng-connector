/*************************************************************************
 *                                                                       *
 *  EJBCA - Proprietary Modules: Enterprise Certificate Authority        *
 *                                                                       *
 *  Copyright (c), PrimeKey Solutions AB. All rights reserved.           *
 *  The use of the Proprietary Modules are subject to specific           *
 *  commercial license terms.                                            *
 *                                                                       *
 *************************************************************************/
package com.otilm.ca.connector.ejbca.dto.ejbca.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class SearchCertificatesRestResponseV2 {
    
    private List<CertificateRestResponseV2> certificates = new ArrayList<>();

    @JsonProperty("pagination_summary")
    private PaginationSummary paginationSummary;

    public SearchCertificatesRestResponseV2() {}

    public SearchCertificatesRestResponseV2(List<CertificateRestResponseV2> certificates, PaginationSummary paginationSummary) {
        this.certificates = certificates;
        this.paginationSummary = paginationSummary;
    }

    public List<CertificateRestResponseV2> getCertificates() {
        return certificates;
    }

    public void setCertificates(final List<CertificateRestResponseV2> certificates) {
        this.certificates = certificates;
    }

    public PaginationSummary getPaginationSummary() {
        return paginationSummary;
    }

    public void setPaginationSummary(PaginationSummary paginationSummary) {
        this.paginationSummary = paginationSummary;
    }
}
