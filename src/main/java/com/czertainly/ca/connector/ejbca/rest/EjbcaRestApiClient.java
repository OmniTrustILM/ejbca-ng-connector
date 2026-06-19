package com.czertainly.ca.connector.ejbca.rest;

import com.czertainly.api.clients.BaseApiClient;
import com.czertainly.api.exception.ValidationException;
import com.czertainly.api.model.common.attribute.common.BaseAttribute;
import com.czertainly.api.model.common.attribute.v2.content.FileAttributeContentV2;
import com.czertainly.api.model.common.attribute.v2.content.SecretAttributeContentV2;
import com.czertainly.api.model.common.attribute.v2.content.StringAttributeContentV2;
import com.czertainly.ca.connector.ejbca.config.TrustedCertificatesConfig;
import com.czertainly.ca.connector.ejbca.dao.entity.AuthorityInstance;
import com.czertainly.ca.connector.ejbca.dto.ejbca.response.ExceptionErrorRestResponse;
import com.czertainly.core.util.AttributeDefinitionUtils;
import com.czertainly.core.util.KeyStoreUtils;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

@Component
public abstract class EjbcaRestApiClient {

    @Autowired
    private TrustedCertificatesConfig trustedCertificatesConfig;

    // Certificate attribute names
    public static final String ATTRIBUTE_KEYSTORE_TYPE = "keyStoreType";
    public static final String ATTRIBUTE_KEYSTORE = "keyStore";
    public static final String ATTRIBUTE_KEYSTORE_PASSWORD = "keyStorePassword";
    public static final String ATTRIBUTE_TRUSTSTORE_TYPE = "trustStoreType";
    public static final String ATTRIBUTE_TRUSTSTORE = "trustStore";
    public static final String ATTRIBUTE_TRUSTSTORE_PASSWORD = "trustStorePassword";
    private static final Logger logger = LoggerFactory.getLogger(BaseApiClient.class);
    private static final ParameterizedTypeReference<List<String>> ERROR_LIST_TYPE_REF = new ParameterizedTypeReference<>() {
    };
    protected WebClient webClient;

    public static SslContext createSslContext(List<BaseAttribute> attributes, TrustManager[] defaultTrustManagers) {
        try {
            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();

            KeyManager km = null;
            FileAttributeContentV2 keyStoreData = AttributeDefinitionUtils.getSingleItemAttributeContentValue(ATTRIBUTE_KEYSTORE, attributes, FileAttributeContentV2.class);
            if (keyStoreData != null && keyStoreData.getData() != null && keyStoreData.getData().getContent() != null && !keyStoreData.getData().getContent().isEmpty()) {
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()); //"SunX509"

                String keyStoreType = AttributeDefinitionUtils.getSingleItemAttributeContentValue(ATTRIBUTE_KEYSTORE_TYPE, attributes, StringAttributeContentV2.class).getData();
                String keyStorePassword = AttributeDefinitionUtils.getSingleItemAttributeContentValue(ATTRIBUTE_KEYSTORE_PASSWORD, attributes, SecretAttributeContentV2.class).getData().getSecret();
                byte[] keyStoreBytes = Base64.getDecoder().decode(keyStoreData.getData().getContent());

                kmf.init(KeyStoreUtils.bytes2KeyStore(keyStoreBytes, keyStorePassword, keyStoreType), keyStorePassword.toCharArray());
                km = kmf.getKeyManagers()[0];
            }

            sslContextBuilder.keyManager(km);

            TrustManager tm;
            FileAttributeContentV2 trustStoreData = AttributeDefinitionUtils.getSingleItemAttributeContentValue(ATTRIBUTE_TRUSTSTORE, attributes, FileAttributeContentV2.class);
            if (trustStoreData != null && trustStoreData.getData() != null && trustStoreData.getData().getContent() != null && !trustStoreData.getData().getContent().isEmpty()) {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); //"SunX509"

                String trustStoreType = AttributeDefinitionUtils.getSingleItemAttributeContentValue(ATTRIBUTE_TRUSTSTORE_TYPE, attributes, StringAttributeContentV2.class).getData();
                String trustStorePassword = AttributeDefinitionUtils.getSingleItemAttributeContentValue(ATTRIBUTE_TRUSTSTORE_PASSWORD, attributes, SecretAttributeContentV2.class).getData().getSecret();
                byte[] trustStoreBytes = Base64.getDecoder().decode(trustStoreData.getData().getContent());

                tmf.init(KeyStoreUtils.bytes2KeyStore(trustStoreBytes, trustStorePassword, trustStoreType));
                tm = tmf.getTrustManagers()[0];
            } else {
                // return default TrustManager
                tm = defaultTrustManagers[0];
            }

            sslContextBuilder.trustManager(tm);
            return sslContextBuilder.protocols("TLSv1.2").build();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize SslContext.", e);
        }
    }

    public static WebClient prepareWebClient() {
        return WebClient.builder()
                .filter(ExchangeFilterFunction.ofResponseProcessor(EjbcaRestApiClient::handleHttpExceptions))
                .build();
    }

    public static Mono<ClientResponse> handleHttpExceptions(ClientResponse clientResponse) {
        if (clientResponse.statusCode().is4xxClientError() || clientResponse.statusCode().is5xxServerError()) {
            return clientResponse.bodyToMono(ExceptionErrorRestResponse.class).flatMap(body ->
                    Mono.error(new EjbcaRestApiException(body.getErrorMessage(), HttpStatus.valueOf(clientResponse.statusCode().value()), body)));
        }

        return Mono.just(clientResponse);
    }

    public static <T, R> R processRequest(Function<T, R> func, T request) {
        try {
            return func.apply(request);
        } catch (Exception e) {
            Throwable unwrapped = Exceptions.unwrap(e);
            logger.error(unwrapped.getMessage(), unwrapped);
        }
        return null;
    }

    public WebClient.RequestBodyUriSpec prepareRequest(HttpMethod method, List<BaseAttribute> authAttributes) {

        WebClient.RequestBodySpec request;

        SslContext sslContext = createSslContext(authAttributes, trustedCertificatesConfig.getDefaultTrustManagers());
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
        webClient.mutate().clientConnector(new ReactorClientHttpConnector(httpClient)).build();

        request = webClient.method(method);

        return (WebClient.RequestBodyUriSpec) request;
    }

    public void searchCertificates(AuthorityInstance instance) {
        List<BaseAttribute> attributes = AttributeDefinitionUtils.deserialize(instance.getCredentialData(), BaseAttribute.class);
        WebClient.RequestBodyUriSpec request = prepareRequest(HttpMethod.POST, attributes);

        processRequest(r -> r
                        .uri(getRestApiUrl(instance))
                        .retrieve()
                        .toEntity(Void.class)
                        .block().getBody(),
                request);

    }

    private String getRestApiUrl(AuthorityInstance instance) {
        URL wsUrl = null;
        try {
            wsUrl = new URL(instance.getUrl());
        } catch (MalformedURLException e) {
            logger.error(e.getMessage());
        }

        if (wsUrl == null)
            throw new ValidationException("Invalid or malformed authority instance URL. Authority instance UUID: " + instance.getUuid());

        return "https://" + wsUrl.getHost() + ":" + wsUrl.getPort() + "/ejbca/ejbca-rest-api/v2/certificate";
    }
}
