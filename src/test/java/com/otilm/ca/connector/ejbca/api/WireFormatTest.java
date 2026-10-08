package com.otilm.ca.connector.ejbca.api;

import com.otilm.api.model.client.attribute.RequestAttribute;
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
import com.otilm.api.model.connector.v2.CertificateDataResponseDto;
import com.otilm.api.model.connector.v2.CertificateSignRequestDto;
import com.otilm.ca.connector.ejbca.service.AttributeService;
import com.otilm.ca.connector.ejbca.service.AuthorityInstanceService;
import com.otilm.ca.connector.ejbca.service.CertificateEjbcaService;
import com.otilm.ca.connector.ejbca.service.EjbcaService;
import com.otilm.ca.connector.ejbca.service.EndEntityProfileEjbcaService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins the JSON that core and the connector exchange, as the REST layer reads and writes it. The golden files under
 * {@code wire/} were recorded on Spring Boot 3.5; {@code -Dwire.record=true} re-records them.
 */
@WebMvcTest(controllers = {
        AttributesControllerImpl.class,
        AuthorityInstanceControllerImpl.class,
        CertificateControllerImpl.class})
class WireFormatTest {

    private static final Path GOLDEN_DIR = Path.of("src/test/resources/wire");
    private static final boolean RECORD = Boolean.getBoolean("wire.record");
    private static final String AUTHORITY_UUID = "dde2cccc-616f-11ec-90d6-0242ac120003";
    private static final ZonedDateTime PRAGUE_DATE_TIME = ZonedDateTime
            .of(2026, 10, 8, 10, 15, 30, 123_000_000, ZoneId.of("Europe/Prague"));

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AttributeService attributeService;

    @MockBean
    private AuthorityInstanceService authorityInstanceService;

    @MockBean
    private EndEntityProfileEjbcaService endEntityProfileEjbcaService;

    @MockBean
    private EjbcaService ejbcaService;

    @MockBean
    private CertificateEjbcaService certificateEjbcaService;

    @Test
    void issueAttributeDefinitions() throws Exception {
        String json = exchange(
                get("/v2/authorityProvider/authorities/{uuid}/certificates/issue/attributes", AUTHORITY_UUID));

        assertMatchesGolden("issue-attributes.json", json);
    }

    @Test
    void raProfileAttributeDefinitions() throws Exception {
        given(endEntityProfileEjbcaService.listEndEntityProfiles(AUTHORITY_UUID))
                .willReturn(List.of(new NameAndIdDto(1, "EMPTY"), new NameAndIdDto(2, "TLS Server")));

        String json = exchange(get("/v1/authorityProvider/authorities/{uuid}/raProfile/attributes", AUTHORITY_UUID));

        assertMatchesGolden("ra-profile-attributes.json", json);
    }

    @Test
    void everyAttributeTypeWithContent() throws Exception {
        given(attributeService.getAttributes("EJBCA")).willReturn(everyAttributeType());

        String json = exchange(get("/v1/authorityProvider/EJBCA/attributes"));

        assertMatchesGolden("every-attribute-type.json", json);
    }

    @Test
    void issuedCertificateWithMetadata() throws Exception {
        given(certificateEjbcaService.issueCertificate(eq(AUTHORITY_UUID), any())).willReturn(issuedCertificate());

        String json = exchange(post("/v2/authorityProvider/authorities/{uuid}/certificates/issue", AUTHORITY_UUID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(readGolden("issue-request.json")));

        assertMatchesGolden("issue-response.json", json);
        ArgumentCaptor<CertificateSignRequestDto> request = ArgumentCaptor.forClass(CertificateSignRequestDto.class);
        then(certificateEjbcaService).should().issueCertificate(eq(AUTHORITY_UUID), request.capture());
        assertMatchesGolden("issue-request.txt", describe(request.getValue()));
    }

    @Test
    void requestAttributesOfEveryContentType() throws Exception {
        exchange(post("/v1/authorityProvider/EJBCA/attributes/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readGolden("request-attributes.json")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RequestAttribute>> attributes = ArgumentCaptor.forClass(List.class);
        then(attributeService).should().validateAttributes(eq("EJBCA"), attributes.capture());
        assertMatchesGolden("request-attributes.txt", describe(attributes.getValue()));
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

    private static String readGolden(String name) throws IOException {
        return Files.readString(GOLDEN_DIR.resolve(name));
    }

    private static void assertMatchesGolden(String name, String actual) throws Exception {
        if (RECORD) {
            Files.createDirectories(GOLDEN_DIR);
            Files.writeString(GOLDEN_DIR.resolve(name), actual);
            return;
        }
        String expected = readGolden(name);
        if (name.endsWith(".json")) {
            JSONAssert.assertEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
        } else {
            assertThat(actual).isEqualTo(expected);
        }
    }
}
