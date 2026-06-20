package com.otilm.ca.connector.ejbca.service.impl;

import com.otilm.api.exception.ValidationException;
import com.otilm.api.model.client.attribute.RequestAttribute;
import com.otilm.api.model.client.attribute.RequestAttributeV2;
import com.otilm.api.model.common.attribute.common.AttributeType;
import com.otilm.api.model.common.attribute.common.BaseAttribute;
import com.otilm.api.model.common.attribute.common.callback.AttributeCallback;
import com.otilm.api.model.common.attribute.common.content.AttributeContentType;
import com.otilm.api.model.common.attribute.common.content.data.CredentialAttributeContentData;
import com.otilm.api.model.common.attribute.v2.DataAttributeV2;
import com.otilm.api.model.common.attribute.v2.content.CredentialAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.StringAttributeContentV2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AttributeServiceImplTest {

    private final AttributeServiceImpl service = new AttributeServiceImpl();

    @Test
    void getAttributes_returnsUrlAndCredentialAttributes() {
        List<BaseAttribute> attrs = service.getAttributes("ejbca");

        assertNotNull(attrs);
        assertEquals(2, attrs.size());

        BaseAttribute url = attrs.stream()
                .filter(a -> AttributeServiceImpl.DATA_ATTRIBUTE_URL_NAME.equals(a.getName()))
                .findFirst().orElse(null);
        assertNotNull(url, "url attribute should be present");
        assertEquals(AttributeType.DATA, url.getType());
        assertEquals(AttributeServiceImpl.DATA_ATTRIBUTE_URL_UUID, url.getUuid());

        BaseAttribute credential = attrs.stream()
                .filter(a -> AttributeServiceImpl.DATA_ATTRIBUTE_CREDENTIAL_NAME.equals(a.getName()))
                .findFirst().orElse(null);
        assertNotNull(credential, "credential attribute should be present");
        assertEquals(AttributeType.DATA, credential.getType());
    }

    @Test
    void getAttributes_urlAttributeHasCorrectContentTypeAndLabel() {
        List<BaseAttribute> attrs = service.getAttributes("ejbca");
        DataAttributeV2 url = (DataAttributeV2) attrs.stream()
                .filter(a -> AttributeServiceImpl.DATA_ATTRIBUTE_URL_NAME.equals(a.getName()))
                .findFirst().orElseThrow();

        assertEquals(AttributeContentType.STRING, url.getContentType());
        assertEquals(AttributeServiceImpl.DATA_ATTRIBUTE_URL_LABEL, url.getProperties().getLabel());
        assertTrue(url.getProperties().isRequired());
        assertFalse(url.getProperties().isMultiSelect());
    }

    @Test
    void getAttributes_credentialAttributeHasCallbackAndCredentialContentType() {
        List<BaseAttribute> attrs = service.getAttributes("any");
        DataAttributeV2 credential = (DataAttributeV2) attrs.stream()
                .filter(a -> AttributeServiceImpl.DATA_ATTRIBUTE_CREDENTIAL_NAME.equals(a.getName()))
                .findFirst().orElseThrow();

        assertEquals(AttributeContentType.CREDENTIAL, credential.getContentType());
        AttributeCallback callback = credential.getAttributeCallback();
        assertNotNull(callback);
        assertEquals("GET", callback.getCallbackMethod());
        assertFalse(callback.getMappings().isEmpty());
    }

    @Test
    void validateAttributes_nullList_returnsFalse() {
        boolean result = service.validateAttributes("ejbca", null);
        assertFalse(result);
    }

    @Test
    void validateAttributes_validList_returnsTrue() {
        // URL attribute: STRING content, required
        RequestAttributeV2 urlAttr = new RequestAttributeV2(
                UUID.fromString(AttributeServiceImpl.DATA_ATTRIBUTE_URL_UUID),
                AttributeServiceImpl.DATA_ATTRIBUTE_URL_NAME,
                AttributeContentType.STRING,
                List.of(new StringAttributeContentV2("https://ejbca.example.com:8443/ejbca/ws"))
        );

        // Credential attribute: CREDENTIAL content, required.
        // Supply a CredentialAttributeContentV2 with a minimal CredentialAttributeContentData
        // so the validator reaches the CredentialDto conversion without finding null data.
        CredentialAttributeContentData credData = new CredentialAttributeContentData();
        credData.setUuid("11111111-1111-1111-1111-111111111111");
        credData.setName("test-credential");
        credData.setKind("SoftKeyStore");
        RequestAttributeV2 credAttr = new RequestAttributeV2(
                UUID.fromString(AttributeServiceImpl.DATA_ATTRIBUTE_CREDENTIAL_UUID),
                AttributeServiceImpl.DATA_ATTRIBUTE_CREDENTIAL_NAME,
                AttributeContentType.CREDENTIAL,
                List.of(new CredentialAttributeContentV2(credData))
        );

        List<RequestAttribute> input = List.of(urlAttr, credAttr);

        boolean result = service.validateAttributes("ejbca", input);

        assertTrue(result);
    }

    @Test
    void validateAttributes_emptyList_throwsValidationException() {
        // empty list with required attributes → ValidationException
        assertThrows(ValidationException.class,
                () -> service.validateAttributes("ejbca", List.of()));
    }
}
