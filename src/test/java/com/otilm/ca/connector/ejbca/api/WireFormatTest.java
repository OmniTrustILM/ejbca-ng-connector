package com.otilm.ca.connector.ejbca.api;

import com.otilm.api.exception.ValidationError;
import com.otilm.api.exception.ValidationException;
import com.otilm.api.model.client.attribute.RequestAttribute;
import com.otilm.api.model.common.HealthDto;
import com.otilm.api.model.common.HealthStatus;
import com.otilm.api.model.common.NameAndIdDto;
import com.otilm.api.model.common.attribute.common.AttributeContent;
import com.otilm.api.model.common.attribute.common.AttributeType;
import com.otilm.api.model.common.attribute.common.BaseAttribute;
import com.otilm.api.model.common.attribute.common.MetadataAttribute;
import com.otilm.api.model.common.attribute.common.callback.AttributeCallback;
import com.otilm.api.model.common.attribute.common.callback.AttributeCallbackMapping;
import com.otilm.api.model.common.attribute.common.callback.AttributeValueTarget;
import com.otilm.api.model.common.attribute.common.content.AttributeContentType;
import com.otilm.api.model.common.attribute.common.properties.CustomAttributeProperties;
import com.otilm.api.model.common.attribute.common.properties.DataAttributeProperties;
import com.otilm.api.model.common.attribute.common.properties.InfoAttributeProperties;
import com.otilm.api.model.common.attribute.common.properties.MetadataAttributeProperties;
import com.otilm.api.model.common.attribute.v2.CustomAttributeV2;
import com.otilm.api.model.common.attribute.v2.DataAttributeV2;
import com.otilm.api.model.common.attribute.v2.GroupAttributeV2;
import com.otilm.api.model.common.attribute.v2.InfoAttributeV2;
import com.otilm.api.model.common.attribute.v2.MetadataAttributeV2;
import com.otilm.api.model.common.attribute.v2.content.BooleanAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.DateAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.DateTimeAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.IntegerAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.StringAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.TextAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.TimeAttributeContentV2;
import com.otilm.api.model.common.attribute.v3.DataAttributeV3;
import com.otilm.api.model.common.attribute.v3.InfoAttributeV3;
import com.otilm.api.model.common.attribute.v3.content.DateAttributeContentV3;
import com.otilm.api.model.common.attribute.v3.content.DateTimeAttributeContentV3;
import com.otilm.api.model.common.attribute.v3.content.StringAttributeContentV3;
import com.otilm.api.model.connector.discovery.DiscoveryDataRequestDto;
import com.otilm.api.model.connector.discovery.DiscoveryProviderCertificateDataDto;
import com.otilm.api.model.connector.discovery.DiscoveryProviderDto;
import com.otilm.api.model.connector.v2.CertificateDataResponseDto;
import com.otilm.api.model.connector.v2.CertificateSignRequestDto;
import com.otilm.api.model.core.discovery.DiscoveryStatus;
import com.otilm.ca.connector.ejbca.EndpointsListener;
import com.otilm.ca.connector.ejbca.Goldens;
import com.otilm.ca.connector.ejbca.dao.entity.DiscoveryHistory;
import com.otilm.ca.connector.ejbca.service.AttributeService;
import com.otilm.ca.connector.ejbca.service.AuthorityInstanceService;
import com.otilm.ca.connector.ejbca.service.CertificateEjbcaService;
import com.otilm.ca.connector.ejbca.service.DiscoveryAttributeService;
import com.otilm.ca.connector.ejbca.service.DiscoveryHistoryService;
import com.otilm.ca.connector.ejbca.service.DiscoveryService;
import com.otilm.ca.connector.ejbca.service.EjbcaService;
import com.otilm.ca.connector.ejbca.service.EndEntityProfileEjbcaService;
import com.otilm.ca.connector.ejbca.service.HealthCheckService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins the JSON that core and the connector exchange, as the REST layer reads and writes it. Every controller is
 * loaded, so the connector info lists the endpoints core reads at registration. {@link Goldens} says how its golden
 * files are recorded.
 */
