package com.otilm.ca.connector.ejbca.service.impl;

import com.otilm.api.exception.AlreadyExistException;
import com.otilm.api.exception.NotFoundException;
import com.otilm.api.model.client.attribute.ResponseAttribute;
import com.otilm.api.model.common.NameAndIdDto;
import com.otilm.api.model.common.attribute.v2.content.BooleanAttributeContentV2;
import com.otilm.api.model.core.authority.*;
import com.otilm.ca.connector.ejbca.service.AuthorityInstanceService;
import com.otilm.ca.connector.ejbca.service.EndEntityEjbcaService;
import com.otilm.ca.connector.ejbca.util.EjbcaUtils;
import com.otilm.ca.connector.ejbca.ws.*;
import com.otilm.core.util.AttributeDefinitionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import static com.otilm.ca.connector.ejbca.api.AuthorityInstanceControllerImpl.ATTRIBUTE_CERTIFICATE_PROFILE;
import static com.otilm.ca.connector.ejbca.api.AuthorityInstanceControllerImpl.ATTRIBUTE_CERTIFICATION_AUTHORITY;
import static com.otilm.ca.connector.ejbca.api.AuthorityInstanceControllerImpl.ATTRIBUTE_END_ENTITY_PROFILE;
import static com.otilm.ca.connector.ejbca.api.AuthorityInstanceControllerImpl.ATTRIBUTE_KEY_RECOVERABLE;
import static com.otilm.ca.connector.ejbca.api.AuthorityInstanceControllerImpl.ATTRIBUTE_SEND_NOTIFICATIONS;

@Service
@Transactional
public class EndEntityEjbcaServiceImpl implements EndEntityEjbcaService {

    private AuthorityInstanceService authorityInstanceService;

    @Autowired
    public void setAuthorityInstanceService(AuthorityInstanceService authorityInstanceService) {
        this.authorityInstanceService = authorityInstanceService;
    }

    @Override
    public List<EndEntityDto> listEntities(String uuid, String endEntityProfileName) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(uuid);

