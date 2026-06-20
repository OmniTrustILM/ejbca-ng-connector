package com.otilm.ca.connector.ejbca.util;

import com.otilm.api.model.core.enums.CertificateRequestFormat;
import com.otilm.ca.connector.ejbca.request.CertificateRequest;
import com.otilm.ca.connector.ejbca.request.Pkcs10CertificateRequest;
import org.bouncycastle.asn1.DEROctetString;
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
        JcaPKCS10CertificationRequest jcaReq = buildCsrWithSan(
                new GeneralName(GeneralName.dNSName, "dns.example.com"));

        List<String> sans = CertificateRequestUtils.extractSanFromCsr(jcaReq);

        assertFalse(sans.isEmpty());
        assertTrue(sans.stream().anyMatch(s -> s.startsWith("DNS:")));
    }

    @Test
    void extractSanFromCsr_ipAddress_returnsIpBranch() throws Exception {
        byte[] ipBytes = InetAddress.getByName("192.168.1.1").getAddress();
        JcaPKCS10CertificationRequest jcaReq = buildCsrWithSan(
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

        JcaPKCS10CertificationRequest jcaReq = buildCsrWithSan(otherNameGn);

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

    // ---- helper ----

    private JcaPKCS10CertificationRequest buildCsrWithSan(GeneralName generalName)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException,
            IOException, OperatorCreationException {
        X500Name subject = new X500Name("CN=SanTest");
        PKCS10CertificationRequestBuilder builder = new JcaPKCS10CertificationRequestBuilder(subject, sharedKeyPair.getPublic());
        ExtensionsGenerator extGen = new ExtensionsGenerator();
        extGen.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(generalName));
        builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(sharedKeyPair.getPrivate());
        return new JcaPKCS10CertificationRequest(builder.build(signer).getEncoded());
    }
}
