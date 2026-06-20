package com.otilm.ca.connector.ejbca.dto;

public class SelectedEjbcaInstanceDto {

    private AuthorityInstanceNameAndUuidDto selectedEjbcaInstance;

    public SelectedEjbcaInstanceDto() {}

    public SelectedEjbcaInstanceDto(AuthorityInstanceNameAndUuidDto selectedEjbcaInstance) {
        this.selectedEjbcaInstance = selectedEjbcaInstance;
    }

    public AuthorityInstanceNameAndUuidDto getSelectedEjbcaInstance() {
        return selectedEjbcaInstance;
    }

    public void setSelectedEjbcaInstance(AuthorityInstanceNameAndUuidDto selectedEjbcaInstance) {
        this.selectedEjbcaInstance = selectedEjbcaInstance;
    }
}
