package com.otilm.ca.connector.ejbca.request;

import com.otilm.api.model.core.enums.CertificateRequestFormat;
import org.bouncycastle.asn1.crmf.CertReqMessages;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.crmf.jcajce.JcaCertificateRequestMessage;

public class CrmfCertificateRequest implements CertificateRequest {

    private final byte[] encoded;
    private final JcaCertificateRequestMessage certificateRequestMessage;

    public CrmfCertificateRequest(byte[] crmfRequest) {
        this.encoded = crmfRequest;
        CertReqMessages certReqMessages = CertReqMessages.getInstance(crmfRequest);
        // we take only the first request
        this.certificateRequestMessage = new JcaCertificateRequestMessage(certReqMessages.toCertReqMsgArray()[0]);
    }

    @Override
    public byte[] getEncoded() {
        return encoded;
    }

    @Override
    public X500Name getSubject() {
        return certificateRequestMessage.getCertTemplate().getSubject();
    }

    @Override
    public CertificateRequestFormat getFormat() {
        return CertificateRequestFormat.CRMF;
    }

}
