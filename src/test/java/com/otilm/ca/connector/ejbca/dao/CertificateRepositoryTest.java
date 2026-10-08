package com.otilm.ca.connector.ejbca.dao;

import com.otilm.ca.connector.ejbca.dao.entity.Certificate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Saves into the schema the Flyway migrations create, which the other tests replace with one generated from the
 * entities, so an entity column the migrations spell differently fails here.
 */
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
