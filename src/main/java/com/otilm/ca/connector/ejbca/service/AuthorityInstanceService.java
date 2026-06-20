package com.otilm.ca.connector.ejbca.service;

import com.otilm.api.exception.AlreadyExistException;
import com.otilm.api.exception.NotFoundException;
import com.otilm.api.model.connector.authority.AuthorityProviderInstanceDto;
import com.otilm.api.model.connector.authority.AuthorityProviderInstanceRequestDto;
import com.otilm.ca.connector.ejbca.dao.entity.AuthorityInstance;
import com.otilm.ca.connector.ejbca.ws.EjbcaWS;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

public interface AuthorityInstanceService {

    List<AuthorityProviderInstanceDto> listAuthorityInstances();
    AuthorityProviderInstanceDto getAuthorityInstance(String uuid) throws NotFoundException;
    AuthorityProviderInstanceDto createAuthorityInstance(AuthorityProviderInstanceRequestDto request) throws AlreadyExistException;
    AuthorityProviderInstanceDto updateAuthorityInstance(String uuid, AuthorityProviderInstanceRequestDto request) throws NotFoundException;
    void removeAuthorityInstance(String uuid) throws NotFoundException;

    EjbcaWS getConnection(String uuid) throws NotFoundException;

    EjbcaWS getConnection(AuthorityInstance instance);

    WebClient getRestApiConnection(String uuid) throws NotFoundException;

    WebClient getRestApiConnection(AuthorityInstance instance);

    String getRestApiUrl(String authorityInstanceUuid) throws NotFoundException;
}
