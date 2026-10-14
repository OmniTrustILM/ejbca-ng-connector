package com.otilm.ca.connector.ejbca.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.otilm.ca.connector.ejbca.dto.ejbca.request.Pagination;
import com.otilm.ca.connector.ejbca.dto.ejbca.request.SearchCertificateCriteriaRestRequest;
import com.otilm.ca.connector.ejbca.dto.ejbca.request.SearchCertificatesRestRequestV2;
import com.otilm.ca.connector.ejbca.dto.ejbca.response.SearchCertificatesRestResponseV2;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;
import reactor.core.Exceptions;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Pins the JSON the connector exchanges with EJBCA's REST API. Its WebClient takes Spring's default codecs rather than
 * the application's JSON mapper, so a change of JSON library reaches this wire on its own. The golden files under
 * {@code wire/} were recorded on Spring Boot 3.5; {@code -Dwire.record=true} re-records them.
 */
class EjbcaRestWireTest {

    private static final Path GOLDEN_DIR = Path.of("src/test/resources/wire");
    private static final boolean RECORD = Boolean.getBoolean("wire.record");
    private static final String SEARCH = "/v2/certificate/search";
    /** Renders what the connector decoded, independently of the codec under test. */
    private static final ObjectMapper DECODED = new ObjectMapper();

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
    void searchesAsTheConnectorDoesAndReadsWhatEjbcaAnswers() throws Exception {
        ejbca
                .stubFor(post(SEARCH)
                        .willReturn(okJson(Files.readString(GOLDEN_DIR.resolve("ejbca-search-response.json")))));

        SearchCertificatesRestResponseV2 response = EjbcaRestApiClient
                .prepareWebClient()
                .post()
                .uri(ejbca.baseUrl() + SEARCH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(searchBySerialNumber("1A2B3C4D5E6F"))
                .retrieve()
                .bodyToMono(SearchCertificatesRestResponseV2.class)
                .block();

        assertMatchesGolden("ejbca-search-request.json",
                ejbca.getAllServeEvents().get(0).getRequest().getBodyAsString());
        assertMatchesGolden("ejbca-search-decoded.json", DECODED.writeValueAsString(response));
    }

    @Test
    void readsTheErrorEjbcaAnswers() {
        ejbca
                .stubFor(post(SEARCH)
                        .willReturn(aResponse()
                                .withStatus(400)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"error_code\":400,\"error_message\":\"Wrong search criteria\"}")));

        Throwable error = Exceptions
                .unwrap(catchThrowable(() -> EjbcaRestApiClient
                        .prepareWebClient()
                        .post()
                        .uri(ejbca.baseUrl() + SEARCH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(searchBySerialNumber("00"))
                        .retrieve()
                        .bodyToMono(SearchCertificatesRestResponseV2.class)
                        .block()));

        assertThat(error).isInstanceOf(EjbcaRestApiException.class).hasMessage("Wrong search criteria");
    }

    /** The request {@code CertificateEjbcaServiceImpl} sends to identify a certificate by its serial number. */
    private static SearchCertificatesRestRequestV2 searchBySerialNumber(String serialNumber) {
        SearchCertificateCriteriaRestRequest criterion = new SearchCertificateCriteriaRestRequest();
        criterion.setOperation(SearchCertificateCriteriaRestRequest.CriteriaOperation.EQUAL.name());
        criterion.setProperty(SearchCertificateCriteriaRestRequest.CriteriaProperty.QUERY.name());
        criterion.setValue(serialNumber);

        SearchCertificatesRestRequestV2 request = new SearchCertificatesRestRequestV2();
        request.setPagination(new Pagination(10, 1));
        request.setCriteria(List.of(criterion));
        return request;
    }

    private static void assertMatchesGolden(String name, String actual) throws Exception {
        if (RECORD) {
            Files.writeString(GOLDEN_DIR.resolve(name), actual);
            return;
        }
        JSONAssert.assertEquals(Files.readString(GOLDEN_DIR.resolve(name)), actual, JSONCompareMode.NON_EXTENSIBLE);
    }
}
