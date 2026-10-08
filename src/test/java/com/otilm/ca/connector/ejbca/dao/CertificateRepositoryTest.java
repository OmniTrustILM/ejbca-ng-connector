package com.otilm.ca.connector.ejbca.dao;

import com.otilm.ca.connector.ejbca.dao.entity.Certificate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Checks that the entities match the tables the Flyway migrations create. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:migrated;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none"})
class CertificateRepositoryTest {

    @Autowired
    private CertificateRepository certificateRepository;

    @Test
    void discoveredCertificateRoundTripsThroughTheMigratedSchema() {
        Certificate certificate = new Certificate();
        certificate.setUuid(UUID.randomUUID().toString());
        certificate.setBase64Content("MIIBszCCAVmgAwIBAgIU");
        certificate.setDiscoveryId(1L);

        Long id = certificateRepository.saveAndFlush(certificate).getId();

        assertEquals("MIIBszCCAVmgAwIBAgIU", certificateRepository.findById(id).orElseThrow().getBase64Content());
    }
}
