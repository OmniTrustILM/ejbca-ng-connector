package com.otilm.ca.connector.ejbca.request;

import com.otilm.api.model.core.enums.CertificateRequestFormat;
import com.otilm.ca.connector.ejbca.exception.CertificateRequestException;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;

import java.io.IOException;

public class Pkcs10CertificateRequest implements CertificateRequest {

    private final byte[] encoded;
    private final JcaPKCS10CertificationRequest pkcs10CertificationRequest;

    public Pkcs10CertificateRequest(byte[] pkcs10Request) {
        this.encoded = pkcs10Request;
        try {
            this.pkcs10CertificationRequest = new JcaPKCS10CertificationRequest(pkcs10Request);
        } catch (IOException e) {
            throw new CertificateRequestException("Cannot process PKCS#10 request", e);
        }
    }

    @Override
    public byte[] getEncoded() {
        return encoded;
    }

    @Override
    public X500Name getSubject() {
        return pkcs10CertificationRequest.getSubject();
    }

    @Override
    public CertificateRequestFormat getFormat() {
        return CertificateRequestFormat.PKCS10;
    }

}
