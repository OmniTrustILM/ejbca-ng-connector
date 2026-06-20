package com.otilm.ca.connector.ejbca.api;

import com.otilm.api.exception.ValidationException;
import com.otilm.api.interfaces.connector.AttributesController;
import com.otilm.api.model.client.attribute.RequestAttribute;
import com.otilm.api.model.common.attribute.common.BaseAttribute;
import com.otilm.ca.connector.ejbca.service.DiscoveryAttributeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/discoveryProvider/{kind}/attributes")
public class DiscoveryAttributesControllerImpl implements AttributesController {

    private DiscoveryAttributeService discoveryAttributeService;

    @Autowired
    public void setDiscoveryAttributeService(DiscoveryAttributeService discoveryAttributeService) {
        this.discoveryAttributeService = discoveryAttributeService;
    }

    @Override
    public List<BaseAttribute> listAttributeDefinitions(String kind) {
        return discoveryAttributeService.getAttributes(kind);
    }

    @Override
    public void validateAttributes(String kind, List<RequestAttribute> attributes) throws ValidationException {
        discoveryAttributeService.validateAttributes(kind, attributes);
    }
}
