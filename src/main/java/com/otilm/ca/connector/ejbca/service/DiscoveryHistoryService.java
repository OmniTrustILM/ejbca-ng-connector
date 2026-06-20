package com.otilm.ca.connector.ejbca.service;

import com.otilm.api.exception.NotFoundException;
import com.otilm.api.model.connector.discovery.DiscoveryRequestDto;
import com.otilm.ca.connector.ejbca.dao.entity.DiscoveryHistory;

public interface DiscoveryHistoryService {

    DiscoveryHistory addHistory(DiscoveryRequestDto request);

    DiscoveryHistory getHistoryById(Long id) throws NotFoundException;

    DiscoveryHistory getHistoryByUuid(String uuid) throws NotFoundException;

    void setHistory(DiscoveryHistory history);

    void deleteHistory(DiscoveryHistory history);
}