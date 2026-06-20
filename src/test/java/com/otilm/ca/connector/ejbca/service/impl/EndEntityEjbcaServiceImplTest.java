package com.otilm.ca.connector.ejbca.service.impl;

import com.otilm.api.exception.AlreadyExistException;
import com.otilm.api.exception.NotFoundException;
import com.otilm.api.model.client.attribute.ResponseAttribute;
import com.otilm.api.model.client.attribute.ResponseAttributeV2;
import com.otilm.api.model.common.NameAndIdDto;
import com.otilm.api.model.common.attribute.v2.content.BaseAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.BooleanAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.ObjectAttributeContentV2;
import com.otilm.api.model.core.authority.AddEndEntityRequestDto;
import com.otilm.api.model.core.authority.EditEndEntityRequestDto;
import com.otilm.api.model.core.authority.EndEntityDto;
import com.otilm.api.model.core.authority.EndEntityExtendedInfoDto;
import com.otilm.api.model.core.authority.EndEntityStatus;
import com.otilm.api.model.core.raprofile.RaProfileDto;
import com.otilm.ca.connector.ejbca.api.AuthorityInstanceControllerImpl;
import com.otilm.ca.connector.ejbca.service.AuthorityInstanceService;
import com.otilm.ca.connector.ejbca.ws.AuthorizationDeniedException;
import com.otilm.ca.connector.ejbca.ws.AuthorizationDeniedException_Exception;
import com.otilm.ca.connector.ejbca.ws.CADoesntExistsException;
import com.otilm.ca.connector.ejbca.ws.CADoesntExistsException_Exception;
import com.otilm.ca.connector.ejbca.ws.EjbcaException;
import com.otilm.ca.connector.ejbca.ws.EjbcaException_Exception;
import com.otilm.ca.connector.ejbca.ws.EjbcaWS;
import com.otilm.ca.connector.ejbca.ws.EndEntityProfileNotFoundException;
import com.otilm.ca.connector.ejbca.ws.EndEntityProfileNotFoundException_Exception;
import com.otilm.ca.connector.ejbca.ws.NotFoundException_Exception;
import com.otilm.ca.connector.ejbca.ws.UserDataVOWS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class EndEntityEjbcaServiceImplTest {

    @Mock
    AuthorityInstanceService authorityInstanceService;

    @Mock
    EjbcaWS ejbcaWS;

    EndEntityEjbcaServiceImpl service;

    private static final String UUID = "test-authority-uuid";
    private static final String EEP_NAME = "EMPTY";
    private static final String ENTITY_NAME = "testUser";

    @BeforeEach
    void setUp() throws NotFoundException {
        service = new EndEntityEjbcaServiceImpl();
        service.setAuthorityInstanceService(authorityInstanceService);
        lenient().when(authorityInstanceService.getConnection(UUID)).thenReturn(ejbcaWS);
    }

    // ── exception helpers ─────────────────────────────────────────────────────

    private AuthorizationDeniedException_Exception authDeniedException() {
        return new AuthorizationDeniedException_Exception("denied", new AuthorizationDeniedException());
    }

    private CADoesntExistsException_Exception caDoesntExistException() {
        return new CADoesntExistsException_Exception("ca not found", new CADoesntExistsException());
    }

    private EndEntityProfileNotFoundException_Exception eepNotFoundException() {
        return new EndEntityProfileNotFoundException_Exception("profile not found", new EndEntityProfileNotFoundException());
    }

    private NotFoundException_Exception notFoundException() {
        return new NotFoundException_Exception("not found", new com.otilm.ca.connector.ejbca.ws.NotFoundException());
    }

    private EjbcaException_Exception ejbcaException() {
        return new EjbcaException_Exception("ejbca error", new EjbcaException());
    }

    // ── fixture builders ──────────────────────────────────────────────────────

    private UserDataVOWS buildUserDataVOWS(String username) {
        UserDataVOWS u = new UserDataVOWS();
        u.setUsername(username);
        u.setPassword("password");
        u.setSubjectDN("CN=" + username);
        u.setCaName("ManagementCA");
        u.setEndEntityProfileName(EEP_NAME);
        u.setCertificateProfileName("ENDUSER");
        u.setStatus(EndEntityStatus.NEW.getCode()); // status 10 = NEW; 0 is not a valid EndEntityStatus
        return u;
    }

    /**
     * Builds a ResponseAttributeV2 with ObjectAttributeContentV2 wrapping a NameAndIdDto,
     * matching what AttributeDefinitionUtils.getObjectAttributeContentData() expects.
     */
    private ResponseAttributeV2 nameAndIdResponseAttr(String name, int id, String attrName) {
        ResponseAttributeV2 attr = new ResponseAttributeV2();
        attr.setName(name);
        NameAndIdDto dto = new NameAndIdDto(id, attrName);
        ObjectAttributeContentV2 content = new ObjectAttributeContentV2(attrName, dto);
        attr.setContent(List.of(content));
        return attr;
    }

    @SuppressWarnings("unchecked")
    private ResponseAttributeV2 booleanResponseAttr(String name, boolean value) {
        ResponseAttributeV2 attr = new ResponseAttributeV2();
        attr.setName(name);
        BooleanAttributeContentV2 content = new BooleanAttributeContentV2(value);
        List<BaseAttributeContentV2<?>> contentList = new ArrayList<>();
        contentList.add(content);
        attr.setContent(contentList);
        return attr;
    }

    /**
     * Builds a minimal RaProfileDto with all attributes that prepareEndEntity() needs.
     */
    private RaProfileDto buildRaProfile() {
        RaProfileDto rp = new RaProfileDto();
        List<ResponseAttribute> attrs = new ArrayList<>();
        attrs.add(nameAndIdResponseAttr(AuthorityInstanceControllerImpl.ATTRIBUTE_END_ENTITY_PROFILE, 1, EEP_NAME));
        attrs.add(nameAndIdResponseAttr(AuthorityInstanceControllerImpl.ATTRIBUTE_CERTIFICATE_PROFILE, 2, "ENDUSER"));
        attrs.add(nameAndIdResponseAttr(AuthorityInstanceControllerImpl.ATTRIBUTE_CERTIFICATION_AUTHORITY, 3, "ManagementCA"));
        attrs.add(booleanResponseAttr(AuthorityInstanceControllerImpl.ATTRIBUTE_SEND_NOTIFICATIONS, false));
        attrs.add(booleanResponseAttr(AuthorityInstanceControllerImpl.ATTRIBUTE_KEY_RECOVERABLE, false));
        rp.setAttributes(attrs);
        return rp;
    }

    private AddEndEntityRequestDto buildAddRequest(String username) {
        AddEndEntityRequestDto req = new AddEndEntityRequestDto();
        req.setUsername(username);
        req.setPassword("secret");
        req.setSubjectDN("CN=" + username);
        req.setEmail("test@example.com");
        req.setSubjectAltName("DNS:example.com");
        req.setRaProfile(buildRaProfile());
        return req;
    }

    private AddEndEntityRequestDto buildAddRequestWithExtensionData(String username) {
        AddEndEntityRequestDto req = buildAddRequest(username);
        List<EndEntityExtendedInfoDto> ei = new ArrayList<>();
        ei.add(new EndEntityExtendedInfoDto("customOid", "customValue"));
        req.setExtensionData(ei);
        return req;
    }

    private EditEndEntityRequestDto buildEditRequest(String username) {
        EditEndEntityRequestDto req = new EditEndEntityRequestDto();
        req.setPassword("newSecret");
        req.setSubjectDN("CN=" + username);
        req.setEmail("updated@example.com");
        req.setSubjectAltName("DNS:updated.example.com");
        req.setRaProfile(buildRaProfile());
        return req;
    }

    // ── listEntities ──────────────────────────────────────────────────────────

    @Test
    void listEntities_returnsMappedList() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS(ENTITY_NAME)));

        List<EndEntityDto> result = service.listEntities(UUID, EEP_NAME);

        assertEquals(1, result.size());
        assertEquals(ENTITY_NAME, result.get(0).getUsername());
    }

    @Test
    void listEntities_emptyList_returnsEmptyList() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of());

        List<EndEntityDto> result = service.listEntities(UUID, EEP_NAME);

        assertTrue(result.isEmpty());
    }

    @Test
    void listEntities_findUser_authDenied_throwsAccessDeniedException() throws Exception {
        given(ejbcaWS.findUser(any())).willThrow(authDeniedException());

        assertThrows(AccessDeniedException.class, () -> service.listEntities(UUID, EEP_NAME));
    }

    @Test
    void listEntities_findUser_eepNotFound_throwsNotFoundException() throws Exception {
        given(ejbcaWS.findUser(any())).willThrow(eepNotFoundException());

        assertThrows(NotFoundException.class, () -> service.listEntities(UUID, EEP_NAME));
    }

    @Test
    void listEntities_findUser_otherException_throwsIllegalStateException() throws Exception {
        given(ejbcaWS.findUser(any())).willThrow(ejbcaException());

        assertThrows(IllegalStateException.class, () -> service.listEntities(UUID, EEP_NAME));
    }

    // ── getEndEntity ──────────────────────────────────────────────────────────

    @Test
    void getEndEntity_existingUser_returnsMappedDto() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS(ENTITY_NAME)));

        EndEntityDto result = service.getEndEntity(UUID, EEP_NAME, ENTITY_NAME);

        assertNotNull(result);
        assertEquals(ENTITY_NAME, result.getUsername());
    }

    @Test
    void getEndEntity_nullList_throwsNotFoundException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);

        assertThrows(NotFoundException.class, () -> service.getEndEntity(UUID, EEP_NAME, ENTITY_NAME));
    }

    @Test
    void getEndEntity_emptyList_throwsNotFoundException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of());

        assertThrows(NotFoundException.class, () -> service.getEndEntity(UUID, EEP_NAME, ENTITY_NAME));
    }

    @Test
    void getEndEntity_findUser_authDenied_throwsAccessDeniedException() throws Exception {
        given(ejbcaWS.findUser(any())).willThrow(authDeniedException());

        assertThrows(AccessDeniedException.class, () -> service.getEndEntity(UUID, EEP_NAME, ENTITY_NAME));
    }

    @Test
    void getEndEntity_findUser_eepNotFound_throwsNotFoundException() throws Exception {
        given(ejbcaWS.findUser(any())).willThrow(eepNotFoundException());

        assertThrows(NotFoundException.class, () -> service.getEndEntity(UUID, EEP_NAME, ENTITY_NAME));
    }

    @Test
    void getEndEntity_findUser_otherException_throwsIllegalStateException() throws Exception {
        given(ejbcaWS.findUser(any())).willThrow(ejbcaException());

        assertThrows(IllegalStateException.class, () -> service.getEndEntity(UUID, EEP_NAME, ENTITY_NAME));
    }

    // ── createEndEntity ───────────────────────────────────────────────────────

    @Test
    void createEndEntity_newUser_callsEditUser() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);

        assertDoesNotThrow(() -> service.createEndEntity(UUID, EEP_NAME, buildAddRequest("newUser")));
    }

    @Test
    void createEndEntity_newUser_withExtensionData_callsEditUser() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);

        assertDoesNotThrow(() -> service.createEndEntity(UUID, EEP_NAME, buildAddRequestWithExtensionData("newUser")));
    }

    @Test
    void createEndEntity_existingUser_throwsAlreadyExistException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS("existingUser")));

        assertThrows(AlreadyExistException.class, () -> service.createEndEntity(UUID, EEP_NAME, buildAddRequest("existingUser")));
    }

    @Test
    void createEndEntity_editUser_authDenied_throwsAccessDeniedException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);
        willThrow(authDeniedException()).given(ejbcaWS).editUser(any());

        assertThrows(AccessDeniedException.class, () -> service.createEndEntity(UUID, EEP_NAME, buildAddRequest("newUser")));
    }

    @Test
    void createEndEntity_editUser_caDoesntExist_throwsNotFoundException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);
        willThrow(caDoesntExistException()).given(ejbcaWS).editUser(any());

        assertThrows(NotFoundException.class, () -> service.createEndEntity(UUID, EEP_NAME, buildAddRequest("newUser")));
    }

    @Test
    void createEndEntity_editUser_otherException_throwsIllegalStateException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);
        willThrow(new RuntimeException("unexpected")).given(ejbcaWS).editUser(any());

        assertThrows(IllegalStateException.class, () -> service.createEndEntity(UUID, EEP_NAME, buildAddRequest("newUser")));
    }

    // ── updateEndEntity ───────────────────────────────────────────────────────

    @Test
    void updateEndEntity_existingUser_callsEditUser() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS(ENTITY_NAME)));

        assertDoesNotThrow(() -> service.updateEndEntity(UUID, EEP_NAME, ENTITY_NAME, buildEditRequest(ENTITY_NAME)));
    }

    @Test
    void updateEndEntity_withStatus_setsStatusCode() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS(ENTITY_NAME)));
        EditEndEntityRequestDto req = buildEditRequest(ENTITY_NAME);
        req.setStatus(EndEntityStatus.NEW);

        assertDoesNotThrow(() -> service.updateEndEntity(UUID, EEP_NAME, ENTITY_NAME, req));
    }

    @Test
    void updateEndEntity_userNotFound_throwsNotFoundException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);

        assertThrows(NotFoundException.class, () -> service.updateEndEntity(UUID, EEP_NAME, ENTITY_NAME, buildEditRequest(ENTITY_NAME)));
    }

    @Test
    void updateEndEntity_editUser_authDenied_throwsAccessDeniedException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS(ENTITY_NAME)));
        willThrow(authDeniedException()).given(ejbcaWS).editUser(any());

        assertThrows(AccessDeniedException.class, () -> service.updateEndEntity(UUID, EEP_NAME, ENTITY_NAME, buildEditRequest(ENTITY_NAME)));
    }

    @Test
    void updateEndEntity_editUser_caDoesntExist_throwsNotFoundException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS(ENTITY_NAME)));
        willThrow(caDoesntExistException()).given(ejbcaWS).editUser(any());

        assertThrows(NotFoundException.class, () -> service.updateEndEntity(UUID, EEP_NAME, ENTITY_NAME, buildEditRequest(ENTITY_NAME)));
    }

    @Test
    void updateEndEntity_editUser_otherException_throwsIllegalStateException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS(ENTITY_NAME)));
        willThrow(new RuntimeException("unexpected")).given(ejbcaWS).editUser(any());

        assertThrows(IllegalStateException.class, () -> service.updateEndEntity(UUID, EEP_NAME, ENTITY_NAME, buildEditRequest(ENTITY_NAME)));
    }

    // ── revokeAndDeleteEndEntity ──────────────────────────────────────────────

    @Test
    void revokeAndDeleteEndEntity_existingUser_callsRevokeUser() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS(ENTITY_NAME)));

        assertDoesNotThrow(() -> service.revokeAndDeleteEndEntity(UUID, EEP_NAME, ENTITY_NAME));
    }

    @Test
    void revokeAndDeleteEndEntity_userNotFound_throwsNotFoundException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);

        assertThrows(NotFoundException.class, () -> service.revokeAndDeleteEndEntity(UUID, EEP_NAME, ENTITY_NAME));
    }

    @Test
    void revokeAndDeleteEndEntity_revokeUser_authDenied_throwsAccessDeniedException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS(ENTITY_NAME)));
        willThrow(authDeniedException()).given(ejbcaWS).revokeUser(any(), anyInt(), anyBoolean());

        assertThrows(AccessDeniedException.class, () -> service.revokeAndDeleteEndEntity(UUID, EEP_NAME, ENTITY_NAME));
    }

    @Test
    void revokeAndDeleteEndEntity_revokeUser_caDoesntExist_throwsNotFoundException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS(ENTITY_NAME)));
        willThrow(caDoesntExistException()).given(ejbcaWS).revokeUser(any(), anyInt(), anyBoolean());

        assertThrows(NotFoundException.class, () -> service.revokeAndDeleteEndEntity(UUID, EEP_NAME, ENTITY_NAME));
    }

    @Test
    void revokeAndDeleteEndEntity_revokeUser_notFound_throwsNotFoundException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS(ENTITY_NAME)));
        willThrow(notFoundException()).given(ejbcaWS).revokeUser(any(), anyInt(), anyBoolean());

        assertThrows(NotFoundException.class, () -> service.revokeAndDeleteEndEntity(UUID, EEP_NAME, ENTITY_NAME));
    }

    @Test
    void revokeAndDeleteEndEntity_revokeUser_otherException_throwsIllegalStateException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS(ENTITY_NAME)));
        willThrow(new RuntimeException("unexpected")).given(ejbcaWS).revokeUser(any(), anyInt(), anyBoolean());

        assertThrows(IllegalStateException.class, () -> service.revokeAndDeleteEndEntity(UUID, EEP_NAME, ENTITY_NAME));
    }

    // ── resetPassword ─────────────────────────────────────────────────────────

    @Test
    void resetPassword_existingUser_setsPasswordAndCallsEditUser() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS(ENTITY_NAME)));

        assertDoesNotThrow(() -> service.resetPassword(UUID, EEP_NAME, ENTITY_NAME));
    }

    @Test
    void resetPassword_userNotFound_throwsNotFoundException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);

        assertThrows(NotFoundException.class, () -> service.resetPassword(UUID, EEP_NAME, ENTITY_NAME));
    }

    @Test
    void resetPassword_editUser_authDenied_throwsAccessDeniedException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS(ENTITY_NAME)));
        willThrow(authDeniedException()).given(ejbcaWS).editUser(any());

        assertThrows(AccessDeniedException.class, () -> service.resetPassword(UUID, EEP_NAME, ENTITY_NAME));
    }

    @Test
    void resetPassword_editUser_caDoesntExist_throwsNotFoundException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS(ENTITY_NAME)));
        willThrow(caDoesntExistException()).given(ejbcaWS).editUser(any());

        assertThrows(NotFoundException.class, () -> service.resetPassword(UUID, EEP_NAME, ENTITY_NAME));
    }

    @Test
    void resetPassword_editUser_otherException_throwsIllegalStateException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS(ENTITY_NAME)));
        willThrow(new RuntimeException("unexpected")).given(ejbcaWS).editUser(any());

        assertThrows(IllegalStateException.class, () -> service.resetPassword(UUID, EEP_NAME, ENTITY_NAME));
    }

    // ── getUser (public helper) ───────────────────────────────────────────────

    @Test
    void getUser_existingUser_returnsUser() throws Exception {
        UserDataVOWS expected = buildUserDataVOWS(ENTITY_NAME);
        given(ejbcaWS.findUser(any())).willReturn(List.of(expected));

        UserDataVOWS result = service.getUser(ejbcaWS, ENTITY_NAME);

        assertNotNull(result);
        assertEquals(ENTITY_NAME, result.getUsername());
    }

    @Test
    void getUser_emptyList_returnsNull() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of());

        UserDataVOWS result = service.getUser(ejbcaWS, ENTITY_NAME);

        assertNull(result);
    }

    @Test
    void getUser_nullList_returnsNull() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);

        UserDataVOWS result = service.getUser(ejbcaWS, ENTITY_NAME);

        assertNull(result);
    }

    @Test
    void getUser_authDenied_throwsAccessDeniedException() throws Exception {
        given(ejbcaWS.findUser(any())).willThrow(authDeniedException());

        assertThrows(AccessDeniedException.class, () -> service.getUser(ejbcaWS, ENTITY_NAME));
    }

    @Test
    void getUser_eepNotFound_throwsNotFoundException() throws Exception {
        given(ejbcaWS.findUser(any())).willThrow(eepNotFoundException());

        assertThrows(NotFoundException.class, () -> service.getUser(ejbcaWS, ENTITY_NAME));
    }

    @Test
    void getUser_otherException_throwsIllegalStateException() throws Exception {
        given(ejbcaWS.findUser(any())).willThrow(ejbcaException());

        assertThrows(IllegalStateException.class, () -> service.getUser(ejbcaWS, ENTITY_NAME));
    }
}
