package com.otilm.ca.connector.ejbca.service.impl;

import com.otilm.api.model.client.attribute.RequestAttribute;
import com.otilm.api.model.client.attribute.RequestAttributeV2;
import com.otilm.api.model.common.NameAndIdDto;
import com.otilm.api.model.common.attribute.common.AttributeContent;
import com.otilm.api.model.common.attribute.common.MetadataAttribute;
import com.otilm.api.model.common.attribute.common.content.AttributeContentType;
import com.otilm.api.model.common.attribute.v2.MetadataAttributeV2;
import com.otilm.api.model.common.attribute.v2.content.BaseAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.DateTimeAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.IntegerAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.ObjectAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.StringAttributeContentV2;
import com.otilm.api.model.connector.discovery.DiscoveryDataRequestDto;
import com.otilm.api.model.connector.discovery.DiscoveryProviderDto;
import com.otilm.api.model.connector.discovery.DiscoveryRequestDto;
import com.otilm.api.model.core.discovery.DiscoveryStatus;
import com.otilm.ca.connector.ejbca.dao.CertificateRepository;
import com.otilm.ca.connector.ejbca.dao.entity.Certificate;
import com.otilm.ca.connector.ejbca.dao.entity.DiscoveryHistory;
import com.otilm.ca.connector.ejbca.dto.AuthorityInstanceNameAndUuidDto;
import com.otilm.ca.connector.ejbca.dto.ejbca.request.SearchCertificateCriteriaRestRequest;
import com.otilm.ca.connector.ejbca.dto.ejbca.request.SearchCertificateCriteriaRestRequest.CertificateStatus;
import com.otilm.ca.connector.ejbca.dto.ejbca.request.SearchCertificatesRestRequestV2;
import com.otilm.ca.connector.ejbca.dto.ejbca.response.CertificateRestResponseV2;
import com.otilm.ca.connector.ejbca.dto.ejbca.response.PaginationSummary;
import com.otilm.ca.connector.ejbca.dto.ejbca.response.SearchCertificatesRestResponseV2;
import com.otilm.ca.connector.ejbca.enums.DiscoveryKind;
import com.otilm.ca.connector.ejbca.service.DiscoveryHistoryService;
import com.otilm.ca.connector.ejbca.service.EjbcaService;
import com.otilm.ca.connector.ejbca.util.EjbcaVersion;
import com.otilm.core.util.AttributeDefinitionUtils;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DiscoveryServiceImplTest {

    @Mock
    EjbcaService ejbcaService;

    @Mock
    CertificateRepository certificateRepository;

    @Mock
    DiscoveryHistoryService discoveryHistoryService;

    @InjectMocks
    DiscoveryServiceImpl service;

    private static final String INSTANCE_UUID = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String REST_API_URL = "https://ejbca.example.com:8443/ejbca/rest";
    private static final String DISCOVERY_NAME = "test-discovery";
    private static final String DISCOVERY_UUID = "dddddddd-0000-0000-0000-000000000001";
    private static final long HISTORY_ID = 42L;

    private static final String ACTIVE = CertificateStatus.CERT_ACTIVE.name();
    private static final String NOTIFIED = CertificateStatus.CERT_NOTIFIEDABOUTEXPIRATION.name();
    private static final String REVOKED = CertificateStatus.CERT_REVOKED.name();
    private static final String KEY_COMPROMISE = CertificateStatus.REVOCATION_REASON_KEYCOMPROMISE.name();
    private static final String SUPERSEDED = CertificateStatus.REVOCATION_REASON_SUPERSEDED.name();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "ejbcaSearchPageSize", 100);
    }

    // ── fixture builders ──────────────────────────────────────────────────────

    private DiscoveryHistory buildHistory() {
        DiscoveryHistory h = new DiscoveryHistory();
        h.setId(HISTORY_ID);
        h.setUuid(DISCOVERY_UUID);
        h.setName(DISCOVERY_NAME);
        h.setStatus(DiscoveryStatus.IN_PROGRESS);
        return h;
    }

    /**
     * Builds a RequestAttributeV2 backed by an ObjectAttributeContentV2 so that
     * AttributeDefinitionUtils.getObjectAttributeContentData() can extract the data object.
     */
    private RequestAttributeV2 objectAttr(String name, java.io.Serializable data) {
        RequestAttributeV2 attr = new RequestAttributeV2();
        attr.setName(name);
        attr.setContentType(AttributeContentType.OBJECT);
        ObjectAttributeContentV2 content = new ObjectAttributeContentV2(name, data);
        attr.setContent(List.of(content));
        return attr;
    }

    private RequestAttributeV2 stringAttr(String name, String value) {
        RequestAttributeV2 attr = new RequestAttributeV2();
        attr.setName(name);
        attr.setContentType(AttributeContentType.STRING);
        attr.setContent(List.of(new StringAttributeContentV2(value)));
        return attr;
    }

    private RequestAttributeV2 integerAttr(String name, int value) {
        RequestAttributeV2 attr = new RequestAttributeV2();
        attr.setName(name);
        attr.setContentType(AttributeContentType.INTEGER);
        attr.setContent(List.of(new IntegerAttributeContentV2(value)));
        return attr;
    }

    private RequestAttributeV2 dateTimeAttr(String name, ZonedDateTime value) {
        RequestAttributeV2 attr = new RequestAttributeV2();
        attr.setName(name);
        attr.setContentType(AttributeContentType.DATETIME);
        attr.setContent(List.of(new DateTimeAttributeContentV2(value)));
        return attr;
    }

    /**
     * Builds a minimal EJBCA-kind DiscoveryRequestDto with all required attributes.
     */
    private DiscoveryRequestDto buildEjbcaRequest() {
        DiscoveryRequestDto req = new DiscoveryRequestDto();
        req.setName(DISCOVERY_NAME);
        req.setKind(DiscoveryKind.EJBCA.name());

        AuthorityInstanceNameAndUuidDto instance = new AuthorityInstanceNameAndUuidDto("test-instance", INSTANCE_UUID);
        NameAndIdDto ca = new NameAndIdDto(1, "ManagementCA");
        NameAndIdDto eeProfile = new NameAndIdDto(2, "EMPTY");

        List<RequestAttribute> attrs = new ArrayList<>();
        attrs.add(objectAttr(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_INSTANCE, instance));
        attrs.add(stringAttr(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_RESTAPI_URL, REST_API_URL));
        attrs.add(objectAttr(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_CA, ca));
        attrs.add(objectAttr(DiscoveryAttributeServiceImpl.ATTRIBUTE_END_ENTITY_PROFILE, eeProfile));
        // status attribute — empty list is fine (AttributeDefinitionUtils returns empty list when attribute missing)
        attrs
                .add(dateTimeAttr(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_ISSUED_AFTER,
                        ZonedDateTime.now().minusDays(30)));

        req.setAttributes(attrs);
        return req;
    }

    /**
     * Builds a minimal EJBCA_SCHEDULE-kind DiscoveryRequestDto.
     */
    private DiscoveryRequestDto buildScheduleRequest() {
        DiscoveryRequestDto req = new DiscoveryRequestDto();
        req.setName(DISCOVERY_NAME);
        req.setKind(DiscoveryKind.EJBCA_SCHEDULE.name());

        AuthorityInstanceNameAndUuidDto instance = new AuthorityInstanceNameAndUuidDto("test-instance", INSTANCE_UUID);
        NameAndIdDto ca = new NameAndIdDto(1, "ManagementCA");
        NameAndIdDto eeProfile = new NameAndIdDto(2, "EMPTY");

        List<RequestAttribute> attrs = new ArrayList<>();
        attrs.add(objectAttr(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_INSTANCE, instance));
        attrs.add(stringAttr(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_RESTAPI_URL, REST_API_URL));
        attrs.add(objectAttr(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_CA, ca));
        attrs.add(objectAttr(DiscoveryAttributeServiceImpl.ATTRIBUTE_END_ENTITY_PROFILE, eeProfile));
        attrs.add(integerAttr(DiscoveryAttributeServiceImpl.ATTRIBUTE_ISSUED_DAYS_BEFORE, 7));

        req.setAttributes(attrs);
        return req;
    }

    private EjbcaVersion ejbcaVersion(String versionString) {
        return new EjbcaVersion("EJBCA " + versionString + " Community");
    }

    /**
     * Produces a non-empty search response page with one certificate.
     */
    private SearchCertificatesRestResponseV2 pageWithOneCert(int currentPage) {
        CertificateRestResponseV2 cert = CertificateRestResponseV2
                .builder()
                .setCertificateProfileId(1)
                .setEndEntityProfileId(2)
                .setUsername("testUser")
                .setCertificate("dGVzdA==".getBytes()) // base64 bytes for test content
                .build();
        PaginationSummary summary = new PaginationSummary();
        summary.setCurrentPage(currentPage);
        summary.setPageSize(100);
        SearchCertificatesRestResponseV2 response = new SearchCertificatesRestResponseV2();
        response.setCertificates(List.of(cert));
        response.setPaginationSummary(summary);
        return response;
    }

    /**
     * Produces an empty search response (terminates the paging loop).
     */
    private SearchCertificatesRestResponseV2 emptyPage(int currentPage) {
        PaginationSummary summary = new PaginationSummary();
        summary.setCurrentPage(currentPage);
        summary.setPageSize(100);
        SearchCertificatesRestResponseV2 response = new SearchCertificatesRestResponseV2();
        response.setCertificates(List.of());
        response.setPaginationSummary(summary);
        return response;
    }

    // ── discoverCertificate (EJBCA kind, version >= 7.11 → searchVersion 2) ───

    @Test
    void discoverCertificate_version711_singlePage_savesAndCompletes() throws Exception {
        DiscoveryHistory history = buildHistory();
        DiscoveryRequestDto request = buildEjbcaRequest();

        given(ejbcaService.getEjbcaVersion(INSTANCE_UUID)).willReturn(ejbcaVersion("7.11.0"));
        given(ejbcaService.searchCertificates(eq(INSTANCE_UUID), eq(REST_API_URL), any()))
                .willReturn(pageWithOneCert(1))
                .willReturn(emptyPage(2));

        service.discoverCertificate(request, history);

        verify(certificateRepository, times(1)).save(any(Certificate.class));
        verify(discoveryHistoryService, times(1)).setHistory(history);
        assertEquals(DiscoveryStatus.COMPLETED, history.getStatus());
    }

    @Test
    void discoverCertificate_marksDiscoveryNameToReplaceTheValueOfAnEarlierRun() throws Exception {
        given(ejbcaService.getEjbcaVersion(INSTANCE_UUID)).willReturn(ejbcaVersion("7.11.0"));
        given(ejbcaService.searchCertificates(eq(INSTANCE_UUID), eq(REST_API_URL), any()))
                .willReturn(pageWithOneCert(1))
                .willReturn(emptyPage(2));

        service.discoverCertificate(buildEjbcaRequest(), buildHistory());

        ArgumentCaptor<Certificate> saved = ArgumentCaptor.forClass(Certificate.class);
        verify(certificateRepository).save(saved.capture());
        MetadataAttribute discoveryName = saved
                .getValue()
                .mapToDto()
                .getMeta()
                .stream()
                .filter(attribute -> "discoveryName".equals(attribute.getName()))
                .findFirst()
                .orElseThrow();
        List<AttributeContent> content = discoveryName.getContent();
        String value = content.getFirst().getData();
        assertEquals(DISCOVERY_NAME, value);
        assertTrue(discoveryName.getProperties().isOverwrite());
    }

    @Test
    void discoverCertificate_version711_emptyFirstPage_completesWithZeroCerts() throws Exception {
        DiscoveryHistory history = buildHistory();
        DiscoveryRequestDto request = buildEjbcaRequest();

        given(ejbcaService.getEjbcaVersion(INSTANCE_UUID)).willReturn(ejbcaVersion("7.11.0"));
        given(ejbcaService.searchCertificates(eq(INSTANCE_UUID), eq(REST_API_URL), any())).willReturn(emptyPage(1));

        service.discoverCertificate(request, history);

        verify(certificateRepository, never()).save(any());
        verify(discoveryHistoryService, times(1)).setHistory(history);
        assertEquals(DiscoveryStatus.COMPLETED, history.getStatus());
    }

    @Test
    void discoverCertificate_versionAbove7_usesSearchVersion2() throws Exception {
        DiscoveryHistory history = buildHistory();
        DiscoveryRequestDto request = buildEjbcaRequest();

        given(ejbcaService.getEjbcaVersion(INSTANCE_UUID)).willReturn(ejbcaVersion("8.0.0"));
        given(ejbcaService.searchCertificates(eq(INSTANCE_UUID), eq(REST_API_URL), any()))
                .willReturn(pageWithOneCert(1))
                .willReturn(emptyPage(2));

        service.discoverCertificate(request, history);

        verify(certificateRepository, times(1)).save(any(Certificate.class));
        assertEquals(DiscoveryStatus.COMPLETED, history.getStatus());
    }

    @Test
    void discoverCertificate_version78_usesSearchVersion1() throws Exception {
        DiscoveryHistory history = buildHistory();
        DiscoveryRequestDto request = buildEjbcaRequest();

        // For searchVersion 1, the loop terminates when totalCerts != null.
        // Return one page with totalCerts set (non-null) so the do-while exits.
        CertificateRestResponseV2 cert = CertificateRestResponseV2
                .builder()
                .setCertificateProfileId(1)
                .setEndEntityProfileId(2)
                .setUsername("testUser")
                .setCertificate("dGVzdA==".getBytes())
                .build();
        PaginationSummary summary = new PaginationSummary(1L);
        summary.setCurrentPage(1);
        summary.setPageSize(100);
        SearchCertificatesRestResponseV2 response = new SearchCertificatesRestResponseV2();
        response.setCertificates(List.of(cert));
        response.setPaginationSummary(summary);

        given(ejbcaService.getEjbcaVersion(INSTANCE_UUID)).willReturn(ejbcaVersion("7.8.0"));
        given(ejbcaService.searchCertificates(eq(INSTANCE_UUID), eq(REST_API_URL), any())).willReturn(response);

        service.discoverCertificate(request, history);

        verify(certificateRepository, times(1)).save(any(Certificate.class));
        assertEquals(DiscoveryStatus.COMPLETED, history.getStatus());
    }

    @Test
    void discoverCertificate_version78_emptyFirstPage_completesWithZeroCerts() throws Exception {
        DiscoveryHistory history = buildHistory();
        DiscoveryRequestDto request = buildEjbcaRequest();

        // Empty page terminates the v1 loop immediately (break on isEmpty)
        PaginationSummary summary = new PaginationSummary();
        summary.setCurrentPage(1);
        summary.setPageSize(100);
        SearchCertificatesRestResponseV2 emptyResponse = new SearchCertificatesRestResponseV2();
        emptyResponse.setCertificates(List.of());
        emptyResponse.setPaginationSummary(summary);

        given(ejbcaService.getEjbcaVersion(INSTANCE_UUID)).willReturn(ejbcaVersion("7.8.0"));
        given(ejbcaService.searchCertificates(eq(INSTANCE_UUID), eq(REST_API_URL), any())).willReturn(emptyResponse);

        service.discoverCertificate(request, history);

        verify(certificateRepository, never()).save(any());
        assertEquals(DiscoveryStatus.COMPLETED, history.getStatus());
    }

    @Test
    void discoverCertificate_unsupportedVersion_setsStatusFailed() throws Exception {
        DiscoveryHistory history = buildHistory();
        DiscoveryRequestDto request = buildEjbcaRequest();

        given(ejbcaService.getEjbcaVersion(INSTANCE_UUID)).willReturn(ejbcaVersion("6.0.0"));

        service.discoverCertificate(request, history);

        assertEquals(DiscoveryStatus.FAILED, history.getStatus());
        assertNotNull(history.getMeta());
        verify(discoveryHistoryService).setHistory(history);
    }

    @Test
    void discoverCertificate_ejbcaServiceThrows_setsStatusFailed() throws Exception {
        DiscoveryHistory history = buildHistory();
        DiscoveryRequestDto request = buildEjbcaRequest();

        given(ejbcaService.getEjbcaVersion(INSTANCE_UUID)).willThrow(new RuntimeException("connection refused"));

        service.discoverCertificate(request, history);

        assertEquals(DiscoveryStatus.FAILED, history.getStatus());
        assertNotNull(history.getMeta());
        verify(discoveryHistoryService).setHistory(history);
    }

    // ── discoverCertificate (EJBCA_SCHEDULE kind) ─────────────────────────────

    @Test
    void discoverCertificate_scheduleKind_computesIssuedAfterFromDaysBefore() throws Exception {
        DiscoveryHistory history = buildHistory();
        DiscoveryRequestDto request = buildScheduleRequest();

        ArgumentCaptor<SearchCertificatesRestRequestV2> requestCaptor = ArgumentCaptor
                .forClass(SearchCertificatesRestRequestV2.class);

        given(ejbcaService.getEjbcaVersion(INSTANCE_UUID)).willReturn(ejbcaVersion("7.11.0"));
        given(ejbcaService.searchCertificates(eq(INSTANCE_UUID), eq(REST_API_URL), requestCaptor.capture()))
                .willReturn(emptyPage(1));

        service.discoverCertificate(request, history);

        assertEquals(DiscoveryStatus.COMPLETED, history.getStatus());
        verify(discoveryHistoryService, times(1)).setHistory(history);

        // Verify that the issuedDaysBefore=7 computation actually produced a date
        // criterion ~7 days before now (ISSUED_DATE AFTER) in the search request.
        SearchCertificatesRestRequestV2 capturedRequest = requestCaptor.getValue();
        SearchCertificateCriteriaRestRequest issuedAfterCriterion = capturedRequest
                .getCriteria()
                .stream()
                .filter(c -> SearchCertificateCriteriaRestRequest.CriteriaProperty.ISSUED_DATE
                        .name()
                        .equals(c.getProperty())
                        && SearchCertificateCriteriaRestRequest.CriteriaOperation.AFTER.name().equals(c.getOperation()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No ISSUED_DATE AFTER criterion found in search request"));

        OffsetDateTime issuedAfterParsed = OffsetDateTime.parse(issuedAfterCriterion.getValue());
        OffsetDateTime expectedLow = OffsetDateTime.now().minusDays(8);
        OffsetDateTime expectedHigh = OffsetDateTime.now().minusDays(6);
        assertTrue(issuedAfterParsed.isAfter(expectedLow) && issuedAfterParsed.isBefore(expectedHigh),
                "issuedAfter should be ~7 days before now, got: " + issuedAfterParsed);
    }

    // ── getProviderDtoData ────────────────────────────────────────────────────

    @Test
    void getProviderDtoData_inProgress_returnsDtoWithEmptyList() {
        DiscoveryHistory history = buildHistory();
        // history.status is IN_PROGRESS (set in buildHistory)

        DiscoveryDataRequestDto dataRequest = new DiscoveryDataRequestDto();
        dataRequest.setPageNumber(1);
        dataRequest.setItemsPerPage(10);

        DiscoveryProviderDto dto = service.getProviderDtoData(dataRequest, history);

        assertNotNull(dto);
        assertEquals(DISCOVERY_UUID, dto.getUuid());
        assertEquals(DISCOVERY_NAME, dto.getName());
        assertEquals(DiscoveryStatus.IN_PROGRESS, dto.getStatus());
        assertTrue(dto.getCertificateData().isEmpty());
        assertEquals(0, dto.getTotalCertificatesDiscovered());
        verifyNoInteractions(certificateRepository);
    }

    @Test
    void getProviderDtoData_completed_returnsPaginatedData() {
        DiscoveryHistory history = buildHistory();
        history.setStatus(DiscoveryStatus.COMPLETED);

        DiscoveryDataRequestDto dataRequest = new DiscoveryDataRequestDto();
        dataRequest.setPageNumber(1);
        dataRequest.setItemsPerPage(10);

        Certificate cert = new Certificate();
        cert.setUuid("cert-uuid-1");
        cert.setDiscoveryId(HISTORY_ID);
        cert.setBase64Content("certContent");

        given(certificateRepository.countByDiscoveryId(HISTORY_ID)).willReturn(1L);
        given(certificateRepository.findAllByDiscoveryId(eq(HISTORY_ID), any(Pageable.class)))
                .willReturn(List.of(cert));

        DiscoveryProviderDto dto = service.getProviderDtoData(dataRequest, history);

        assertNotNull(dto);
        assertEquals(DiscoveryStatus.COMPLETED, dto.getStatus());
        assertEquals(1, dto.getTotalCertificatesDiscovered());
        assertEquals(1, dto.getCertificateData().size());
    }

    @Test
    void getProviderDtoData_completed_pageNumberZero_usesFirstPage() {
        DiscoveryHistory history = buildHistory();
        history.setStatus(DiscoveryStatus.COMPLETED);

        DiscoveryDataRequestDto dataRequest = new DiscoveryDataRequestDto();
        dataRequest.setPageNumber(0);
        dataRequest.setItemsPerPage(10);

        given(certificateRepository.countByDiscoveryId(HISTORY_ID)).willReturn(0L);
        given(certificateRepository.findAllByDiscoveryId(eq(HISTORY_ID), any(Pageable.class))).willReturn(List.of());

        DiscoveryProviderDto dto = service.getProviderDtoData(dataRequest, history);

        assertNotNull(dto);
        assertTrue(dto.getCertificateData().isEmpty());
        assertEquals(0, dto.getTotalCertificatesDiscovered());
    }

    // ── deleteDiscovery ───────────────────────────────────────────────────────

    @Test
    void deleteDiscovery_deletesTheRunsCertificatesAndHistory() throws Exception {
        DiscoveryHistory history = buildHistory();

        given(discoveryHistoryService.getHistoryByUuid(DISCOVERY_UUID)).willReturn(history);

        service.deleteDiscovery(DISCOVERY_UUID);

        verify(certificateRepository).deleteByDiscoveryId(HISTORY_ID);
        verify(discoveryHistoryService).deleteHistory(history);
    }

    // ── prepareSearchRequest: null CA/profile/status paths ───────────────────

    /**
     * When no CA, EE-profile, or status attributes are present in the request, AttributeDefinitionUtils returns null
     * for those lists: prepareSearchRequest skips the missing CA and EE-profile lists, and a missing status list means
     * no status filter. Uses EJBCA kind with issuedAfter set.
     */
    @Test
    void discoverCertificate_noOptionalFilters_completes() throws Exception {
        DiscoveryHistory history = buildHistory();

        // Build request with only mandatory attributes (no ca/eeProfile/status)
        DiscoveryRequestDto req = new DiscoveryRequestDto();
        req.setName(DISCOVERY_NAME);
        req.setKind(DiscoveryKind.EJBCA.name());

        AuthorityInstanceNameAndUuidDto instance = new AuthorityInstanceNameAndUuidDto("test-instance", INSTANCE_UUID);
        List<RequestAttribute> attrs = new ArrayList<>();
        attrs.add(objectAttr(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_INSTANCE, instance));
        attrs.add(stringAttr(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_RESTAPI_URL, REST_API_URL));
        attrs
                .add(dateTimeAttr(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_ISSUED_AFTER,
                        ZonedDateTime.now().minusDays(30)));
        req.setAttributes(attrs);

        given(ejbcaService.getEjbcaVersion(INSTANCE_UUID)).willReturn(ejbcaVersion("7.11.0"));
        given(ejbcaService.searchCertificates(eq(INSTANCE_UUID), eq(REST_API_URL), any())).willReturn(emptyPage(1));

        service.discoverCertificate(req, history);

        assertEquals(DiscoveryStatus.COMPLETED, history.getStatus());
    }

    // ── runPagedSearch: v1 multi-page loop (totalCerts == null keeps looping) ──

    /**
     * For EJBCA v7.8 (searchVersion 1), the loop continues while totalCerts == null. This test returns two pages: first
     * with totalCerts null, second with totalCerts set.
     */
    @Test
    void discoverCertificate_version78_multiPage_stopsWhenTotalCertsIsSet() throws Exception {
        DiscoveryHistory history = buildHistory();
        DiscoveryRequestDto request = buildEjbcaRequest();

        // Page 1: cert found, totalCerts == null → continue loop
        CertificateRestResponseV2 cert = CertificateRestResponseV2
                .builder()
                .setCertificateProfileId(1)
                .setEndEntityProfileId(2)
                .setUsername("user1")
                .setCertificate("dGVzdA==".getBytes())
                .build();
        PaginationSummary page1Summary = new PaginationSummary(); // totalCerts is null
        page1Summary.setCurrentPage(1);
        page1Summary.setPageSize(100);
        SearchCertificatesRestResponseV2 page1 = new SearchCertificatesRestResponseV2();
        page1.setCertificates(List.of(cert));
        page1.setPaginationSummary(page1Summary);

        // Page 2: cert found, totalCerts set → exit loop
        PaginationSummary page2Summary = new PaginationSummary(1L);
        page2Summary.setCurrentPage(2);
        page2Summary.setPageSize(100);
        SearchCertificatesRestResponseV2 page2 = new SearchCertificatesRestResponseV2();
        page2.setCertificates(List.of(cert));
        page2.setPaginationSummary(page2Summary);

        given(ejbcaService.getEjbcaVersion(INSTANCE_UUID)).willReturn(ejbcaVersion("7.8.0"));
        given(ejbcaService.searchCertificates(eq(INSTANCE_UUID), eq(REST_API_URL), any()))
                .willReturn(page1)
                .willReturn(page2);

        service.discoverCertificate(request, history);

        // Two certs saved across two pages
        verify(certificateRepository, org.mockito.Mockito.times(2)).save(any(Certificate.class));
        assertEquals(DiscoveryStatus.COMPLETED, history.getStatus());
    }

    @Test
    void discoverCertificate_ejbcaKind_sendsIssuedAfterFromAttribute() throws Exception {
        DiscoveryHistory history = buildHistory();
        DiscoveryRequestDto request = buildEjbcaRequest();
        ZonedDateTime issuedAfter = ZonedDateTime.of(2026, 1, 15, 11, 20, 30, 0, ZoneOffset.ofHours(1));
        request
                .getAttributes()
                .removeIf(a -> DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_ISSUED_AFTER.equals(a.getName()));
        request
                .getAttributes()
                .add(dateTimeAttr(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_ISSUED_AFTER, issuedAfter));

        ArgumentCaptor<SearchCertificatesRestRequestV2> requestCaptor = ArgumentCaptor
                .forClass(SearchCertificatesRestRequestV2.class);
        given(ejbcaService.getEjbcaVersion(INSTANCE_UUID)).willReturn(ejbcaVersion("7.11.0"));
        given(ejbcaService.searchCertificates(eq(INSTANCE_UUID), eq(REST_API_URL), requestCaptor.capture()))
                .willReturn(emptyPage(1));

        service.discoverCertificate(request, history);

        assertEquals(DiscoveryStatus.COMPLETED, history.getStatus());
        assertEquals("2026-01-15T10:20:30Z", issuedAfterCriterion(requestCaptor.getValue()));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -3})
    void discoverCertificate_scheduleKind_nonPositiveDays_failsWithoutSearching(int days) throws Exception {
        DiscoveryHistory history = buildHistory();
        DiscoveryRequestDto request = buildScheduleRequest();
        request
                .getAttributes()
                .removeIf(a -> DiscoveryAttributeServiceImpl.ATTRIBUTE_ISSUED_DAYS_BEFORE.equals(a.getName()));
        request.getAttributes().add(integerAttr(DiscoveryAttributeServiceImpl.ATTRIBUTE_ISSUED_DAYS_BEFORE, days));

        service.discoverCertificate(request, history);

        assertFailedOnNumberOfDays(history);
    }

    @Test
    void discoverCertificate_scheduleKind_missingDays_failsWithoutSearching() throws Exception {
        DiscoveryHistory history = buildHistory();
        DiscoveryRequestDto request = buildScheduleRequest();
        request
                .getAttributes()
                .removeIf(a -> DiscoveryAttributeServiceImpl.ATTRIBUTE_ISSUED_DAYS_BEFORE.equals(a.getName()));

        service.discoverCertificate(request, history);

        assertFailedOnNumberOfDays(history);
    }

    // ── Certificate status → EJBCA STATUS criteria ────────────────────────────

    /**
     * CERT_ACTIVE and revocation reasons never share one search, and selecting both active and revoked certificates
     * sends no status filter.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("statusSelections")
    void discoverCertificate_statusSelection_sendsStatusCriteriaEjbcaCanCombine(String selection, List<String> selected,
            List<List<String>> expectedStatusCriteriaPerSearch) throws Exception {
        DiscoveryHistory history = buildHistory();
        DiscoveryRequestDto request = buildEjbcaRequest();
        request.getAttributes().add(statusAttr(selected));

        ArgumentCaptor<SearchCertificatesRestRequestV2> requestCaptor = ArgumentCaptor
                .forClass(SearchCertificatesRestRequestV2.class);
        given(ejbcaService.getEjbcaVersion(INSTANCE_UUID)).willReturn(ejbcaVersion("7.11.0"));
        given(ejbcaService.searchCertificates(eq(INSTANCE_UUID), eq(REST_API_URL), requestCaptor.capture()))
                .willReturn(emptyPage(1));

        service.discoverCertificate(request, history);

        assertEquals(DiscoveryStatus.COMPLETED, history.getStatus());
        assertEquals(expectedStatusCriteriaPerSearch,
                requestCaptor.getAllValues().stream().map(DiscoveryServiceImplTest::statusCriteria).toList());
    }

    static Stream<Arguments> statusSelections() {
        List<String> everyOption = Arrays.stream(CertificateStatus.values()).map(Enum::name).toList();
        List<String> everyReason = CertificateStatus.revocationReasons().stream().map(Enum::name).toList();

        return Stream
                .of(Arguments.of("every option, the default", everyOption, List.of(List.of())),
                        Arguments.of("active and revoked", List.of(ACTIVE, REVOKED), List.of(List.of())),
                        Arguments.of("active and every reason", with(ACTIVE, everyReason), List.of(List.of())),
                        Arguments
                                .of("active, revoked and one reason", List.of(ACTIVE, REVOKED, KEY_COMPROMISE),
                                        List.of(List.of(ACTIVE), List.of(REVOKED, KEY_COMPROMISE))),
                        Arguments
                                .of("reasons only", List.of(SUPERSEDED, KEY_COMPROMISE),
                                        List.of(List.of(REVOKED, KEY_COMPROMISE, SUPERSEDED))),
                        Arguments.of("every reason, no status", everyReason, List.of(List.of(REVOKED))),
                        Arguments.of("revoked only", List.of(REVOKED), List.of(List.of(REVOKED))),
                        Arguments
                                .of("revoked with every reason", with(REVOKED, everyReason), List.of(List.of(REVOKED))),
                        Arguments.of("notified about expiration only", List.of(NOTIFIED), List.of(List.of(ACTIVE))),
                        Arguments.of("active only", List.of(ACTIVE), List.of(List.of(ACTIVE))),
                        Arguments.of("nothing selected", List.of(), List.of(List.of())));
    }

    private static List<String> with(String status, List<String> others) {
        List<String> selection = new ArrayList<>(List.of(status));
        selection.addAll(others);
        return selection;
    }

    @Test
    void discoverCertificate_activeAndSomeReasons_countsCertificatesFromBothSearches() throws Exception {
        DiscoveryHistory history = buildHistory();
        DiscoveryRequestDto request = buildEjbcaRequest();
        request.getAttributes().add(statusAttr(List.of(ACTIVE, KEY_COMPROMISE)));

        given(ejbcaService.getEjbcaVersion(INSTANCE_UUID)).willReturn(ejbcaVersion("7.11.0"));
        given(ejbcaService.searchCertificates(eq(INSTANCE_UUID), eq(REST_API_URL), any()))
                .willReturn(pageWithOneCert(1))
                .willReturn(emptyPage(2))
                .willReturn(pageWithOneCert(1))
                .willReturn(emptyPage(2));

        service.discoverCertificate(request, history);

        assertEquals(DiscoveryStatus.COMPLETED, history.getStatus());
        verify(certificateRepository, times(2)).save(any(Certificate.class));
        List<MetadataAttributeV2> meta = AttributeDefinitionUtils
                .deserialize(history.getMeta(), MetadataAttributeV2.class);
        assertEquals(2, meta.get(0).getContent().get(0).getData());
    }

    @Test
    void discoverCertificate_unknownStatus_failsWithoutSearching() throws Exception {
        DiscoveryHistory history = buildHistory();
        DiscoveryRequestDto request = buildEjbcaRequest();
        request.getAttributes().add(statusAttr(List.of(ACTIVE, "CERT_EXPIRED")));

        service.discoverCertificate(request, history);

        assertEquals(DiscoveryStatus.FAILED, history.getStatus());
        assertTrue(history.getMeta().contains("CERT_EXPIRED"),
                "the failure reason should name the unknown status, got: " + history.getMeta());
        verify(ejbcaService, never()).searchCertificates(any(), any(), any());
    }

    private RequestAttributeV2 statusAttr(List<String> statuses) {
        RequestAttributeV2 attr = new RequestAttributeV2();
        attr.setName(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_STATUS);
        attr.setContentType(AttributeContentType.STRING);
        attr.setContent(statuses.stream().<BaseAttributeContentV2<?>>map(StringAttributeContentV2::new).toList());
        return attr;
    }

    private static List<String> statusCriteria(SearchCertificatesRestRequestV2 searchRequest) {
        return searchRequest
                .getCriteria()
                .stream()
                .filter(c -> SearchCertificateCriteriaRestRequest.CriteriaProperty.STATUS
                        .name()
                        .equals(c.getProperty()))
                .map(SearchCertificateCriteriaRestRequest::getValue)
                .toList();
    }

    private void assertFailedOnNumberOfDays(DiscoveryHistory history) throws Exception {
        assertEquals(DiscoveryStatus.FAILED, history.getStatus());
        assertTrue(history.getMeta().contains("Number of days must be a positive whole number"),
                "the failure reason should name Number of days, got: " + history.getMeta());
        verify(ejbcaService, never()).searchCertificates(any(), any(), any());
    }

    private static String issuedAfterCriterion(SearchCertificatesRestRequestV2 searchRequest) {
        return searchRequest
                .getCriteria()
                .stream()
                .filter(c -> SearchCertificateCriteriaRestRequest.CriteriaProperty.ISSUED_DATE
                        .name()
                        .equals(c.getProperty())
                        && SearchCertificateCriteriaRestRequest.CriteriaOperation.AFTER.name().equals(c.getOperation()))
                .map(SearchCertificateCriteriaRestRequest::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No ISSUED_DATE AFTER criterion in the search request"));
    }
}
