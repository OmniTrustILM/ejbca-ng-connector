package com.czertainly.ca.connector.ejbca.service.impl;

import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.model.common.NameAndIdDto;
import com.czertainly.api.model.common.attribute.common.AttributeType;
import com.czertainly.api.model.common.attribute.common.MetadataAttribute;
import com.czertainly.api.model.common.attribute.common.content.AttributeContentType;
import com.czertainly.api.model.common.attribute.v2.MetadataAttributeV2;
import com.czertainly.api.model.common.attribute.v2.content.BaseAttributeContentV2;
import com.czertainly.api.model.common.attribute.v2.content.DateTimeAttributeContentV2;
import com.czertainly.api.model.common.attribute.v2.content.IntegerAttributeContentV2;
import com.czertainly.api.model.common.attribute.v2.content.StringAttributeContentV2;
import com.czertainly.api.model.common.attribute.common.properties.MetadataAttributeProperties;
import com.czertainly.api.model.connector.discovery.DiscoveryDataRequestDto;
import com.czertainly.api.model.connector.discovery.DiscoveryProviderDto;
import com.czertainly.api.model.connector.discovery.DiscoveryRequestDto;
import com.czertainly.api.model.core.discovery.DiscoveryStatus;
import com.czertainly.ca.connector.ejbca.dao.CertificateRepository;
import com.czertainly.ca.connector.ejbca.dao.entity.Certificate;
import com.czertainly.ca.connector.ejbca.dao.entity.DiscoveryHistory;
import com.czertainly.ca.connector.ejbca.dto.AuthorityInstanceNameAndUuidDto;
import com.czertainly.ca.connector.ejbca.dto.ejbca.request.Pagination;
import com.czertainly.ca.connector.ejbca.dto.ejbca.request.SearchCertificateCriteriaRestRequest;
import com.czertainly.ca.connector.ejbca.dto.ejbca.request.SearchCertificateSortRestRequest;
import com.czertainly.ca.connector.ejbca.dto.ejbca.request.SearchCertificatesRestRequestV2;
import com.czertainly.ca.connector.ejbca.dto.ejbca.response.CertificateRestResponseV2;
import com.czertainly.ca.connector.ejbca.dto.ejbca.response.SearchCertificatesRestResponseV2;
import com.czertainly.ca.connector.ejbca.service.DiscoveryHistoryService;
import com.czertainly.ca.connector.ejbca.service.DiscoveryService;
import com.czertainly.ca.connector.ejbca.service.EjbcaService;
import com.czertainly.ca.connector.ejbca.util.EjbcaVersion;
import com.czertainly.core.util.AttributeDefinitionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class DiscoveryServiceImpl implements DiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryServiceImpl.class);

    /**
     * This constant represents the number of certificate per page in searching
     */
    @Value("${ejbca.search.pageSize:100}")
    private int EJBCA_SEARCH_PAGE_SIZE;
    private EjbcaService ejbcaService;
    private CertificateRepository certificateRepository;
    private DiscoveryHistoryService discoveryHistoryService;

    @Autowired
    public void setEjbcaService(EjbcaService ejbcaService) {
        this.ejbcaService = ejbcaService;
    }

    @Autowired
    public void setCertificateRepository(CertificateRepository certificateRepository) {
        this.certificateRepository = certificateRepository;
    }

    @Autowired
    public void setDiscoveryHistoryService(DiscoveryHistoryService discoveryHistoryService) {
        this.discoveryHistoryService = discoveryHistoryService;
    }

    @Override
    @Async
    public void discoverCertificate(DiscoveryRequestDto request, DiscoveryHistory history) throws NotFoundException {
        try {
            discoverCertificatesInternal(request, history);
        } catch (Exception e) {
            history.setStatus(DiscoveryStatus.FAILED);
            history.setMeta(AttributeDefinitionUtils.serialize(getReasonMeta(e.getMessage())));
            discoveryHistoryService.setHistory(history);
            logger.error(e.getMessage());
        }
    }

    @Override
    public DiscoveryProviderDto getProviderDtoData(DiscoveryDataRequestDto request, DiscoveryHistory history) {
        DiscoveryProviderDto dto = new DiscoveryProviderDto();
        dto.setUuid(history.getUuid());
        dto.setName(history.getName());
        dto.setStatus(history.getStatus());
        dto.setMeta(AttributeDefinitionUtils.deserialize(history.getMeta(), MetadataAttribute.class));
        int totalCertificateSize = certificateRepository.findByDiscoveryId(history.getId()).size();
        dto.setTotalCertificatesDiscovered(totalCertificateSize);
        if (history.getStatus() == DiscoveryStatus.IN_PROGRESS) {
            dto.setCertificateData(new ArrayList<>());
            dto.setTotalCertificatesDiscovered(0);
        } else {
            Pageable page = PageRequest.of(request.getPageNumber() <= 0 ? 0 : request.getPageNumber() - 1, request.getItemsPerPage(), Sort.by(Sort.Direction.ASC, "id"));
            dto.setCertificateData(certificateRepository.findAllByDiscoveryId(history.getId(), page).stream().map(Certificate::mapToDto).collect(Collectors.toList()));
        }
        return dto;
    }

    @Override
    public void deleteDiscovery(String uuid) throws NotFoundException {
        DiscoveryHistory discoveryHistory = discoveryHistoryService.getHistoryByUuid(uuid);
        List<Certificate> certificates = certificateRepository.findByDiscoveryId(discoveryHistory.getId());
        certificateRepository.deleteAll(certificates);
        discoveryHistoryService.deleteHistory(discoveryHistory);
    }

    private List<MetadataAttribute> getReasonMeta(String exception) {
        List<MetadataAttribute> attributes = new ArrayList<>();

        //Exception Reason
        MetadataAttributeV2 attribute = new MetadataAttributeV2();
        attribute.setName("reason");
        attribute.setUuid("abc0412a-60f6-11ed-9b6a-0242ac120002");
        attribute.setContentType(AttributeContentType.STRING);
        attribute.setType(AttributeType.META);
        attribute.setDescription("Reason for failure");

        MetadataAttributeProperties attributeProperties = new MetadataAttributeProperties();
        attributeProperties.setLabel("Reason");
        attributeProperties.setVisible(true);

        attribute.setProperties(attributeProperties);
        attribute.setContent(List.of(new StringAttributeContentV2(exception)));
        attributes.add(attribute);

        return attributes;
    }

    private void discoverCertificatesInternal(DiscoveryRequestDto request, DiscoveryHistory history) throws Exception {
        logger.info("Discovery initiated for the request with name {}", request.getName());
        int certificatesFound = 0;

        final AuthorityInstanceNameAndUuidDto instance = AttributeDefinitionUtils.getObjectAttributeContentData(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_INSTANCE, request.getAttributes(), AuthorityInstanceNameAndUuidDto.class).get(0);
        final String restApiUrl = AttributeDefinitionUtils.getSingleItemAttributeContentValue(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_RESTAPI_URL, request.getAttributes(), StringAttributeContentV2.class).getData();
        final List<NameAndIdDto> cas = AttributeDefinitionUtils.getObjectAttributeContentDataList(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_CA, request.getAttributes(), NameAndIdDto.class);
        final List<NameAndIdDto> eeProfiles = AttributeDefinitionUtils.getObjectAttributeContentDataList(DiscoveryAttributeServiceImpl.ATTRIBUTE_END_ENTITY_PROFILE, request.getAttributes(), NameAndIdDto.class);
        final List<String> statuses = AttributeDefinitionUtils.getAttributeContentValueList(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_STATUS, request.getAttributes(), BaseAttributeContentV2.class);

        ZonedDateTime issuedAfter = null;
        if (request.getKind().equals("EJBCA")) {
            issuedAfter = AttributeDefinitionUtils.getSingleItemAttributeContentValue(DiscoveryAttributeServiceImpl.ATTRIBUTE_EJBCA_ISSUED_AFTER, request.getAttributes(), DateTimeAttributeContentV2.class).getData();
        }
        if (request.getKind().equals("EJBCA-SCHEDULE")) {
            Integer issuedDaysBefore = AttributeDefinitionUtils.getSingleItemAttributeContentValue(DiscoveryAttributeServiceImpl.ATTRIBUTE_ISSUED_DAYS_BEFORE, request.getAttributes(), IntegerAttributeContentV2.class).getData();
            issuedAfter = ZonedDateTime.now();
            issuedAfter = issuedAfter.minusDays(issuedDaysBefore);
        }

        SearchCertificatesRestRequestV2 searchRequest = prepareSearchRequest(cas, eeProfiles, statuses, issuedAfter);
        SearchCertificatesRestResponseV2 searchResponse;

        // behaviour of the EJBCA REST API for searching certificates differs between versions
        // we need to check the version and decide on the implementation
        // TODO: this can be improved once there are more versions of implementation
        EjbcaVersion ejbcaVersion = ejbcaService.getEjbcaVersion(instance.getUuid());
        logger.debug("Searching for certificates in EJBCA version {}, with page size {}", ejbcaVersion.getVersion(), EJBCA_SEARCH_PAGE_SIZE);

        int searchVersion = 0;
        // we need to check version of EJBCA for different implementation of the REST API
        if (ejbcaVersion.getTechVersion() > 7) {
            searchVersion = 2;
        } else if (ejbcaVersion.getTechVersion() == 7 && ejbcaVersion.getMajorVersion() >= 11) {
            searchVersion = 2;
        } else if (ejbcaVersion.getTechVersion() == 7 && ejbcaVersion.getMajorVersion() >= 8) {
            searchVersion = 1;
        } else {
            throw new Exception("Unsupported EJBCA version");
        }

        // when the version is at least 7.11
        if (searchVersion == 2) {
            do {
                logger.info("Request: {}", searchRequest);
                searchResponse = ejbcaService.searchCertificates(instance.getUuid(), restApiUrl, searchRequest);
                logger.info("Page: {}, Found {}", searchResponse.getPaginationSummary().getCurrentPage() ,searchResponse.getCertificates().size());
                // break the loop if there are no certificates returned from EJBCA
                if (searchResponse.getCertificates().isEmpty()) {
                    break;
                }
                // set the next page
                searchRequest.getPagination().setCurrentPage(searchResponse.getPaginationSummary().getCurrentPage() + 1);
                parseAndCreateCertificateEntry(searchResponse, history);
                certificatesFound = certificatesFound + searchResponse.getCertificates().size();
                logger.info("Before while: isEmpty: {}", searchResponse.getCertificates().isEmpty());
            } while (!searchResponse.getCertificates().isEmpty());
        } else { // when the version is lower than 7.11, but higher than 7.8
            do {
                searchResponse = ejbcaService.searchCertificates(instance.getUuid(), restApiUrl, searchRequest);
                // break the loop if there are no certificates returned from EJBCA
                if (searchResponse.getCertificates().isEmpty()) {
                    break;
                }
                // set the next page
                searchRequest.getPagination().setCurrentPage(searchResponse.getPaginationSummary().getCurrentPage() + 1);
                parseAndCreateCertificateEntry(searchResponse, history);
                certificatesFound = certificatesFound + searchResponse.getCertificates().size();
            } while (searchResponse.getPaginationSummary().getTotalCerts() == null);
        }

        history.setStatus(DiscoveryStatus.COMPLETED);
        history.setMeta(AttributeDefinitionUtils.serialize(getDiscoveryMeta(certificatesFound)));
        discoveryHistoryService.setHistory(history);
        logger.info("Discovery Completed. Name of the discovery is {}", request.getName());
    }

    private List<MetadataAttribute> getDiscoveryMeta(Integer totalCertificates) {
        List<MetadataAttribute> attributes = new ArrayList<>();

        //Total Certificates
        MetadataAttributeV2 attribute = new MetadataAttributeV2();
        attribute.setName("totalCertificates");
        attribute.setUuid("20add2d6-60f7-11ed-9b6a-0242ac120002");
        attribute.setContentType(AttributeContentType.INTEGER);
        attribute.setType(AttributeType.META);
        attribute.setDescription("Total Number of Certificates Discovered");

        MetadataAttributeProperties attributeProperties = new MetadataAttributeProperties();
        attributeProperties.setLabel("Total Certificates Discovered");
        attributeProperties.setVisible(true);

        attribute.setProperties(attributeProperties);
        attribute.setContent(List.of(new IntegerAttributeContentV2(totalCertificates.toString(), totalCertificates)));
        attributes.add(attribute);
        return attributes;
    }

    private SearchCertificatesRestRequestV2 prepareSearchRequest(
            List<NameAndIdDto> cas, List<NameAndIdDto> eeProfiles,
            List<String> statuses, ZonedDateTime issuedAfter
    ) {
        SearchCertificatesRestRequestV2 request = new SearchCertificatesRestRequestV2();

        Pagination pagination = new Pagination();
        pagination.setPageSize(EJBCA_SEARCH_PAGE_SIZE);
        pagination.setCurrentPage(1);

        SearchCertificateSortRestRequest sort = new SearchCertificateSortRestRequest();
        sort.setOperation(SearchCertificateSortRestRequest.SortOperation.ASC.name());
        sort.setProperty(SearchCertificateSortRestRequest.SortProperty.USERNAME.name());

        List<SearchCertificateCriteriaRestRequest> criteria = new ArrayList<>();

        if (cas != null) {
            for (NameAndIdDto ca : cas) {
                SearchCertificateCriteriaRestRequest c = new SearchCertificateCriteriaRestRequest();
                c.setOperation(SearchCertificateCriteriaRestRequest.CriteriaOperation.EQUAL.name());
                c.setProperty(SearchCertificateCriteriaRestRequest.CriteriaProperty.CA.name());
                c.setValue(ca.getName());
                criteria.add(c);
            }
        }

        if (eeProfiles != null) {
            for (NameAndIdDto eeProfile : eeProfiles) {
                SearchCertificateCriteriaRestRequest c = new SearchCertificateCriteriaRestRequest();
                c.setOperation(SearchCertificateCriteriaRestRequest.CriteriaOperation.EQUAL.name());
                c.setProperty(SearchCertificateCriteriaRestRequest.CriteriaProperty.END_ENTITY_PROFILE.name());
                c.setValue(eeProfile.getName());
                criteria.add(c);
            }
        }

        if (statuses != null) {
            for (String status : statuses) {
                SearchCertificateCriteriaRestRequest c = new SearchCertificateCriteriaRestRequest();
                c.setOperation(SearchCertificateCriteriaRestRequest.CriteriaOperation.EQUAL.name());
                c.setProperty(SearchCertificateCriteriaRestRequest.CriteriaProperty.STATUS.name());
                c.setValue(status);
                criteria.add(c);
            }
        }

        SearchCertificateCriteriaRestRequest c = new SearchCertificateCriteriaRestRequest();
        c.setOperation(SearchCertificateCriteriaRestRequest.CriteriaOperation.AFTER.name());
        c.setProperty(SearchCertificateCriteriaRestRequest.CriteriaProperty.ISSUED_DATE.name());
        if (issuedAfter != null) {
            // convert ZonedDateTime to appropriate format for EJBCA (2019-04-18T07:47:26Z)
            OffsetDateTime odt = issuedAfter.toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
            String dateValue = odt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));

            c.setValue(dateValue);
        } else {
            c.setValue("2000-01-01T00:00:00Z"); // before the EJBCA was born
        }
        criteria.add(c);

        request.setPagination(pagination);
        request.setSort(sort);
        request.setCriteria(criteria);

        return request;
    }

    private void parseAndCreateCertificateEntry(SearchCertificatesRestResponseV2 searchResponse, DiscoveryHistory discoveryHistory) throws NullPointerException {
        logger.info("Parsing {} certificates from page {} in discovery {}",
                searchResponse.getCertificates().size(), searchResponse.getPaginationSummary().getCurrentPage(), discoveryHistory.getName());

        for (CertificateRestResponseV2 certificateRestResponseV2 : searchResponse.getCertificates()) {
            Certificate cert = new Certificate();

            cert.setUuid(UUID.randomUUID().toString());
            cert.setDiscoveryId(discoveryHistory.getId());
            cert.setBase64Content(new String(certificateRestResponseV2.getCertificate(), StandardCharsets.UTF_8));

            cert.setMeta(AttributeDefinitionUtils.serialize(getCertificateMeta(
                    certificateRestResponseV2.getCertificateProfileId().toString(),
                    certificateRestResponseV2.getEndEntityProfileId().toString(),
                    certificateRestResponseV2.getUsername(),
                    discoveryHistory.getName()
            )));

            certificateRepository.save(cert);
        }
    }

    private List<MetadataAttribute> getCertificateMeta(String certificateProfileId, String endEntityProfileId, String username, String discoveryName) {
        List<MetadataAttribute> attributes = new ArrayList<>();

        //Certificate Profile ID
        MetadataAttributeV2 attribute = new MetadataAttributeV2();
        attribute.setName("certificateProfileId");
        attribute.setUuid("df2fb570-60fd-11ed-9b6a-0242ac120002");
        attribute.setContentType(AttributeContentType.STRING);
        attribute.setType(AttributeType.META);
        attribute.setDescription("Certificate Profile ID from where the certificate is discovered");

        MetadataAttributeProperties attributeProperties = new MetadataAttributeProperties();
        attributeProperties.setLabel("Certificate Profile ID");
        attributeProperties.setVisible(true);

        attribute.setProperties(attributeProperties);
        attribute.setContent(List.of(new StringAttributeContentV2(certificateProfileId)));
        attributes.add(attribute);

        //End Entity Profile ID
        MetadataAttributeV2 endEntityProfileIdAttribute = new MetadataAttributeV2();
        endEntityProfileIdAttribute.setName("endEntityProfileId");
        endEntityProfileIdAttribute.setUuid("df2fb93a-60fd-11ed-9b6a-0242ac120002");
        endEntityProfileIdAttribute.setContentType(AttributeContentType.STRING);
        endEntityProfileIdAttribute.setType(AttributeType.META);
        endEntityProfileIdAttribute.setDescription("End Entity Profile ID from where the certificate is discovered");

        MetadataAttributeProperties endEntityProfileIdAttributeProperties = new MetadataAttributeProperties();
        endEntityProfileIdAttributeProperties.setLabel("End Entity Profile ID");
        endEntityProfileIdAttributeProperties.setVisible(true);

        endEntityProfileIdAttribute.setProperties(endEntityProfileIdAttributeProperties);
        endEntityProfileIdAttribute.setContent(List.of(new StringAttributeContentV2(endEntityProfileId)));
        attributes.add(endEntityProfileIdAttribute);

        //Username
        MetadataAttributeV2 usernameAttribute = new MetadataAttributeV2();
        usernameAttribute.setName("username");
        usernameAttribute.setUuid("df2fbaa2-60fd-11ed-9b6a-0242ac120002");
        usernameAttribute.setContentType(AttributeContentType.STRING);
        usernameAttribute.setType(AttributeType.META);
        usernameAttribute.setDescription("Username of certificate");

        MetadataAttributeProperties usernameAttributeProperties = new MetadataAttributeProperties();
        usernameAttributeProperties.setLabel("Username");
        usernameAttributeProperties.setVisible(true);

        usernameAttribute.setProperties(usernameAttributeProperties);
        usernameAttribute.setContent(List.of(new StringAttributeContentV2(username)));
        attributes.add(usernameAttribute);

        //Discovery Source
        MetadataAttributeV2 discoverySourceAttribute = new MetadataAttributeV2();
        discoverySourceAttribute.setName("discoverySource");
        discoverySourceAttribute.setUuid("df2fbebc-60fd-11ed-9b6a-0242ac120002");
        discoverySourceAttribute.setContentType(AttributeContentType.STRING);
        discoverySourceAttribute.setType(AttributeType.META);
        discoverySourceAttribute.setDescription("Source from where the certificate is discovered");

        MetadataAttributeProperties discoverySourceAttributeProperties = new MetadataAttributeProperties();
        discoverySourceAttributeProperties.setLabel("Discovery Source");
        discoverySourceAttributeProperties.setVisible(true);

        discoverySourceAttribute.setProperties(discoverySourceAttributeProperties);
        discoverySourceAttribute.setContent(List.of(new StringAttributeContentV2("EJBCA-NG")));
        attributes.add(discoverySourceAttribute);

        //Discovery Name
        MetadataAttributeV2 discoveryNameAttribute = new MetadataAttributeV2();
        discoveryNameAttribute.setName("discoveryName");
        discoveryNameAttribute.setUuid("df2fbffc-60fd-11ed-9b6a-0242ac120002");
        discoveryNameAttribute.setContentType(AttributeContentType.STRING);
        discoveryNameAttribute.setType(AttributeType.META);
        discoveryNameAttribute.setDescription("Name of the discovery");

        MetadataAttributeProperties discoveryNameAttributeProperties = new MetadataAttributeProperties();
        discoveryNameAttributeProperties.setLabel("Discovery Name");
        discoveryNameAttributeProperties.setVisible(true);

        discoveryNameAttribute.setProperties(discoveryNameAttributeProperties);
        discoveryNameAttribute.setContent(List.of(new StringAttributeContentV2(discoveryName)));
        attributes.add(discoveryNameAttribute);

        return attributes;
    }
}
