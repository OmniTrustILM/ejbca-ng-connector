package com.otilm.ca.connector.ejbca.dto;

public class EjbcaVersionResponseDto {

    private String ejbcaVersion;

    public EjbcaVersionResponseDto(String ejbcaVersion) {
        this.ejbcaVersion = ejbcaVersion;
    }

    public String getEjbcaVersion() {
        return ejbcaVersion;
    }

    public void setEjbcaVersion(String ejbcaVersion) {
        this.ejbcaVersion = ejbcaVersion;
    }
}
