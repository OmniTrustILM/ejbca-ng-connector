package com.otilm.ca.connector.ejbca.util;

import java.io.IOException;

import com.otilm.api.model.core.enums.CertificateRequestFormat;
import com.otilm.ca.connector.ejbca.request.CertificateRequest;
import com.otilm.ca.connector.ejbca.request.CrmfCertificateRequest;
import com.otilm.ca.connector.ejbca.request.Pkcs10CertificateRequest;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.style.IETFUtils;
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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.StringJoiner;

public class CertificateRequestUtils {

    private static final Logger logger = LoggerFactory.getLogger(CertificateRequestUtils.class);

    private static final String SAN_SEPARATOR = ", ";
    private static final ASN1ObjectIdentifier OID_MS_UPN = new ASN1ObjectIdentifier("1.3.6.1.4.1.311.20.2.3");
    private static final ASN1ObjectIdentifier OID_XMPP_ADDR = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.8.5");
    private static final ASN1ObjectIdentifier OID_SRV_NAME = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.8.7");
    private static final ASN1ObjectIdentifier OID_PERMANENT_IDENTIFIER = new ASN1ObjectIdentifier("1.3.6.1.5.5.7.8.3");

    private CertificateRequestUtils() {
        // utility class
    }

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
        if (certificateRequest == null) {
            return null;
        }
        final GeneralNames generalNames = subjectAlternativeNames(certificateRequest);
        if (generalNames == null) {
            return null;
        }
        final List<String> parts = new ArrayList<>();
        for (GeneralName generalName : generalNames.getNames()) {
            final String part = toEjbcaSanEntry(generalName);
            if (part != null) {
                parts.add(part);
            } else {
                logger.warn("Skipping subject alternative name of unsupported type {}", generalName.getTagNo());
            }
        }
        return parts.isEmpty() ? null : String.join(SAN_SEPARATOR, parts);
    }

    private static GeneralNames subjectAlternativeNames(CertificateRequest certificateRequest) throws IOException {
        if (certificateRequest instanceof CrmfCertificateRequest crmf) {
            final Extensions extensions = crmf.getCertTemplateExtensions();
            return extensions == null ? null : GeneralNames.fromExtensions(extensions, Extension.subjectAlternativeName);
        }
        if (certificateRequest.getFormat() == CertificateRequestFormat.PKCS10) {
            return subjectAlternativeNames(new PKCS10CertificationRequest(certificateRequest.getEncoded()));
        }
        return null;
    }

    private static GeneralNames subjectAlternativeNames(PKCS10CertificationRequest csr) {
        for (Attribute attribute : csr.getAttributes(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)) {
            if (attribute.getAttrValues().size() == 0) {
                continue;
            }
            final Extensions extensions = Extensions.getInstance(attribute.getAttrValues().getObjectAt(0));
            final GeneralNames generalNames = GeneralNames.fromExtensions(extensions, Extension.subjectAlternativeName);
            if (generalNames != null) {
                return generalNames;
            }
        }
        return null;
    }

    private static String toEjbcaSanEntry(GeneralName generalName) {
        final SanEntry entry = sanEntry(generalName);
        if (entry == null || entry.value() == null) {
            return null;
        }
        return entry.key() + "=" + escape(entry.value());
    }

    private static SanEntry sanEntry(GeneralName generalName) {
        return switch (generalName.getTagNo()) {
            case GeneralName.dNSName -> new SanEntry("dNSName", generalName.getName().toString());
            case GeneralName.rfc822Name -> new SanEntry("rfc822name", generalName.getName().toString());
            case GeneralName.uniformResourceIdentifier -> new SanEntry("UNIFORMRESOURCEIDENTIFIER", generalName.getName().toString());
            case GeneralName.registeredID -> new SanEntry("registeredID", generalName.getName().toString());
            case GeneralName.directoryName -> new SanEntry("DIRECTORYNAME", generalName.getName().toString());
            case GeneralName.iPAddress -> new SanEntry("iPAddress", ipAddress(generalName));
            case GeneralName.otherName -> otherName(generalName);
            default -> null;
        };
    }

    /**
     * Escapes a value the way EJBCA's subject alternative name parser expects: RFC 2253 rules,
     * except that {@code =} is left as-is.
     */
    private static String escape(String value) {
        return IETFUtils.valueToString(new DERUTF8String(value)).replace("\\=", "=");
    }

    private static String ipAddress(GeneralName generalName) {
        final byte[] octets = DEROctetString.getInstance(generalName.getName()).getOctets();
        try {
            final InetAddress address = InetAddress.getByAddress(octets);
            if (address instanceof Inet4Address) {
                return address.getHostAddress();
            }
            final StringJoiner groups = new StringJoiner(":");
            for (int i = 0; i < octets.length; i += 2) {
                groups.add(Integer.toHexString(((octets[i] & 0xFF) << 8) | (octets[i + 1] & 0xFF)));
            }
            return groups.toString();
        } catch (UnknownHostException e) {
            logger.warn("Subject alternative name contains an IP address of unexpected length {}", octets.length);
            return null;
        }
    }

    private static SanEntry otherName(GeneralName generalName) {
        final ASN1Sequence sequence = ASN1Sequence.getInstance(generalName.getName());
        if (sequence.size() < 2) {
            return null;
        }
        final ASN1ObjectIdentifier oid = ASN1ObjectIdentifier.getInstance(sequence.getObjectAt(0));
        final ASN1Primitive value = ASN1TaggedObject.getInstance(sequence.getObjectAt(1)).getBaseObject().toASN1Primitive();
        if (OID_MS_UPN.equals(oid)) {
            return stringOtherName("UPN", value);
        }
        if (OID_XMPP_ADDR.equals(oid)) {
            return stringOtherName("XMPPADDR", value);
        }
        if (OID_SRV_NAME.equals(oid)) {
            return stringOtherName("SRVNAME", value);
        }
        if (OID_PERMANENT_IDENTIFIER.equals(oid)) {
            return permanentIdentifier(value);
        }
        return null;
    }

    private static SanEntry stringOtherName(String key, ASN1Primitive value) {
        return value instanceof ASN1String asn1String ? new SanEntry(key, asn1String.getString()) : null;
    }

    /**
     * PermanentIdentifier is a sequence of an optional value and an optional assigner OID,
     * rendered by EJBCA as {@code value/assigner}.
     */
    private static SanEntry permanentIdentifier(ASN1Primitive value) {
        if (!(value instanceof ASN1Sequence sequence)) {
            return null;
        }
        String identifier = "";
        String assigner = "";
        for (int i = 0; i < sequence.size(); i++) {
            final ASN1Primitive element = sequence.getObjectAt(i).toASN1Primitive();
            if (element instanceof ASN1String asn1String) {
                identifier = asn1String.getString();
            } else if (element instanceof ASN1ObjectIdentifier objectIdentifier) {
                assigner = objectIdentifier.getId();
            }
        }
        return new SanEntry("PERMANENTIDENTIFIER", identifier + "/" + assigner);
    }

    private record SanEntry(String key, String value) {
    }

}
