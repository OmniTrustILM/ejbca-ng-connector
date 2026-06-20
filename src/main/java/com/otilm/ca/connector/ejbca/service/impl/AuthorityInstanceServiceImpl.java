package com.otilm.ca.connector.ejbca.service.impl;

import com.otilm.api.exception.AlreadyExistException;
import com.otilm.api.exception.NotFoundException;
import com.otilm.api.exception.ValidationError;
import com.otilm.api.exception.ValidationException;
import com.otilm.api.model.common.attribute.v2.content.StringAttributeContentV2;
import com.otilm.api.model.common.attribute.common.content.data.CredentialAttributeContentData;
import com.otilm.api.model.connector.authority.AuthorityProviderInstanceDto;
import com.otilm.api.model.connector.authority.AuthorityProviderInstanceRequestDto;
import com.otilm.ca.connector.ejbca.dao.AuthorityInstanceRepository;
import com.otilm.ca.connector.ejbca.dao.entity.AuthorityInstance;
import com.otilm.ca.connector.ejbca.service.AttributeService;
import com.otilm.ca.connector.ejbca.service.AuthorityInstanceService;
import com.otilm.ca.connector.ejbca.ws.EjbcaWS;
import com.otilm.core.util.AttributeDefinitionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthorityInstanceServiceImpl implements AuthorityInstanceService {
    private static final Logger logger = LoggerFactory.getLogger(AuthorityInstanceServiceImpl.class);

    @Autowired
    private AuthorityInstanceRepository authorityInstanceRepository;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private EjbcaConnectionFactory ejbcaConnectionFactory;

    @Override
    public List<AuthorityProviderInstanceDto> listAuthorityInstances() {
        List<AuthorityInstance> authorities;
        authorities = authorityInstanceRepository.findAll();
        if (!authorities.isEmpty()) {
            return authorities
                    .stream().map(AuthorityInstance::mapToDto)
                    .collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public AuthorityProviderInstanceDto getAuthorityInstance(String uuid) throws NotFoundException {
        return authorityInstanceRepository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(AuthorityInstance.class, uuid))
                .mapToDto();
    }

    @Override
    public AuthorityProviderInstanceDto createAuthorityInstance(AuthorityProviderInstanceRequestDto request) throws AlreadyExistException {
        if (authorityInstanceRepository.findByName(request.getName()).isPresent()) {
            throw new AlreadyExistException(AuthorityInstance.class, request.getName());
        }

        if (!attributeService.validateAttributes(
                request.getKind(), request.getAttributes())) {
            throw new ValidationException("Authority instance attributes validation failed.");
        }

        AuthorityInstance instance = new AuthorityInstance();
        instance.setName(request.getName());
        instance.setUrl(AttributeDefinitionUtils.getSingleItemAttributeContentValue("url", request.getAttributes(), StringAttributeContentV2.class).getData());
        instance.setUuid(UUID.randomUUID().toString());
        CredentialAttributeContentData credential = AttributeDefinitionUtils.getCredentialContent("credential", request.getAttributes());
        instance.setCredentialUuid(credential.getUuid());
        instance.setCredentialData(AttributeDefinitionUtils.serialize(credential.getAttributes()));

        instance.setAttributes(AttributeDefinitionUtils.serialize(AttributeDefinitionUtils.mergeAttributes(attributeService.getAttributes(request.getKind()), request.getAttributes())));

        EjbcaWS connection;
        try {
            connection = ejbcaConnectionFactory.createConnection(instance);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ValidationException(ValidationError.create(ExceptionUtils.getRootCauseMessage(e)));
        }

        authorityInstanceRepository.save(instance);

        try {
            ejbcaConnectionFactory.put(instance.getId(), connection);
        } catch (Exception e) {
            logger.error("Fail to cache connection to CA {} due to error {}", instance.getId(), e.getMessage(), e);
        }

        return instance.mapToDto();
    }

    @Override
    public AuthorityProviderInstanceDto updateAuthorityInstance(String uuid, AuthorityProviderInstanceRequestDto request) throws NotFoundException {
        AuthorityInstance instance = authorityInstanceRepository
                .findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(AuthorityInstance.class, uuid));

        if (!attributeService.validateAttributes(
                request.getKind(), request.getAttributes())) {
            throw new ValidationException("Authority instance attributes validation failed.");
        }

        instance.setName(request.getName());
        instance.setUrl(AttributeDefinitionUtils.getSingleItemAttributeContentValue("url", request.getAttributes(), StringAttributeContentV2.class).getData());

        CredentialAttributeContentData credential = AttributeDefinitionUtils.getCredentialContent("credential", request.getAttributes());
        instance.setCredentialUuid(credential.getUuid());
        instance.setCredentialData(AttributeDefinitionUtils.serialize(credential.getAttributes()));

        instance.setAttributes(AttributeDefinitionUtils.serialize(AttributeDefinitionUtils.mergeAttributes(attributeService.getAttributes(request.getKind()), request.getAttributes())));

        EjbcaWS connection;
        try {
            connection = ejbcaConnectionFactory.createConnection(instance);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ValidationException(ValidationError.create(ExceptionUtils.getRootCauseMessage(e)));
        }

        authorityInstanceRepository.save(instance);

        try {
            ejbcaConnectionFactory.replace(instance.getId(), connection);
        } catch (Exception e) {
            logger.error("Fail to cache connection to CA {} due to error {}", instance.getId(), e.getMessage(), e);
        }

        return instance.mapToDto();
    }

    @Override
    public void removeAuthorityInstance(String uuid) throws NotFoundException {
        AuthorityInstance instance = authorityInstanceRepository
                .findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(AuthorityInstance.class, uuid));

        authorityInstanceRepository.delete(instance);

        try {
            ejbcaConnectionFactory.evict(instance.getId());
        } catch (Exception e) {
            logger.error("Fail to evict connection to CA {} due to error {}", instance.getId(), e.getMessage(), e);
        }
    }

    @Override
    public EjbcaWS getConnection(String uuid) throws NotFoundException {
        AuthorityInstance instance = authorityInstanceRepository
                .findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(AuthorityInstance.class, uuid));
        return getConnection(instance);
    }

    @Override
    public EjbcaWS getConnection(AuthorityInstance instance) {
        return ejbcaConnectionFactory.getOrCreate(instance);
    }

    @Override
    public WebClient getRestApiConnection(String uuid) throws NotFoundException {
        AuthorityInstance instance = authorityInstanceRepository
                .findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(AuthorityInstance.class, uuid));
        return getRestApiConnection(instance);
    }

    @Override
    public WebClient getRestApiConnection(AuthorityInstance instance) {
        return ejbcaConnectionFactory.getOrCreateRestApi(instance);
    }

    @Override
    public String getRestApiUrl(String authorityInstanceUuid) throws NotFoundException {
        AuthorityInstance instance = authorityInstanceRepository
                .findByUuid(authorityInstanceUuid)
                .orElseThrow(() -> new NotFoundException(AuthorityInstance.class, authorityInstanceUuid));

        URL wsUrl = null;
        try {
            wsUrl = new URL(instance.getUrl());
        } catch (MalformedURLException e) {
            logger.error(e.getMessage());
        }

        if (wsUrl == null)
            throw new ValidationException("Invalid or malformed authority instance URL. Authority instance UUID: " + authorityInstanceUuid);

        return "https://" + wsUrl.getHost() + (wsUrl.getPort() != -1 ? ":" + wsUrl.getPort() : "") + "/ejbca/ejbca-rest-api";
    }
}
