package com.otilm.ca.connector.ejbca.service.impl;

import com.otilm.api.exception.NotFoundException;
import com.otilm.api.model.common.NameAndIdDto;
import com.otilm.ca.connector.ejbca.service.AuthorityInstanceService;
import com.otilm.ca.connector.ejbca.ws.AuthorizationDeniedException;
import com.otilm.ca.connector.ejbca.ws.AuthorizationDeniedException_Exception;
import com.otilm.ca.connector.ejbca.ws.EjbcaException;
import com.otilm.ca.connector.ejbca.ws.EjbcaException_Exception;
import com.otilm.ca.connector.ejbca.ws.EjbcaWS;
import com.otilm.ca.connector.ejbca.ws.NameAndId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EndEntityProfileEjbcaServiceImplTest {

    @Mock
    AuthorityInstanceService authorityInstanceService;

    @Mock
    EjbcaWS ejbcaWS;

    EndEntityProfileEjbcaServiceImpl service;

    private static final String UUID = "test-authority-uuid";

    @BeforeEach
    void setUp() throws NotFoundException {
        service = new EndEntityProfileEjbcaServiceImpl();
        service.setAuthorityInstanceService(authorityInstanceService);
        given(authorityInstanceService.getConnection(UUID)).willReturn(ejbcaWS);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private NameAndId nameAndId(int id, String name) {
        NameAndId n = new NameAndId();
        n.setId(id);
        n.setName(name);
        return n;
    }

    private AuthorizationDeniedException_Exception authDeniedException() {
        return new AuthorizationDeniedException_Exception("denied", new AuthorizationDeniedException());
    }

    private EjbcaException_Exception ejbcaException() {
        return new EjbcaException_Exception("error", new EjbcaException());
    }

    // ── listEndEntityProfiles ─────────────────────────────────────────────────

    @Test
    void listEndEntityProfiles_returnsMappedList() throws Exception {
        given(ejbcaWS.getAuthorizedEndEntityProfiles())
                .willReturn(List.of(nameAndId(1, "EMPTY"), nameAndId(2, "TestEEP")));

        List<NameAndIdDto> result = service.listEndEntityProfiles(UUID);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getId());
        assertEquals("EMPTY", result.get(0).getName());
    }

    @Test
    void listEndEntityProfiles_emptyList_throwsNotFoundException() throws Exception {
        given(ejbcaWS.getAuthorizedEndEntityProfiles()).willReturn(List.of());

        assertThrows(NotFoundException.class, () -> service.listEndEntityProfiles(UUID));
    }

    @Test
    void listEndEntityProfiles_authDenied_throwsAccessDeniedException() throws Exception {
        given(ejbcaWS.getAuthorizedEndEntityProfiles()).willThrow(authDeniedException());

        assertThrows(AccessDeniedException.class, () -> service.listEndEntityProfiles(UUID));
    }

    @Test
    void listEndEntityProfiles_ejbcaException_throwsIllegalStateException() throws Exception {
        given(ejbcaWS.getAuthorizedEndEntityProfiles()).willThrow(ejbcaException());

        assertThrows(IllegalStateException.class, () -> service.listEndEntityProfiles(UUID));
    }

    // ── listCertificateProfiles ───────────────────────────────────────────────

    @Test
    void listCertificateProfiles_returnsMappedList() throws Exception {
        given(ejbcaWS.getAvailableCertificateProfiles(10))
                .willReturn(List.of(nameAndId(10, "ENDUSER")));

        List<NameAndIdDto> result = service.listCertificateProfiles(UUID, 10);

        assertEquals(1, result.size());
        assertEquals("ENDUSER", result.get(0).getName());
    }

    @Test
    void listCertificateProfiles_emptyList_throwsNotFoundException() throws Exception {
        given(ejbcaWS.getAvailableCertificateProfiles(10)).willReturn(List.of());

        assertThrows(NotFoundException.class, () -> service.listCertificateProfiles(UUID, 10));
    }

    @Test
    void listCertificateProfiles_authDenied_throwsAccessDeniedException() throws Exception {
        given(ejbcaWS.getAvailableCertificateProfiles(10)).willThrow(authDeniedException());

        assertThrows(AccessDeniedException.class, () -> service.listCertificateProfiles(UUID, 10));
    }

    @Test
    void listCertificateProfiles_ejbcaException_throwsIllegalStateException() throws Exception {
        given(ejbcaWS.getAvailableCertificateProfiles(10)).willThrow(ejbcaException());

        assertThrows(IllegalStateException.class, () -> service.listCertificateProfiles(UUID, 10));
    }

    // ── listCAsInProfile ─────────────────────────────────────────────────────

    @Test
    void listCAsInProfile_returnsMappedList() throws Exception {
        given(ejbcaWS.getAvailableCAsInProfile(5))
                .willReturn(List.of(nameAndId(5, "MyCA"), nameAndId(6, "OtherCA")));

        List<NameAndIdDto> result = service.listCAsInProfile(UUID, 5);

        assertEquals(2, result.size());
        assertEquals("MyCA", result.get(0).getName());
    }

    @Test
    void listCAsInProfile_emptyList_throwsNotFoundException() throws Exception {
        given(ejbcaWS.getAvailableCAsInProfile(5)).willReturn(List.of());

        assertThrows(NotFoundException.class, () -> service.listCAsInProfile(UUID, 5));
    }

    @Test
    void listCAsInProfile_authDenied_throwsAccessDeniedException() throws Exception {
        given(ejbcaWS.getAvailableCAsInProfile(5)).willThrow(authDeniedException());

        assertThrows(AccessDeniedException.class, () -> service.listCAsInProfile(UUID, 5));
    }

    @Test
    void listCAsInProfile_ejbcaException_throwsIllegalStateException() throws Exception {
        given(ejbcaWS.getAvailableCAsInProfile(5)).willThrow(ejbcaException());

        assertThrows(IllegalStateException.class, () -> service.listCAsInProfile(UUID, 5));
    }
}
