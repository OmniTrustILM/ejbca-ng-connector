package com.otilm.ca.connector.ejbca.util;

import java.io.IOException;

import com.otilm.api.model.core.enums.CertificateRequestFormat;
import com.otilm.ca.connector.ejbca.request.CertificateRequest;
import com.otilm.ca.connector.ejbca.request.CrmfCertificateRequest;
import com.otilm.ca.connector.ejbca.request.Pkcs10CertificateRequest;
import com.keyfactor.util.CertTools;
import com.keyfactor.util.certificate.DnComponents;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class CertificateRequestUtils {

    private static final Logger logger = LoggerFactory.getLogger(CertificateRequestUtils.class);

    public static JcaPKCS10CertificationRequest csrStringToJcaObject(String csr) throws IOException {
        csr = csr.replace("-----BEGIN CERTIFICATE REQUEST-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END CERTIFICATE REQUEST-----", "");
        byte[] decoded = Base64.getDecoder().decode(csr);
        return new JcaPKCS10CertificationRequest(decoded);
    }

    public static List<String> extractSanFromCsr(JcaPKCS10CertificationRequest csr) {
        List<String> sans = new ArrayList<>();
        Attribute[] certAttributes = csr.getAttributes();
        for (Attribute attribute : certAttributes) {
            if (attribute.getAttrType().equals(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)) {
                Extensions extensions = Extensions.getInstance(attribute.getAttrValues().getObjectAt(0));
                GeneralNames gns = GeneralNames.fromExtensions(extensions, Extension.subjectAlternativeName);
                GeneralName[] names = gns.getNames();
                for (GeneralName name : names) {
                    logger.info("Type: " + name.getTagNo() + " | Name: " + name.getName());
                    String title = "";
                    if (name.getTagNo() == GeneralName.dNSName) {
                        title = "DNS";
                    } else if (name.getTagNo() == GeneralName.iPAddress) {
                        title = "IP Address";
                        // name.toASN1Primitive();
                    } else if (name.getTagNo() == GeneralName.otherName) {
                        title = "Other Name";
                    }
                    sans.add(title + ": " + name.getName());
                }
            }
        }
        return sans;
    }

    public static CertificateRequest createCertificateRequest(byte[] csr, CertificateRequestFormat format) {
        return switch (format) {
            case PKCS10 -> new Pkcs10CertificateRequest(csr);
            case CRMF -> new CrmfCertificateRequest(csr);
            default -> throw new IllegalArgumentException("Unsupported certificate request format: " + format);
        };
    }

    public static String getEjbcaSanExtension(CertificateRequest certificateRequest) throws IOException {
        if (certificateRequest != null) {
            if (certificateRequest.getFormat() == CertificateRequestFormat.PKCS10) {
                final PKCS10CertificationRequest pkcs10CertificateRequest = new PKCS10CertificationRequest(certificateRequest.getEncoded());
                final Extension sanExtension = CertTools.getExtension(pkcs10CertificateRequest, Extension.subjectAlternativeName.getId());
                if (sanExtension != null) {
                    return DnComponents.getAltNameStringFromExtension(sanExtension);
                }
            }
        }
        return null;
    }
}
