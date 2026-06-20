package com.otilm.ca.connector.ejbca.dto.ejbca.request;

import java.util.ArrayList;
import java.util.List;

public class SearchCertificatesRestRequestV2 {

    private Pagination pagination;
    private SearchCertificateSortRestRequest sort = null;
    private List<SearchCertificateCriteriaRestRequest> criteria = new ArrayList<>();

    public SearchCertificatesRestRequestV2() {}

    public SearchCertificatesRestRequestV2(Pagination pagination, SearchCertificateSortRestRequest sort, List<SearchCertificateCriteriaRestRequest> criteria) {
        this.pagination = pagination;
        this.sort = sort;
        this.criteria = criteria;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
    
    public SearchCertificateSortRestRequest getSort() {
        return sort;
    }

    public void setSort(SearchCertificateSortRestRequest sort) {
        this.sort = sort;
    }

    public List<SearchCertificateCriteriaRestRequest> getCriteria() {
        return criteria;
    }

    public void setCriteria(List<SearchCertificateCriteriaRestRequest> criteria) {
        this.criteria = criteria;
    }
}
