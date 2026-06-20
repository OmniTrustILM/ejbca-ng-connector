package com.otilm.ca.connector.ejbca.api;

import com.otilm.api.interfaces.connector.HealthController;
import com.otilm.api.model.common.HealthDto;
import com.otilm.ca.connector.ejbca.service.HealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthControllerImpl implements HealthController {

    @Autowired
    public void setHealthCheckService(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    HealthCheckService healthCheckService;

    @Override
    public HealthDto checkHealth() {
        return healthCheckService.checkHealth();
    }
}
