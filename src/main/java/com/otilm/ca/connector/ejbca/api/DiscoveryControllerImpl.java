package com.otilm.ca.connector.ejbca.api;

import com.otilm.api.exception.NotFoundException;
import com.otilm.api.interfaces.connector.DiscoveryController;
import com.otilm.api.model.connector.discovery.DiscoveryDataRequestDto;
import com.otilm.api.model.connector.discovery.DiscoveryProviderDto;
import com.otilm.api.model.connector.discovery.DiscoveryRequestDto;
import com.otilm.ca.connector.ejbca.dao.entity.DiscoveryHistory;
import com.otilm.ca.connector.ejbca.service.DiscoveryHistoryService;
import com.otilm.ca.connector.ejbca.service.DiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class DiscoveryControllerImpl implements DiscoveryController {

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryControllerImpl.class);

    @Autowired
    public void setDiscoveryHistoryService(DiscoveryHistoryService discoveryHistoryService) {
        this.discoveryHistoryService = discoveryHistoryService;
    }
    @Autowired
    public void setDiscoveryService(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    private DiscoveryHistoryService discoveryHistoryService;
    private DiscoveryService discoveryService;

    @Override
    public DiscoveryProviderDto discoverCertificate(DiscoveryRequestDto request) throws IOException, NotFoundException {
        logger.debug("Initiating certificate discovery: {}", request);
        DiscoveryHistory history;
        history = discoveryHistoryService.addHistory(request);
        discoveryService.discoverCertificate(request, history);
        DiscoveryDataRequestDto dto = new DiscoveryDataRequestDto();
        dto.setName(request.getName());
        // initial values when discovery is finished before the Async method discoverCertificate
        dto.setPageNumber(0);
        dto.setItemsPerPage(10);
        return discoveryService.getProviderDtoData(dto, history);
    }

    @Override
    public DiscoveryProviderDto getDiscovery(String uuid, DiscoveryDataRequestDto request) throws IOException, NotFoundException {
        DiscoveryHistory history = discoveryHistoryService.getHistoryByUuid(uuid);
        return discoveryService.getProviderDtoData(request, history);
    }

    @Override
    public void deleteDiscovery(String uuid) throws IOException, NotFoundException {
        discoveryService.deleteDiscovery(uuid);
    }
}
