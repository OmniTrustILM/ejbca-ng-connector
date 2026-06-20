package com.otilm.ca.connector.ejbca.service.impl;

import com.otilm.api.model.client.attribute.RequestAttribute;
import com.otilm.api.model.common.attribute.common.AttributeType;
import com.otilm.api.model.common.attribute.common.BaseAttribute;
import com.otilm.api.model.common.attribute.common.DataAttribute;
import com.otilm.api.model.common.attribute.common.callback.AttributeCallback;
import com.otilm.api.model.common.attribute.common.callback.AttributeCallbackMapping;
import com.otilm.api.model.common.attribute.common.callback.AttributeValueTarget;
import com.otilm.api.model.common.attribute.common.content.AttributeContentType;
import com.otilm.api.model.common.attribute.v2.DataAttributeV2;
import com.otilm.api.model.common.attribute.common.properties.DataAttributeProperties;
import com.otilm.ca.connector.ejbca.service.AttributeService;
import com.otilm.core.util.AttributeDefinitionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class AttributeServiceImpl implements AttributeService {
    private static final Logger logger = LoggerFactory.getLogger(AttributeServiceImpl.class);

    public static final String DATA_ATTRIBUTE_URL_NAME = "url";
    public static final String DATA_ATTRIBUTE_URL_UUID = "87e968ca-9404-4128-8b58-3ab5db2ba06e";
    public static final String DATA_ATTRIBUTE_URL_LABEL = "EJBCA WS URL";
    public static final String DATA_ATTRIBUTE_URL_DESCRIPTION = "URL of EJBCA web services";

    public static final String DATA_ATTRIBUTE_CREDENTIAL_NAME = "credential";
    // attribute-definition UUID, not a secret
    @SuppressWarnings("java:S6418")
    public static final String DATA_ATTRIBUTE_CREDENTIAL_UUID = "9379ca2c-aa51-42c8-8afd-2a2d16c99c57";
    public static final String DATA_ATTRIBUTE_CREDENTIAL_LABEL = "Credential";
    public static final String DATA_ATTRIBUTE_CREDENTIAL_DESCRIPTION = "SoftKeyStore Credential representing EJBCA administrator for the communication";

	@Override
	public List<BaseAttribute> getAttributes(String kind) {
		logger.debug("Getting the attributes for {}", kind);
		List<BaseAttribute> attrs = new ArrayList<>();

        DataAttributeV2 url = new DataAttributeV2();
        url.setUuid(DATA_ATTRIBUTE_URL_UUID);
        url.setName(DATA_ATTRIBUTE_URL_NAME);
        url.setDescription(DATA_ATTRIBUTE_URL_DESCRIPTION);
        url.setType(AttributeType.DATA);
        url.setContentType(AttributeContentType.STRING);
        DataAttributeProperties urlProperties = new DataAttributeProperties();
        urlProperties.setLabel(DATA_ATTRIBUTE_URL_LABEL);
        urlProperties.setRequired(true);
        urlProperties.setReadOnly(false);
        urlProperties.setVisible(true);
        urlProperties.setList(false);
        urlProperties.setMultiSelect(false);
        url.setProperties(urlProperties);
        attrs.add(url);

        DataAttributeV2 credential = new DataAttributeV2();
        credential.setUuid(DATA_ATTRIBUTE_CREDENTIAL_UUID);
        credential.setName(DATA_ATTRIBUTE_CREDENTIAL_NAME);
        credential.setDescription(DATA_ATTRIBUTE_CREDENTIAL_DESCRIPTION);
        credential.setType(AttributeType.DATA);
        credential.setContentType(AttributeContentType.CREDENTIAL);
        DataAttributeProperties credentialProperties = new DataAttributeProperties();
        credentialProperties.setLabel(DATA_ATTRIBUTE_CREDENTIAL_LABEL);
        credentialProperties.setRequired(true);
        credentialProperties.setReadOnly(false);
        credentialProperties.setVisible(true);
        credentialProperties.setList(true);
        credentialProperties.setMultiSelect(false);
        credential.setProperties(credentialProperties);

        Set<AttributeCallbackMapping> mappings = new HashSet<>();
        mappings.add(new AttributeCallbackMapping(
                "credentialKind",
                AttributeValueTarget.PATH_VARIABLE,
                "SoftKeyStore"));

        AttributeCallback listCredentialCallback = new AttributeCallback();
        listCredentialCallback.setCallbackContext("core/getCredentials");
        listCredentialCallback.setCallbackMethod("GET");
        listCredentialCallback.setMappings(mappings);
        credential.setAttributeCallback(listCredentialCallback);
        
        attrs.add(credential);

        return attrs;
	}

	@Override
	public boolean validateAttributes(String kind, List<RequestAttribute> attributes) {
        if (attributes == null) {
            return false;
        }

		AttributeDefinitionUtils.validateAttributes(getAttributes(kind), attributes);
        return true;
	}
}