@WebMvcTest(controllers = {
        AttributesControllerImpl.class,
        AuthorityInstanceControllerImpl.class,
        CertificateControllerImpl.class,
        DiscoveryAttributesControllerImpl.class,
        DiscoveryControllerImpl.class,
        DiscoverySupportController.class,
        EndEntityProfilesControllerImpl.class,
        HealthControllerImpl.class,
        InfoControllerImpl.class})
@Import(EndpointsListener.class)
class WireFormatTest {

    private static final String AUTHORITY_UUID = "dde2cccc-616f-11ec-90d6-0242ac120003";
    private static final String DISCOVERY_UUID = "6f1b2c3d-0000-4000-8000-000000000010";
    private static final ZonedDateTime PRAGUE_DATE_TIME = ZonedDateTime
            .of(2026, 10, 8, 10, 15, 30, 123_000_000, ZoneId.of("Europe/Prague"));

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AttributeService attributeService;

    @MockitoBean
    private AuthorityInstanceService authorityInstanceService;

    @MockitoBean
    private EndEntityProfileEjbcaService endEntityProfileEjbcaService;

    @MockitoBean
    private EjbcaService ejbcaService;

    @MockitoBean
    private CertificateEjbcaService certificateEjbcaService;

    @MockitoBean
    private DiscoveryAttributeService discoveryAttributeService;

    @MockitoBean
    private DiscoveryHistoryService discoveryHistoryService;

    @MockitoBean
    private DiscoveryService discoveryService;

    @MockitoBean
    private HealthCheckService healthCheckService;

    @Test
    void issueAttributeDefinitions() throws Exception {
        String json = exchange(
                get("/v2/authorityProvider/authorities/{uuid}/certificates/issue/attributes", AUTHORITY_UUID));

        Goldens.assertMatches("issue-attributes.json", json);
    }

    @Test
    void raProfileAttributeDefinitions() throws Exception {
        given(endEntityProfileEjbcaService.listEndEntityProfiles(AUTHORITY_UUID))
                .willReturn(List.of(new NameAndIdDto(1, "EMPTY"), new NameAndIdDto(2, "TLS Server")));

        String json = exchange(get("/v1/authorityProvider/authorities/{uuid}/raProfile/attributes", AUTHORITY_UUID));

        Goldens.assertMatches("ra-profile-attributes.json", json);
    }

    @Test
    void everyAttributeTypeWithContent() throws Exception {
        given(attributeService.getAttributes("EJBCA")).willReturn(everyAttributeType());

        String json = exchange(get("/v1/authorityProvider/EJBCA/attributes"));

        Goldens.assertMatches("every-attribute-type.json", json);
    }

