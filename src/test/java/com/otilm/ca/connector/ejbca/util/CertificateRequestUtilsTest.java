package com.otilm.ca.connector.ejbca.util;

import com.otilm.api.model.core.enums.CertificateRequestFormat;
import com.otilm.ca.connector.ejbca.request.CertificateRequest;
import com.otilm.ca.connector.ejbca.request.Pkcs10CertificateRequest;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.crmf.CertReqMessages;
import org.bouncycastle.asn1.crmf.CertReqMsg;
import org.bouncycastle.cert.crmf.jcajce.JcaCertificateRequestMessageBuilder;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.*;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CertificateRequestUtilsTest {

    private PKCS10CertificationRequest pkcs10CertificationRequest;
    private PKCS10CertificationRequest pkcs10NoSan;
    private KeyPair sharedKeyPair;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException, OperatorCreationException {
        // install BouncyCastle provider
        Security.addProvider(new BouncyCastleProvider());

        // generate RSA key pair
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");
        kpGen.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));
        sharedKeyPair = kpGen.generateKeyPair();

        X500Name subject = new X500Name("CN=Test");
        PKCS10CertificationRequestBuilder requestBuilder = new JcaPKCS10CertificationRequestBuilder(subject, sharedKeyPair.getPublic());
        ExtensionsGenerator extGen = new ExtensionsGenerator();
        extGen.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName(GeneralName.dNSName, "test.example.com")));
        Extensions extensions = extGen.generate();
        requestBuilder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensions);
        String sigAlg = "SHA256withRSA";
        ContentSigner signer = new JcaContentSignerBuilder(sigAlg).setProvider("BC").build(sharedKeyPair.getPrivate());
        pkcs10CertificationRequest = requestBuilder.build(signer);

        // CSR without SAN
        KeyPairGenerator kpGen2 = KeyPairGenerator.getInstance("RSA", "BC");
        kpGen2.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));
        KeyPair keyPair2 = kpGen2.generateKeyPair();
        ContentSigner signer2 = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(keyPair2.getPrivate());
        pkcs10NoSan = new JcaPKCS10CertificationRequestBuilder(new X500Name("CN=NoSan"), keyPair2.getPublic()).build(signer2);
    }

    @Test
    void test() throws IOException {
        CertificateRequest certificateRequest = CertificateRequestUtils.createCertificateRequest(pkcs10CertificationRequest.getEncoded(), CertificateRequestFormat.PKCS10);
        String ejbcaSanString = CertificateRequestUtils.getEjbcaSanExtension(certificateRequest);

        Assertions.assertEquals("dNSName=test.example.com", ejbcaSanString);
    }

    @Test
    void createCertificateRequest_pkcs10_returnsPkcs10Request() throws IOException {
        CertificateRequest request = CertificateRequestUtils.createCertificateRequest(
                pkcs10CertificationRequest.getEncoded(), CertificateRequestFormat.PKCS10);

        assertNotNull(request);
        assertEquals(CertificateRequestFormat.PKCS10, request.getFormat());
        assertInstanceOf(Pkcs10CertificateRequest.class, request);
    }

    @Test
    void createCertificateRequest_crmf_returnsCrmfRequest() throws Exception {
        // minimal valid CRMF using BouncyCastle
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");
        kpGen.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));
        KeyPair keyPair = kpGen.generateKeyPair();

        org.bouncycastle.cert.crmf.jcajce.JcaCertificateRequestMessageBuilder builder =
                new org.bouncycastle.cert.crmf.jcajce.JcaCertificateRequestMessageBuilder(java.math.BigInteger.ONE);
        builder.setPublicKey(keyPair.getPublic());
        builder.setSubject(new X500Name("CN=CrmfTest"));
        builder.setProofOfPossessionSigningKeySigner(
                new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(keyPair.getPrivate()));

        org.bouncycastle.asn1.crmf.CertReqMsg certReqMsg =
                org.bouncycastle.asn1.crmf.CertReqMsg.getInstance(builder.build().getEncoded());
        byte[] crmfBytes = new org.bouncycastle.asn1.crmf.CertReqMessages(certReqMsg).getEncoded();

        CertificateRequest request = CertificateRequestUtils.createCertificateRequest(crmfBytes, CertificateRequestFormat.CRMF);

        assertNotNull(request);
        assertEquals(CertificateRequestFormat.CRMF, request.getFormat());
    }

    @Test
    void getEjbcaSanExtension_crmf_readsCertTemplateSan() throws Exception {
        GeneralNames san = new GeneralNames(new GeneralName[]{
                new GeneralName(GeneralName.dNSName, "crmf.example.com"),
                new GeneralName(GeneralName.rfc822Name, "device@example.com")});
        CertificateRequest request = CertificateRequestUtils.createCertificateRequest(crmf(san), CertificateRequestFormat.CRMF);

        assertEquals("dNSName=crmf.example.com, rfc822name=device@example.com",
                CertificateRequestUtils.getEjbcaSanExtension(request));
    }

    @Test
    void getEjbcaSanExtension_crmfWithoutSan_returnsNull() throws Exception {
        CertificateRequest request = CertificateRequestUtils.createCertificateRequest(crmf(null), CertificateRequestFormat.CRMF);

        assertNull(CertificateRequestUtils.getEjbcaSanExtension(request));
    }

    private byte[] crmf(GeneralNames san) throws Exception {
        JcaCertificateRequestMessageBuilder builder = new JcaCertificateRequestMessageBuilder(BigInteger.ONE);
        builder.setPublicKey(sharedKeyPair.getPublic());
        builder.setSubject(new X500Name("CN=CrmfSan"));
        if (san != null) {
            builder.addExtension(Extension.subjectAlternativeName, false, san);
        }
        builder.setProofOfPossessionSigningKeySigner(
                new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(sharedKeyPair.getPrivate()));
        CertReqMsg certReqMsg = CertReqMsg.getInstance(builder.build().getEncoded());
        return new CertReqMessages(certReqMsg).getEncoded();
    }

    @Test
    void getEjbcaSanExtension_noSan_returnsNull() throws IOException {
        CertificateRequest request = CertificateRequestUtils.createCertificateRequest(
                pkcs10NoSan.getEncoded(), CertificateRequestFormat.PKCS10);

        String san = CertificateRequestUtils.getEjbcaSanExtension(request);

        assertNull(san);
    }

    @Test
    void getEjbcaSanExtension_nullRequest_returnsNull() throws IOException {
        assertNull(CertificateRequestUtils.getEjbcaSanExtension(null));
    }

    // ---- csrStringToJcaObject tests ----

    @Test
    void csrStringToJcaObject_withPemHeaders_parsesSuccessfully() throws Exception {
        String base64 = Base64.getEncoder().encodeToString(pkcs10CertificationRequest.getEncoded());
        String pemWithHeaders = "-----BEGIN CERTIFICATE REQUEST-----" + System.lineSeparator()
                + base64 + System.lineSeparator()
                + "-----END CERTIFICATE REQUEST-----";

        JcaPKCS10CertificationRequest result = CertificateRequestUtils.csrStringToJcaObject(pemWithHeaders);

        assertNotNull(result);
        assertNotNull(result.getPublicKey());
    }

    @Test
    void csrStringToJcaObject_withoutHeaders_parsesSuccessfully() throws Exception {
        String rawBase64 = Base64.getEncoder().encodeToString(pkcs10CertificationRequest.getEncoded());

        JcaPKCS10CertificationRequest result = CertificateRequestUtils.csrStringToJcaObject(rawBase64);

        assertNotNull(result);
        assertNotNull(result.getPublicKey());
    }

    // ---- extractSanFromCsr tests ----

    @Test
    void extractSanFromCsr_dnsName_returnsDnsBranch() throws Exception {
        JcaPKCS10CertificationRequest jcaReq = buildCsrWithSans(
                new GeneralName(GeneralName.dNSName, "dns.example.com"));

        List<String> sans = CertificateRequestUtils.extractSanFromCsr(jcaReq);

        assertFalse(sans.isEmpty());
        assertTrue(sans.stream().anyMatch(s -> s.startsWith("DNS:")));
    }

    @Test
    void extractSanFromCsr_ipAddress_returnsIpBranch() throws Exception {
        byte[] ipBytes = InetAddress.getByName("192.168.1.1").getAddress();
        JcaPKCS10CertificationRequest jcaReq = buildCsrWithSans(
                new GeneralName(GeneralName.iPAddress, new DEROctetString(ipBytes)));

        List<String> sans = CertificateRequestUtils.extractSanFromCsr(jcaReq);

        assertFalse(sans.isEmpty());
        assertTrue(sans.stream().anyMatch(s -> s.startsWith("IP Address:")));
    }

    @Test
    void extractSanFromCsr_otherName_returnsOtherNameBranch() throws Exception {
        // Build a minimal otherName: OID + value wrapped as DERSequence
        org.bouncycastle.asn1.ASN1ObjectIdentifier oid = new org.bouncycastle.asn1.ASN1ObjectIdentifier("1.3.6.1.4.1.99999.1");
        org.bouncycastle.asn1.ASN1Encodable value = new org.bouncycastle.asn1.DERTaggedObject(true, 0, new DERUTF8String("testValue"));
        GeneralName otherNameGn = new GeneralName(GeneralName.otherName, new DERSequence(new org.bouncycastle.asn1.ASN1Encodable[]{oid, value}));

        JcaPKCS10CertificationRequest jcaReq = buildCsrWithSans(otherNameGn);

        List<String> sans = CertificateRequestUtils.extractSanFromCsr(jcaReq);

        assertFalse(sans.isEmpty());
        assertTrue(sans.stream().anyMatch(s -> s.startsWith("Other Name:")));
    }

    @Test
    void extractSanFromCsr_noSan_returnsEmptyList() throws Exception {
        JcaPKCS10CertificationRequest jcaReq = new JcaPKCS10CertificationRequest(pkcs10NoSan.getEncoded());

        List<String> sans = CertificateRequestUtils.extractSanFromCsr(jcaReq);

        assertTrue(sans.isEmpty());
    }

    // ---- getEjbcaSanExtension: one case per GeneralName type ----
    // The expected strings are the format EJBCA's end-entity API accepts, recorded from the
    // previous implementation's output. The inconsistent casing is deliberate.

    @Test
    void getEjbcaSanExtension_ipv4_returnsDottedQuad() throws Exception {
        assertEquals("iPAddress=192.168.1.1", ejbcaSan(
                new GeneralName(GeneralName.iPAddress, new DEROctetString(InetAddress.getByName("192.168.1.1").getAddress()))));
    }

    @Test
    void getEjbcaSanExtension_ipv6_returnsUncompressedGroups() throws Exception {
        assertEquals("iPAddress=2001:db8:0:0:0:0:0:1", ejbcaSan(
                new GeneralName(GeneralName.iPAddress, new DEROctetString(InetAddress.getByName("2001:db8::1").getAddress()))));
    }

    @Test
    void getEjbcaSanExtension_rfc822Name_usesLowerCaseKey() throws Exception {
        assertEquals("rfc822name=user@example.com", ejbcaSan(
                new GeneralName(GeneralName.rfc822Name, "user@example.com")));
    }

    @Test
    void getEjbcaSanExtension_uri_usesUpperCaseKey() throws Exception {
        assertEquals("UNIFORMRESOURCEIDENTIFIER=https://example.com/x", ejbcaSan(
                new GeneralName(GeneralName.uniformResourceIdentifier, "https://example.com/x")));
    }

    @Test
    void getEjbcaSanExtension_directoryName_escapesCommas() throws Exception {
        assertEquals("DIRECTORYNAME=CN=Dir\\,O=Org", ejbcaSan(
                new GeneralName(GeneralName.directoryName, new X500Name("CN=Dir,O=Org"))));
    }

    @Test
    void getEjbcaSanExtension_registeredId_returnsOid() throws Exception {
        assertEquals("registeredID=1.2.3.4", ejbcaSan(
                new GeneralName(GeneralName.registeredID, new org.bouncycastle.asn1.ASN1ObjectIdentifier("1.2.3.4"))));
    }

    @Test
    void getEjbcaSanExtension_msUpn_returnsUpnKey() throws Exception {
        assertEquals("UPN=upn@example.com", ejbcaSan(otherName("1.3.6.1.4.1.311.20.2.3", "upn@example.com")));
    }

    @Test
    void getEjbcaSanExtension_xmppAddr_returnsXmppKey() throws Exception {
        assertEquals("XMPPADDR=user@xmpp.example", ejbcaSan(otherName("1.3.6.1.5.5.7.8.5", "user@xmpp.example")));
    }

    @Test
    void getEjbcaSanExtension_srvName_returnsSrvKey() throws Exception {
        assertEquals("SRVNAME=_svc.example.com", ejbcaSan(otherName("1.3.6.1.5.5.7.8.7", "_svc.example.com")));
    }

    @Test
    void getEjbcaSanExtension_unsupportedOtherName_isSkippedNotSerialisedAsNull() throws Exception {
        assertNull(ejbcaSan(otherName("1.2.3.4.5.6.7.8.9", "someValue")));
    }

    @Test
    void getEjbcaSanExtension_unsupportedType_isSkipped() throws Exception {
        assertNull(ejbcaSan(new GeneralName(GeneralName.x400Address, new DERSequence())));
    }

    @Test
    void getEjbcaSanExtension_multipleNames_joinsPreservingOrder() throws Exception {
        JcaPKCS10CertificationRequest csr = buildCsrWithSans(
                new GeneralName(GeneralName.dNSName, "a.example.com"),
                new GeneralName(GeneralName.rfc822Name, "u@example.com"),
                new GeneralName(GeneralName.iPAddress, new DEROctetString(InetAddress.getByName("10.0.0.1").getAddress())),
                new GeneralName(GeneralName.dNSName, "b.example.com"));
        CertificateRequest request = CertificateRequestUtils.createCertificateRequest(csr.getEncoded(), CertificateRequestFormat.PKCS10);

        assertEquals("dNSName=a.example.com, rfc822name=u@example.com, iPAddress=10.0.0.1, dNSName=b.example.com",
                CertificateRequestUtils.getEjbcaSanExtension(request));
    }

    @Test
    void getEjbcaSanExtension_supportedNameAmongUnsupported_keepsTheSupportedOne() throws Exception {
        JcaPKCS10CertificationRequest csr = buildCsrWithSans(
                new GeneralName(GeneralName.x400Address, new DERSequence()),
                new GeneralName(GeneralName.dNSName, "kept.example.com"));
        CertificateRequest request = CertificateRequestUtils.createCertificateRequest(csr.getEncoded(), CertificateRequestFormat.PKCS10);

        assertEquals("dNSName=kept.example.com", CertificateRequestUtils.getEjbcaSanExtension(request));
    }

    @Test
    void getEjbcaSanExtension_crmfFormat_returnsNull() throws Exception {
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");
        kpGen.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));
        KeyPair keyPair = kpGen.generateKeyPair();
        org.bouncycastle.cert.crmf.jcajce.JcaCertificateRequestMessageBuilder builder =
                new org.bouncycastle.cert.crmf.jcajce.JcaCertificateRequestMessageBuilder(java.math.BigInteger.ONE);
        builder.setPublicKey(keyPair.getPublic());
        builder.setSubject(new X500Name("CN=CrmfSan"));
        builder.setProofOfPossessionSigningKeySigner(
                new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(keyPair.getPrivate()));
        org.bouncycastle.asn1.crmf.CertReqMsg certReqMsg =
                org.bouncycastle.asn1.crmf.CertReqMsg.getInstance(builder.build().getEncoded());
        byte[] crmfBytes = new org.bouncycastle.asn1.crmf.CertReqMessages(certReqMsg).getEncoded();

        CertificateRequest request = CertificateRequestUtils.createCertificateRequest(crmfBytes, CertificateRequestFormat.CRMF);

        assertNull(CertificateRequestUtils.getEjbcaSanExtension(request));
    }


    // ---- escaping: EJBCA's parser is comma/plus delimited, so values must be escaped ----

    @Test
    void getEjbcaSanExtension_uriWithComma_escapesTheComma() throws Exception {
        assertEquals("UNIFORMRESOURCEIDENTIFIER=https://example.test/a\\,b", ejbcaSan(
                new GeneralName(GeneralName.uniformResourceIdentifier, "https://example.test/a,b")));
    }

    @Test
    void getEjbcaSanExtension_emailWithPlus_escapesThePlus() throws Exception {
        assertEquals("rfc822name=user\\+tag@example.com", ejbcaSan(
                new GeneralName(GeneralName.rfc822Name, "user+tag@example.com")));
    }

    @Test
    void getEjbcaSanExtension_dnsWithSpecialCharacters_escapesThemButNotEquals() throws Exception {
        assertEquals("dNSName=a\\,b\\+c\\\\d\\\"e\\;f\\<g\\>h=i", ejbcaSan(
                new GeneralName(GeneralName.dNSName, "a,b+c\\d\"e;f<g>h=i")));
    }

    @Test
    void getEjbcaSanExtension_leadingHash_isEscaped() throws Exception {
        assertEquals("dNSName=\\#value", ejbcaSan(new GeneralName(GeneralName.dNSName, "#value")));
    }

    @Test
    void getEjbcaSanExtension_permanentIdentifier_returnsValueAndAssigner() throws Exception {
        GeneralName gn = new GeneralName(GeneralName.otherName, new DERSequence(new org.bouncycastle.asn1.ASN1Encodable[]{
                new org.bouncycastle.asn1.ASN1ObjectIdentifier("1.3.6.1.5.5.7.8.3"),
                new org.bouncycastle.asn1.DERTaggedObject(true, 0, new DERSequence(new org.bouncycastle.asn1.ASN1Encodable[]{
                        new DERUTF8String("ID-123"), new org.bouncycastle.asn1.ASN1ObjectIdentifier("1.2.3.4")}))}));

        assertEquals("PERMANENTIDENTIFIER=ID-123/1.2.3.4", ejbcaSan(gn));
    }

    @Test
    void getEjbcaSanExtension_permanentIdentifierWithoutAssigner_returnsTrailingSlash() throws Exception {
        GeneralName gn = new GeneralName(GeneralName.otherName, new DERSequence(new org.bouncycastle.asn1.ASN1Encodable[]{
                new org.bouncycastle.asn1.ASN1ObjectIdentifier("1.3.6.1.5.5.7.8.3"),
                new org.bouncycastle.asn1.DERTaggedObject(true, 0, new DERSequence(
                        new org.bouncycastle.asn1.ASN1Encodable[]{new DERUTF8String("ID-123")}))}));

        assertEquals("PERMANENTIDENTIFIER=ID-123/", ejbcaSan(gn));
    }

    @Test
    void getEjbcaSanExtension_malformedIpAddress_isSkippedNotSerialisedAsNull() throws Exception {
        assertNull(ejbcaSan(new GeneralName(GeneralName.iPAddress, new DEROctetString(new byte[]{1, 2, 3}))));
    }

    // ---- helpers ----

    private String ejbcaSan(GeneralName generalName) throws Exception {
        JcaPKCS10CertificationRequest csr = buildCsrWithSans(generalName);
        CertificateRequest request = CertificateRequestUtils.createCertificateRequest(csr.getEncoded(), CertificateRequestFormat.PKCS10);
        return CertificateRequestUtils.getEjbcaSanExtension(request);
    }

    private static GeneralName otherName(String oid, String value) {
        return new GeneralName(GeneralName.otherName, new DERSequence(new org.bouncycastle.asn1.ASN1Encodable[]{
                new org.bouncycastle.asn1.ASN1ObjectIdentifier(oid),
                new org.bouncycastle.asn1.DERTaggedObject(true, 0, new DERUTF8String(value))}));
    }

    private JcaPKCS10CertificationRequest buildCsrWithSans(GeneralName... generalNames)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException,
            IOException, OperatorCreationException {
        X500Name subject = new X500Name("CN=SanTest");
        PKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(subject, sharedKeyPair.getPublic());
        ExtensionsGenerator extGen = new ExtensionsGenerator();
        extGen.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(generalNames));
        builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(sharedKeyPair.getPrivate());
        return new JcaPKCS10CertificationRequest(builder.build(signer).getEncoded());
    }
}
