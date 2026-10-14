package com.otilm.ca.connector.ejbca.ws;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.otilm.ca.connector.ejbca.Goldens;
import com.otilm.ca.connector.ejbca.config.ApplicationConfig;
import jakarta.xml.ws.BindingProvider;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Pins the SOAP the connector exchanges with EJBCA's web service, through the JAX-WS port built from the bundled WSDL
 * as {@code EjbcaConnectionFactory} builds it, pointed at a stand-in EJBCA. {@link Goldens} says how its golden files
 * are recorded.
 */
class EjbcaSoapWireTest {

    private static final String EJBCA_WS = "/ejbca/ejbcaws/ejbcaws";

    private static WireMockServer ejbca;

    @BeforeAll
    static void startEjbca() {
        ejbca = new WireMockServer(options().dynamicPort());
        ejbca.start();
    }

    @AfterAll
    static void stopEjbca() {
        ejbca.stop();
    }

    @BeforeEach
    void resetEjbca() {
        ejbca.resetAll();
    }

    @Test
    void readsTheEjbcaVersion() throws Exception {
        answer("soap-getEjbcaVersion-response.xml");

        String version = port().getEjbcaVersion();

        assertThat(version).isEqualTo("EJBCA 9.1.1 Community (5045c834d0c99315db96d88d32cc449ff334b07f)");
        Goldens.assertMatches("soap-getEjbcaVersion-request.xml", receivedEnvelope());
    }

    @Test
    void readsTheAvailableCas() throws Exception {
        answer("soap-getAvailableCAs-response.xml");

        List<NameAndId> cas = port().getAvailableCAs();

        assertThat(cas)
                .extracting(NameAndId::getId, NameAndId::getName)
                .containsExactly(tuple(-1688117755, "ManagementCA"), tuple(1652389506, "Issuing CA & TLS"));
        Goldens.assertMatches("soap-getAvailableCAs-request.xml", receivedEnvelope());
    }

    /** The port {@code EjbcaConnectionFactory.createConnection} builds, without the TLS an authority adds. */
    private static EjbcaWS port() {
        EjbcaWS port = new EjbcaWSService(ApplicationConfig.WSDL_URL).getEjbcaWSPort();
        Map<String, Object> requestContext = ((BindingProvider) port).getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, ejbca.baseUrl() + EJBCA_WS);
        requestContext.put(ApplicationConfig.CONNECT_TIMEOUT, 5000);
        requestContext.put(ApplicationConfig.REQUEST_TIMEOUT, 30000);
        return port;
    }

    private static void answer(String envelope) throws Exception {
        ejbca
                .stubFor(post(EJBCA_WS)
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "text/xml; charset=UTF-8")
                                .withBody(Goldens.read(envelope))));
    }

    private static String receivedEnvelope() {
        assertThat(ejbca.getAllServeEvents()).hasSize(1);
        return ejbca.getAllServeEvents().get(0).getRequest().getBodyAsString();
    }
}
