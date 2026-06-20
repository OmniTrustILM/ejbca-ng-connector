package com.otilm.ca.connector.ejbca.dao.entity;

import com.otilm.api.model.common.attribute.common.MetadataAttribute;
import com.otilm.api.model.connector.discovery.DiscoveryProviderCertificateDataDto;
import com.otilm.core.util.AttributeDefinitionUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "discovery_certificate")
public class Certificate implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "discovery_certificate_seq")
    @SequenceGenerator(name = "discovery_certificate_seq", sequenceName = "discovery_certificate_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "base64Content")
    private String base64Content;

    @Column(name = "discoveryId")
    private Long discoveryId;

    @Column(name = "meta")
    private String meta;

    public DiscoveryProviderCertificateDataDto mapToDto() {
        DiscoveryProviderCertificateDataDto dto = new DiscoveryProviderCertificateDataDto();
        dto.setUuid(uuid);
        dto.setBase64Content(base64Content);
        dto.setMeta(AttributeDefinitionUtils.deserialize(meta, MetadataAttribute.class));
        return dto;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", id)
                .toString();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getBase64Content() {
        return base64Content;
    }

    public void setBase64Content(String base64Content) {
        this.base64Content = base64Content;
    }

    public Long getDiscoveryId() {
        return discoveryId;
    }

    public void setDiscoveryId(Long discoveryId) {
        this.discoveryId = discoveryId;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }
}

