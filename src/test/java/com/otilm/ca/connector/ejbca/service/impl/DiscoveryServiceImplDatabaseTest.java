package com.otilm.ca.connector.ejbca.service.impl;

import com.otilm.api.model.connector.discovery.DiscoveryDataRequestDto;
import com.otilm.api.model.connector.discovery.DiscoveryProviderCertificateDataDto;
import com.otilm.api.model.connector.discovery.DiscoveryProviderDto;
import com.otilm.api.model.core.discovery.DiscoveryStatus;
import com.otilm.ca.connector.ejbca.dao.CertificateRepository;
import com.otilm.ca.connector.ejbca.dao.DiscoveryHistoryRepository;
import com.otilm.ca.connector.ejbca.dao.entity.Certificate;
import com.otilm.ca.connector.ejbca.dao.entity.DiscoveryHistory;
import com.otilm.ca.connector.ejbca.service.DiscoveryService;
import jakarta.persistence.EntityManagerFactory;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs the discovery service against the migrated schema and counts what Hibernate loads and deletes. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:discovery-results;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN"})
class DiscoveryServiceImplDatabaseTest {

    @Autowired
    private DiscoveryService discoveryService;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private DiscoveryHistoryRepository discoveryHistoryRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    @AfterEach
    void cleanUp() {
        certificateRepository.deleteAll();
        discoveryHistoryRepository.deleteAll();
    }

    @Test
    void pageOfACompletedRunLoadsOnlyTheCertificatesOnThatPage() {
        DiscoveryHistory run = completedRun("run", 5);
        completedRun("other", 3);
        List<String> runUuids = uuidsOf(run);
        statistics.clear();

        DiscoveryProviderDto dto = discoveryService.getProviderDtoData(page(2, 2), run);

        assertEquals(5, dto.getTotalCertificatesDiscovered());
        assertEquals(runUuids.subList(2, 4),
                dto.getCertificateData().stream().map(DiscoveryProviderCertificateDataDto::getUuid).toList());
        assertEquals(2, certificateStatistics().getLoadCount());
    }

    @Test
    void deletingARunRemovesItsCertificatesWithoutLoadingThem() throws Exception {
        DiscoveryHistory run = completedRun("run", 3);
        DiscoveryHistory other = completedRun("other", 2);
        statistics.clear();

        discoveryService.deleteDiscovery(run.getUuid());

        assertEquals(0, certificateStatistics().getLoadCount());
        assertEquals(0, certificateStatistics().getDeleteCount());
        assertEquals(List.of(other.getId(), other.getId()),
                certificateRepository.findAll().stream().map(Certificate::getDiscoveryId).toList());
        assertTrue(discoveryHistoryRepository.findByUuid(run.getUuid()).isEmpty());
    }

    private DiscoveryHistory completedRun(String name, int certificates) {
        DiscoveryHistory history = new DiscoveryHistory();
        history.setUuid(UUID.randomUUID().toString());
        history.setName(name);
        history.setStatus(DiscoveryStatus.COMPLETED);
        DiscoveryHistory saved = discoveryHistoryRepository.save(history);
        for (int i = 0; i < certificates; i++) {
            Certificate certificate = new Certificate();
            certificate.setUuid(UUID.randomUUID().toString());
            certificate.setBase64Content("MIIBszCCAVmgAwIBAgIU");
            certificate.setDiscoveryId(saved.getId());
            certificateRepository.save(certificate);
        }
        return saved;
    }

    private List<String> uuidsOf(DiscoveryHistory run) {
        return certificateRepository
                .findAll()
                .stream()
                .filter(certificate -> run.getId().equals(certificate.getDiscoveryId()))
                .sorted(Comparator.comparing(Certificate::getId))
                .map(Certificate::getUuid)
                .toList();
    }

    private static DiscoveryDataRequestDto page(int pageNumber, int itemsPerPage) {
        DiscoveryDataRequestDto request = new DiscoveryDataRequestDto();
        request.setPageNumber(pageNumber);
        request.setItemsPerPage(itemsPerPage);
        return request;
    }

    private EntityStatistics certificateStatistics() {
        return statistics.getEntityStatistics(Certificate.class.getName());
    }
}
