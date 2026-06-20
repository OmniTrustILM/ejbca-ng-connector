package com.otilm.ca.connector.ejbca.service.impl;

import com.otilm.api.model.common.attribute.common.BaseAttribute;
import com.otilm.api.model.common.attribute.v2.content.FileAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.SecretAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.StringAttributeContentV2;
import com.otilm.ca.connector.ejbca.config.ApplicationConfig;
import com.otilm.ca.connector.ejbca.config.TrustedCertificatesConfig;
import com.otilm.ca.connector.ejbca.dao.entity.AuthorityInstance;
import com.otilm.ca.connector.ejbca.rest.EjbcaRestApiClient;
import com.otilm.ca.connector.ejbca.ws.EjbcaWS;
import com.otilm.ca.connector.ejbca.ws.EjbcaWSService;
import com.otilm.core.util.AttributeDefinitionUtils;
import com.otilm.core.util.KeyStoreUtils;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import jakarta.xml.ws.BindingProvider;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns all EJBCA network/SSL connection creation and caching.
 * Exercised by EJBCAIT (needs a live EJBCA) — coverage-excluded in pom.xml.
 */
@Component
public class EjbcaConnectionFactory {

    private static final Logger logger = LoggerFactory.getLogger(EjbcaConnectionFactory.class);

    /**
     * This is the maximum size in bytes of the payload
     */
    @Value("${spring.codec.max-in-memory-size:2000000}")
    private int maxPayloadSize;

    @Value("${ejbca.timeout.connect:5000}")
    private int connectionTimeout;

    @Value("${ejbca.timeout.request:30000}")
    private int requestTimeout;

    @Autowired
    private TrustedCertificatesConfig trustedCertificatesConfig;

    private final Map<Long, EjbcaWS> connectionsCache = new ConcurrentHashMap<>();
    private final Map<Long, WebClient> connectionsRestApiCache = new ConcurrentHashMap<>();

    /**
     * Returns a cached SOAP connection for the given instance, creating one if absent.
     * Thread-safe: synchronized to prevent duplicate connection creation under contention.
     */
    public synchronized EjbcaWS getOrCreate(AuthorityInstance instance) {
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

    /**
     * Returns a cached REST WebClient for the given instance, creating one if absent.
     * Thread-safe: synchronized to prevent duplicate connection creation under contention.
     */
    public synchronized WebClient getOrCreateRestApi(AuthorityInstance instance) {
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

    /** Stores (or replaces) a SOAP connection in the cache, keyed by instance id. */
    public void put(Long instanceId, EjbcaWS connection) {
        connectionsCache.put(instanceId, connection);
    }

    /** Replaces an existing SOAP connection in the cache, keyed by instance id. */
    public void replace(Long instanceId, EjbcaWS connection) {
        connectionsCache.replace(instanceId, connection);
    }

    /** Removes the SOAP and REST connections for the given instance id from both caches. */
    public void evict(Long instanceId) {
        connectionsCache.remove(instanceId);
        connectionsRestApiCache.remove(instanceId);
    }

    /**
     * Creates and verifies a new JAX-WS SOAP connection to the given authority instance.
     * Makes a live network call ({@code port.getEjbcaVersion()}) — requires a reachable EJBCA.
     */
    public EjbcaWS createConnection(AuthorityInstance instance) {
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
     */
    void applyTimeouts(Map<String, Object> requestContext) {
        requestContext.put(ApplicationConfig.CONNECT_TIMEOUT, connectionTimeout);
        requestContext.put(ApplicationConfig.REQUEST_TIMEOUT, requestTimeout);
    }

    /**
     * Applies the configured connect and response (read) timeouts to the reactor-netty HTTP client
     * used for EJBCA REST calls.
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
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

                String keyStoreType = AttributeDefinitionUtils.getSingleItemAttributeContentValue("keyStoreType", attributes, StringAttributeContentV2.class).getData();
                String keyStorePassword = AttributeDefinitionUtils.getSingleItemAttributeContentValue("keyStorePassword", attributes, SecretAttributeContentV2.class).getData().getSecret();
                byte[] keyStoreBytes = Base64.getDecoder().decode(keyStoreData.getData().getContent());

                kmf.init(KeyStoreUtils.bytes2KeyStore(keyStoreBytes, keyStorePassword, keyStoreType), keyStorePassword.toCharArray());
                km = kmf.getKeyManagers();
            }

            TrustManager[] tm = null;
            FileAttributeContentV2 trustStoreData = AttributeDefinitionUtils.getSingleItemAttributeContentValue("trustStore", attributes, FileAttributeContentV2.class);
            if (trustStoreData != null && trustStoreData.getData() != null && trustStoreData.getData().getContent() != null && !trustStoreData.getData().getContent().isEmpty()) {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

                String trustStoreType = AttributeDefinitionUtils.getSingleItemAttributeContentValue("trustStoreType", attributes, StringAttributeContentV2.class).getData();
                String trustStorePassword = AttributeDefinitionUtils.getSingleItemAttributeContentValue("trustStorePassword", attributes, SecretAttributeContentV2.class).getData().getSecret();
                byte[] trustStoreBytes = Base64.getDecoder().decode(trustStoreData.getData().getContent());

                tmf.init(KeyStoreUtils.bytes2KeyStore(trustStoreBytes, trustStorePassword, trustStoreType));
                tm = tmf.getTrustManagers();
            } else {
                tm = trustedCertificatesConfig.getDefaultTrustManagers();
            }

            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(km, tm, new SecureRandom());

            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize SSLSocketFactory.", e);
        }
    }

    private WebClient createRestApiConnection(AuthorityInstance instance) {
        List<BaseAttribute> attributes = AttributeDefinitionUtils.deserialize(instance.getCredentialData(), BaseAttribute.class);

        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(maxPayloadSize))
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
