package com.czertainly.ca.connector.ejbca.service.impl;

import com.czertainly.api.exception.AlreadyExistException;
import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.model.client.attribute.RequestAttribute;
import com.czertainly.api.model.common.NameAndIdDto;
import com.czertainly.api.model.common.attribute.common.MetadataAttribute;
import com.czertainly.api.model.common.attribute.v2.content.BooleanAttributeContentV2;
import com.czertainly.api.model.common.attribute.v2.content.StringAttributeContentV2;
import com.czertainly.api.model.connector.v2.CertificateDataResponseDto;
import com.czertainly.api.model.core.enums.CertificateRequestFormat;
import com.czertainly.ca.connector.ejbca.EjbcaException;
import com.czertainly.ca.connector.ejbca.api.CertificateControllerImpl;
import com.czertainly.ca.connector.ejbca.dto.ejbca.request.SearchCertificatesRestRequestV2;
import com.czertainly.ca.connector.ejbca.dto.ejbca.response.SearchCertificatesRestResponseV2;
import com.czertainly.ca.connector.ejbca.exception.CertificateRequestException;
import com.czertainly.ca.connector.ejbca.service.AuthorityInstanceService;
import com.czertainly.ca.connector.ejbca.service.EjbcaService;
import com.czertainly.ca.connector.ejbca.util.EjbcaUtils;
import com.czertainly.ca.connector.ejbca.util.EjbcaVersion;
import com.czertainly.ca.connector.ejbca.ws.*;
import com.czertainly.core.util.AttributeDefinitionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static com.czertainly.ca.connector.ejbca.api.AuthorityInstanceControllerImpl.ATTRIBUTE_CERTIFICATE_PROFILE;
import static com.czertainly.ca.connector.ejbca.api.AuthorityInstanceControllerImpl.ATTRIBUTE_CERTIFICATION_AUTHORITY;
import static com.czertainly.ca.connector.ejbca.api.AuthorityInstanceControllerImpl.ATTRIBUTE_END_ENTITY_PROFILE;
import static com.czertainly.ca.connector.ejbca.api.AuthorityInstanceControllerImpl.ATTRIBUTE_KEY_RECOVERABLE;
import static com.czertainly.ca.connector.ejbca.api.AuthorityInstanceControllerImpl.ATTRIBUTE_SEND_NOTIFICATIONS;

@Service
@Transactional
public class EjbcaServiceImpl implements EjbcaService {

    private AuthorityInstanceService authorityInstanceService;

    @Autowired
    public void setAuthorityInstanceService(AuthorityInstanceService authorityInstanceService) {
        this.authorityInstanceService = authorityInstanceService;
    }

    @Override
    public void createEndEntity(String authorityUuid, String username, String password, String subjectDn, String subjectAltName, List<RequestAttribute> raProfileAttributes, List<RequestAttribute> issueAttributes) throws NotFoundException, AlreadyExistException, EjbcaException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(authorityUuid);

        if (getUser(ejbcaWS, username) != null) {
            throw new AlreadyExistException("End Entity " + username + " already exists");
        }

        UserDataVOWS user = new UserDataVOWS();
        user.setUsername(username);
        user.setPassword(password);
        user.setSubjectDN(subjectDn);
        user.setSubjectAltName(subjectAltName);

        prepareEndEntity(user, raProfileAttributes, issueAttributes);

