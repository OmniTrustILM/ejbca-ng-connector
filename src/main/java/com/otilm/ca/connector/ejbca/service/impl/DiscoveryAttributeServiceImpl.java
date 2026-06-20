package com.otilm.ca.connector.ejbca.service.impl;

import com.otilm.api.exception.ValidationError;
import com.otilm.api.exception.ValidationException;
import com.otilm.api.model.client.attribute.RequestAttribute;
import com.otilm.api.model.common.attribute.common.AttributeType;
import com.otilm.api.model.common.attribute.common.BaseAttribute;
import com.otilm.api.model.common.attribute.common.DataAttribute;
import com.otilm.api.model.common.attribute.common.callback.AttributeCallback;
import com.otilm.api.model.common.attribute.common.callback.AttributeCallbackMapping;
import com.otilm.api.model.common.attribute.common.callback.AttributeValueTarget;
import com.otilm.api.model.common.attribute.common.content.AttributeContentType;
import com.otilm.api.model.common.attribute.common.properties.DataAttributeProperties;
import com.otilm.api.model.common.attribute.common.properties.InfoAttributeProperties;
import com.otilm.api.model.common.attribute.v2.DataAttributeV2;
import com.otilm.api.model.common.attribute.v2.GroupAttributeV2;
import com.otilm.api.model.common.attribute.v2.InfoAttributeV2;
import com.otilm.api.model.common.attribute.v2.content.BaseAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.IntegerAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.ObjectAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.StringAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.TextAttributeContentV2;
import com.otilm.ca.connector.ejbca.dao.AuthorityInstanceRepository;
import com.otilm.ca.connector.ejbca.dao.entity.AuthorityInstance;
import com.otilm.ca.connector.ejbca.dto.AuthorityInstanceNameAndUuidDto;
import com.otilm.ca.connector.ejbca.dto.ejbca.request.SearchCertificateCriteriaRestRequest;
import com.otilm.ca.connector.ejbca.enums.DiscoveryKind;
import com.otilm.ca.connector.ejbca.service.DiscoveryAttributeService;
import com.otilm.core.util.AttributeDefinitionUtils;
import net.steppschuh.markdowngenerator.list.UnorderedList;
import net.steppschuh.markdowngenerator.text.Text;
import net.steppschuh.markdowngenerator.text.heading.Heading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DiscoveryAttributeServiceImpl implements DiscoveryAttributeService {

    public static final String ATTRIBUTE_EJBCA_INSTANCE = "ejbcaInstance";
    public static final String ATTRIBUTE_END_ENTITY_PROFILE = "endEntityProfile";
    public static final String ATTRIBUTE_EJBCA_RESTAPI_URL = "ejbcaRestApiUrl";
    public static final String ATTRIBUTE_EJBCA_CA = "ca";
    public static final String ATTRIBUTE_EJBCA_STATUS = "status";
    public static final String ATTRIBUTE_EJBCA_ISSUED_AFTER = "issuedAfter";
    public static final String ATTRIBUTE_ISSUED_DAYS_BEFORE = "issuedDaysBefore";
    public static final String ATTRIBUTE_EJBCA_INSTANCE_LABEL = "EJBCA instance";
    public static final String ATTRIBUTE_END_ENTITY_PROFILE_LABEL = "End Entity Profile";
    public static final String ATTRIBUTE_EJBCA_RESTAPI_URL_LABEL = "EJBCA REST API base URL";
    public static final String ATTRIBUTE_EJBCA_CA_LABEL = "Certification Authority";
    public static final String ATTRIBUTE_EJBCA_STATUS_LABEL = "Certificate status";
    public static final String ATTRIBUTE_EJBCA_ISSUED_AFTER_LABEL = "Certificates issued after";
    public static final String ATTRIBUTE_ISSUED_DAYS_BEFORE_LABEL = "Number of days";

    public static final String ATTRIBUTE_GROUP_DISCOVERY_CONF = "discoveryConfiguration";
    public static final String ATTRIBUTE_GROUP_DISCOVERY_CONF_LABEL = "Discovery configuration";

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryAttributeServiceImpl.class);

    private AuthorityInstanceRepository authorityInstanceRepository;


    @Autowired
    public void setAuthorityInstanceRepository(AuthorityInstanceRepository authorityInstanceRepository) {
        this.authorityInstanceRepository = authorityInstanceRepository;
    }

    @Override
    public List<BaseAttribute> getAttributes(String kind) {
        if (!kind.equals(DiscoveryKind.EJBCA.name()) && !kind.equals(DiscoveryKind.EJBCA_SCHEDULE.name())) {
            throw new ValidationException("Unsupported kind: " + kind, new ValidationError("Unsupported kind: " + kind));
        }
        logger.debug("Listing discovery attributes for {}", kind);

        List<BaseAttribute> attributes = new ArrayList<>();
        attributes.add(infoDiscoveryDescription());
        attributes.add(prepareEjbcaInstanceAttribute());
        attributes.add(discoveryConfiguration(kind));

        return attributes;
    }

    @Override
    public boolean validateAttributes(String kind, List<RequestAttribute> attributes) {
        if (attributes == null) {
            return false;
        }

        if (!kind.equals(DiscoveryKind.EJBCA.name()) && !kind.equals(DiscoveryKind.EJBCA_SCHEDULE.name())) {
            throw new ValidationException("Unsupported kind: " + kind);
        }

        AttributeDefinitionUtils.validateAttributes(getAttributes(kind), attributes);

        return true;
    }

    @Override
    public List<BaseAttribute> getInstanceAndKindAttributes(
            String kind,
            List<BaseAttributeContentV2<?>> eeProfilesContent,
            List<BaseAttributeContentV2<?>> casContent,
            List<BaseAttributeContentV2<?>> urlContent
    ) {
        List<BaseAttribute> attributes = new ArrayList<>();

        attributes.add(ejbcaRestApiUrlWithContent(urlContent));
        attributes.add(listCasWithContent(casContent));
        attributes.add(listEndEntityProfilesWithContent(eeProfilesContent));
        attributes.add(listStatus());

        if (kind.equals(DiscoveryKind.EJBCA.name())) {
            attributes.add(issuedAfter());
        }

        if (kind.equals(DiscoveryKind.EJBCA_SCHEDULE.name())) {
            attributes.add(issuedDaysBefore());
        }

        return attributes;
    }

    private DataAttribute prepareEjbcaInstanceAttribute() {
        List<AuthorityInstanceNameAndUuidDto> instanceNames = authorityInstanceRepository.findAll().stream().map(AuthorityInstance::mapToNameAndUuidDto).toList();
        List<BaseAttributeContentV2<?>> contentList = new ArrayList<>();
        for (AuthorityInstanceNameAndUuidDto instance : instanceNames) {
            ObjectAttributeContentV2 content = new ObjectAttributeContentV2(instance.getName(), instance);
            contentList.add(content);
        }

        DataAttributeV2 attribute = new DataAttributeV2();
        attribute.setUuid("dce22e96-3335-4181-b90c-c7f887d8d109");
        attribute.setName(ATTRIBUTE_EJBCA_INSTANCE);
        attribute.setContentType(AttributeContentType.OBJECT);
        attribute.setDescription("The EJBCA instance where Discovery process should search for Certificates");
        attribute.setType(AttributeType.DATA);
        DataAttributeProperties attributeProperties = new DataAttributeProperties();
        attributeProperties.setLabel(ATTRIBUTE_EJBCA_INSTANCE_LABEL);
        attributeProperties.setRequired(true);
        attributeProperties.setReadOnly(false);
        attributeProperties.setVisible(true);
        attributeProperties.setList(true);
        attributeProperties.setMultiSelect(false);
        attribute.setProperties(attributeProperties);
        attribute.setContent(contentList);

        return attribute;
    }

    private GroupAttributeV2 discoveryConfiguration(String kind) {
        // create group attribute
        GroupAttributeV2 attribute = new GroupAttributeV2();
        attribute.setUuid("cff2d66d-2f5a-420b-a2ad-44754dac6195");
        attribute.setName(ATTRIBUTE_GROUP_DISCOVERY_CONF);
        attribute.setType(AttributeType.GROUP);
        attribute.setDescription(ATTRIBUTE_GROUP_DISCOVERY_CONF_LABEL);

        // prepare mappings for callback
        Set<AttributeCallbackMapping> mappings = new HashSet<>();
        mappings.add(new AttributeCallbackMapping(ATTRIBUTE_EJBCA_INSTANCE + ".data.uuid", "ejbcaInstanceUuid", AttributeValueTarget.PATH_VARIABLE));
        mappings.add(new AttributeCallbackMapping("kind", AttributeValueTarget.PATH_VARIABLE, kind));

        // create attribute callback
        AttributeCallback attributeCallback = new AttributeCallback();
        attributeCallback.setCallbackContext("/v1/discoveryProvider/{ejbcaInstanceUuid}/{kind}/configuration");
        attributeCallback.setCallbackMethod("GET");
        attributeCallback.setMappings(mappings);

        // set attribute callback
        attribute.setAttributeCallback(attributeCallback);

        return attribute;
    }

    private DataAttribute listCasAttributeBase() {
        DataAttributeV2 attribute = new DataAttributeV2();
        attribute.setUuid("ffe7c27a-48e4-41fa-93de-8ddac65fec46");
        attribute.setName(ATTRIBUTE_EJBCA_CA);
        attribute.setDescription("Available certification authorities for Discovery");
        attribute.setContentType(AttributeContentType.OBJECT);
        attribute.setType(AttributeType.DATA);
        DataAttributeProperties attributeProperties = new DataAttributeProperties();
        attributeProperties.setLabel(ATTRIBUTE_EJBCA_CA_LABEL);
        attributeProperties.setRequired(false);
        attributeProperties.setReadOnly(false);
        attributeProperties.setVisible(true);
        attributeProperties.setList(true);
        attributeProperties.setMultiSelect(true);
        attribute.setProperties(attributeProperties);
        attribute.setContent(List.of());

        return attribute;
    }

    private DataAttribute listCasWithContent(List<BaseAttributeContentV2<?>> casContent) {
        DataAttribute attribute = listCasAttributeBase();

        attribute.setContent(casContent);

        return attribute;
    }

    private DataAttribute listEndEntityProfilesAttributeBase() {
        DataAttributeV2 attribute = new DataAttributeV2();
        attribute.setUuid("bbf2d142-f35a-437f-81c7-35c128881fc0");
        attribute.setName(ATTRIBUTE_END_ENTITY_PROFILE);
        attribute.setDescription("The End Entity Profile where Discovery process should search for Certificates");
        attribute.setType(AttributeType.DATA);
        attribute.setContentType(AttributeContentType.OBJECT);
        DataAttributeProperties attributeProperties = new DataAttributeProperties();
        attributeProperties.setLabel(ATTRIBUTE_END_ENTITY_PROFILE_LABEL);
        attributeProperties.setRequired(false);
        attributeProperties.setReadOnly(false);
        attributeProperties.setVisible(true);
        attributeProperties.setList(true);
        attributeProperties.setMultiSelect(true);
        attribute.setProperties(attributeProperties);
        attribute.setContent(List.of());

        return attribute;
    }

    private DataAttribute listEndEntityProfilesWithContent(List<BaseAttributeContentV2<?>> eeProfilesContent) {
        DataAttribute attribute = listEndEntityProfilesAttributeBase();

        attribute.setContent(eeProfilesContent);

        return attribute;
    }

    private DataAttribute listStatus() {
        List<BaseAttributeContentV2<?>> statuses = new ArrayList<>();
        for (SearchCertificateCriteriaRestRequest.CertificateStatus status : SearchCertificateCriteriaRestRequest.CertificateStatus.values()) {
            StringAttributeContentV2 content = new StringAttributeContentV2(status.name());
            statuses.add(content);
        }

        DataAttributeV2 attribute = new DataAttributeV2();
        attribute.setUuid("2aba1544-abdf-46a0-ab56-ac79f9163018");
        attribute.setName(ATTRIBUTE_EJBCA_STATUS);
        attribute.setDescription("Filter certificate status");
        attribute.setType(AttributeType.DATA);
        attribute.setContentType(AttributeContentType.OBJECT);
        DataAttributeProperties attributeProperties = new DataAttributeProperties();
        attributeProperties.setLabel(ATTRIBUTE_EJBCA_STATUS_LABEL);
        attributeProperties.setRequired(false);
        attributeProperties.setReadOnly(false);
        attributeProperties.setVisible(true);
        attributeProperties.setList(true);
        attributeProperties.setMultiSelect(true);
        attribute.setProperties(attributeProperties);
        attribute.setContent(statuses);

        return attribute;
    }

    private DataAttribute ejbcaRestApiUrlAttributeBase() {
        DataAttributeV2 attribute = new DataAttributeV2();
        attribute.setUuid("c5b974dd-e00a-44b6-b9bc-0946e79730a2");
        attribute.setName(ATTRIBUTE_EJBCA_RESTAPI_URL);
        attribute.setDescription("Base URL of the EJBCA REST API to be used");
        attribute.setType(AttributeType.DATA);
        attribute.setContentType(AttributeContentType.STRING);
        DataAttributeProperties attributeProperties = new DataAttributeProperties();
        attributeProperties.setLabel(ATTRIBUTE_EJBCA_RESTAPI_URL_LABEL);
        attributeProperties.setRequired(true);
        attributeProperties.setReadOnly(false);
        attributeProperties.setVisible(true);
        attributeProperties.setList(false);
        attributeProperties.setMultiSelect(false);
        attribute.setProperties(attributeProperties);

        return attribute;
    }

    private DataAttribute ejbcaRestApiUrlWithContent(List<BaseAttributeContentV2<?>> urlContent) {
        DataAttribute attribute = ejbcaRestApiUrlAttributeBase();

        attribute.setContent(urlContent);

        return attribute;
    }

    private DataAttribute issuedAfter() {
        DataAttributeV2 attribute = new DataAttributeV2();
        attribute.setUuid("4954adc0-47f0-442d-9347-f270d9ac0074");
        attribute.setName(ATTRIBUTE_EJBCA_ISSUED_AFTER);
        attribute.setDescription("The date after the certificates were issued");
        attribute.setType(AttributeType.DATA);
        attribute.setContentType(AttributeContentType.DATETIME);
        DataAttributeProperties attributeProperties = new DataAttributeProperties();
        attributeProperties.setLabel(ATTRIBUTE_EJBCA_ISSUED_AFTER_LABEL);
        attributeProperties.setRequired(false);
        attributeProperties.setReadOnly(false);
        attributeProperties.setVisible(true);
        attributeProperties.setList(false);
        attributeProperties.setMultiSelect(false);
        attribute.setProperties(attributeProperties);
        return attribute;
    }

    private DataAttribute issuedDaysBefore() {
        DataAttributeV2 attribute = new DataAttributeV2();
        attribute.setUuid("4a92a6c5-38c0-4ebf-8297-594d39572c9c");
        attribute.setName(ATTRIBUTE_ISSUED_DAYS_BEFORE);
        attribute.setDescription("Maximum number of days before the certificate was issued, from running the discovery");
        attribute.setType(AttributeType.DATA);
        attribute.setContentType(AttributeContentType.INTEGER);
        DataAttributeProperties attributeProperties = new DataAttributeProperties();
        attributeProperties.setLabel(ATTRIBUTE_ISSUED_DAYS_BEFORE_LABEL);
        attributeProperties.setRequired(true);
        attributeProperties.setReadOnly(false);
        attributeProperties.setVisible(true);
        attributeProperties.setList(false);
        attributeProperties.setMultiSelect(false);
        attribute.setProperties(attributeProperties);
        attribute.setContent(List.of(new IntegerAttributeContentV2(5)));

        return attribute;
    }

    private InfoAttributeV2 infoDiscoveryDescription() {
        InfoAttributeV2 attribute = new InfoAttributeV2();
        attribute.setUuid("a0bd9e99-7ee5-4e1c-bae1-4321209cf658");
        attribute.setName("info_discoveryProcess");
        attribute.setDescription("Discovery process information");
        attribute.setType(AttributeType.INFO);
        attribute.setContentType(AttributeContentType.TEXT);
        InfoAttributeProperties attributeProperties = new InfoAttributeProperties();
        attributeProperties.setLabel("Discovery process information");
        attributeProperties.setVisible(true);
        attribute.setProperties(attributeProperties);

        // prepare markdown string
        List<Object> items = Arrays.asList(
                "Certification authority",
                "End Entity Profile",
                "Certificate status"
        );
        StringBuilder sb = new StringBuilder()
                .append(new Heading("Overview", 2)).append("\n")
                .append(new Text("Select EJBCA instance where Discovery process should search for Certificates and then you can optionally select:")).append("\n")
                .append(new UnorderedList<>(items));

        attribute.setContent(List.of(new TextAttributeContentV2(sb.toString())));

        return attribute;
    }
}
