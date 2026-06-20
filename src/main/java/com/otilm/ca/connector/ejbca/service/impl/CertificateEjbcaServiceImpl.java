package com.otilm.ca.connector.ejbca.service.impl;

import com.otilm.api.exception.NotFoundException;
import com.otilm.api.exception.ValidationException;
import com.otilm.api.model.common.NameAndIdDto;
import com.otilm.api.model.common.attribute.common.AttributeType;
import com.otilm.api.model.common.attribute.common.MetadataAttribute;
import com.otilm.api.model.common.attribute.common.content.AttributeContentType;
import com.otilm.api.model.common.attribute.v2.MetadataAttributeV2;
import com.otilm.api.model.common.attribute.v2.content.StringAttributeContentV2;
import com.otilm.api.model.common.attribute.common.properties.MetadataAttributeProperties;
import com.otilm.api.model.connector.v2.*;
import com.otilm.ca.connector.ejbca.api.AuthorityInstanceControllerImpl;
import com.otilm.ca.connector.ejbca.api.CertificateControllerImpl;
import com.otilm.ca.connector.ejbca.dto.ejbca.request.Pagination;
import com.otilm.ca.connector.ejbca.dto.ejbca.request.SearchCertificateCriteriaRestRequest;
import com.otilm.ca.connector.ejbca.dto.ejbca.request.SearchCertificatesRestRequestV2;
import com.otilm.ca.connector.ejbca.dto.ejbca.response.CertificateRestResponseV2;
import com.otilm.ca.connector.ejbca.dto.ejbca.response.SearchCertificatesRestResponseV2;
import com.otilm.ca.connector.ejbca.enums.UsernameGenMethod;
import com.otilm.ca.connector.ejbca.request.CertificateRequest;
import com.otilm.ca.connector.ejbca.service.AuthorityInstanceService;
import com.otilm.ca.connector.ejbca.service.CertificateEjbcaService;
import com.otilm.ca.connector.ejbca.service.EjbcaService;
import com.otilm.ca.connector.ejbca.util.CertificateUtil;
import com.otilm.ca.connector.ejbca.util.CertificateRequestUtils;
import com.otilm.core.util.AttributeDefinitionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static com.otilm.ca.connector.ejbca.api.AuthorityInstanceControllerImpl.*;

@Service
@Transactional
public class CertificateEjbcaServiceImpl implements CertificateEjbcaService {

