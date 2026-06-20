package com.otilm.ca.connector.ejbca.service;

import com.otilm.api.exception.NotFoundException;
import com.otilm.api.model.common.NameAndIdDto;

import java.util.List;

public interface EndEntityProfileEjbcaService {

    List<NameAndIdDto> listEndEntityProfiles(String uuid) throws NotFoundException;

    List<NameAndIdDto> listCertificateProfiles(String uuid, int endEntityProfileId) throws NotFoundException;

    List<NameAndIdDto> listCAsInProfile(String uuid, int endEntityProfileId) throws NotFoundException;
}
