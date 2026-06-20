package com.otilm.ca.connector.ejbca.service;

import com.otilm.api.exception.AlreadyExistException;
import com.otilm.api.exception.NotFoundException;
import com.otilm.api.model.core.authority.AddEndEntityRequestDto;
import com.otilm.api.model.core.authority.EditEndEntityRequestDto;
import com.otilm.api.model.core.authority.EndEntityDto;
import com.otilm.ca.connector.ejbca.ws.EjbcaWS;
import com.otilm.ca.connector.ejbca.ws.UserDataVOWS;

import java.util.List;

public interface EndEntityEjbcaService {

    List<EndEntityDto> listEntities(String uuid, String endEntityProfileName) throws NotFoundException;

    EndEntityDto getEndEntity(String uuid, String endEntityProfileName, String endEntityName) throws NotFoundException;

    void createEndEntity(String uuid, String endEntityProfileName, AddEndEntityRequestDto request) throws NotFoundException, AlreadyExistException;

    void updateEndEntity(String uuid, String endEntityProfileName, String endEntityName, EditEndEntityRequestDto request) throws NotFoundException;

    void revokeAndDeleteEndEntity(String uuid, String endEntityProfileName, String username) throws NotFoundException;

    void resetPassword(String uuid, String endEntityProfileName, String username) throws NotFoundException;

    UserDataVOWS getUser(EjbcaWS ejbcaWS, String username) throws NotFoundException;
}
