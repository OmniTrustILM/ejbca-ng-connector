package com.czertainly.ca.connector.ejbca.dao.entity;

import com.czertainly.api.model.common.attribute.common.BaseAttribute;
import com.czertainly.api.model.connector.authority.AuthorityProviderInstanceDto;
import com.czertainly.ca.connector.ejbca.dto.AuthorityInstanceNameAndUuidDto;
import com.czertainly.core.util.AttributeDefinitionUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "authority_instance")
@EntityListeners(AuditingEntityListener.class)
public class AuthorityInstance {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "authority_instance_seq")
    @SequenceGenerator(name = "authority_instance_seq", sequenceName = "authority_instance_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "url")
    private String url;

    @Column(name = "credential_uuid")
    private String credentialUuid;

    //    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "credential_data", length = 40960)
    private String credentialData;

    @Column(name = "i_cre", nullable = false)
    @CreatedDate
    protected LocalDateTime created;

    @Column(name = "i_upd", nullable = false)
    @LastModifiedDate
    protected LocalDateTime updated;

    @Column(name="attributes")
    private String attributes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCredentialData() {
        return credentialData;
    }

    public void setCredentialData(String credentialData) {
        this.credentialData = credentialData;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getCredentialUuid() {
        return credentialUuid;
    }

    public void setCredentialUuid(String credentialUuid) {
        this.credentialUuid = credentialUuid;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public AuthorityProviderInstanceDto mapToDto() {
        AuthorityProviderInstanceDto dto = new AuthorityProviderInstanceDto();
        dto.setUuid(this.uuid);
        dto.setName(this.name);

        if (attributes != null) {
            dto.setAttributes(AttributeDefinitionUtils.deserialize(attributes, BaseAttribute.class));
        }

        return dto;
    }

    public AuthorityInstanceNameAndUuidDto mapToNameAndUuidDto() {
        AuthorityInstanceNameAndUuidDto dto = new AuthorityInstanceNameAndUuidDto();
        dto.setName(this.name);
        dto.setUuid(this.uuid);

        return dto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorityInstance that = (AuthorityInstance) o;
        return new EqualsBuilder().append(id, that.id).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(id).toHashCode();
    }
}
