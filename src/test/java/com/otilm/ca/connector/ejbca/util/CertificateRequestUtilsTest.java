package com.otilm.ca.connector.ejbca.util;

import com.otilm.api.model.core.enums.CertificateRequestFormat;
import com.otilm.ca.connector.ejbca.request.CertificateRequest;
import com.otilm.ca.connector.ejbca.request.Pkcs10CertificateRequest;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.security.*;
import java.security.spec.RSAKeyGenParameterSpec;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CertificateRequestUtilsTest {

    private PKCS10CertificationRequest pkcs10CertificationRequest;
    private PKCS10CertificationRequest pkcs10NoSan;

    @BeforeEach
    public void setUp() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException, OperatorCreationException {
        // install BouncyCastle provider
        Security.addProvider(new BouncyCastleProvider());

        // generate RSA key pair
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");
        kpGen.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));
        KeyPair keyPair = kpGen.generateKeyPair();

        X500Name subject = new X500Name("CN=Test");
        PKCS10CertificationRequestBuilder requestBuilder = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
        ExtensionsGenerator extGen = new ExtensionsGenerator();
        extGen.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName(GeneralName.dNSName, "test.example.com")));
        Extensions extensions = extGen.generate();
        requestBuilder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensions);
        String sigAlg = "SHA256withRSA";
        ContentSigner signer = new JcaContentSignerBuilder(sigAlg).setProvider("BC").build(keyPair.getPrivate());
        pkcs10CertificationRequest = requestBuilder.build(signer);

        // CSR without SAN
        KeyPairGenerator kpGen2 = KeyPairGenerator.getInstance("RSA", "BC");
        kpGen2.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));
        KeyPair keyPair2 = kpGen2.generateKeyPair();
        ContentSigner signer2 = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(keyPair2.getPrivate());
        pkcs10NoSan = new JcaPKCS10CertificationRequestBuilder(new X500Name("CN=NoSan"), keyPair2.getPublic()).build(signer2);
    }

    @Test
    public void test() throws IOException {
        CertificateRequest certificateRequest = CertificateRequestUtils.createCertificateRequest(pkcs10CertificationRequest.getEncoded(), CertificateRequestFormat.PKCS10);
        String ejbcaSanString = CertificateRequestUtils.getEjbcaSanExtension(certificateRequest);

        Assertions.assertEquals("dNSName=test.example.com", ejbcaSanString);
    }

    @Test
    public void createCertificateRequest_pkcs10_returnsPkcs10Request() throws IOException {
        CertificateRequest request = CertificateRequestUtils.createCertificateRequest(
                pkcs10CertificationRequest.getEncoded(), CertificateRequestFormat.PKCS10);

        assertNotNull(request);
        assertEquals(CertificateRequestFormat.PKCS10, request.getFormat());
        assertInstanceOf(Pkcs10CertificateRequest.class, request);
    }

    @Test
    public void createCertificateRequest_crmf_returnsCrmfRequest() throws Exception {
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
    public void getEjbcaSanExtension_noSan_returnsNull() throws IOException {
        CertificateRequest request = CertificateRequestUtils.createCertificateRequest(
                pkcs10NoSan.getEncoded(), CertificateRequestFormat.PKCS10);

        String san = CertificateRequestUtils.getEjbcaSanExtension(request);

        assertNull(san);
    }

    @Test
    public void getEjbcaSanExtension_nullRequest_returnsNull() throws IOException {
        assertNull(CertificateRequestUtils.getEjbcaSanExtension(null));
    }
}
