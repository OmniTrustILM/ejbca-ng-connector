package com.otilm.ca.connector.ejbca.util;

import com.otilm.api.model.core.enums.CertificateRequestFormat;
import com.otilm.ca.connector.ejbca.request.CertificateRequest;
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

@SpringBootTest
public class CertificateRequestUtilsTest {

    private PKCS10CertificationRequest pkcs10CertificationRequest;

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
        extGen.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName(GeneralName.dNSName, "test.czertainly.com")));
        Extensions extensions = extGen.generate();
        requestBuilder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensions);
        String sigAlg = "SHA256withRSA";
        ContentSigner signer = new JcaContentSignerBuilder(sigAlg).setProvider("BC").build(keyPair.getPrivate());
        pkcs10CertificationRequest = requestBuilder.build(signer);
    }

    @Test
    public void test() throws IOException {
        CertificateRequest certificateRequest = CertificateRequestUtils.createCertificateRequest(pkcs10CertificationRequest.getEncoded(), CertificateRequestFormat.PKCS10);
        String ejbcaSanString = CertificateRequestUtils.getEjbcaSanExtension(certificateRequest);

        Assertions.assertEquals("dNSName=test.czertainly.com", ejbcaSanString);
    }

}