        List<UserDataVOWS> users = listUsers(ejbcaWS, endEntityProfileName);
        return users.stream().map(EjbcaUtils::mapToUserDetailDTO).toList();
    }

    @Override
    public EndEntityDto getEndEntity(String uuid, String endEntityProfileName, String endEntityName) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(uuid);

        UserDataVOWS user = getUser(ejbcaWS, endEntityName);
        if (user == null) {
            throw new NotFoundException("EndEntity", endEntityName);
        }
        return EjbcaUtils.mapToUserDetailDTO(user);
    }

    @Override
    public void createEndEntity(String uuid, String endEntityProfileName, AddEndEntityRequestDto request) throws NotFoundException, AlreadyExistException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(uuid);

        if (getUser(ejbcaWS, request.getUsername()) != null) {
            throw new AlreadyExistException("EndEntity", request.getUsername());
        }

        UserDataVOWS user = new UserDataVOWS();
        prepareEndEntity(user, request, request.getUsername());

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
    public void updateEndEntity(String uuid, String endEntityProfileName, String endEntityName, EditEndEntityRequestDto request) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(uuid);

        UserDataVOWS user = getUser(ejbcaWS, endEntityName);
        if (user == null) {
            throw new NotFoundException("EndEntity", endEntityName);
        }

        prepareEndEntity(user, request, endEntityName);

        if (request.getStatus() != null) {
            user.setStatus(request.getStatus().getCode());
        }
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
    public void revokeAndDeleteEndEntity(String uuid, String endEntityProfileName, String endEntityName) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(uuid);

        UserDataVOWS user = getUser(ejbcaWS, endEntityName);
        if (user == null) {
            throw new NotFoundException("EndEntity", endEntityName);
        }

        try {
            ejbcaWS.revokeUser(endEntityName, CertificateRevocationReason.UNSPECIFIED.getReasonCode(), true);
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (CADoesntExistsException_Exception e) {
            throw new NotFoundException("CA for EndEntity", endEntityName);
        } catch (NotFoundException_Exception e) {
            throw new NotFoundException("EndEntity", endEntityName);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void resetPassword(String uuid, String raProfileName, String endEntityName) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(uuid);

        UserDataVOWS user = getUser(ejbcaWS, endEntityName);
        if (user == null) {
            throw new NotFoundException("EndEntity", endEntityName);
        }

        user.setPassword("NEWPASSWORD"); // constant for resetting passwords

        try {
            ejbcaWS.editUser(user);
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (CADoesntExistsException_Exception e) {
            throw new NotFoundException("CA for EndEntity", endEntityName);
        } catch (Exception e) {
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

    private List<UserDataVOWS> listUsers(EjbcaWS ejbcaWS, String endEntityProfileName) throws NotFoundException {
        UserMatch userMatch = EjbcaUtils.prepareEndEntityProfileMatch(endEntityProfileName);

        try {
            return ejbcaWS.findUser(userMatch);
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (EndEntityProfileNotFoundException_Exception e) {
            throw new NotFoundException("EndEntityProfile", endEntityProfileName);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void prepareEndEntity(UserDataVOWS user, BaseEndEntityRequestDto request, String username) {
        List<ResponseAttribute> raProfileAttrs = request.getRaProfile().getAttributes();

        NameAndIdDto endEntityProfile = AttributeDefinitionUtils.getObjectAttributeContentData(ATTRIBUTE_END_ENTITY_PROFILE, raProfileAttrs, NameAndIdDto.class).get(0);
        user.setEndEntityProfileName(endEntityProfile.getName());

        NameAndIdDto certificateProfile = AttributeDefinitionUtils.getObjectAttributeContentData(ATTRIBUTE_CERTIFICATE_PROFILE, raProfileAttrs, NameAndIdDto.class).get(0);
        user.setCertificateProfileName(certificateProfile.getName());

        NameAndIdDto ca = AttributeDefinitionUtils.getObjectAttributeContentData(ATTRIBUTE_CERTIFICATION_AUTHORITY, raProfileAttrs, NameAndIdDto.class).get(0);
        user.setCaName(ca.getName());

        Boolean sendNotifications = AttributeDefinitionUtils.getSingleItemAttributeContentValue(ATTRIBUTE_SEND_NOTIFICATIONS, raProfileAttrs, BooleanAttributeContentV2.class).getData();
        if (sendNotifications != null) {
            user.setSendNotification(sendNotifications);
        }

        Boolean keyRecoverable = AttributeDefinitionUtils.getSingleItemAttributeContentValue(ATTRIBUTE_KEY_RECOVERABLE, raProfileAttrs, BooleanAttributeContentV2.class).getData();
        if (keyRecoverable != null) {
            user.setKeyRecoverable(keyRecoverable);
        }

        if (StringUtils.isNotBlank(username)) {
            user.setUsername(username);
        }
        if (StringUtils.isNotBlank(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (StringUtils.isNotBlank(request.getPassword())) {
            user.setPassword(request.getPassword());
        }
        if (StringUtils.isNotBlank(request.getSubjectAltName())) {
            user.setSubjectAltName(request.getSubjectAltName());
        }
        if (StringUtils.isNotBlank(request.getSubjectDN())) {
            user.setSubjectDN(request.getSubjectDN());
        }

        // include extended information of end entity if available
        if (request.getExtensionData() != null && !request.getExtensionData().isEmpty()) {
            List<ExtendedInformationWS> ei = new ArrayList<>();
            for (EndEntityExtendedInfoDto extendedInformation : request.getExtensionData()) {
                ExtendedInformationWS eiWs = new ExtendedInformationWS();
                eiWs.setName(extendedInformation.getName());
                eiWs.setValue(extendedInformation.getValue());
                ei.add(eiWs);
            }
            user.getExtendedInformation().addAll(ei);
        }
    }
}
