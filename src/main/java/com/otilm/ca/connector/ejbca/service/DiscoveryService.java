package com.otilm.ca.connector.ejbca.service;

import com.otilm.api.exception.NotFoundException;
import com.otilm.api.model.connector.discovery.DiscoveryDataRequestDto;
import com.otilm.api.model.connector.discovery.DiscoveryProviderDto;
import com.otilm.api.model.connector.discovery.DiscoveryRequestDto;
import com.otilm.ca.connector.ejbca.dao.entity.DiscoveryHistory;

import java.io.IOException;

public interface DiscoveryService {

    void discoverCertificate(DiscoveryRequestDto request, DiscoveryHistory history) throws IOException, NotFoundException;

    DiscoveryProviderDto getProviderDtoData(DiscoveryDataRequestDto request, DiscoveryHistory history);

    void deleteDiscovery(String uuid) throws NotFoundException;
}
