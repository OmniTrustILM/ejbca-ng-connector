package com.czertainly.ca.connector.ejbca.api;

import com.czertainly.api.exception.CertificateOperationException;
import com.czertainly.api.exception.CertificateRequestException;
import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.exception.ValidationException;
import com.czertainly.api.interfaces.connector.v2.CertificateController;
import com.czertainly.api.model.client.attribute.RequestAttribute;
import com.czertainly.api.model.common.attribute.common.AttributeType;
import com.czertainly.api.model.common.attribute.common.BaseAttribute;
import com.czertainly.api.model.common.attribute.common.DataAttribute;
import com.czertainly.api.model.common.attribute.common.content.AttributeContentType;
import com.czertainly.api.model.common.attribute.v2.DataAttributeV2;
import com.czertainly.api.model.common.attribute.common.properties.DataAttributeProperties;
import com.czertainly.api.model.connector.v2.*;
import com.czertainly.ca.connector.ejbca.service.CertificateEjbcaService;
import org.springframework.http.ResponseEntity;
import com.czertainly.core.util.AttributeDefinitionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class CertificateControllerImpl implements CertificateController {

    public static final String ATTRIBUTE_EMAIL = "email";
    public static final String ATTRIBUTE_SAN = "san";
    public static final String ATTRIBUTE_EXTENSION = "extension";

    public static final String ATTRIBUTE_EMAIL_LABEL = "Email";
    public static final String ATTRIBUTE_SAN_LABEL = "Subject Alternative Name";
    public static final String ATTRIBUTE_EXTENSION_LABEL = "Extension Data";

    public static final String ATTRIBUTE_EMAIL_UUID = "0b378474-ebe9-4a17-9d3d-0577eb16aa34";
    public static final String ATTRIBUTE_SAN_UUID = "2cfd8c1a-e867-42f1-ab6c-67fb1964e163";
    public static final String ATTRIBUTE_EXTENSION_UUID = "72324d22-12cb-47ee-a02e-0b1da2013eee";

    private CertificateEjbcaService certificateEjbcaService;

    @Autowired
    public void setCertificateEjbcaService(CertificateEjbcaService certificateEjbcaService) {
        this.certificateEjbcaService = certificateEjbcaService;
    }

    @Override
    public List<BaseAttribute> listIssueCertificateAttributes(String uuid) {
        List<BaseAttribute> attrs = new ArrayList<>();

        DataAttributeV2 email = new DataAttributeV2();
        email.setUuid(ATTRIBUTE_EMAIL_UUID);
        email.setName(ATTRIBUTE_EMAIL);
        email.setDescription("End Entity email address");
        email.setType(AttributeType.DATA);
        email.setContentType(AttributeContentType.STRING);
        DataAttributeProperties emailProperties = new DataAttributeProperties();
        emailProperties.setLabel(ATTRIBUTE_EMAIL_LABEL);
        emailProperties.setRequired(false);
        emailProperties.setReadOnly(false);
        emailProperties.setVisible(true);
        emailProperties.setList(false);
        emailProperties.setMultiSelect(false);
        email.setProperties(emailProperties);
        attrs.add(email);

        DataAttributeV2 san = new DataAttributeV2();
        san.setUuid(ATTRIBUTE_SAN_UUID);
        san.setName(ATTRIBUTE_SAN);
        san.setDescription("Comma separated Subject Alternative Names. If present, it will override the SANs in the CSR");
        san.setType(AttributeType.DATA);
        san.setContentType(AttributeContentType.STRING);
        DataAttributeProperties sanProperties = new DataAttributeProperties();
        sanProperties.setLabel(ATTRIBUTE_SAN_LABEL);
        sanProperties.setRequired(false);
        sanProperties.setReadOnly(false);
        sanProperties.setVisible(true);
        sanProperties.setList(false);
        sanProperties.setMultiSelect(false);
        san.setProperties(sanProperties);
        attrs.add(san);

        DataAttributeV2 extension = new DataAttributeV2();
        extension.setUuid(ATTRIBUTE_EXTENSION_UUID);
        extension.setName(ATTRIBUTE_EXTENSION);
        extension.setDescription("Comma separated Extension Data in the format OID=Value");
        extension.setType(AttributeType.DATA);
        extension.setContentType(AttributeContentType.STRING);
        DataAttributeProperties extensionProperties = new DataAttributeProperties();
        extensionProperties.setLabel(ATTRIBUTE_EXTENSION_LABEL);
        extensionProperties.setRequired(false);
        extensionProperties.setReadOnly(false);
        extensionProperties.setVisible(true);
        extensionProperties.setList(false);
        extensionProperties.setMultiSelect(false);
        extension.setProperties(extensionProperties);
        attrs.add(extension);

        return attrs;
    }

    @Override
    public void validateIssueCertificateAttributes(String uuid, List<RequestAttribute> attributes) throws ValidationException {
        AttributeDefinitionUtils.validateAttributes(
                listIssueCertificateAttributes(uuid),
                attributes);
    }

    @Override
    public ResponseEntity<CertificateDataResponseDto> issueCertificate(String uuid, CertificateSignRequestDto request) throws NotFoundException, CertificateOperationException, CertificateRequestException {
        try {
            return ResponseEntity.ok(certificateEjbcaService.issueCertificate(uuid, request));
        } catch (CertificateOperationException | CertificateRequestException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            CertificateOperationException coe = new CertificateOperationException(e.getMessage());
            coe.initCause(e);
            throw coe;
        }
    }

    @Override
    public ResponseEntity<CertificateDataResponseDto> renewCertificate(String uuid, CertificateRenewRequestDto request) throws NotFoundException, CertificateOperationException, CertificateRequestException {
        try {
            return ResponseEntity.ok(certificateEjbcaService.renewCertificate(uuid, request));
        } catch (CertificateOperationException | CertificateRequestException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            CertificateOperationException coe = new CertificateOperationException(e.getMessage());
            coe.initCause(e);
            throw coe;
        }
    }

    @Override
    public List<BaseAttribute> listRevokeCertificateAttributes(String uuid) {
        return List.of();
    }

    @Override
    public void validateRevokeCertificateAttributes(String uuid, List<RequestAttribute> attributes) throws ValidationException {
        AttributeDefinitionUtils.validateAttributes(
                listRevokeCertificateAttributes(uuid),
                attributes);
    }

    @Override
    public ResponseEntity<Void> revokeCertificate(String uuid, CertRevocationDto request) throws NotFoundException, CertificateOperationException {
        try {
            certificateEjbcaService.revokeCertificate(uuid, request);
        } catch (NotFoundException | AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            CertificateOperationException coe = new CertificateOperationException(e.getMessage());
            coe.initCause(e);
            throw coe;
        }
        return ResponseEntity.noContent().build();
    }

    @Override
    public CertificateOperationStatusResponseDto getIssueCertificateStatus(String uuid, CertificateOperationStatusRequestDto request) throws NotFoundException {
        throw new UnsupportedOperationException("getIssueCertificateStatus is not supported");
    }

    @Override
    public void cancelIssueCertificate(String uuid, CertificateOperationCancelRequestDto request) throws NotFoundException, ValidationException {
        throw new UnsupportedOperationException("cancelIssueCertificate is not supported");
    }

    @Override
    public void cancelRevokeCertificate(String uuid, CertificateOperationCancelRequestDto request) throws NotFoundException, ValidationException {
        throw new UnsupportedOperationException("cancelRevokeCertificate is not supported");
    }

    @Override
    public CertificateOperationStatusResponseDto getRevokeCertificateStatus(String uuid, CertificateOperationStatusRequestDto request) throws NotFoundException {
        throw new UnsupportedOperationException("getRevokeCertificateStatus is not supported");
    }

    @Override
    public CertificateIdentificationResponseDto identifyCertificate(String uuid, CertificateIdentificationRequestDto request) throws NotFoundException, ValidationException {
        return certificateEjbcaService.identifyCertificate(uuid, request);
    }
}
