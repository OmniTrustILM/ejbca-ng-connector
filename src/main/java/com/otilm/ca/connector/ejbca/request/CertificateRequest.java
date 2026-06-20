package com.otilm.ca.connector.ejbca.request;

import com.otilm.api.model.core.enums.CertificateRequestFormat;
import org.bouncycastle.asn1.x500.X500Name;

public interface CertificateRequest {

    /**
     * Get encoded request
     *
     * @return encoded request
     */
    byte[] getEncoded();

    /**
     * Get subject of the request as X500Name
     *
     * @return subject
     */
    X500Name getSubject();

    /**
     * Get format of the request
     *
     * @return format
     */
    CertificateRequestFormat getFormat();

}