        try {
            ejbcaWS.editUser(user);
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (CADoesntExistsException_Exception e) {
            throw new NotFoundException("CA", user.getCaName());
        } catch (UserDoesntFullfillEndEntityProfile_Exception e) {
            throw new EjbcaException(e.getMessage().split(": ")[1]);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void createEndEntityWithMeta(String authorityUuid, String username, String password, String subjectDn, String subjectAltName, List<RequestAttribute> raProfileAttributes, List<MetadataAttribute> metadata) throws NotFoundException, AlreadyExistException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(authorityUuid);

        if (getUser(ejbcaWS, username) != null) {
            throw new AlreadyExistException("End Entity " + username + " already exists");
        }

        UserDataVOWS user = new UserDataVOWS();
        user.setUsername(username);
        user.setPassword(password);
        user.setSubjectDN(subjectDn);
        user.setSubjectAltName(subjectAltName);

        prepareEndEntityWithMeta(user, raProfileAttributes, metadata);

        try {
            ejbcaWS.editUser(user);
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (CADoesntExistsException_Exception e) {
            throw new NotFoundException("CA", user.getCaName());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void renewEndEntity(String authorityUuid, String username, String password, String subjectDn, String subjectAltName) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(authorityUuid);

        UserDataVOWS user = getUser(ejbcaWS, username);
        if (user == null) {
            throw new NotFoundException("EndEntity", username);
        }

        user.setPassword(password);
        user.setStatus(EndEntityStatus.NEW.getCode());
        user.setSubjectDN(subjectDn);
        user.setSubjectAltName(subjectAltName);

        try {
            ejbcaWS.editUser(user);
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (CADoesntExistsException_Exception e) {
            throw new NotFoundException("CA", user.getCaName());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public CertificateDataResponseDto issueCertificate(String authorityUuid, String username, String password, String certificateRequest, CertificateRequestFormat requestFormat) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(authorityUuid);

        try {
            CertificateResponse certificateResponse;
            if (requestFormat == CertificateRequestFormat.CRMF) {
                certificateResponse = ejbcaWS.crmfRequest(
                        username,
                        password,
                        certificateRequest,
                        null,
                        "PKCS7WITHCHAIN");
            } else if (requestFormat == CertificateRequestFormat.PKCS10) {
                certificateResponse = ejbcaWS.pkcs10Request(
                        username,
                        password,
                        certificateRequest,
                        null,
                        "PKCS7WITHCHAIN");
            } else {
                // we should not get here anyway
                throw new CertificateRequestException("Unsupported certificate request format");
            }
            CertificateDataResponseDto response = new CertificateDataResponseDto();
            response.setCertificateData(new String(certificateResponse.getData(), StandardCharsets.UTF_8));
            return response;
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (CADoesntExistsException_Exception e) {
            throw new NotFoundException("CA", "N/A");
        } catch (NotFoundException_Exception e) {
            throw new NotFoundException("EndEntity", username);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void revokeCertificate(String uuid, String issuerDn, String serialNumber, int revocationReason) throws NotFoundException, AccessDeniedException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(uuid);
        try {
            ejbcaWS.revokeCert(issuerDn, serialNumber, revocationReason);
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA " + e.getMessage());
        } catch (CADoesntExistsException_Exception e) {
            throw new NotFoundException("CA of Certificate");
        } catch (NotFoundException_Exception e) {
            throw new NotFoundException("Certificate");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public EjbcaVersion getEjbcaVersion(String authorityInstanceUuid) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(authorityInstanceUuid);
        String ejbcaVersion = ejbcaWS.getEjbcaVersion();
        return new EjbcaVersion(ejbcaVersion);
    }

    @Override
    public SearchCertificatesRestResponseV2 searchCertificates(String authorityInstanceUuid, String restUrl, SearchCertificatesRestRequestV2 request) throws Exception {
        WebClient ejbcaWC = authorityInstanceService.getRestApiConnection(authorityInstanceUuid);
        try {
            return ejbcaWC.post()
                    .uri(restUrl + "/v2/certificate/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(SearchCertificatesRestResponseV2.class)
                    .block();
        } catch (UnsupportedMediaTypeException e) {
            throw new Exception("Failed to communicate to EJBCA using REST API");
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    @Override
    public List<NameAndIdDto> getAvailableCas(String authorityInstanceUuid) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(authorityInstanceUuid);
        try {
            List<NameAndId> cas = ejbcaWS.getAvailableCAs();
            if (cas == null || cas.isEmpty()) {
                throw new NotFoundException("CertificateProfile on ca", authorityInstanceUuid);
            }
            return cas.stream().map(p -> new NameAndIdDto(p.getId(), p.getName())).collect(Collectors.toList());
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (EjbcaException_Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Certificate> getLastCAChain(String authorityInstanceUuid, String caName) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(authorityInstanceUuid);
        try {
            return ejbcaWS.getLastCAChain(caName);
        } catch (CADoesntExistsException_Exception e) {
            throw new NotFoundException("CA", caName);
        } catch (EjbcaException_Exception e) {
            throw new IllegalStateException(e);
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        }
    }

    @Override
    public byte[] getLatestCRL(String authorityInstanceUuid, String caName, boolean deltaCRL) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(authorityInstanceUuid);
        try {
            return ejbcaWS.getLatestCRL(caName, deltaCRL);
        } catch (CADoesntExistsException_Exception e) {
            throw new NotFoundException("CA", caName);
        } catch (EjbcaException_Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public UserDataVOWS getUser(EjbcaWS ejbcaWS, String username) throws NotFoundException {
        UserMatch userMatch = EjbcaUtils.prepareUsernameMatch(username);

        try {
            List<UserDataVOWS> users = ejbcaWS.findUser(userMatch);
            return (users != null && !users.isEmpty()) ? users.get(0) : null;
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (EndEntityProfileNotFoundException_Exception e) {
            throw new NotFoundException("EndEntity", username);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void prepareEndEntity(UserDataVOWS user, List<RequestAttribute> raProfileAttrs, List<RequestAttribute> issueAttrs) {
        setUserProfiles(user, raProfileAttrs);

        String email = AttributeDefinitionUtils.getSingleItemAttributeContentValue(CertificateControllerImpl.ATTRIBUTE_EMAIL, issueAttrs, StringAttributeContentV2.class).getData();
        if (StringUtils.isNotBlank(email)) {
            user.setEmail(email);
        }

        String san = AttributeDefinitionUtils.getSingleItemAttributeContentValue(CertificateControllerImpl.ATTRIBUTE_SAN, issueAttrs, StringAttributeContentV2.class).getData();
        if (StringUtils.isNotBlank(san)) {
            user.setSubjectAltName(san);
        }

        String extension = AttributeDefinitionUtils.getSingleItemAttributeContentValue(CertificateControllerImpl.ATTRIBUTE_EXTENSION, issueAttrs, StringAttributeContentV2.class).getData();
        EjbcaUtils.setUserExtensions(user, extension);
    }

    private void prepareEndEntityWithMeta(UserDataVOWS user, List<RequestAttribute> raProfileAttrs, List<MetadataAttribute> metadata) {
        setUserProfiles(user, raProfileAttrs);

        String email = AttributeDefinitionUtils.getSingleItemAttributeContentValue(CertificateEjbcaServiceImpl.META_EMAIL, metadata, StringAttributeContentV2.class).getData();
        if (StringUtils.isNotBlank(email)) {
            user.setEmail(email);
        }

        String san = AttributeDefinitionUtils.getSingleItemAttributeContentValue(CertificateEjbcaServiceImpl.META_SAN, metadata, StringAttributeContentV2.class).getData();
        if (StringUtils.isNotBlank(san)) {
            user.setSubjectAltName(san);
        }

        String extension = AttributeDefinitionUtils.getSingleItemAttributeContentValue(CertificateEjbcaServiceImpl.META_EXTENSION, metadata, StringAttributeContentV2.class).getData();
        EjbcaUtils.setUserExtensions(user, extension);
    }

    private void setUserProfiles(UserDataVOWS user, List<RequestAttribute> raProfileAttrs) {
        //String tokenType = AttributeDefinitionUtils.getAttributeValue(ATTRIBUTE_TOKEN_TYPE, raProfileAttrs);
        //user.setTokenType(tokenType);
        user.setTokenType("USERGENERATED");

        NameAndIdDto endEntityProfile = AttributeDefinitionUtils.getNameAndIdData(ATTRIBUTE_END_ENTITY_PROFILE, raProfileAttrs);
        user.setEndEntityProfileName(endEntityProfile.getName());

        NameAndIdDto certificateProfile = AttributeDefinitionUtils.getNameAndIdData(ATTRIBUTE_CERTIFICATE_PROFILE, raProfileAttrs);
        user.setCertificateProfileName(certificateProfile.getName());

        NameAndIdDto ca = AttributeDefinitionUtils.getNameAndIdData(ATTRIBUTE_CERTIFICATION_AUTHORITY, raProfileAttrs);
        user.setCaName(ca.getName());

        boolean sendNotifications = false;
        Boolean value = AttributeDefinitionUtils.getSingleItemAttributeContentValue(ATTRIBUTE_SEND_NOTIFICATIONS, raProfileAttrs, BooleanAttributeContentV2.class).getData();
        if (value != null) {
            sendNotifications = value;
        }
        user.setSendNotification(sendNotifications);

        boolean keyRecoverable = false;
        value = AttributeDefinitionUtils.getSingleItemAttributeContentValue(ATTRIBUTE_KEY_RECOVERABLE, raProfileAttrs, BooleanAttributeContentV2.class).getData();
        if (value != null) {
            keyRecoverable = value;
        }
        user.setKeyRecoverable(keyRecoverable);
    }

}
