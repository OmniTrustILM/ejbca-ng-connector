package com.otilm.ca.connector.ejbca.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.otilm.ca.connector.ejbca.Goldens;
import com.otilm.ca.connector.ejbca.dto.ejbca.request.Pagination;
import com.otilm.ca.connector.ejbca.dto.ejbca.request.SearchCertificateCriteriaRestRequest;
import com.otilm.ca.connector.ejbca.dto.ejbca.request.SearchCertificatesRestRequestV2;
import com.otilm.ca.connector.ejbca.dto.ejbca.response.SearchCertificatesRestResponseV2;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 * the application's JSON mapper, so a change of JSON library reaches this wire on its own. {@link Goldens} says how its
 * golden files are recorded.
 */
class EjbcaRestWireTest {

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
        ejbca.stubFor(post(SEARCH).willReturn(okJson(Goldens.read("ejbca-search-response.json"))));

        SearchCertificatesRestResponseV2 response = EjbcaRestApiClient
                .prepareWebClient()
                .post()
                .uri(ejbca.baseUrl() + SEARCH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(searchBySerialNumber("1A2B3C4D5E6F"))
                .retrieve()
                .bodyToMono(SearchCertificatesRestResponseV2.class)
                .block();

        Goldens
                .assertMatches("ejbca-search-request.json",
                        ejbca.getAllServeEvents().get(0).getRequest().getBodyAsString());
        Goldens.assertMatches("ejbca-search-decoded.json", DECODED.writeValueAsString(response));
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

}
