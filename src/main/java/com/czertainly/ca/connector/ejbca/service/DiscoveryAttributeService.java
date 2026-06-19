package com.czertainly.ca.connector.ejbca.service;

import com.czertainly.api.model.client.attribute.RequestAttribute;
import com.czertainly.api.model.common.attribute.common.BaseAttribute;
import com.czertainly.api.model.common.attribute.v2.content.BaseAttributeContentV2;

import java.util.List;

public interface DiscoveryAttributeService {

    List<BaseAttribute> getAttributes(String kind);

    boolean validateAttributes(String kind, List<RequestAttribute> attributes);

    List<BaseAttribute> getInstanceAndKindAttributes(
            String kind,
            List<BaseAttributeContentV2<?>> eeProfilesContent,
            List<BaseAttributeContentV2<?>> casContent,
            List<BaseAttributeContentV2<?>> urlContent
    );
}
