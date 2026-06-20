package com.otilm.ca.connector.ejbca.service.impl;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.otilm.api.exception.AlreadyExistException;
import com.otilm.api.exception.NotFoundException;
import com.otilm.api.model.client.attribute.RequestAttribute;
import com.otilm.api.model.client.attribute.RequestAttributeV2;
import com.otilm.api.model.common.NameAndIdDto;
import com.otilm.api.model.common.attribute.common.MetadataAttribute;
import com.otilm.api.model.common.attribute.v2.MetadataAttributeV2;
import com.otilm.api.model.common.attribute.v2.content.BooleanAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.StringAttributeContentV2;
import com.otilm.api.model.common.attribute.common.AttributeType;
import com.otilm.api.model.common.attribute.common.content.AttributeContentType;
import com.otilm.api.model.common.attribute.common.properties.DataAttributeProperties;
import com.otilm.api.model.common.attribute.common.properties.MetadataAttributeProperties;
import com.otilm.api.model.common.attribute.v2.DataAttributeV2;
import com.otilm.api.model.connector.v2.CertificateDataResponseDto;
import com.otilm.api.model.core.enums.CertificateRequestFormat;
import com.otilm.ca.connector.ejbca.EjbcaException;
import com.otilm.ca.connector.ejbca.api.AuthorityInstanceControllerImpl;
import com.otilm.ca.connector.ejbca.dto.ejbca.request.SearchCertificatesRestRequestV2;
import com.otilm.ca.connector.ejbca.dto.ejbca.response.SearchCertificatesRestResponseV2;
import com.otilm.ca.connector.ejbca.service.AuthorityInstanceService;
import com.otilm.ca.connector.ejbca.util.LocalAttributeUtil;
import com.otilm.ca.connector.ejbca.ws.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class EjbcaServiceImplTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Mock
    AuthorityInstanceService authorityInstanceService;

    @Mock
    EjbcaWS ejbcaWS;

    EjbcaServiceImpl service;

    private static final String UUID = "test-authority-uuid";

    @BeforeEach
    void setUp() throws NotFoundException {
        service = new EjbcaServiceImpl();
        service.setAuthorityInstanceService(authorityInstanceService);
        lenient().when(authorityInstanceService.getConnection(UUID)).thenReturn(ejbcaWS);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private AuthorizationDeniedException_Exception authDeniedException() {
        return new AuthorizationDeniedException_Exception("denied", new AuthorizationDeniedException());
    }

    private CADoesntExistsException_Exception caDoesntExistException() {
        return new CADoesntExistsException_Exception("ca not found", new CADoesntExistsException());
    }

    private EjbcaException_Exception ejbcaException() {
        return new EjbcaException_Exception("ejbca error", new com.otilm.ca.connector.ejbca.ws.EjbcaException());
    }

    private NotFoundException_Exception notFoundException() {
        return new NotFoundException_Exception("not found", new com.otilm.ca.connector.ejbca.ws.NotFoundException());
    }

    private EndEntityProfileNotFoundException_Exception endEntityProfileNotFoundException() {
        return new EndEntityProfileNotFoundException_Exception("profile not found", new EndEntityProfileNotFoundException());
    }

    private UserDoesntFullfillEndEntityProfile_Exception userDoesntFullfillException() {
        // Message must have a ": " separator because EjbcaServiceImpl does getMessage().split(": ")[1]
        return new UserDoesntFullfillEndEntityProfile_Exception("prefix: profile validation failed", new UserDoesntFullfillEndEntityProfile());
    }

    /**
     * Builds a minimal set of RA profile attributes for setUserProfiles().
     */
    private List<RequestAttribute> buildRaProfileAttributes() {
        List<RequestAttribute> attrs = new ArrayList<>();
        attrs.add(nameAndIdAttr(AuthorityInstanceControllerImpl.ATTRIBUTE_END_ENTITY_PROFILE, 1, "EMPTY"));
        attrs.add(nameAndIdAttr(AuthorityInstanceControllerImpl.ATTRIBUTE_CERTIFICATE_PROFILE, 2, "ENDUSER"));
        attrs.add(nameAndIdAttr(AuthorityInstanceControllerImpl.ATTRIBUTE_CERTIFICATION_AUTHORITY, 3, "ManagementCA"));
        attrs.add(booleanAttr(AuthorityInstanceControllerImpl.ATTRIBUTE_SEND_NOTIFICATIONS, false));
        attrs.add(booleanAttr(AuthorityInstanceControllerImpl.ATTRIBUTE_KEY_RECOVERABLE, false));
        return attrs;
    }

    /**
     * Builds a minimal set of issue attributes for prepareEndEntity().
     */
    private List<RequestAttribute> buildIssueAttributes() {
        List<RequestAttribute> attrs = new ArrayList<>();
        attrs.add(stringAttr("email", ""));
        attrs.add(stringAttr("san", ""));
        attrs.add(stringAttr("extension", ""));
        return attrs;
    }

    /**
     * Builds metadata attributes for prepareEndEntityWithMeta().
     */
    private List<MetadataAttribute> buildMetadataAttributes() {
        List<MetadataAttribute> attrs = new ArrayList<>();
        attrs.add(stringMetaAttr(CertificateEjbcaServiceImpl.META_EMAIL, ""));
        attrs.add(stringMetaAttr(CertificateEjbcaServiceImpl.META_SAN, ""));
        attrs.add(stringMetaAttr(CertificateEjbcaServiceImpl.META_EXTENSION, ""));
        return attrs;
    }

    private RequestAttributeV2 nameAndIdAttr(String name, int id, String attrName) {
        RequestAttributeV2 attr = new RequestAttributeV2();
        attr.setName(name);
        NameAndIdDto nameAndIdDto = new NameAndIdDto(id, attrName);
        attr.setContent(LocalAttributeUtil.convertFromNameAndIdToBase(List.of(nameAndIdDto)));
        return attr;
    }

    private RequestAttributeV2 stringAttr(String name, String value) {
        RequestAttributeV2 attr = new RequestAttributeV2();
        attr.setName(name);
        DataAttributeV2 def = new DataAttributeV2();
        def.setName(name);
        def.setType(AttributeType.DATA);
        def.setContentType(AttributeContentType.STRING);
        DataAttributeProperties props = new DataAttributeProperties();
        props.setRequired(false);
        def.setProperties(props);
        StringAttributeContentV2 content = new StringAttributeContentV2();
        content.setData(value);
        attr.setContent(List.of(content));
        return attr;
    }

    private RequestAttributeV2 booleanAttr(String name, boolean value) {
        RequestAttributeV2 attr = new RequestAttributeV2();
        attr.setName(name);
        BooleanAttributeContentV2 content = new BooleanAttributeContentV2();
        content.setData(value);
        attr.setContent(List.of(content));
        return attr;
    }

    private MetadataAttributeV2 stringMetaAttr(String name, String value) {
        MetadataAttributeV2 attr = new MetadataAttributeV2();
        attr.setName(name);
        attr.setType(AttributeType.META);
        attr.setContentType(AttributeContentType.STRING);
        MetadataAttributeProperties props = new MetadataAttributeProperties();
        attr.setProperties(props);
        StringAttributeContentV2 content = new StringAttributeContentV2();
        content.setData(value);
        attr.setContent(List.of(content));
        return attr;
    }

    private CertificateResponse buildCertificateResponse() {
        CertificateResponse cr = new CertificateResponse();
        cr.setData("TEST_CERT_DATA".getBytes(StandardCharsets.UTF_8));
        return cr;
    }

    private UserDataVOWS buildUserDataVOWS(String username) {
        UserDataVOWS u = new UserDataVOWS();
        u.setUsername(username);
        u.setPassword("password");
        u.setSubjectDN("CN=" + username);
        u.setCaName("ManagementCA");
        return u;
    }

    // ── getUser (findUser) ────────────────────────────────────────────────────

    @Test
    void getUser_existingUser_returnsUser() throws Exception {
        UserDataVOWS expected = buildUserDataVOWS("testUser");
        given(ejbcaWS.findUser(any())).willReturn(List.of(expected));

        UserDataVOWS result = service.getUser(ejbcaWS, "testUser");

        assertNotNull(result);
        assertEquals("testUser", result.getUsername());
    }

    @Test
    void getUser_noUsers_returnsNull() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of());

        UserDataVOWS result = service.getUser(ejbcaWS, "unknownUser");

        assertNull(result);
    }

    @Test
    void getUser_nullList_returnsNull() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);

        UserDataVOWS result = service.getUser(ejbcaWS, "unknownUser");

        assertNull(result);
    }

    @Test
    void getUser_authDenied_throwsAccessDeniedException() throws Exception {
        given(ejbcaWS.findUser(any())).willThrow(authDeniedException());

        assertThrows(AccessDeniedException.class, () -> service.getUser(ejbcaWS, "testUser"));
    }

    @Test
    void getUser_endEntityProfileNotFound_throwsNotFoundException() throws Exception {
        given(ejbcaWS.findUser(any())).willThrow(endEntityProfileNotFoundException());

        assertThrows(NotFoundException.class, () -> service.getUser(ejbcaWS, "testUser"));
    }

    @Test
    void getUser_otherException_throwsIllegalStateException() throws Exception {
        given(ejbcaWS.findUser(any())).willThrow(new RuntimeException("unexpected"));

        assertThrows(IllegalStateException.class, () -> service.getUser(ejbcaWS, "testUser"));
    }

    // ── createEndEntity ───────────────────────────────────────────────────────

    @Test
    void createEndEntity_newUser_callsEditUser() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);

        assertDoesNotThrow(() -> service.createEndEntity(
                UUID, "newUser", "pass", "CN=newUser", "",
                buildRaProfileAttributes(), buildIssueAttributes()));
    }

    @Test
    void createEndEntity_existingUser_throwsAlreadyExistException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS("existingUser")));

        assertThrows(AlreadyExistException.class, () -> service.createEndEntity(
                UUID, "existingUser", "pass", "CN=existingUser", "",
                buildRaProfileAttributes(), buildIssueAttributes()));
    }

    @Test
    void createEndEntity_editUser_authDenied_throwsAccessDeniedException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);
        willThrow(authDeniedException()).given(ejbcaWS).editUser(any());

        assertThrows(AccessDeniedException.class, () -> service.createEndEntity(
                UUID, "newUser", "pass", "CN=newUser", "",
                buildRaProfileAttributes(), buildIssueAttributes()));
    }

    @Test
    void createEndEntity_editUser_caDoesntExist_throwsNotFoundException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);
        willThrow(caDoesntExistException()).given(ejbcaWS).editUser(any());

        assertThrows(NotFoundException.class, () -> service.createEndEntity(
                UUID, "newUser", "pass", "CN=newUser", "",
                buildRaProfileAttributes(), buildIssueAttributes()));
    }

    @Test
    void createEndEntity_editUser_userDoesntFullfill_throwsEjbcaException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);
        willThrow(userDoesntFullfillException()).given(ejbcaWS).editUser(any());

        assertThrows(EjbcaException.class, () -> service.createEndEntity(
                UUID, "newUser", "pass", "CN=newUser", "",
                buildRaProfileAttributes(), buildIssueAttributes()));
    }

    @Test
    void createEndEntity_editUser_otherException_throwsIllegalStateException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);
        willThrow(new RuntimeException("unexpected")).given(ejbcaWS).editUser(any());

        assertThrows(IllegalStateException.class, () -> service.createEndEntity(
                UUID, "newUser", "pass", "CN=newUser", "",
                buildRaProfileAttributes(), buildIssueAttributes()));
    }

    // ── createEndEntityWithMeta ───────────────────────────────────────────────

    @Test
    void createEndEntityWithMeta_newUser_callsEditUser() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);

        assertDoesNotThrow(() -> service.createEndEntityWithMeta(
                UUID, "newUser", "pass", "CN=newUser", "",
                buildRaProfileAttributes(), buildMetadataAttributes()));
    }

    @Test
    void createEndEntityWithMeta_existingUser_throwsAlreadyExistException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS("existingUser")));

        assertThrows(AlreadyExistException.class, () -> service.createEndEntityWithMeta(
                UUID, "existingUser", "pass", "CN=existingUser", "",
                buildRaProfileAttributes(), buildMetadataAttributes()));
    }

    @Test
    void createEndEntityWithMeta_editUser_authDenied_throwsAccessDeniedException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);
        willThrow(authDeniedException()).given(ejbcaWS).editUser(any());

        assertThrows(AccessDeniedException.class, () -> service.createEndEntityWithMeta(
                UUID, "newUser", "pass", "CN=newUser", "",
                buildRaProfileAttributes(), buildMetadataAttributes()));
    }

    @Test
    void createEndEntityWithMeta_editUser_caDoesntExist_throwsNotFoundException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);
        willThrow(caDoesntExistException()).given(ejbcaWS).editUser(any());

        assertThrows(NotFoundException.class, () -> service.createEndEntityWithMeta(
                UUID, "newUser", "pass", "CN=newUser", "",
                buildRaProfileAttributes(), buildMetadataAttributes()));
    }

    @Test
    void createEndEntityWithMeta_editUser_otherException_throwsIllegalStateException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);
        willThrow(new RuntimeException("unexpected")).given(ejbcaWS).editUser(any());

        assertThrows(IllegalStateException.class, () -> service.createEndEntityWithMeta(
                UUID, "newUser", "pass", "CN=newUser", "",
                buildRaProfileAttributes(), buildMetadataAttributes()));
    }

    // ── renewEndEntity ────────────────────────────────────────────────────────

    @Test
    void renewEndEntity_existingUser_callsEditUser() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS("existingUser")));

        assertDoesNotThrow(() -> service.renewEndEntity(UUID, "existingUser", "newPass", "CN=existingUser", ""));
    }

    @Test
    void renewEndEntity_noSuchUser_throwsNotFoundException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(null);

        assertThrows(NotFoundException.class, () -> service.renewEndEntity(UUID, "unknownUser", "newPass", "CN=unknownUser", ""));
    }

    @Test
    void renewEndEntity_editUser_authDenied_throwsAccessDeniedException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS("existingUser")));
        willThrow(authDeniedException()).given(ejbcaWS).editUser(any());

        assertThrows(AccessDeniedException.class, () -> service.renewEndEntity(UUID, "existingUser", "newPass", "CN=existingUser", ""));
    }

    @Test
    void renewEndEntity_editUser_caDoesntExist_throwsNotFoundException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS("existingUser")));
        willThrow(caDoesntExistException()).given(ejbcaWS).editUser(any());

        assertThrows(NotFoundException.class, () -> service.renewEndEntity(UUID, "existingUser", "newPass", "CN=existingUser", ""));
    }

    @Test
    void renewEndEntity_editUser_otherException_throwsIllegalStateException() throws Exception {
        given(ejbcaWS.findUser(any())).willReturn(List.of(buildUserDataVOWS("existingUser")));
        willThrow(new RuntimeException("unexpected")).given(ejbcaWS).editUser(any());

        assertThrows(IllegalStateException.class, () -> service.renewEndEntity(UUID, "existingUser", "newPass", "CN=existingUser", ""));
    }

    // ── issueCertificate (PKCS10) ─────────────────────────────────────────────

    @Test
    void issueCertificate_pkcs10_happyPath() throws Exception {
        given(ejbcaWS.pkcs10Request(any(), any(), any(), any(), any())).willReturn(buildCertificateResponse());

        CertificateDataResponseDto result = service.issueCertificate(UUID, "user", "pass", "CSRDATA", CertificateRequestFormat.PKCS10);

        assertNotNull(result);
        assertEquals("TEST_CERT_DATA", result.getCertificateData());
    }

    @Test
    void issueCertificate_pkcs10_authDenied_throwsAccessDeniedException() throws Exception {
        given(ejbcaWS.pkcs10Request(any(), any(), any(), any(), any())).willThrow(authDeniedException());

        assertThrows(AccessDeniedException.class, () -> service.issueCertificate(UUID, "user", "pass", "CSRDATA", CertificateRequestFormat.PKCS10));
    }

    @Test
    void issueCertificate_pkcs10_caDoesntExist_throwsNotFoundException() throws Exception {
        given(ejbcaWS.pkcs10Request(any(), any(), any(), any(), any())).willThrow(caDoesntExistException());

        assertThrows(NotFoundException.class, () -> service.issueCertificate(UUID, "user", "pass", "CSRDATA", CertificateRequestFormat.PKCS10));
    }

    @Test
    void issueCertificate_pkcs10_notFound_throwsNotFoundException() throws Exception {
        given(ejbcaWS.pkcs10Request(any(), any(), any(), any(), any())).willThrow(notFoundException());

        assertThrows(NotFoundException.class, () -> service.issueCertificate(UUID, "user", "pass", "CSRDATA", CertificateRequestFormat.PKCS10));
    }

    @Test
    void issueCertificate_pkcs10_otherException_throwsIllegalStateException() throws Exception {
        given(ejbcaWS.pkcs10Request(any(), any(), any(), any(), any())).willThrow(new RuntimeException("unexpected"));

        assertThrows(IllegalStateException.class, () -> service.issueCertificate(UUID, "user", "pass", "CSRDATA", CertificateRequestFormat.PKCS10));
    }

    // ── issueCertificate (CRMF) ───────────────────────────────────────────────

    @Test
    void issueCertificate_crmf_happyPath() throws Exception {
        given(ejbcaWS.crmfRequest(any(), any(), any(), any(), any())).willReturn(buildCertificateResponse());

        CertificateDataResponseDto result = service.issueCertificate(UUID, "user", "pass", "CRMFDATA", CertificateRequestFormat.CRMF);

        assertNotNull(result);
        assertEquals("TEST_CERT_DATA", result.getCertificateData());
    }

    @Test
    void issueCertificate_crmf_authDenied_throwsAccessDeniedException() throws Exception {
        given(ejbcaWS.crmfRequest(any(), any(), any(), any(), any())).willThrow(authDeniedException());

        assertThrows(AccessDeniedException.class, () -> service.issueCertificate(UUID, "user", "pass", "CRMFDATA", CertificateRequestFormat.CRMF));
    }

    @Test
    void issueCertificate_crmf_caDoesntExist_throwsNotFoundException() throws Exception {
        given(ejbcaWS.crmfRequest(any(), any(), any(), any(), any())).willThrow(caDoesntExistException());

        assertThrows(NotFoundException.class, () -> service.issueCertificate(UUID, "user", "pass", "CRMFDATA", CertificateRequestFormat.CRMF));
    }

    @Test
    void issueCertificate_crmf_notFound_throwsNotFoundException() throws Exception {
        given(ejbcaWS.crmfRequest(any(), any(), any(), any(), any())).willThrow(notFoundException());

        assertThrows(NotFoundException.class, () -> service.issueCertificate(UUID, "user", "pass", "CRMFDATA", CertificateRequestFormat.CRMF));
    }

    @Test
    void issueCertificate_crmf_otherException_throwsIllegalStateException() throws Exception {
        given(ejbcaWS.crmfRequest(any(), any(), any(), any(), any())).willThrow(new RuntimeException("unexpected"));

        assertThrows(IllegalStateException.class, () -> service.issueCertificate(UUID, "user", "pass", "CRMFDATA", CertificateRequestFormat.CRMF));
    }

    // ── revokeCertificate ─────────────────────────────────────────────────────

    @Test
    void revokeCertificate_happyPath() throws Exception {
        assertDoesNotThrow(() -> service.revokeCertificate(UUID, "CN=TestCA", "123abc", 0));
    }

    @Test
    void revokeCertificate_authDenied_throwsAccessDeniedException() throws Exception {
        willThrow(authDeniedException()).given(ejbcaWS).revokeCert(any(), any(), anyInt());

        assertThrows(AccessDeniedException.class, () -> service.revokeCertificate(UUID, "CN=TestCA", "123abc", 0));
    }

    @Test
    void revokeCertificate_caDoesntExist_throwsNotFoundException() throws Exception {
        willThrow(caDoesntExistException()).given(ejbcaWS).revokeCert(any(), any(), anyInt());

        assertThrows(NotFoundException.class, () -> service.revokeCertificate(UUID, "CN=TestCA", "123abc", 0));
    }

    @Test
    void revokeCertificate_notFound_throwsNotFoundException() throws Exception {
        willThrow(notFoundException()).given(ejbcaWS).revokeCert(any(), any(), anyInt());

        assertThrows(NotFoundException.class, () -> service.revokeCertificate(UUID, "CN=TestCA", "123abc", 0));
    }

    @Test
    void revokeCertificate_otherException_throwsIllegalStateException() throws Exception {
        willThrow(new RuntimeException("unexpected")).given(ejbcaWS).revokeCert(any(), any(), anyInt());

        assertThrows(IllegalStateException.class, () -> service.revokeCertificate(UUID, "CN=TestCA", "123abc", 0));
    }

    // ── getEjbcaVersion ───────────────────────────────────────────────────────

    @Test
    void getEjbcaVersion_happyPath() throws Exception {
        given(ejbcaWS.getEjbcaVersion()).willReturn("EJBCA 7.5.0 Enterprise");

        var result = service.getEjbcaVersion(UUID);

        assertNotNull(result);
        assertEquals(7, result.getTechVersion());
        assertEquals(5, result.getMajorVersion());
        assertEquals("Enterprise", result.getVersion());
    }

    // ── getAvailableCas ───────────────────────────────────────────────────────

    @Test
    void getAvailableCas_returnsList() throws Exception {
        NameAndId n = new NameAndId();
        n.setId(1);
        n.setName("ManagementCA");
        given(ejbcaWS.getAvailableCAs()).willReturn(List.of(n));

        List<NameAndIdDto> result = service.getAvailableCas(UUID);

        assertEquals(1, result.size());
        assertEquals("ManagementCA", result.get(0).getName());
    }

    @Test
    void getAvailableCas_emptyList_throwsNotFoundException() throws Exception {
        given(ejbcaWS.getAvailableCAs()).willReturn(List.of());

        assertThrows(NotFoundException.class, () -> service.getAvailableCas(UUID));
    }

    @Test
    void getAvailableCas_nullList_throwsNotFoundException() throws Exception {
        given(ejbcaWS.getAvailableCAs()).willReturn(null);

        assertThrows(NotFoundException.class, () -> service.getAvailableCas(UUID));
    }

    @Test
    void getAvailableCas_authDenied_throwsAccessDeniedException() throws Exception {
        given(ejbcaWS.getAvailableCAs()).willThrow(authDeniedException());

        assertThrows(AccessDeniedException.class, () -> service.getAvailableCas(UUID));
    }

    @Test
    void getAvailableCas_ejbcaException_throwsIllegalStateException() throws Exception {
        given(ejbcaWS.getAvailableCAs()).willThrow(ejbcaException());

        assertThrows(IllegalStateException.class, () -> service.getAvailableCas(UUID));
    }

    // ── getLastCAChain ────────────────────────────────────────────────────────

    @Test
    void getLastCAChain_happyPath() throws Exception {
        given(ejbcaWS.getLastCAChain("ManagementCA")).willReturn(List.of(new Certificate()));

        List<Certificate> result = service.getLastCAChain(UUID, "ManagementCA");

        assertEquals(1, result.size());
    }

    @Test
    void getLastCAChain_caDoesntExist_throwsNotFoundException() throws Exception {
        given(ejbcaWS.getLastCAChain(any())).willThrow(caDoesntExistException());

        assertThrows(NotFoundException.class, () -> service.getLastCAChain(UUID, "UnknownCA"));
    }

    @Test
    void getLastCAChain_ejbcaException_throwsIllegalStateException() throws Exception {
        given(ejbcaWS.getLastCAChain(any())).willThrow(ejbcaException());

        assertThrows(IllegalStateException.class, () -> service.getLastCAChain(UUID, "ManagementCA"));
    }

    @Test
    void getLastCAChain_authDenied_throwsAccessDeniedException() throws Exception {
        given(ejbcaWS.getLastCAChain(any())).willThrow(authDeniedException());

        assertThrows(AccessDeniedException.class, () -> service.getLastCAChain(UUID, "ManagementCA"));
    }

    // ── getLatestCRL ──────────────────────────────────────────────────────────

    @Test
    void getLatestCRL_happyPath() throws Exception {
        byte[] crl = "CRLDATA".getBytes(StandardCharsets.UTF_8);
        given(ejbcaWS.getLatestCRL("ManagementCA", false)).willReturn(crl);

        byte[] result = service.getLatestCRL(UUID, "ManagementCA", false);

        assertArrayEquals(crl, result);
    }

    @Test
    void getLatestCRL_caDoesntExist_throwsNotFoundException() throws Exception {
        given(ejbcaWS.getLatestCRL(any(), any(Boolean.class))).willThrow(caDoesntExistException());

        assertThrows(NotFoundException.class, () -> service.getLatestCRL(UUID, "UnknownCA", false));
    }

    @Test
    void getLatestCRL_ejbcaException_throwsIllegalStateException() throws Exception {
        given(ejbcaWS.getLatestCRL(any(), any(Boolean.class))).willThrow(ejbcaException());

        assertThrows(IllegalStateException.class, () -> service.getLatestCRL(UUID, "ManagementCA", false));
    }

    // ── searchCertificates (WireMock) ─────────────────────────────────────────

    @Test
    void searchCertificates_200_returnsParsedResponse() throws Exception {
        String responseJson = "{\"certificates\":[],\"pagination_summary\":{\"total_count\":0}}";
        wireMock.stubFor(post(urlEqualTo("/v2/certificate/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseJson)));

        WebClient webClient = WebClient.builder().build();
        given(authorityInstanceService.getRestApiConnection(UUID)).willReturn(webClient);

        String restUrl = wireMock.baseUrl();
        SearchCertificatesRestRequestV2 request = new SearchCertificatesRestRequestV2();

        SearchCertificatesRestResponseV2 result = service.searchCertificates(UUID, restUrl, request);

        assertNotNull(result);
        assertNotNull(result.getCertificates());
    }

    @Test
    void searchCertificates_500_throwsException() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/v2/certificate/search"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error_message\":\"Internal Server Error\"}")));

        WebClient webClient = WebClient.builder().build();
        given(authorityInstanceService.getRestApiConnection(UUID)).willReturn(webClient);

        String restUrl = wireMock.baseUrl();
        SearchCertificatesRestRequestV2 request = new SearchCertificatesRestRequestV2();

        assertThrows(IOException.class, () -> service.searchCertificates(UUID, restUrl, request));
    }

    @Test
    void searchCertificates_401_throwsException() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/v2/certificate/search"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error_message\":\"Unauthorized\"}")));

        WebClient webClient = WebClient.builder().build();
        given(authorityInstanceService.getRestApiConnection(UUID)).willReturn(webClient);

        String restUrl = wireMock.baseUrl();
        SearchCertificatesRestRequestV2 request = new SearchCertificatesRestRequestV2();

        assertThrows(IOException.class, () -> service.searchCertificates(UUID, restUrl, request));
    }

    @Test
    void searchCertificates_unknownAuthorityUuid_propagatesNotFoundException() throws Exception {
        // getRestApiConnection throws NotFoundException for an unknown UUID — must NOT be
        // wrapped into IOException (regression guard for the S112 refactor).
        given(authorityInstanceService.getRestApiConnection(UUID))
                .willThrow(new NotFoundException("AuthorityInstance", UUID));

        SearchCertificatesRestRequestV2 request = new SearchCertificatesRestRequestV2();

        assertThrows(NotFoundException.class,
                () -> service.searchCertificates(UUID, "http://irrelevant", request));
    }
}