    @Test
    void issuedCertificateWithMetadata() throws Exception {
        given(certificateEjbcaService.issueCertificate(eq(AUTHORITY_UUID), any())).willReturn(issuedCertificate());

        String json = exchange(post("/v2/authorityProvider/authorities/{uuid}/certificates/issue", AUTHORITY_UUID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(Goldens.read("issue-request.json")));

        Goldens.assertMatches("issue-response.json", json);
        ArgumentCaptor<CertificateSignRequestDto> request = ArgumentCaptor.forClass(CertificateSignRequestDto.class);
        then(certificateEjbcaService).should().issueCertificate(eq(AUTHORITY_UUID), request.capture());
        Goldens.assertMatches("issue-request.txt", describe(request.getValue()));
    }

    @Test
    void requestAttributesOfEveryContentType() throws Exception {
        exchange(post("/v1/authorityProvider/EJBCA/attributes/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(Goldens.read("request-attributes.json")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RequestAttribute>> attributes = ArgumentCaptor.forClass(List.class);
        then(attributeService).should().validateAttributes(eq("EJBCA"), attributes.capture());
        Goldens.assertMatches("request-attributes.txt", describe(attributes.getValue()));
    }

    /** The status and the page of results core polls for until a discovery has finished. */
    @Test
    void discoveryStatusAndResults() throws Exception {
        DiscoveryHistory history = new DiscoveryHistory();
        given(discoveryHistoryService.getHistoryByUuid(DISCOVERY_UUID)).willReturn(history);
        given(discoveryService.getProviderDtoData(any(), eq(history))).willReturn(completedDiscovery());

        String json = exchange(post("/v1/discoveryProvider/discover/{uuid}", DISCOVERY_UUID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ejbca-discovery\",\"kind\":\"EJBCA\",\"pageNumber\":1,\"itemsPerPage\":100}"));

        Goldens.assertMatches("discovery-status.json", json);
        ArgumentCaptor<DiscoveryDataRequestDto> page = ArgumentCaptor.forClass(DiscoveryDataRequestDto.class);
        then(discoveryService).should().getProviderDtoData(page.capture(), eq(history));
        assertThat(page.getValue())
                .extracting(DiscoveryDataRequestDto::getName, DiscoveryDataRequestDto::getKind,
                        DiscoveryDataRequestDto::getPageNumber, DiscoveryDataRequestDto::getItemsPerPage)
                .containsExactly("ejbca-discovery", "EJBCA", 1, 100);
    }

    @Test
    void validationErrorsAnswerUnprocessableContent() throws Exception {
        willThrow(new ValidationException("Invalid attributes",
                List
                        .of(ValidationError.create("Attribute email is required"),
                                ValidationError.create("Attribute san must name a DNS host"))))
                .given(attributeService)
                .validateAttributes(eq("EJBCA"), any());

        String json = mvc
                .perform(post("/v1/authorityProvider/EJBCA/attributes/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().is(422))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        Goldens.assertMatches("validation-errors.json", json);
    }

    @Test
    void connectorInfo() throws Exception {
        String json = exchange(get("/v1"));

        Goldens.assertMatches("connector-info.json", json);
    }

    @Test
    void health() throws Exception {
        HealthDto database = new HealthDto();
        database.setStatus(HealthStatus.OK);
        database.setDescription("Database connection ok");
        HealthDto health = new HealthDto();
        health.setStatus(HealthStatus.OK);
        health.setParts(Map.of("database", database));
        given(healthCheckService.checkHealth()).willReturn(health);

        String json = exchange(get("/v1/health"));

        Goldens.assertMatches("health.json", json);
    }

    private String exchange(MockHttpServletRequestBuilder request) throws Exception {
        return mvc
                .perform(request)
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private static List<BaseAttribute> everyAttributeType() {
        DataAttributeProperties dataProperties = new DataAttributeProperties();
        dataProperties.setLabel("Valid From");
        dataProperties.setRequired(true);
        dataProperties.setVisible(true);
        DataAttributeV2 dataV2 = new DataAttributeV2();
        dataV2.setUuid("6f1b2c3d-0000-4000-8000-000000000001");
        dataV2.setName("validFrom");
        dataV2.setDescription("Data attribute with every V2 temporal content");
        dataV2.setType(AttributeType.DATA);
        dataV2.setContentType(AttributeContentType.DATETIME);
        dataV2.setProperties(dataProperties);
        dataV2
                .setContent(List
                        .of(new DateTimeAttributeContentV2("prague", PRAGUE_DATE_TIME),
                                new DateAttributeContentV2(LocalDate.of(2026, 10, 8)),
                                new TimeAttributeContentV2(LocalTime.of(10, 15, 30))));
        AttributeCallback callback = new AttributeCallback();
        callback.setCallbackContext("core/getCredentials");
        callback.setCallbackMethod("GET");
        callback
                .setMappings(Set
                        .of(new AttributeCallbackMapping("credentialKind", AttributeValueTarget.PATH_VARIABLE,
                                "SoftKeyStore")));
        dataV2.setAttributeCallback(callback);

        InfoAttributeProperties infoProperties = new InfoAttributeProperties();
        infoProperties.setLabel("Notice");
        infoProperties.setVisible(true);
        InfoAttributeV2 infoV2 = new InfoAttributeV2();
        infoV2.setUuid("6f1b2c3d-0000-4000-8000-000000000002");
        infoV2.setName("notice");
        infoV2.setDescription("Info attribute");
        infoV2.setType(AttributeType.INFO);
        infoV2.setContentType(AttributeContentType.TEXT);
        infoV2.setProperties(infoProperties);
        infoV2.setContent(List.of(new TextAttributeContentV2("**Read me**")));

        GroupAttributeV2 groupV2 = new GroupAttributeV2();
        groupV2.setUuid("6f1b2c3d-0000-4000-8000-000000000003");
        groupV2.setName("profileGroup");
        groupV2.setDescription("Group attribute");
        groupV2.setType(AttributeType.GROUP);
        groupV2.setAttributeCallback(callback);

        MetadataAttributeProperties metadataProperties = new MetadataAttributeProperties();
        metadataProperties.setLabel("Retries");
        metadataProperties.setVisible(true);
        MetadataAttributeV2 metadataV2 = new MetadataAttributeV2();
        metadataV2.setUuid("6f1b2c3d-0000-4000-8000-000000000004");
        metadataV2.setName("retries");
        metadataV2.setDescription("Metadata attribute");
        metadataV2.setType(AttributeType.META);
        metadataV2.setContentType(AttributeContentType.INTEGER);
        metadataV2.setProperties(metadataProperties);
        metadataV2.setContent(List.of(new IntegerAttributeContentV2(3)));

        CustomAttributeProperties customProperties = new CustomAttributeProperties();
        customProperties.setLabel("Approved");
        customProperties.setVisible(true);
        CustomAttributeV2 customV2 = new CustomAttributeV2();
        customV2.setUuid("6f1b2c3d-0000-4000-8000-000000000005");
        customV2.setName("approved");
        customV2.setDescription("Custom attribute");
        customV2.setType(AttributeType.CUSTOM);
        customV2.setContentType(AttributeContentType.BOOLEAN);
        customV2.setProperties(customProperties);
        customV2.setContent(List.of(new BooleanAttributeContentV2(true)));

        DataAttributeV3 dataV3 = new DataAttributeV3();
        dataV3.setUuid("6f1b2c3d-0000-4000-8000-000000000006");
        dataV3.setName("expiresAt");
        dataV3.setDescription("Data attribute with V3 content");
        dataV3.setType(AttributeType.DATA);
        dataV3.setContentType(AttributeContentType.DATETIME);
        dataV3.setProperties(dataProperties);
        dataV3
                .setContent(List
                        .of(new DateTimeAttributeContentV3(PRAGUE_DATE_TIME),
                                new DateAttributeContentV3(LocalDate.of(2026, 10, 8))));

        InfoAttributeV3 infoV3 = new InfoAttributeV3();
        infoV3.setUuid("6f1b2c3d-0000-4000-8000-000000000007");
        infoV3.setName("noticeV3");
        infoV3.setDescription("Info attribute with V3 content");
        infoV3.setType(AttributeType.INFO);
        infoV3.setContentType(AttributeContentType.STRING);
        infoV3.setProperties(infoProperties);
        infoV3.setContent(List.of(new StringAttributeContentV3("three")));

        return List.of(dataV2, infoV2, groupV2, metadataV2, customV2, dataV3, infoV3);
    }

    private static CertificateDataResponseDto issuedCertificate() {
        MetadataAttributeProperties properties = new MetadataAttributeProperties();
        properties.setLabel("EJBCA Username");
        properties.setVisible(true);
        MetadataAttributeV2 username = new MetadataAttributeV2();
        username.setUuid("6f1b2c3d-0000-4000-8000-000000000008");
        username.setName("ejbcaUsername");
        username.setDescription("End entity the certificate was issued to");
        username.setType(AttributeType.META);
        username.setContentType(AttributeContentType.STRING);
        username.setProperties(properties);
        username.setContent(List.of(new StringAttributeContentV2("ilm-user")));
        List<MetadataAttribute> meta = new ArrayList<>();
        meta.add(username);

        CertificateDataResponseDto response = new CertificateDataResponseDto();
        response.setCertificateData("MIIBszCCAVmgAwIBAgIUQ==");
        response.setUuid("6f1b2c3d-0000-4000-8000-000000000009");
        response.setMeta(meta);
        return response;
    }

    private static DiscoveryProviderDto completedDiscovery() {
        DiscoveryProviderCertificateDataDto certificate = new DiscoveryProviderCertificateDataDto();
        certificate.setUuid("6f1b2c3d-0000-4000-8000-000000000011");
        certificate.setBase64Content("MIIBszCCAVmgAwIBAgIUQ==");
        certificate
                .setMeta(List
                        .of(metadata("df2fbaa2-60fd-11ed-9b6a-0242ac120002", "username", "Username",
                                AttributeContentType.STRING, new StringAttributeContentV2("ilm-user"))));

        DiscoveryProviderDto discovery = new DiscoveryProviderDto();
        discovery.setUuid(DISCOVERY_UUID);
        discovery.setName("ejbca-discovery");
        discovery.setStatus(DiscoveryStatus.COMPLETED);
        discovery.setTotalCertificatesDiscovered(1);
        discovery.setCertificateData(List.of(certificate));
        discovery
                .setMeta(List
                        .of(metadata("20add2d6-60f7-11ed-9b6a-0242ac120002", "totalCertificates",
                                "Total Certificates Discovered", AttributeContentType.INTEGER,
                                new IntegerAttributeContentV2("1", 1))));
        return discovery;
    }

    private static MetadataAttribute metadata(String uuid, String name, String label, AttributeContentType contentType,
            AttributeContent content) {
        MetadataAttributeProperties properties = new MetadataAttributeProperties();
        properties.setLabel(label);
        properties.setVisible(true);
        MetadataAttributeV2 attribute = new MetadataAttributeV2();
        attribute.setUuid(uuid);
        attribute.setName(name);
        attribute.setType(AttributeType.META);
        attribute.setContentType(contentType);
        attribute.setProperties(properties);
        attribute.setContent(List.of(content));
        return attribute;
    }

    private static String describe(CertificateSignRequestDto request) {
        return "request=" + request.getRequest() + "\nformat=" + request.getFormat() + "\nraProfileAttributes:\n"
                + describe(request.getRaProfileAttributes()) + "attributes:\n" + describe(request.getAttributes());
    }

    /** Names the Java types the REST layer chose, which the JSON a connector echoes back cannot show. */
    private static String describe(List<RequestAttribute> attributes) {
        StringBuilder text = new StringBuilder();
        for (RequestAttribute attribute : attributes) {
            text
                    .append(attribute.getClass().getSimpleName())
                    .append(' ')
                    .append(attribute.getName())
                    .append(' ')
                    .append(attribute.getContentType())
                    .append('\n');
            List<? extends AttributeContent> contents = attribute.getContent();
            for (AttributeContent content : contents) {
                Object data = content.getData();
                text
                        .append("  ")
                        .append(content.getClass().getSimpleName())
                        .append(" reference=")
                        .append(content.getReference())
                        .append(" data=")
                        .append(data == null ? "null" : data.getClass().getSimpleName() + ":" + data)
                        .append('\n');
            }
        }
        return text.toString();
    }

}