    public static final String META_EMAIL = "email";
    public static final String META_SAN = "san";
    public static final String META_EXTENSION = "extension";
    private static final Logger logger = LoggerFactory.getLogger(CertificateEjbcaServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    public final String META_EJBCA_USERNAME = "ejbcaUsername";
    private final String COMMON_NAME = "2.5.4.3";
    private EjbcaService ejbcaService;
    private AuthorityInstanceService authorityInstanceService;

    @Autowired
    public void setEjbcaService(EjbcaService ejbcaService) {
        this.ejbcaService = ejbcaService;
    }

    @Autowired
    public void setAuthorityInstanceService(AuthorityInstanceService authorityInstanceService) {
        this.authorityInstanceService = authorityInstanceService;
    }

    @Override
    public CertificateDataResponseDto issueCertificate(String uuid, CertificateSignRequestDto request) throws Exception {
        // generate username based on the request
        String usernameGenMethod = AttributeDefinitionUtils.getSingleItemAttributeContentValue(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_GEN_METHOD, request.getRaProfileAttributes(), StringAttributeContentV2.class).getData();
        String usernamePrefix = AttributeDefinitionUtils.getSingleItemAttributeContentValue(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_PREFIX, request.getRaProfileAttributes(), StringAttributeContentV2.class).getData();
        String usernamePostfix = AttributeDefinitionUtils.getSingleItemAttributeContentValue(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_POSTFIX, request.getRaProfileAttributes(), StringAttributeContentV2.class).getData();

        CertificateRequest certificateRequest = CertificateRequestUtils.createCertificateRequest(
                Base64.getDecoder().decode(request.getRequest()), request.getFormat());

        String username = generateUsername(usernameGenMethod, usernamePrefix, usernamePostfix, certificateRequest);
        String subjectDn = certificateRequest.getSubject().toString();
        String subjectAltName = CertificateRequestUtils.getEjbcaSanExtension(certificateRequest);
        String password = username;

        // try to create end entity and issue certificate
        ejbcaService.createEndEntity(uuid, username, password, subjectDn, subjectAltName, request.getRaProfileAttributes(), request.getAttributes());
        // issue certificate
        CertificateDataResponseDto certificate = ejbcaService.issueCertificate(uuid, username, password, Base64.getEncoder().encodeToString(certificateRequest.getEncoded()), request.getFormat());

        certificate.setMeta(getIssueMetadata(
                username,
                AttributeDefinitionUtils.getSingleItemAttributeContentValue(CertificateControllerImpl.ATTRIBUTE_EMAIL, request.getAttributes(), StringAttributeContentV2.class).getData(),
                AttributeDefinitionUtils.getSingleItemAttributeContentValue(CertificateControllerImpl.ATTRIBUTE_SAN, request.getAttributes(), StringAttributeContentV2.class).getData(),
                AttributeDefinitionUtils.getSingleItemAttributeContentValue(CertificateControllerImpl.ATTRIBUTE_EXTENSION, request.getAttributes(), StringAttributeContentV2.class).getData()
        ));

        return certificate;
    }

    @Override
    public CertificateDataResponseDto renewCertificate(String uuid, CertificateRenewRequestDto request) throws Exception {
        CertificateRequest certificateRequest = CertificateRequestUtils.createCertificateRequest(
                Base64.getDecoder().decode(request.getRequest()), request.getFormat());

        List<MetadataAttribute> metadata = request.getMeta();

        // check if we have the username in the metadata, and if not, generate username
        String username = null;
        if (!request.getMeta().isEmpty()) {
            username = AttributeDefinitionUtils.getSingleItemAttributeContentValue(META_EJBCA_USERNAME, metadata, StringAttributeContentV2.class).getData();
        }
        if (StringUtils.isBlank(username)) {
            String usernameGenMethod = AttributeDefinitionUtils.getSingleItemAttributeContentValue(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_GEN_METHOD, request.getRaProfileAttributes(), StringAttributeContentV2.class).getData();
            String usernamePrefix = AttributeDefinitionUtils.getSingleItemAttributeContentValue(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_PREFIX, request.getRaProfileAttributes(), StringAttributeContentV2.class).getData();
            String usernamePostfix = AttributeDefinitionUtils.getSingleItemAttributeContentValue(AuthorityInstanceControllerImpl.ATTRIBUTE_USERNAME_POSTFIX, request.getRaProfileAttributes(), StringAttributeContentV2.class).getData();
            username = generateUsername(usernameGenMethod, usernamePrefix, usernamePostfix, certificateRequest);
        }

        String subjectDn = certificateRequest.getSubject().toString();
        String subjectAltName = CertificateRequestUtils.getEjbcaSanExtension(certificateRequest);
        String password = username;

        try {
            // update end entity
            ejbcaService.renewEndEntity(uuid, username, password, subjectDn, subjectAltName);
        } catch (NotFoundException e) {
            ejbcaService.createEndEntityWithMeta(uuid, username, password, subjectDn, subjectAltName, request.getRaProfileAttributes(), metadata);
        }

        // issue certificate
        CertificateDataResponseDto certificate = ejbcaService.issueCertificate(uuid, username, password, Base64.getEncoder().encodeToString(certificateRequest.getEncoded()), request.getFormat());

        List<MetadataAttribute> meta = new ArrayList<>();
        meta.addAll(metadata.stream().filter(e -> !e.getName().equals(META_EJBCA_USERNAME)).collect(Collectors.toList()));
        meta.addAll(getUsernameMetadata(username));
        certificate.setMeta(meta);

        return certificate;
    }

    private List<MetadataAttribute> getUsernameMetadata(String username) {
        List<MetadataAttribute> attributes = new ArrayList<>();

        // Username
        MetadataAttributeV2 attribute = new MetadataAttributeV2();
        attribute.setUuid("b42ab690-60fd-11ed-9b6a-0242ac120002");
        attribute.setName(META_EJBCA_USERNAME);
        attribute.setDescription("EJBCA Username");
        attribute.setType(AttributeType.META);
        attribute.setContentType(AttributeContentType.STRING);
        attribute.setContent(List.of(new StringAttributeContentV2(username)));

        MetadataAttributeProperties attributeProperties = new MetadataAttributeProperties();
        attributeProperties.setVisible(true);
        attributeProperties.setLabel("EJBCA Username");
        attribute.setProperties(attributeProperties);

        attributes.add(attribute);
        return attributes;
    }

    private MetadataAttribute getUsernameMetadataAttribute(String username) {
        // Username
        MetadataAttributeV2 attribute = new MetadataAttributeV2();
        attribute.setUuid("b42ab690-60fd-11ed-9b6a-0242ac120002");
        attribute.setName(META_EJBCA_USERNAME);
        attribute.setDescription("EJBCA Username");
        attribute.setType(AttributeType.META);
        attribute.setContentType(AttributeContentType.STRING);
        attribute.setContent(List.of(new StringAttributeContentV2(username)));

        MetadataAttributeProperties attributeProperties = new MetadataAttributeProperties();
        attributeProperties.setVisible(true);
        attributeProperties.setLabel("EJBCA Username");
        attribute.setProperties(attributeProperties);

        return attribute;
    }

    private List<MetadataAttribute> getIssueMetadata(String username, String email, String san, String extensions) {
        List<MetadataAttribute> attributes = new ArrayList<>();

        // Username
        attributes.addAll(getUsernameMetadata(username));

        // EMAIL
        if (StringUtils.isNotBlank(email)) {
            MetadataAttributeV2 emailAttribute = new MetadataAttributeV2();
            emailAttribute.setUuid("b42ab942-60fd-11ed-9b6a-0242ac120002");
            emailAttribute.setName(META_EMAIL);
            emailAttribute.setDescription("Email");
            emailAttribute.setType(AttributeType.META);
            emailAttribute.setContentType(AttributeContentType.STRING);
            emailAttribute.setContent(List.of(new StringAttributeContentV2(email)));

            MetadataAttributeProperties emailAttributeProperties = new MetadataAttributeProperties();
            emailAttributeProperties.setVisible(true);
            emailAttributeProperties.setLabel("Email");
            emailAttribute.setProperties(emailAttributeProperties);

            attributes.add(emailAttribute);
        }

        // SAN Attribute
        if (StringUtils.isNotBlank(san)) {
            MetadataAttributeV2 sanAttribute = new MetadataAttributeV2();
            sanAttribute.setUuid("b42abc58-60fd-11ed-9b6a-0242ac120002");
            sanAttribute.setName(META_SAN);
            sanAttribute.setDescription("SAN");
            sanAttribute.setType(AttributeType.META);
            sanAttribute.setContentType(AttributeContentType.STRING);
            sanAttribute.setContent(List.of(new StringAttributeContentV2(san)));

            MetadataAttributeProperties sanAttributeProperties = new MetadataAttributeProperties();
            sanAttributeProperties.setVisible(true);
            sanAttributeProperties.setLabel("SAN");
            sanAttribute.setProperties(sanAttributeProperties);

            attributes.add(sanAttribute);
        }

        //Extension
        if (StringUtils.isNotBlank(extensions)) {
            MetadataAttributeV2 extensionAttribute = new MetadataAttributeV2();
            extensionAttribute.setUuid("b42abe38-60fd-11ed-9b6a-0242ac120002");
            extensionAttribute.setName(META_EXTENSION);
            extensionAttribute.setDescription("Extension");
            extensionAttribute.setType(AttributeType.META);
            extensionAttribute.setContentType(AttributeContentType.STRING);
            extensionAttribute.setContent(List.of(new StringAttributeContentV2(extensions)));

            MetadataAttributeProperties extensionAttributeProperties = new MetadataAttributeProperties();
            extensionAttributeProperties.setVisible(true);
            extensionAttributeProperties.setLabel("Extension");
            extensionAttribute.setProperties(extensionAttributeProperties);

            attributes.add(extensionAttribute);
        }

        return attributes;
    }

    private String generateUsername(String usernameGenMethod, String usernamePrefix, String usernamePostfix, CertificateRequest csr) throws Exception {
        String username;
        if (usernameGenMethod.equals(UsernameGenMethod.RANDOM.name())) {
            byte[] r = new byte[8];
            RANDOM.nextBytes(r);
            username = Base64.getEncoder().encodeToString(r);
        } else if (usernameGenMethod.equals(UsernameGenMethod.CN.name())) {
            if (csr == null) {
                throw new IOException("CSR failed to be decoded!");
            }
            username = getX500Field(COMMON_NAME, csr.getSubject());
        } else {
            String message = "Unsupported username generation method: " + usernameGenMethod;
            logger.debug(message);
            throw new Exception(message);
        }
        if (StringUtils.isNotBlank(username)) {
            if (StringUtils.isNotBlank(usernamePrefix)) {
                username = usernamePrefix + username;
            }
            if (StringUtils.isNotBlank(usernamePostfix)) {
                username = username + usernamePostfix;
            }
        } else {
            String message = "Username is null or empty, username was not properly generated";
            logger.debug(message);
            throw new Exception(message);
        }
        return username;
    }

    @Override
    public void revokeCertificate(String uuid, CertRevocationDto request) throws AccessDeniedException {
        try {
            X509Certificate certificate = CertificateUtil.parseCertificate(request.getCertificate());
            String issuerDn = CertificateUtil.getIssuerDnFromX509Certificate(certificate);
            String serialNumber = CertificateUtil.getSerialNumberFromX509Certificate(certificate);
            int revocationCode = request.getReason().getReasonCode();
            ejbcaService.revokeCertificate(uuid, issuerDn, serialNumber, revocationCode);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public CertificateIdentificationResponseDto identifyCertificate(String uuid, CertificateIdentificationRequestDto request) throws NotFoundException, ValidationException {
        // load and parse data we need to identify the certificate
        NameAndIdDto endEntityProfile = AttributeDefinitionUtils.getNameAndIdData(ATTRIBUTE_END_ENTITY_PROFILE, request.getRaProfileAttributes());
        NameAndIdDto certificateProfile = AttributeDefinitionUtils.getNameAndIdData(ATTRIBUTE_CERTIFICATE_PROFILE, request.getRaProfileAttributes());
        //NameAndIdDto certificationAuthority = AttributeDefinitionUtils.getNameAndIdData(ATTRIBUTE_CERTIFICATION_AUTHORITY, request.getRaProfileAttributes());
        //Boolean sendNotifications = AttributeDefinitionUtils.getSingleItemAttributeContentValue(ATTRIBUTE_SEND_NOTIFICATIONS, request.getRaProfileAttributes(), BooleanAttributeContent.class).getData();
        //Boolean keyRecoverable = AttributeDefinitionUtils.getSingleItemAttributeContentValue(ATTRIBUTE_KEY_RECOVERABLE, request.getRaProfileAttributes(), BooleanAttributeContent.class).getData();
        String restApiUrl = authorityInstanceService.getRestApiUrl(uuid);
        String sn;
        try {
            X509Certificate certificate = CertificateUtil.parseCertificate(request.getCertificate());
            sn = CertificateUtil.getSerialNumberFromX509Certificate(certificate);
        } catch (CertificateException e) {
            throw new ValidationException("Cannot read certificate: " + e);
        }

        // prepare search request
        SearchCertificatesRestRequestV2 searchRequest = new SearchCertificatesRestRequestV2();
        // set pagination
        Pagination pagination = new Pagination();
        pagination.setPageSize(10);
        pagination.setCurrentPage(1);
        // add serial number as search criteria
        List<SearchCertificateCriteriaRestRequest> criteria = new ArrayList<>();
        SearchCertificateCriteriaRestRequest c = new SearchCertificateCriteriaRestRequest();
        c.setOperation(SearchCertificateCriteriaRestRequest.CriteriaOperation.EQUAL.name());
        c.setProperty(SearchCertificateCriteriaRestRequest.CriteriaProperty.QUERY.name());
        c.setValue(sn);
        criteria.add(c);
        searchRequest.setPagination(pagination);
        searchRequest.setCriteria(criteria);

        // search for the certificate
        SearchCertificatesRestResponseV2 response;
        try {
            response = ejbcaService.searchCertificates(uuid, restApiUrl, searchRequest);
        } catch (Exception e) {
            throw new ValidationException("Cannot identify certificate: serialnumber=" + sn + ", " + e);
        }

        // check if we found the certificate and process result
        if (response.getCertificates().isEmpty()) {
            throw new NotFoundException();
        } else if (response.getCertificates().size() > 1) { // this should not happen
            throw new ValidationException("More than one certificate found with serial number: " + sn);
        } else { // check the properties of the certificate
            CertificateRestResponseV2 certificate = response.getCertificates().get(0);
            if (certificate.getEndEntityProfileId() == endEntityProfile.getId() &&
                    certificate.getCertificateProfileId() == certificateProfile.getId()
                // we do not need to check the CA, as it should be already checked by the RA Profile
                // TODO: check the end entity profile for all attributes that are not present in CertificateRestResponseV2
                // certificate.getSendNotifications() == sendNotifications &&
                // certificate.getKeyRecoverable() == keyRecoverable
            ) {
                CertificateIdentificationResponseDto responseDto = new CertificateIdentificationResponseDto();

                List<MetadataAttribute> meta = new ArrayList<>();
                meta.add(getUsernameMetadataAttribute(certificate.getUsername()));

                responseDto.setMeta(meta);
                return responseDto;
            } else {
                throw new ValidationException("Certificate found with serial number: " + sn + " but it does not match according to RA Profile attributes");
            }
        }
    }

    private String getX500Field(String asn1ObjectIdentifier, X500Name x500Name) {
        RDN[] rdnArray = x500Name.getRDNs(new ASN1ObjectIdentifier(asn1ObjectIdentifier));
        String retVal = null;
        for (RDN item : rdnArray) {
            retVal = item.getFirst().getValue().toString();
        }
        return retVal;
    }
}
