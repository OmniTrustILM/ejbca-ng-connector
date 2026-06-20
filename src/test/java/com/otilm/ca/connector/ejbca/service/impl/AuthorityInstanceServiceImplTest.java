package com.otilm.ca.connector.ejbca.service.impl;

import com.otilm.api.exception.AlreadyExistException;
import com.otilm.api.exception.NotFoundException;
import com.otilm.api.exception.ValidationException;
import com.otilm.api.model.client.attribute.RequestAttribute;
import com.otilm.api.model.client.attribute.RequestAttributeV2;
import com.otilm.api.model.common.attribute.common.content.AttributeContentType;
import com.otilm.api.model.common.attribute.common.content.data.CredentialAttributeContentData;
import com.otilm.api.model.common.attribute.v2.content.CredentialAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.StringAttributeContentV2;
import com.otilm.api.model.connector.authority.AuthorityProviderInstanceDto;
import com.otilm.api.model.connector.authority.AuthorityProviderInstanceRequestDto;
import com.otilm.ca.connector.ejbca.dao.AuthorityInstanceRepository;
import com.otilm.ca.connector.ejbca.dao.entity.AuthorityInstance;
import com.otilm.ca.connector.ejbca.service.AttributeService;
import com.otilm.ca.connector.ejbca.ws.EjbcaWS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorityInstanceServiceImplTest {

    // ---------------------------------------------------------------------------
    // Mocks & SUT
    // ---------------------------------------------------------------------------

    @Mock
    AuthorityInstanceRepository authorityInstanceRepository;

    @Mock
    AttributeService attributeService;

    @Mock
    EjbcaConnectionFactory ejbcaConnectionFactory;

    @InjectMocks
    AuthorityInstanceServiceImpl service;

    // ---------------------------------------------------------------------------
    // listAuthorityInstances
    // ---------------------------------------------------------------------------

    @Test
    void listAuthorityInstances_returnsNullWhenRepositoryIsEmpty() {
        when(authorityInstanceRepository.findAll()).thenReturn(new ArrayList<>());

        List<AuthorityProviderInstanceDto> result = service.listAuthorityInstances();

        assertNull(result);
    }

    @Test
    void listAuthorityInstances_returnsMappedDtosWhenInstancesExist() {
        AuthorityInstance a1 = authorityInstance(1L, "uuid-1", "first");
        AuthorityInstance a2 = authorityInstance(2L, "uuid-2", "second");
        when(authorityInstanceRepository.findAll()).thenReturn(List.of(a1, a2));

        List<AuthorityProviderInstanceDto> result = service.listAuthorityInstances();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("uuid-1", result.get(0).getUuid());
        assertEquals("first", result.get(0).getName());
        assertEquals("uuid-2", result.get(1).getUuid());
        assertEquals("second", result.get(1).getName());
    }

    // ---------------------------------------------------------------------------
    // getAuthorityInstance
    // ---------------------------------------------------------------------------

    @Test
    void getAuthorityInstance_returnsDto_whenInstanceExists() throws NotFoundException {
        AuthorityInstance instance = authorityInstance(1L, "uuid-abc", "myCA");
        when(authorityInstanceRepository.findByUuid("uuid-abc")).thenReturn(Optional.of(instance));

        AuthorityProviderInstanceDto dto = service.getAuthorityInstance("uuid-abc");

        assertEquals("uuid-abc", dto.getUuid());
        assertEquals("myCA", dto.getName());
    }

    @Test
    void getAuthorityInstance_throwsNotFoundException_whenInstanceMissing() {
        when(authorityInstanceRepository.findByUuid("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getAuthorityInstance("missing"));
    }

    // ---------------------------------------------------------------------------
    // removeAuthorityInstance
    // ---------------------------------------------------------------------------

    @Test
    void removeAuthorityInstance_deletesInstanceAndEvictsFromCache() throws NotFoundException {
        AuthorityInstance instance = authorityInstance(42L, "uuid-del", "toDelete");
        when(authorityInstanceRepository.findByUuid("uuid-del")).thenReturn(Optional.of(instance));

        service.removeAuthorityInstance("uuid-del");

        verify(authorityInstanceRepository).delete(instance);
        verify(ejbcaConnectionFactory).evict(42L);
    }

    @Test
    void removeAuthorityInstance_throwsNotFoundException_whenInstanceMissing() {
        when(authorityInstanceRepository.findByUuid("ghost")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.removeAuthorityInstance("ghost"));
    }

    // ---------------------------------------------------------------------------
    // getRestApiUrl
    // ---------------------------------------------------------------------------

    @Test
    void getRestApiUrl_returnsExpectedUrl_forWellFormedUrl() throws NotFoundException {
        AuthorityInstance instance = authorityInstance(1L, "uuid-url", "myCA");
        instance.setUrl("https://ejbca.example.com:8443/ejbca/ejbcaws/ejbcaws?wsdl");
        when(authorityInstanceRepository.findByUuid("uuid-url")).thenReturn(Optional.of(instance));

        String result = service.getRestApiUrl("uuid-url");

        assertEquals("https://ejbca.example.com:8443/ejbca/ejbca-rest-api", result);
    }

    @Test
    void getRestApiUrl_returnsExpectedUrl_forUrlWithoutPort() throws NotFoundException {
        AuthorityInstance instance = authorityInstance(1L, "uuid-url2", "myCA");
        instance.setUrl("https://ejbca.example.com/ejbca/ejbcaws/ejbcaws?wsdl");
        when(authorityInstanceRepository.findByUuid("uuid-url2")).thenReturn(Optional.of(instance));

        String result = service.getRestApiUrl("uuid-url2");

        assertEquals("https://ejbca.example.com/ejbca/ejbca-rest-api", result);
    }

    @Test
    void getRestApiUrl_throwsValidationException_forMalformedUrl() {
        AuthorityInstance instance = authorityInstance(1L, "uuid-bad", "myCA");
        instance.setUrl("not-a-valid-url");
        when(authorityInstanceRepository.findByUuid("uuid-bad")).thenReturn(Optional.of(instance));

        assertThrows(ValidationException.class, () -> service.getRestApiUrl("uuid-bad"));
    }

    @Test
    void getRestApiUrl_throwsNotFoundException_whenInstanceMissing() {
        when(authorityInstanceRepository.findByUuid("no-such")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getRestApiUrl("no-such"));
    }

    // ---------------------------------------------------------------------------
    // createAuthorityInstance
    // ---------------------------------------------------------------------------

    @Test
    void createAuthorityInstance_throwsAlreadyExistException_whenNameAlreadyTaken() {
        AuthorityInstance existing = authorityInstance(1L, "uuid-existing", "existingCA");
        when(authorityInstanceRepository.findByName("existingCA")).thenReturn(Optional.of(existing));

        AuthorityProviderInstanceRequestDto request = new AuthorityProviderInstanceRequestDto();
        request.setName("existingCA");

        assertThrows(AlreadyExistException.class, () -> service.createAuthorityInstance(request));
    }

    @Test
    void createAuthorityInstance_throwsValidationException_whenAttributeValidationFails() {
        when(authorityInstanceRepository.findByName("newCA")).thenReturn(Optional.empty());
        when(attributeService.validateAttributes("EJBCA", List.of())).thenReturn(false);

        AuthorityProviderInstanceRequestDto request = new AuthorityProviderInstanceRequestDto();
        request.setName("newCA");
        request.setKind("EJBCA");
        request.setAttributes(List.of());

        assertThrows(ValidationException.class, () -> service.createAuthorityInstance(request));
    }

    @Test
    void createAuthorityInstance_throwsValidationException_whenConnectionFails() {
        List<RequestAttribute> attrs = buildInstanceAttributes("https://ejbca.example.com:8443/ejbca/ejbcaws/ejbcaws?wsdl", "cred-uuid");
        when(authorityInstanceRepository.findByName("newCA")).thenReturn(Optional.empty());
        when(attributeService.validateAttributes("EJBCA", attrs)).thenReturn(true);
        when(attributeService.getAttributes("EJBCA")).thenReturn(List.of());
        when(ejbcaConnectionFactory.createConnection(any())).thenThrow(new RuntimeException("connection refused"));

        AuthorityProviderInstanceRequestDto request = new AuthorityProviderInstanceRequestDto();
        request.setName("newCA");
        request.setKind("EJBCA");
        request.setAttributes(attrs);

        assertThrows(ValidationException.class, () -> service.createAuthorityInstance(request));
    }

    @Test
    void createAuthorityInstance_savesInstanceAndCachesConnection_onSuccess() throws AlreadyExistException {
        List<RequestAttribute> attrs = buildInstanceAttributes("https://ejbca.example.com:8443/ejbca/ejbcaws/ejbcaws?wsdl", "cred-uuid");
        EjbcaWS ejbcaWS = mock(EjbcaWS.class);
        when(authorityInstanceRepository.findByName("newCA")).thenReturn(Optional.empty());
        when(attributeService.validateAttributes("EJBCA", attrs)).thenReturn(true);
        when(attributeService.getAttributes("EJBCA")).thenReturn(List.of());
        when(ejbcaConnectionFactory.createConnection(any())).thenReturn(ejbcaWS);

        AuthorityProviderInstanceRequestDto request = new AuthorityProviderInstanceRequestDto();
        request.setName("newCA");
        request.setKind("EJBCA");
        request.setAttributes(attrs);

        AuthorityProviderInstanceDto result = service.createAuthorityInstance(request);

        assertNotNull(result);
        assertEquals("newCA", result.getName());
        assertNotNull(result.getUuid());
        verify(authorityInstanceRepository).save(any(AuthorityInstance.class));
        verify(ejbcaConnectionFactory).put(any(), any());
    }

    // ---------------------------------------------------------------------------
    // updateAuthorityInstance
    // ---------------------------------------------------------------------------

    @Test
    void updateAuthorityInstance_throwsNotFoundException_whenInstanceMissing() {
        when(authorityInstanceRepository.findByUuid("no-such")).thenReturn(Optional.empty());

        AuthorityProviderInstanceRequestDto request = new AuthorityProviderInstanceRequestDto();

        assertThrows(NotFoundException.class, () -> service.updateAuthorityInstance("no-such", request));
    }

    @Test
    void updateAuthorityInstance_throwsValidationException_whenAttributeValidationFails() {
        AuthorityInstance existing = authorityInstance(5L, "uuid-upd", "existingCA");
        when(authorityInstanceRepository.findByUuid("uuid-upd")).thenReturn(Optional.of(existing));
        when(attributeService.validateAttributes("EJBCA", List.of())).thenReturn(false);

        AuthorityProviderInstanceRequestDto request = new AuthorityProviderInstanceRequestDto();
        request.setKind("EJBCA");
        request.setAttributes(List.of());

        assertThrows(ValidationException.class, () -> service.updateAuthorityInstance("uuid-upd", request));
    }

    @Test
    void updateAuthorityInstance_throwsValidationException_whenConnectionFails() {
        AuthorityInstance existing = authorityInstance(5L, "uuid-upd", "existingCA");
        List<RequestAttribute> attrs = buildInstanceAttributes("https://ejbca.example.com:8443/ejbca/ejbcaws/ejbcaws?wsdl", "cred-uuid");
        when(authorityInstanceRepository.findByUuid("uuid-upd")).thenReturn(Optional.of(existing));
        when(attributeService.validateAttributes("EJBCA", attrs)).thenReturn(true);
        when(attributeService.getAttributes("EJBCA")).thenReturn(List.of());
        when(ejbcaConnectionFactory.createConnection(any())).thenThrow(new RuntimeException("ssl error"));

        AuthorityProviderInstanceRequestDto request = new AuthorityProviderInstanceRequestDto();
        request.setName("updatedCA");
        request.setKind("EJBCA");
        request.setAttributes(attrs);

        assertThrows(ValidationException.class, () -> service.updateAuthorityInstance("uuid-upd", request));
    }

    @Test
    void updateAuthorityInstance_savesInstanceAndReplacesConnection_onSuccess() throws NotFoundException {
        AuthorityInstance existing = authorityInstance(5L, "uuid-upd", "existingCA");
        List<RequestAttribute> attrs = buildInstanceAttributes("https://ejbca.example.com:8443/ejbca/ejbcaws/ejbcaws?wsdl", "cred-uuid");
        EjbcaWS ejbcaWS = mock(EjbcaWS.class);
        when(authorityInstanceRepository.findByUuid("uuid-upd")).thenReturn(Optional.of(existing));
        when(attributeService.validateAttributes("EJBCA", attrs)).thenReturn(true);
        when(attributeService.getAttributes("EJBCA")).thenReturn(List.of());
        when(ejbcaConnectionFactory.createConnection(any())).thenReturn(ejbcaWS);

        AuthorityProviderInstanceRequestDto request = new AuthorityProviderInstanceRequestDto();
        request.setName("updatedCA");
        request.setKind("EJBCA");
        request.setAttributes(attrs);

        AuthorityProviderInstanceDto result = service.updateAuthorityInstance("uuid-upd", request);

        assertNotNull(result);
        assertEquals("updatedCA", result.getName());
        verify(authorityInstanceRepository).save(existing);
        verify(ejbcaConnectionFactory).replace(any(), any());
    }

    // ---------------------------------------------------------------------------
    // getConnection overloads
    // ---------------------------------------------------------------------------

    @Test
    void getConnection_byUuid_throwsNotFoundException_whenInstanceMissing() {
        when(authorityInstanceRepository.findByUuid("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getConnection("missing"));
    }

    @Test
    void getConnection_byUuid_delegatesToFactory_whenInstanceFound() throws NotFoundException {
        AuthorityInstance instance = authorityInstance(7L, "uuid-conn", "myCA");
        EjbcaWS ejbcaWS = mock(EjbcaWS.class);
        when(authorityInstanceRepository.findByUuid("uuid-conn")).thenReturn(Optional.of(instance));
        when(ejbcaConnectionFactory.getOrCreate(instance)).thenReturn(ejbcaWS);

        EjbcaWS result = service.getConnection("uuid-conn");

        assertEquals(ejbcaWS, result);
        verify(ejbcaConnectionFactory).getOrCreate(instance);
    }

    @Test
    void getConnection_byInstance_delegatesToFactory() {
        AuthorityInstance instance = authorityInstance(7L, "uuid-conn", "myCA");
        EjbcaWS ejbcaWS = mock(EjbcaWS.class);
        when(ejbcaConnectionFactory.getOrCreate(instance)).thenReturn(ejbcaWS);

        EjbcaWS result = service.getConnection(instance);

        assertEquals(ejbcaWS, result);
        verify(ejbcaConnectionFactory).getOrCreate(instance);
    }

    // ---------------------------------------------------------------------------
    // getRestApiConnection overloads
    // ---------------------------------------------------------------------------

    @Test
    void getRestApiConnection_byUuid_throwsNotFoundException_whenInstanceMissing() {
        when(authorityInstanceRepository.findByUuid("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getRestApiConnection("missing"));
    }

    @Test
    void getRestApiConnection_byUuid_delegatesToFactory_whenInstanceFound() throws NotFoundException {
        AuthorityInstance instance = authorityInstance(8L, "uuid-rest", "myCA");
        WebClient webClient = mock(WebClient.class);
        when(authorityInstanceRepository.findByUuid("uuid-rest")).thenReturn(Optional.of(instance));
        when(ejbcaConnectionFactory.getOrCreateRestApi(instance)).thenReturn(webClient);

        WebClient result = service.getRestApiConnection("uuid-rest");

        assertEquals(webClient, result);
        verify(ejbcaConnectionFactory).getOrCreateRestApi(instance);
    }

    @Test
    void getRestApiConnection_byInstance_delegatesToFactory() {
        AuthorityInstance instance = authorityInstance(8L, "uuid-rest", "myCA");
        WebClient webClient = mock(WebClient.class);
        when(ejbcaConnectionFactory.getOrCreateRestApi(instance)).thenReturn(webClient);

        WebClient result = service.getRestApiConnection(instance);

        assertEquals(webClient, result);
        verify(ejbcaConnectionFactory).getOrCreateRestApi(instance);
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private static AuthorityInstance authorityInstance(Long id, String uuid, String name) {
        AuthorityInstance i = new AuthorityInstance();
        i.setId(id);
        i.setUuid(uuid);
        i.setName(name);
        return i;
    }

    /**
     * Builds a minimal RequestAttributeV2 list with a STRING "url" attribute and a
     * CREDENTIAL "credential" attribute — sufficient for
     * AttributeDefinitionUtils.getSingleItemAttributeContentValue("url", ...) and
     * AttributeDefinitionUtils.getCredentialContent("credential", ...) to work.
     */
    private static List<RequestAttribute> buildInstanceAttributes(String url, String credentialUuid) {
        RequestAttributeV2 urlAttr = new RequestAttributeV2();
        urlAttr.setName("url");
        urlAttr.setContentType(AttributeContentType.STRING);
        StringAttributeContentV2 urlContent = new StringAttributeContentV2(url);
        urlAttr.setContent(List.of(urlContent));

        CredentialAttributeContentData credData = new CredentialAttributeContentData();
        credData.setUuid(credentialUuid);
        credData.setName("testCredential");
        credData.setAttributes(List.of());
        CredentialAttributeContentV2 credContent = new CredentialAttributeContentV2(credData);
        RequestAttributeV2 credAttr = new RequestAttributeV2();
        credAttr.setName("credential");
        credAttr.setContentType(AttributeContentType.CREDENTIAL);
        credAttr.setContent(List.of(credContent));

        return List.of(urlAttr, credAttr);
    }
}
