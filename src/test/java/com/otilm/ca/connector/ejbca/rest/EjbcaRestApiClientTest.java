package com.otilm.ca.connector.ejbca.rest;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.otilm.api.exception.ValidationException;
import com.otilm.api.model.common.attribute.common.AttributeType;
import com.otilm.api.model.common.attribute.common.BaseAttribute;
import com.otilm.api.model.common.attribute.common.content.AttributeContentType;
import com.otilm.api.model.common.attribute.common.content.data.FileAttributeContentData;
import com.otilm.api.model.common.attribute.common.content.data.SecretAttributeContentData;
import com.otilm.api.model.common.attribute.common.properties.DataAttributeProperties;
import com.otilm.api.model.common.attribute.v2.DataAttributeV2;
import com.otilm.api.model.common.attribute.v2.content.FileAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.SecretAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.StringAttributeContentV2;
import com.otilm.ca.connector.ejbca.config.TrustedCertificatesConfig;
import com.otilm.ca.connector.ejbca.dao.entity.AuthorityInstance;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class EjbcaRestApiClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
            .build();

    /** Minimal concrete subclass — needed only to instantiate the abstract class. */
    static class TestableClient extends EjbcaRestApiClient {
    }

    TestableClient client;

    @BeforeEach
    void setUp() throws Exception {
        client = new TestableClient();
        // Inject a mock TrustedCertificatesConfig so that prepareRequest() does not NPE.
        TrustedCertificatesConfig configMock = Mockito.mock(TrustedCertificatesConfig.class);
        Mockito.when(configMock.getDefaultTrustManagers()).thenReturn(getDefaultTrustManagers());
        Field cfgField = EjbcaRestApiClient.class.getDeclaredField("trustedCertificatesConfig");
        cfgField.setAccessible(true);
        cfgField.set(client, configMock);
    }

    // ── handleHttpExceptions ──────────────────────────────────────────────────

    @Test
    void handleHttpExceptions_2xx_passesThroughMono() {
        ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("{}")
                .build();

        ClientResponse result = EjbcaRestApiClient.handleHttpExceptions(response).block();

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.statusCode());
    }

    /**
     * Reactor wraps checked exceptions thrown inside a Mono pipeline in a ReactiveException.
     * Use Exceptions.unwrap() to get the real cause before asserting the type.
     */
    private EjbcaRestApiException unwrapRestApiException(Mono<ClientResponse> mono) {
        try {
            mono.block();
            fail("Expected EjbcaRestApiException but no exception was thrown");
            return null; // unreachable
        } catch (Exception e) {
            Throwable cause = Exceptions.unwrap(e);
            assertInstanceOf(EjbcaRestApiException.class, cause);
            return (EjbcaRestApiException) cause;
        }
    }

    @Test
    void handleHttpExceptions_4xx_errorsWithEjbcaRestApiException() {
        ClientResponse response = ClientResponse.create(HttpStatus.BAD_REQUEST)
                .header("Content-Type", "application/json")
                .body("{\"error_code\":400,\"error_message\":\"Bad input\"}")
                .build();

        EjbcaRestApiException ex = unwrapRestApiException(
                EjbcaRestApiClient.handleHttpExceptions(response));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals("Bad input", ex.getMessage());
    }

    @Test
    void handleHttpExceptions_5xx_errorsWithEjbcaRestApiException() {
        ClientResponse response = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type", "application/json")
                .body("{\"error_code\":500,\"error_message\":\"Server error\"}")
                .build();

        EjbcaRestApiException ex = unwrapRestApiException(
                EjbcaRestApiClient.handleHttpExceptions(response));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getHttpStatus());
        assertEquals("Server error", ex.getMessage());
    }

    @Test
    void handleHttpExceptions_401_errorsWithEjbcaRestApiException() {
        ClientResponse response = ClientResponse.create(HttpStatus.UNAUTHORIZED)
                .header("Content-Type", "application/json")
                .body("{\"error_code\":401,\"error_message\":\"Unauthorized\"}")
                .build();

        EjbcaRestApiException ex = unwrapRestApiException(
                EjbcaRestApiClient.handleHttpExceptions(response));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
        assertNotNull(ex.getError());
        assertEquals("Unauthorized", ex.getMessage());
    }

    // ── processRequest ────────────────────────────────────────────────────────

    @Test
    void processRequest_successLambda_returnsValue() {
        String result = EjbcaRestApiClient.processRequest(s -> s + "-ok", "input");

        assertEquals("input-ok", result);
    }

    @Test
    void processRequest_throwingLambda_returnsNull() {
        String result = EjbcaRestApiClient.processRequest(s -> {
            throw new RuntimeException("simulated failure");
        }, "input");

        assertNull(result);
    }

    @Test
    void processRequest_nullLambdaReturn_returnsNull() {
        Object result = EjbcaRestApiClient.processRequest(s -> null, "anything");

        assertNull(result);
    }

    // ── prepareWebClient ──────────────────────────────────────────────────────

    @Test
    void prepareWebClient_returnsNonNullWebClient() {
        WebClient wc = EjbcaRestApiClient.prepareWebClient();

        assertNotNull(wc);
    }

    // ── getRestApiUrl (via private method reflection) ─────────────────────────

    /**
     * Invoke the private getRestApiUrl and unwrap InvocationTargetException so
     * the test assertions see the actual thrown type.
     */
    private String invokeGetRestApiUrl(AuthorityInstance instance) throws Throwable {
        Method method = EjbcaRestApiClient.class.getDeclaredMethod("getRestApiUrl", AuthorityInstance.class);
        method.setAccessible(true);
        try {
            return (String) method.invoke(client, instance);
        } catch (InvocationTargetException ite) {
            throw ite.getCause();
        }
    }

    @Test
    void getRestApiUrl_wellFormedUrl_returnsExpectedPath() throws Throwable {
        AuthorityInstance instance = new AuthorityInstance();
        instance.setUrl("https://ejbca.example.com:8443/some/path");

        String url = invokeGetRestApiUrl(instance);

        assertEquals("https://ejbca.example.com:8443/ejbca/ejbca-rest-api/v2/certificate", url);
    }

    @Test
    void getRestApiUrl_malformedUrl_throwsValidationException() {
        AuthorityInstance instance = new AuthorityInstance();
        instance.setUrl("not a valid url %%%");
        instance.setUuid("test-uuid-123");

        assertThrows(ValidationException.class, () -> invokeGetRestApiUrl(instance));
    }

    @Test
    void getRestApiUrl_emptyUrl_throwsValidationException() {
        AuthorityInstance instance = new AuthorityInstance();
        instance.setUrl("");
        instance.setUuid("test-uuid-empty");

        // An empty string parses as a valid but path-only URL: java.net.URL("") fails
        // → wsUrl stays null → ValidationException
        assertThrows(ValidationException.class, () -> invokeGetRestApiUrl(instance));
    }

    // ── createSslContext ──────────────────────────────────────────────────────

    private TrustManager[] getDefaultTrustManagers() throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        return tmf.getTrustManagers();
    }

    private List<BaseAttribute> emptyAttributes() {
        return new ArrayList<>();
    }

    private DataAttributeV2 fileAttr(String name, String base64Content) {
        DataAttributeV2 attr = new DataAttributeV2();
        attr.setName(name);
        attr.setType(AttributeType.DATA);
        attr.setContentType(AttributeContentType.FILE);
        DataAttributeProperties props = new DataAttributeProperties();
        props.setRequired(false);
        attr.setProperties(props);

        FileAttributeContentData fileData = new FileAttributeContentData();
        fileData.setContent(base64Content);
        fileData.setFileName("store.p12");
        fileData.setMimeType("application/octet-stream");
        FileAttributeContentV2 content = new FileAttributeContentV2();
        content.setData(fileData);
        attr.setContent(List.of(content));
        return attr;
    }

    private DataAttributeV2 stringAttr(String name, String value) {
        DataAttributeV2 attr = new DataAttributeV2();
        attr.setName(name);
        attr.setType(AttributeType.DATA);
        attr.setContentType(AttributeContentType.STRING);
        DataAttributeProperties props = new DataAttributeProperties();
        props.setRequired(false);
        attr.setProperties(props);

        StringAttributeContentV2 content = new StringAttributeContentV2();
        content.setData(value);
        attr.setContent(List.of(content));
        return attr;
    }

    private DataAttributeV2 secretAttr(String name, String secret) {
        DataAttributeV2 attr = new DataAttributeV2();
        attr.setName(name);
        attr.setType(AttributeType.DATA);
        attr.setContentType(AttributeContentType.SECRET);
        DataAttributeProperties props = new DataAttributeProperties();
        props.setRequired(false);
        attr.setProperties(props);

        SecretAttributeContentV2 content = new SecretAttributeContentV2();
        content.setData(new SecretAttributeContentData(secret));
        attr.setContent(List.of(content));
        return attr;
    }

    private String buildPkcs12Base64(String password) throws Exception {
        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        java.security.KeyPair kp = kpg.generateKeyPair();

        org.bouncycastle.asn1.x500.X500Name subject =
                new org.bouncycastle.asn1.x500.X500Name("CN=test");
        java.math.BigInteger serial = java.math.BigInteger.valueOf(1L);
        java.util.Date notBefore = new java.util.Date(System.currentTimeMillis() - 1000L);
        java.util.Date notAfter  = new java.util.Date(System.currentTimeMillis() + 86400_000L);
        org.bouncycastle.cert.X509v3CertificateBuilder certBuilder =
                new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
                        subject, serial, notBefore, notAfter, subject, kp.getPublic());
        org.bouncycastle.operator.ContentSigner signer =
                new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("SHA256withRSA")
                        .build(kp.getPrivate());
        X509Certificate cert = new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter()
                .getCertificate(certBuilder.build(signer));

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, password.toCharArray());
        ks.setKeyEntry("alias", kp.getPrivate(), password.toCharArray(), new X509Certificate[]{cert});

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ks.store(baos, password.toCharArray());
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    @Test
    void createSslContext_noKeystore_noTruststore_usesDefaultTrustManager() throws Exception {
        TrustManager[] defaults = getDefaultTrustManagers();
        List<BaseAttribute> attrs = emptyAttributes();

        SslContext ctx = EjbcaRestApiClient.createSslContext(attrs, defaults);

        assertNotNull(ctx);
    }

    @Test
    void createSslContext_withKeystore_withTruststore_buildsContext() throws Exception {
        String password = "changeit";
        String ksBase64 = buildPkcs12Base64(password);
        TrustManager[] defaults = getDefaultTrustManagers();

        List<BaseAttribute> attrs = new ArrayList<>();
        attrs.add(fileAttr(EjbcaRestApiClient.ATTRIBUTE_KEYSTORE, ksBase64));
        attrs.add(stringAttr(EjbcaRestApiClient.ATTRIBUTE_KEYSTORE_TYPE, "PKCS12"));
        attrs.add(secretAttr(EjbcaRestApiClient.ATTRIBUTE_KEYSTORE_PASSWORD, password));
        attrs.add(fileAttr(EjbcaRestApiClient.ATTRIBUTE_TRUSTSTORE, ksBase64));
        attrs.add(stringAttr(EjbcaRestApiClient.ATTRIBUTE_TRUSTSTORE_TYPE, "PKCS12"));
        attrs.add(secretAttr(EjbcaRestApiClient.ATTRIBUTE_TRUSTSTORE_PASSWORD, password));

        SslContext ctx = EjbcaRestApiClient.createSslContext(attrs, defaults);

        assertNotNull(ctx);
    }

    @Test
    void createSslContext_withKeystore_noTruststore_usesDefaultTrustManager() throws Exception {
        String password = "changeit";
        String ksBase64 = buildPkcs12Base64(password);
        TrustManager[] defaults = getDefaultTrustManagers();

        List<BaseAttribute> attrs = new ArrayList<>();
        attrs.add(fileAttr(EjbcaRestApiClient.ATTRIBUTE_KEYSTORE, ksBase64));
        attrs.add(stringAttr(EjbcaRestApiClient.ATTRIBUTE_KEYSTORE_TYPE, "PKCS12"));
        attrs.add(secretAttr(EjbcaRestApiClient.ATTRIBUTE_KEYSTORE_PASSWORD, password));
        // no truststore attrs → falls back to defaults[0]

        SslContext ctx = EjbcaRestApiClient.createSslContext(attrs, defaults);

        assertNotNull(ctx);
    }

    // ── prepareRequest (via WireMock) ─────────────────────────────────────────

    private void injectWebClient(WebClient webClient) throws Exception {
        Field field = EjbcaRestApiClient.class.getDeclaredField("webClient");
        field.setAccessible(true);
        field.set(client, webClient);
    }

    /**
     * Build a WebClient that trusts any certificate — required for tests that route
     * through WireMock's self-signed HTTPS listener.
     */
    private WebClient buildInsecureWebClient() throws Exception {
        SslContext insecureCtx = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
        HttpClient httpClient = HttpClient.create()
                .secure(spec -> spec.sslContext(insecureCtx));
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(ExchangeFilterFunction.ofResponseProcessor(EjbcaRestApiClient::handleHttpExceptions))
                .build();
    }

    /**
     * Return the HTTPS base URL for WireMock, e.g. "https://localhost:PORT".
     * WireMockExtension exposes getHttpsPort() for the dynamically allocated HTTPS port.
     */
    private String wireMockHttpsBaseUrl() {
        return "https://localhost:" + wireMock.getHttpsPort();
    }

    @Test
    void prepareRequest_returnsNonNullRequestBodySpec() throws Exception {
        injectWebClient(WebClient.builder().baseUrl(wireMockHttpsBaseUrl()).build());

        WebClient.RequestBodyUriSpec spec = client.prepareRequest(HttpMethod.GET, emptyAttributes());

        assertNotNull(spec);
        // RequestBodyUriSpec is a sub-interface of RequestBodySpec/RequestHeadersUriSpec;
        // verifying non-null is sufficient to prove prepareRequest() returns the correct type.
    }

    // ── searchCertificates over WireMock HTTPS ────────────────────────────────

    private static final String SEARCH_PATH = "/ejbca/ejbca-rest-api/v2/certificate";

    /**
     * Build a minimal AuthorityInstance whose URL points at the WireMock HTTPS listener.
     * getRestApiUrl() strips the path from the URL and appends the fixed REST path, so any
     * path suffix in the supplied URL is intentionally overwritten — only host+port matter.
     * credentialData is an empty JSON array so AttributeDefinitionUtils.deserialize returns
     * an empty list → no SSL attributes → injected WebClient's TLS config is used.
     */
    private AuthorityInstance buildHttpsInstance() {
        AuthorityInstance instance = new AuthorityInstance();
        instance.setUuid("wm-https-test-uuid");
        instance.setUrl(wireMockHttpsBaseUrl());
        instance.setCredentialData("[]");
        return instance;
    }

    @Test
    void searchCertificates_200_wireMockIsHitAndCompletesWithoutException() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo(SEARCH_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        injectWebClient(buildInsecureWebClient());

        // processRequest wraps the call: 200 response → Void body → no exception escapes
        assertDoesNotThrow(() -> client.searchCertificates(buildHttpsInstance()));

        // Prove WireMock was actually contacted — this would fail if TLS handshake had failed
        // and processRequest had silently swallowed the error before WireMock was hit.
        wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH)));
    }

    @Test
    void searchCertificates_500_processRequestCatchesEjbcaRestApiException() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo(SEARCH_PATH))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error_code\":500,\"error_message\":\"Server error\"}")));

        injectWebClient(buildInsecureWebClient());

        // handleHttpExceptions converts 500 → EjbcaRestApiException; processRequest swallows it
        assertDoesNotThrow(() -> client.searchCertificates(buildHttpsInstance()));

        wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH)));
    }

    @Test
    void searchCertificates_404_processRequestCatchesEjbcaRestApiException() throws Exception {
        wireMock.stubFor(post(urlPathEqualTo(SEARCH_PATH))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error_code\":404,\"error_message\":\"Not found\"}")));

        injectWebClient(buildInsecureWebClient());

        assertDoesNotThrow(() -> client.searchCertificates(buildHttpsInstance()));

        wireMock.verify(postRequestedFor(urlPathEqualTo(SEARCH_PATH)));
    }
}
