package com.otilm.ca.connector.ejbca.service.impl;

import com.otilm.api.model.common.HealthDto;
import com.otilm.api.model.common.HealthStatus;
import com.otilm.ca.connector.ejbca.service.AuthorityInstanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class HealthCheckServiceImplTest {

    @Mock
    AuthorityInstanceService authorityInstanceService;

    HealthCheckServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HealthCheckServiceImpl();
        service.setAuthorityInstanceService(authorityInstanceService);
    }

    @Test
    void checkHealth_dbOk_returnsStatusOK() {
        // listAuthorityInstances returns without throwing → DB is healthy
        given(authorityInstanceService.listAuthorityInstances()).willReturn(java.util.List.of());

        HealthDto health = service.checkHealth();

        assertNotNull(health);
        assertEquals(HealthStatus.OK, health.getStatus());
        assertNotNull(health.getParts());
        assertTrue(health.getParts().containsKey("database"));
        assertEquals(HealthStatus.OK, health.getParts().get("database").getStatus());
    }

    @Test
    void checkHealth_dbThrows_returnsStatusNOK() {
        RuntimeException dbError = new RuntimeException("DB connection refused");
        given(authorityInstanceService.listAuthorityInstances()).willThrow(dbError);

        HealthDto health = service.checkHealth();

        assertNotNull(health);
        assertEquals(HealthStatus.NOK, health.getStatus());
        HealthDto dbPart = health.getParts().get("database");
        assertNotNull(dbPart);
        assertEquals(HealthStatus.NOK, dbPart.getStatus());
        assertEquals("DB connection refused", dbPart.getDescription());
    }
}
