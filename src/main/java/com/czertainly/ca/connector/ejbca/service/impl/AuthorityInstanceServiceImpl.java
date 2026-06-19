package com.czertainly.ca.connector.ejbca.service.impl;

import com.czertainly.api.exception.AlreadyExistException;
import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.exception.ValidationError;
import com.czertainly.api.exception.ValidationException;
import com.czertainly.api.model.common.attribute.common.BaseAttribute;
import com.czertainly.api.model.common.attribute.v2.content.FileAttributeContentV2;
import com.czertainly.api.model.common.attribute.v2.content.SecretAttributeContentV2;
import com.czertainly.api.model.common.attribute.v2.content.StringAttributeContentV2;
import com.czertainly.api.model.common.attribute.common.content.data.CredentialAttributeContentData;
import com.czertainly.api.model.connector.authority.AuthorityProviderInstanceDto;
import com.czertainly.api.model.connector.authority.AuthorityProviderInstanceRequestDto;
import com.czertainly.ca.connector.ejbca.config.ApplicationConfig;
import com.czertainly.ca.connector.ejbca.config.TrustedCertificatesConfig;
import com.czertainly.ca.connector.ejbca.dao.AuthorityInstanceRepository;
import com.czertainly.ca.connector.ejbca.dao.entity.AuthorityInstance;
import com.czertainly.ca.connector.ejbca.rest.EjbcaRestApiClient;
import com.czertainly.ca.connector.ejbca.service.AttributeService;
import com.czertainly.ca.connector.ejbca.service.AuthorityInstanceService;
import com.czertainly.ca.connector.ejbca.ws.EjbcaWS;
import com.czertainly.ca.connector.ejbca.ws.EjbcaWSService;
import com.czertainly.core.util.AttributeDefinitionUtils;
import com.czertainly.core.util.KeyStoreUtils;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import jakarta.xml.ws.BindingProvider;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthorityInstanceServiceImpl implements AuthorityInstanceService {
    private static final Logger logger = LoggerFactory.getLogger(AuthorityInstanceServiceImpl.class);

    /**
     * This is the maximum size in bytes of the payload
     */
    @Value("${spring.codec.max-in-memory-size:2000000}")
    private int MAX_PAYLOAD_SIZE;

    private static final Map<Long, EjbcaWS> connectionsCache = new ConcurrentHashMap<>();
    private static final Map<Long, WebClient> connectionsRestApiCache = new ConcurrentHashMap<>();

    @Value("${ejbca.timeout.connect:5000}")
    private int connectionTimeout;

    @Value("${ejbca.timeout.request:30000}")
    private int requestTimeout;

    @Autowired
    private AuthorityInstanceRepository authorityInstanceRepository;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private TrustedCertificatesConfig trustedCertificatesConfig;

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
            connection = createConnection(instance);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ValidationException(ValidationError.create(ExceptionUtils.getRootCauseMessage(e)));
        }

        authorityInstanceRepository.save(instance);

        try {
            connectionsCache.put(instance.getId(), connection);
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
            connection = createConnection(instance);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ValidationException(ValidationError.create(ExceptionUtils.getRootCauseMessage(e)));
        }

        authorityInstanceRepository.save(instance);

        try {
            connectionsCache.replace(instance.getId(), connection);
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
            connectionsCache.remove(instance.getId());
        } catch (Exception e) {
            logger.error("Fail to cache connection to CA {} due to error {}", instance.getId(), e.getMessage(), e);
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
    public synchronized EjbcaWS getConnection(AuthorityInstance instance) {
        EjbcaWS port = connectionsCache.get(instance.getId());
        if (port != null) {
            return port;
        }

        port = createConnection(instance);

        try {
            connectionsCache.put(instance.getId(), port);
        } catch (Exception e) {
            logger.error("Fail to cache connection to CA {} due to error {}", instance.getId(), e.getMessage(), e);
        }

        return port;
    }

    private EjbcaWS createConnection(AuthorityInstance instance) {
        EjbcaWSService service = new EjbcaWSService(ApplicationConfig.WSDL_URL);
        EjbcaWS port = service.getEjbcaWSPort();
        final Map<String, Object> requestContext = ((BindingProvider) port).getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, instance.getUrl());
        applyTimeouts(requestContext);
        requestContext.put(ApplicationConfig.REQUEST_SSL_SOCKET_FACTORY, createSSLSocketFactory(instance));

        logger.info("Connected to EJBCA {}", port.getEjbcaVersion());

        return port;
    }

    /**
     * Applies the configured connect and request (read) timeouts to the JAX-WS request context.
     * The request timeout is driven by {@code ejbca.timeout.request}; it was previously hardcoded,
     * which ignored the configured value and caused SocketTimeoutException ("Read timed out") once
     * an EJBCA SOAP call took longer than the fixed limit.
     */
    void applyTimeouts(Map<String, Object> requestContext) {
        requestContext.put(ApplicationConfig.CONNECT_TIMEOUT, connectionTimeout);
        requestContext.put(ApplicationConfig.REQUEST_TIMEOUT, requestTimeout);
    }

    /**
     * Applies the configured connect and response (read) timeouts to the reactor-netty HTTP client
     * used for EJBCA REST calls. The client previously had no read timeout, so an unresponsive REST
     * endpoint could hang the call indefinitely.
     */
    HttpClient withTimeouts(HttpClient httpClient) {
        return httpClient
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                .responseTimeout(Duration.ofMillis(requestTimeout));
    }

    private SSLSocketFactory createSSLSocketFactory(AuthorityInstance instance) {
        try {
            List<BaseAttribute> attributes = AttributeDefinitionUtils.deserialize(instance.getCredentialData(), BaseAttribute.class);

            KeyManager[] km = null;
            FileAttributeContentV2 keyStoreData = AttributeDefinitionUtils.getSingleItemAttributeContentValue("keyStore", attributes, FileAttributeContentV2.class);
            if (keyStoreData != null && keyStoreData.getData() != null && keyStoreData.getData().getContent() != null && !keyStoreData.getData().getContent().isEmpty()) {
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()); //"SunX509"

                String keyStoreType = AttributeDefinitionUtils.getSingleItemAttributeContentValue("keyStoreType", attributes, StringAttributeContentV2.class).getData();
                String keyStorePassword = AttributeDefinitionUtils.getSingleItemAttributeContentValue("keyStorePassword", attributes, SecretAttributeContentV2.class).getData().getSecret();
                byte[] keyStoreBytes = Base64.getDecoder().decode(keyStoreData.getData().getContent());

                kmf.init(KeyStoreUtils.bytes2KeyStore(keyStoreBytes, keyStorePassword, keyStoreType), keyStorePassword.toCharArray());
                km = kmf.getKeyManagers();
            }

            TrustManager[] tm = null;
            FileAttributeContentV2 trustStoreData = AttributeDefinitionUtils.getSingleItemAttributeContentValue("trustStore", attributes, FileAttributeContentV2.class);
            if (trustStoreData != null && trustStoreData.getData() != null && trustStoreData.getData().getContent() != null && !trustStoreData.getData().getContent().isEmpty()) {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); //"SunX509"

                String trustStoreType = AttributeDefinitionUtils.getSingleItemAttributeContentValue("trustStoreType", attributes, StringAttributeContentV2.class).getData();
                String trustStorePassword = AttributeDefinitionUtils.getSingleItemAttributeContentValue("trustStorePassword", attributes, SecretAttributeContentV2.class).getData().getSecret();
                byte[] trustStoreBytes = Base64.getDecoder().decode(trustStoreData.getData().getContent());

                tmf.init(KeyStoreUtils.bytes2KeyStore(trustStoreBytes, trustStorePassword, trustStoreType));
                tm = tmf.getTrustManagers();
            } else {
                // return default TrustManagers
                tm = trustedCertificatesConfig.getDefaultTrustManagers();
            }

            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(km, tm, new SecureRandom());

            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize SSLSocketFactory.", e);
        }
    }

    @Override
    public WebClient getRestApiConnection(String uuid) throws NotFoundException {
        AuthorityInstance instance = authorityInstanceRepository
                .findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(AuthorityInstance.class, uuid));
        return getRestApiConnection(instance);
    }

    @Override
    public synchronized WebClient getRestApiConnection(AuthorityInstance instance) {
        WebClient webClient = connectionsRestApiCache.get(instance.getId());
        if (webClient != null) {
            return webClient;
        }

        webClient = createRestApiConnection(instance);

        try {
            connectionsRestApiCache.put(instance.getId(), webClient);
        } catch (Exception e) {
            logger.error("Fail to cache REST API connection to CA {} due to error {}", instance.getId(), e.getMessage(), e);
        }

        return webClient;
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

    private WebClient createRestApiConnection(AuthorityInstance instance) {
        List<BaseAttribute> attributes = AttributeDefinitionUtils.deserialize(instance.getCredentialData(), BaseAttribute.class);

        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(MAX_PAYLOAD_SIZE))
                .build();

        SslContext sslContext = EjbcaRestApiClient.createSslContext(attributes, trustedCertificatesConfig.getDefaultTrustManagers());

        HttpClient httpClient = withTimeouts(HttpClient.create()).secure(t -> t.sslContext(sslContext));

        return WebClient
                .builder()
                .filter(ExchangeFilterFunction.ofResponseProcessor(EjbcaRestApiClient::handleHttpExceptions))
                .exchangeStrategies(strategies)
                .clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }
}
