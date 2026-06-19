package com.czertainly.ca.connector.ejbca.dto;

public class AuthorityInstanceNameAndUuidDto implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String uuid;

    public AuthorityInstanceNameAndUuidDto() {
    }

    public AuthorityInstanceNameAndUuidDto(String name, String uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
