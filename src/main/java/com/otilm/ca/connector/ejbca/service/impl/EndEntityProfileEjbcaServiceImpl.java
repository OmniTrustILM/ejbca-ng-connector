package com.otilm.ca.connector.ejbca.service.impl;

import com.otilm.api.exception.NotFoundException;
import com.otilm.api.model.common.NameAndIdDto;
import com.otilm.ca.connector.ejbca.service.AuthorityInstanceService;
import com.otilm.ca.connector.ejbca.service.EndEntityProfileEjbcaService;
import com.otilm.ca.connector.ejbca.ws.AuthorizationDeniedException_Exception;
import com.otilm.ca.connector.ejbca.ws.EjbcaException_Exception;
import com.otilm.ca.connector.ejbca.ws.EjbcaWS;
import com.otilm.ca.connector.ejbca.ws.NameAndId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EndEntityProfileEjbcaServiceImpl implements EndEntityProfileEjbcaService {

    @Autowired
    public void setAuthorityInstanceService(AuthorityInstanceService authorityInstanceService) {
        this.authorityInstanceService = authorityInstanceService;
    }

    private AuthorityInstanceService authorityInstanceService;

    @Override
    public List<NameAndIdDto> listEndEntityProfiles(String uuid) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(uuid);

        try {
            List<NameAndId> endEntityProfiles = ejbcaWS.getAuthorizedEndEntityProfiles();
            if (endEntityProfiles == null || endEntityProfiles.isEmpty()) {
                throw new NotFoundException("EndEntityProfile on ca", uuid);
            }

            return endEntityProfiles.stream()
                    .map(p -> new NameAndIdDto(p.getId(), p.getName()))
                    .collect(Collectors.toList());
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (EjbcaException_Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<NameAndIdDto> listCertificateProfiles(String uuid, int endEntityProfileId) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(uuid);
        try {
            List<NameAndId> profiles = ejbcaWS.getAvailableCertificateProfiles(endEntityProfileId);
            if (profiles == null || profiles.isEmpty()) {
                throw new NotFoundException("CertificateProfile on ca", uuid);
            }

            return profiles.stream().map(p -> new NameAndIdDto(p.getId(), p.getName())).collect(Collectors.toList());
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (EjbcaException_Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<NameAndIdDto> listCAsInProfile(String uuid, int endEntityProfileId) throws NotFoundException {
        EjbcaWS ejbcaWS = authorityInstanceService.getConnection(uuid);
        try {
            List<NameAndId> profiles = ejbcaWS.getAvailableCAsInProfile(endEntityProfileId);
            if (profiles == null || profiles.isEmpty()) {
                throw new NotFoundException("CAInProfile on ca", uuid);
            }

            return profiles.stream().map(p -> new NameAndIdDto(p.getId(), p.getName())).collect(Collectors.toList());
        } catch (AuthorizationDeniedException_Exception e) {
            throw new AccessDeniedException("Authorization denied on EJBCA", e);
        } catch (EjbcaException_Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
