package com.otilm.ca.connector.ejbca.service.impl;

import com.otilm.api.exception.NotFoundException;
import com.otilm.api.exception.ValidationException;
import com.otilm.api.model.connector.authority.AuthorityProviderInstanceDto;
import com.otilm.api.model.connector.authority.AuthorityProviderInstanceRequestDto;
import com.otilm.ca.connector.ejbca.config.ApplicationConfig;
import com.otilm.ca.connector.ejbca.dao.AuthorityInstanceRepository;
import com.otilm.ca.connector.ejbca.dao.entity.AuthorityInstance;
import com.otilm.ca.connector.ejbca.service.AttributeService;
import io.netty.channel.ChannelOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorityInstanceServiceImplTest {

    // ---------------------------------------------------------------------------
    // Existing factory-timeout tests (kept as-is — they tested methods that still
    // live in EjbcaConnectionFactory after the extraction in Task 4)
    // ---------------------------------------------------------------------------

    private EjbcaConnectionFactory factoryWithTimeouts() {
        EjbcaConnectionFactory factory = new EjbcaConnectionFactory();
        ReflectionTestUtils.setField(factory, "connectionTimeout", 1234);
        ReflectionTestUtils.setField(factory, "requestTimeout", 5678);
        return factory;
    }

    @Test
    void applyTimeouts_usesConfiguredConnectAndRequestTimeouts() {
        Map<String, Object> requestContext = new HashMap<>();
        factoryWithTimeouts().applyTimeouts(requestContext);

        assertEquals(1234, requestContext.get(ApplicationConfig.CONNECT_TIMEOUT));
        assertEquals(5678, requestContext.get(ApplicationConfig.REQUEST_TIMEOUT));
    }

    @Test
    void withTimeouts_appliesConfiguredConnectAndResponseTimeoutsToHttpClient() {
        HttpClient client = factoryWithTimeouts().withTimeouts(HttpClient.create());

        assertEquals(Duration.ofMillis(5678), client.configuration().responseTimeout());
        assertEquals(1234, client.configuration().options().get(ChannelOption.CONNECT_TIMEOUT_MILLIS));
    }

    // ---------------------------------------------------------------------------
    // Business-logic tests for AuthorityInstanceServiceImpl
    // ---------------------------------------------------------------------------

    @Mock
    AuthorityInstanceRepository authorityInstanceRepository;

    @Mock
    AttributeService attributeService;

    @Mock
    EjbcaConnectionFactory ejbcaConnectionFactory;

    @InjectMocks
    AuthorityInstanceServiceImpl service;

    // -- listAuthorityInstances -------------------------------------------------

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

    // -- getAuthorityInstance ---------------------------------------------------

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

    // -- removeAuthorityInstance ------------------------------------------------

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

    // -- getRestApiUrl ----------------------------------------------------------

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

    // -- createAuthorityInstance: validation-failure branch ---------------------

    @Test
    void createAuthorityInstance_throwsValidationException_whenAttributeValidationFails() {
        AuthorityProviderInstanceRequestDto request = mock(AuthorityProviderInstanceRequestDto.class);
        when(request.getName()).thenReturn("newCA");
        when(request.getKind()).thenReturn("EJBCA");
        when(request.getAttributes()).thenReturn(List.of());
        when(authorityInstanceRepository.findByName("newCA")).thenReturn(Optional.empty());
        when(attributeService.validateAttributes("EJBCA", List.of())).thenReturn(false);

        assertThrows(ValidationException.class, () -> service.createAuthorityInstance(request));
    }

    // -- updateAuthorityInstance: validation-failure branch ---------------------

    @Test
    void updateAuthorityInstance_throwsValidationException_whenAttributeValidationFails() {
        AuthorityInstance existing = authorityInstance(5L, "uuid-upd", "existingCA");
        when(authorityInstanceRepository.findByUuid("uuid-upd")).thenReturn(Optional.of(existing));

        AuthorityProviderInstanceRequestDto request = mock(AuthorityProviderInstanceRequestDto.class);
        when(request.getKind()).thenReturn("EJBCA");
        when(request.getAttributes()).thenReturn(List.of());
        when(attributeService.validateAttributes("EJBCA", List.of())).thenReturn(false);

        assertThrows(ValidationException.class, () -> service.updateAuthorityInstance("uuid-upd", request));
    }

    @Test
    void updateAuthorityInstance_throwsNotFoundException_whenInstanceMissing() {
        when(authorityInstanceRepository.findByUuid("no-such")).thenReturn(Optional.empty());

        AuthorityProviderInstanceRequestDto request = mock(AuthorityProviderInstanceRequestDto.class);
        assertThrows(NotFoundException.class, () -> service.updateAuthorityInstance("no-such", request));
    }

    // -- getConnection (uuid overload) -------------------------------------------

    @Test
    void getConnection_byUuid_throwsNotFoundException_whenInstanceMissing() {
        when(authorityInstanceRepository.findByUuid("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getConnection("missing"));
    }

    // -- getRestApiConnection (uuid overload) ------------------------------------

    @Test
    void getRestApiConnection_byUuid_throwsNotFoundException_whenInstanceMissing() {
        when(authorityInstanceRepository.findByUuid("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getRestApiConnection("missing"));
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
}
