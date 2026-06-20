package com.otilm.ca.connector.ejbca.service.impl;

import com.otilm.api.exception.NotFoundException;
import com.otilm.api.exception.ValidationException;
import com.otilm.api.model.client.attribute.RequestAttribute;
import com.otilm.api.model.client.attribute.RequestAttributeV2;
import com.otilm.api.model.common.NameAndIdDto;
import com.otilm.api.model.common.attribute.common.AttributeType;
import com.otilm.api.model.common.attribute.common.MetadataAttribute;
import com.otilm.api.model.common.attribute.common.content.AttributeContentType;
import com.otilm.api.model.common.attribute.common.properties.DataAttributeProperties;
import com.otilm.api.model.common.attribute.v2.DataAttributeV2;
import com.otilm.api.model.common.attribute.v2.MetadataAttributeV2;
import com.otilm.api.model.common.attribute.v2.content.StringAttributeContentV2;
import com.otilm.api.model.connector.v2.CertRevocationDto;
import com.otilm.api.model.connector.v2.CertificateDataResponseDto;
import com.otilm.api.model.connector.v2.CertificateRenewRequestDto;
import com.otilm.api.model.connector.v2.CertificateSignRequestDto;
import com.otilm.api.model.core.authority.CertificateRevocationReason;
import com.otilm.api.model.core.enums.CertificateRequestFormat;
import com.otilm.ca.connector.ejbca.api.AuthorityInstanceControllerImpl;
import com.otilm.ca.connector.ejbca.api.CertificateControllerImpl;
import com.otilm.ca.connector.ejbca.dto.ejbca.response.CertificateRestResponseV2;
import com.otilm.ca.connector.ejbca.dto.ejbca.response.SearchCertificatesRestResponseV2;
import com.otilm.ca.connector.ejbca.service.AuthorityInstanceService;
import com.otilm.ca.connector.ejbca.service.EjbcaService;
import com.otilm.ca.connector.ejbca.util.LocalAttributeUtil;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CertificateEjbcaServiceImplTest {

    @InjectMocks
    private CertificateEjbcaServiceImpl certificateEjbcaServiceImpl;

    @Mock
    private AuthorityInstanceService authorityInstanceService;

    @Mock
    private EjbcaService ejbcaService;

    // A well-known self-signed cert used across revoke / identify tests (CN=www.example.com)
    private static final String SAMPLE_CERT =
            "MIIC1TCCAb2gAwIBAgIJANQeIhz8h9A3MA0GCSqGSIb3DQEBBQUAMBoxGDAWBgNVBAMTD3d3dy5leGFtcGxlLmNvbTAeFw0yMjAxMDEwOTUwMDhaFw0zMTEyMzAwOTUwMDhaMBoxGDAWBgNVBAMTD3d3dy5leGFtcGxlLmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALG+wuvOrdMjD5nwhLwmd2+FcO0htFcMi4/Ciu1/9NlHjy55JO+poBih+3JnaJ+u+BY/GCTjbn3RGvC8y2J+1RuAalU0252R0lSWOC2SqUOvUMtOJTufbr/jW0xYk2UePqPj4FX3h3zK3Byw8UaQuUmr9n9acTwyD0oYcxutFm4FqjRZ88eCm7EqNZm+52DmJBHokZPd/z+PLuN6X+Yog5DHS9E1VodHLVVcf3/9KTb3jFhKfNM9y/4pwclRKU1KbjSLStVZmGP3etYYYcFjPswy7zPgWtE8waprQxSJo+Cdqb7+16m69UjaJ1B507xhN8LUjdzZfRJVjSjiP3VKOtMCAwEAAaMeMBwwGgYDVR0RBBMwEYIPd3d3LmV4YW1wbGUuY29tMA0GCSqGSIb3DQEBBQUAA4IBAQAiUmsCNTv/pAxbAB8R9xlarMV/dL42slWJ7bI2e3e03GycVP3eajCfkEKG6XB7aaX4Epn0/jRpEPfplRXkXrxNZ8/bwkwlNN5CiziUcyqVANFC8r/GVlcg+n2+hvu7ZLXmGqBvAJBsbLuvdBKo2iqF4R3BklScDVAHhuXTYwPXd3n7iHEYnuxnGo5yshm6vZ7FKPyIroN9bFc0llJ/n5r4h8WNqaN77M6TycZm4Dlw6EGGM8Bk+IrcRoNE1JLdhIOm3YI5g1zwCprXJ4L+3X6IC20tJUK4PpMGAAdS6ak4/Sq3UM+JxF7oZ2fRCIJrKyfsN3rridYJe0tg5bQnkqmQ";

    // Base64-encoded PKCS#10 CSR with CN=TestUser and SAN=dNSName:test.example.com, generated once
    private static String CSR_WITH_SAN_BASE64;
    // Base64-encoded PKCS#10 CSR with CN=TestUser and no SAN
    private static String CSR_NO_SAN_BASE64;

    @BeforeAll
    static void generateCsrs() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");
        kpGen.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));
        KeyPair keyPair = kpGen.generateKeyPair();

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(keyPair.getPrivate());

        // CSR with CN and SAN
        X500Name subject = new X500Name("CN=TestUser");
        PKCS10CertificationRequestBuilder builderWithSan = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
        ExtensionsGenerator extGen = new ExtensionsGenerator();
        extGen.addExtension(Extension.subjectAlternativeName, false,
                new GeneralNames(new GeneralName(GeneralName.dNSName, "test.example.com")));
        builderWithSan.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate());
        PKCS10CertificationRequest csrWithSan = builderWithSan.build(signer);
        CSR_WITH_SAN_BASE64 = Base64.getEncoder().encodeToString(csrWithSan.getEncoded());

        // CSR with CN but no SAN
        KeyPairGenerator kpGen2 = KeyPairGenerator.getInstance("RSA", "BC");
        kpGen2.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));
        KeyPair keyPair2 = kpGen2.generateKeyPair();
        ContentSigner signer2 = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(keyPair2.getPrivate());
        PKCS10CertificationRequest csrNoSan = new JcaPKCS10CertificationRequestBuilder(new X500Name("CN=TestUser"), keyPair2.getPublic()).build(signer2);
        CSR_NO_SAN_BASE64 = Base64.getEncoder().encodeToString(csrNoSan.getEncoded());
    }

    // ── issueCertificate ─────────────────────────────────────────────────────

    @Test
    void issueCertificate_randomUsername_returnsCertWithMeta() throws Exception {
        String uuid = "auth-uuid-1";
        CertificateSignRequestDto request = buildSignRequest(
                CSR_WITH_SAN_BASE64, "RANDOM", "", "", "user@example.com", "dNSName=test.example.com", "");

        CertificateDataResponseDto certDto = new CertificateDataResponseDto();
        given(ejbcaService.issueCertificate(anyString(), anyString(), anyString(), anyString(), any()))
                .willReturn(certDto);

        CertificateDataResponseDto result = certificateEjbcaServiceImpl.issueCertificate(uuid, request);

        assertNotNull(result);
        assertNotNull(result.getMeta());
        // meta contains ejbcaUsername + email + san (no extension)
        assertEquals(3, result.getMeta().size());
        assertTrue(result.getMeta().stream().anyMatch(m -> CertificateEjbcaServiceImpl.META_EMAIL.equals(m.getName())));
        assertTrue(result.getMeta().stream().anyMatch(m -> CertificateEjbcaServiceImpl.META_SAN.equals(m.getName())));
        assertTrue(result.getMeta().stream().anyMatch(m -> "ejbcaUsername".equals(m.getName())));
        verify(ejbcaService).createEndEntity(anyString(), anyString(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void issueCertificate_cnUsername_returnsCertWithUsernameFromCn() throws Exception {
        String uuid = "auth-uuid-2";
        CertificateSignRequestDto request = buildSignRequest(
                CSR_WITH_SAN_BASE64, "CN", "pre-", "-post", "", "", "ext-data");

        CertificateDataResponseDto certDto = new CertificateDataResponseDto();
        given(ejbcaService.issueCertificate(anyString(), anyString(), anyString(), anyString(), any()))
                .willReturn(certDto);

        CertificateDataResponseDto result = certificateEjbcaServiceImpl.issueCertificate(uuid, request);

        assertNotNull(result);
        // username is pre-TestUser-post; meta contains ejbcaUsername + extension (no email, no san in blanks)
        List<MetadataAttribute> meta = result.getMeta();
        assertNotNull(meta);
        assertTrue(meta.stream().anyMatch(m -> "ejbcaUsername".equals(m.getName())));
        assertTrue(meta.stream().anyMatch(m -> CertificateEjbcaServiceImpl.META_EXTENSION.equals(m.getName())));
        verify(ejbcaService).createEndEntity(anyString(), anyString(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void issueCertificate_unsupportedMethod_throwsException() {
        String uuid = "auth-uuid-3";
        CertificateSignRequestDto request = buildSignRequest(
                CSR_WITH_SAN_BASE64, "UNSUPPORTED_METHOD", "", "", "", "", "");

        assertThrows(IOException.class, () -> certificateEjbcaServiceImpl.issueCertificate(uuid, request));
    }

    @Test
    void issueCertificate_allMetaFields_returnsAllFour() throws Exception {
        String uuid = "auth-uuid-4";
        CertificateSignRequestDto request = buildSignRequest(
                CSR_WITH_SAN_BASE64, "RANDOM", "", "", "user@example.com", "dNSName=test.example.com", "extdata");

        CertificateDataResponseDto certDto = new CertificateDataResponseDto();
        given(ejbcaService.issueCertificate(anyString(), anyString(), anyString(), anyString(), any()))
                .willReturn(certDto);

        CertificateDataResponseDto result = certificateEjbcaServiceImpl.issueCertificate(uuid, request);

        List<MetadataAttribute> meta = result.getMeta();
        assertEquals(4, meta.size());
        assertTrue(meta.stream().anyMatch(m -> CertificateEjbcaServiceImpl.META_EMAIL.equals(m.getName())));
        assertTrue(meta.stream().anyMatch(m -> CertificateEjbcaServiceImpl.META_SAN.equals(m.getName())));
        assertTrue(meta.stream().anyMatch(m -> CertificateEjbcaServiceImpl.META_EXTENSION.equals(m.getName())));
        assertTrue(meta.stream().anyMatch(m -> "ejbcaUsername".equals(m.getName())));
    }

    @Test
    void issueCertificate_cnWithPrefixAndPostfix_usernameContainsBoth() throws Exception {
        String uuid = "auth-uuid-5";
        CertificateSignRequestDto request = buildSignRequest(
                CSR_NO_SAN_BASE64, "CN", "PRE-", "-POST", "", "", "");

        CertificateDataResponseDto certDto = new CertificateDataResponseDto();
        given(ejbcaService.issueCertificate(anyString(), anyString(), anyString(), anyString(), any()))
                .willReturn(certDto);

        CertificateDataResponseDto result = certificateEjbcaServiceImpl.issueCertificate(uuid, request);

        MetadataAttribute usernameAttr = result.getMeta().stream()
                .filter(m -> "ejbcaUsername".equals(m.getName()))
                .findFirst()
                .orElseThrow();
        @SuppressWarnings("unchecked")
        List<StringAttributeContentV2> usernameContent = (List<StringAttributeContentV2>) usernameAttr.getContent();
        String usernameValue = usernameContent.get(0).getData();
        assertTrue(usernameValue.startsWith("PRE-"), "Username should start with PRE-");
        assertTrue(usernameValue.endsWith("-POST"), "Username should end with -POST");
        assertTrue(usernameValue.contains("TestUser"), "Username should contain CN value TestUser");
    }

    // ── renewCertificate ─────────────────────────────────────────────────────

    @Test
    void renewCertificate_withExistingEjbcaUsername_renewsEndEntity() throws Exception {
        String uuid = "renew-uuid-1";
        CertificateRenewRequestDto request = buildRenewRequest(CSR_WITH_SAN_BASE64, "existingUser", "RANDOM", "", "");

        CertificateDataResponseDto certDto = new CertificateDataResponseDto();
        given(ejbcaService.issueCertificate(anyString(), anyString(), anyString(), anyString(), any()))
                .willReturn(certDto);

        CertificateDataResponseDto result = certificateEjbcaServiceImpl.renewCertificate(uuid, request);

        assertNotNull(result);
        verify(ejbcaService).renewEndEntity(anyString(), anyString(), anyString(), anyString(), anyString());
        List<MetadataAttribute> meta = result.getMeta();
        assertNotNull(meta);
        assertTrue(meta.stream().anyMatch(m -> "ejbcaUsername".equals(m.getName())));
    }

    @Test
    void renewCertificate_withoutEjbcaUsername_generatesUsername() throws Exception {
        String uuid = "renew-uuid-2";
        // meta has no ejbcaUsername entry → service generates one
        CertificateRenewRequestDto request = buildRenewRequestNoUsername(CSR_WITH_SAN_BASE64, "RANDOM", "", "");

        CertificateDataResponseDto certDto = new CertificateDataResponseDto();
        given(ejbcaService.issueCertificate(anyString(), anyString(), anyString(), anyString(), any()))
                .willReturn(certDto);

        CertificateDataResponseDto result = certificateEjbcaServiceImpl.renewCertificate(uuid, request);

        assertNotNull(result);
        verify(ejbcaService).renewEndEntity(anyString(), anyString(), anyString(), anyString(), anyString());
        List<MetadataAttribute> meta = result.getMeta();
        assertTrue(meta.stream().anyMatch(m -> "ejbcaUsername".equals(m.getName())));
    }

    @Test
    void renewCertificate_renewEndEntityThrowsNotFoundException_fallsBackToCreate() throws Exception {
        String uuid = "renew-uuid-3";
        CertificateRenewRequestDto request = buildRenewRequest(CSR_WITH_SAN_BASE64, "existingUser", "RANDOM", "", "");

        CertificateDataResponseDto certDto = new CertificateDataResponseDto();
        given(ejbcaService.issueCertificate(anyString(), anyString(), anyString(), anyString(), any()))
                .willReturn(certDto);
        willThrow(new NotFoundException()).given(ejbcaService).renewEndEntity(anyString(), anyString(), anyString(), anyString(), anyString());

        CertificateDataResponseDto result = certificateEjbcaServiceImpl.renewCertificate(uuid, request);

        assertNotNull(result);
        verify(ejbcaService).renewEndEntity(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(ejbcaService).createEndEntityWithMeta(anyString(), anyString(), anyString(), anyString(), anyString(), any(), any());
    }

    // ── revokeCertificate ────────────────────────────────────────────────────

    @Test
    void revokeCertificate_validCert_callsEjbcaRevoke() throws Exception {
        String uuid = "revoke-uuid-1";
        CertRevocationDto request = new CertRevocationDto();
        request.setCertificate(SAMPLE_CERT);
        request.setReason(CertificateRevocationReason.KEY_COMPROMISE);

        certificateEjbcaServiceImpl.revokeCertificate(uuid, request);

        verify(ejbcaService).revokeCertificate(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void revokeCertificate_invalidCert_throwsIllegalStateException() {
        String uuid = "revoke-uuid-2";
        CertRevocationDto request = new CertRevocationDto();
        request.setCertificate("not-a-valid-certificate");
        request.setReason(CertificateRevocationReason.UNSPECIFIED);

        assertThrows(IllegalStateException.class, () -> certificateEjbcaServiceImpl.revokeCertificate(uuid, request));
    }

    // ── identifyCertificate ──────────────────────────────────────────────────

    @Test
    void identifyCertificate_NotKnown() throws Exception {
        String uuid = "dde2cccc-616f-11ec-90d6-0242ac120003";
        given(authorityInstanceService.getRestApiUrl(uuid)).willReturn("https://ejbca.example.com:8443/ejbca/rest");
        given(ejbcaService.searchCertificates(anyString(), any(), any())).willReturn(getSearchCertificatesRestResponseV2_NoCertificates());

        assertThrows(NotFoundException.class, () -> certificateEjbcaServiceImpl.identifyCertificate(uuid, getCertificateIdentificationRequestDto(12345, "Test")));
    }

    @Test
    void identifyCertificate_WrongProfile() throws Exception {
        String uuid = "dde2cccc-616f-11ec-90d6-0242ac120003";
        given(authorityInstanceService.getRestApiUrl(uuid)).willReturn("https://ejbca.example.com:8443/ejbca/rest");
        given(ejbcaService.searchCertificates(anyString(), any(), any())).willReturn(getSearchCertificatesRestResponseV2_Certificate(12345, "TestProfile"));

        assertThrows(ValidationException.class, () -> certificateEjbcaServiceImpl.identifyCertificate(uuid, getCertificateIdentificationRequestDto(98765, "TestProfile")));
    }

    @Test
    void identifyCertificate_Ok() throws Exception {
        String uuid = "dde2cccc-616f-11ec-90d6-0242ac120003";
        given(authorityInstanceService.getRestApiUrl(uuid)).willReturn("https://ejbca.example.com:8443/ejbca/rest");
        given(ejbcaService.searchCertificates(anyString(), any(), any())).willReturn(getSearchCertificatesRestResponseV2_Certificate(123456, "Test"));

        com.otilm.api.model.connector.v2.CertificateIdentificationResponseDto dto =
                certificateEjbcaServiceImpl.identifyCertificate(uuid, getCertificateIdentificationRequestDto(123456, "Test"));
        assertEquals(1, dto.getMeta().size());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CertificateSignRequestDto buildSignRequest(
            String csrBase64, String genMethod, String prefix, String postfix,
            String email, String san, String extension) {
        CertificateSignRequestDto dto = new CertificateSignRequestDto();
        dto.setRequest(csrBase64);
        dto.setFormat(CertificateRequestFormat.PKCS10);

        List<RequestAttribute> raAttrs = new ArrayList<>();
        raAttrs.add(stringAttr(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_GEN_METHOD, genMethod));
        raAttrs.add(stringAttr(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_PREFIX, prefix));
        raAttrs.add(stringAttr(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_POSTFIX, postfix));
        dto.setRaProfileAttributes(raAttrs);

        List<RequestAttribute> issueAttrs = new ArrayList<>();
        issueAttrs.add(stringAttr(CertificateControllerImpl.ATTRIBUTE_EMAIL, email));
        issueAttrs.add(stringAttr(CertificateControllerImpl.ATTRIBUTE_SAN, san));
        issueAttrs.add(stringAttr(CertificateControllerImpl.ATTRIBUTE_EXTENSION, extension));
        dto.setAttributes(issueAttrs);

        return dto;
    }

    private CertificateRenewRequestDto buildRenewRequest(
            String csrBase64, String ejbcaUsername, String genMethod, String prefix, String postfix) {
        CertificateRenewRequestDto dto = new CertificateRenewRequestDto();
        dto.setRequest(csrBase64);
        dto.setFormat(CertificateRequestFormat.PKCS10);

        List<RequestAttribute> raAttrs = new ArrayList<>();
        raAttrs.add(stringAttr(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_GEN_METHOD, genMethod));
        raAttrs.add(stringAttr(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_PREFIX, prefix));
        raAttrs.add(stringAttr(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_POSTFIX, postfix));
        dto.setRaProfileAttributes(raAttrs);

        List<MetadataAttribute> meta = new ArrayList<>();
        meta.add(stringMetaAttr("ejbcaUsername", ejbcaUsername));
        dto.setMeta(meta);

        return dto;
    }

    private CertificateRenewRequestDto buildRenewRequestNoUsername(
            String csrBase64, String genMethod, String prefix, String postfix) {
        CertificateRenewRequestDto dto = new CertificateRenewRequestDto();
        dto.setRequest(csrBase64);
        dto.setFormat(CertificateRequestFormat.PKCS10);

        List<RequestAttribute> raAttrs = new ArrayList<>();
        raAttrs.add(stringAttr(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_GEN_METHOD, genMethod));
        raAttrs.add(stringAttr(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_PREFIX, prefix));
        raAttrs.add(stringAttr(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_POSTFIX, postfix));
        dto.setRaProfileAttributes(raAttrs);

        // empty meta — no ejbcaUsername key at all
        dto.setMeta(new ArrayList<>());

        return dto;
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

    private MetadataAttributeV2 stringMetaAttr(String name, String value) {
        MetadataAttributeV2 attr = new MetadataAttributeV2();
        attr.setName(name);
        attr.setType(AttributeType.META);
        attr.setContentType(AttributeContentType.STRING);
        StringAttributeContentV2 content = new StringAttributeContentV2();
        content.setData(value);
        attr.setContent(List.of(content));
        return attr;
    }

    private com.otilm.api.model.connector.v2.CertificateIdentificationRequestDto getCertificateIdentificationRequestDto(int profileId, String profileName) {
        com.otilm.api.model.connector.v2.CertificateIdentificationRequestDto dto =
                new com.otilm.api.model.connector.v2.CertificateIdentificationRequestDto();
        List<RequestAttribute> attrs = new java.util.ArrayList<>();
        attrs.add(getEndEntityProfileRequestAttributeV2(profileId, profileName));
        attrs.add(getCertificateProfileRequestAttributeV2(profileId, profileName));
        dto.setRaProfileAttributes(attrs);
        dto.setCertificate(SAMPLE_CERT);
        return dto;
    }

    private RequestAttributeV2 getEndEntityProfileRequestAttributeV2(int profileId, String profileName) {
        RequestAttributeV2 dto = new RequestAttributeV2();
        dto.setName(AuthorityInstanceControllerImpl.ATTRIBUTE_END_ENTITY_PROFILE);
        NameAndIdDto nameAndIdDto = new NameAndIdDto(profileId, profileName);
        dto.setContent(LocalAttributeUtil.convertFromNameAndIdToBase(List.of(nameAndIdDto)));
        return dto;
    }

    private RequestAttributeV2 getCertificateProfileRequestAttributeV2(int profileId, String profileName) {
        RequestAttributeV2 dto = new RequestAttributeV2();
        dto.setName(AuthorityInstanceControllerImpl.ATTRIBUTE_CERTIFICATE_PROFILE);
        NameAndIdDto nameAndIdDto = new NameAndIdDto(profileId, profileName);
        dto.setContent(LocalAttributeUtil.convertFromNameAndIdToBase(List.of(nameAndIdDto)));
        return dto;
    }

    private SearchCertificatesRestResponseV2 getSearchCertificatesRestResponseV2_NoCertificates() {
        SearchCertificatesRestResponseV2 dto = new SearchCertificatesRestResponseV2();
        dto.setCertificates(List.of());
        return dto;
    }

    private SearchCertificatesRestResponseV2 getSearchCertificatesRestResponseV2_Certificate(int profileId, String username) {
        SearchCertificatesRestResponseV2 dto = new SearchCertificatesRestResponseV2();
        CertificateRestResponseV2 certificateRestResponseV2 = CertificateRestResponseV2.builder()
                .setCertificateProfileId(profileId)
                .setEndEntityProfileId(profileId)
                .setUsername(username)
                .build();
        dto.setCertificates(List.of(certificateRestResponseV2));
        return dto;
    }
}
