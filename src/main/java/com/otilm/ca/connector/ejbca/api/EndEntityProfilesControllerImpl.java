package com.otilm.ca.connector.ejbca.api;

import com.otilm.api.exception.NotFoundException;
import com.otilm.api.model.common.NameAndIdDto;
import com.otilm.api.model.common.attribute.v2.content.ObjectAttributeContentV2;
import com.otilm.ca.connector.ejbca.service.EndEntityProfileEjbcaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v1/authorityProvider/authorities/{uuid}/endEntityProfiles")
public class EndEntityProfilesControllerImpl {

    @Autowired
    private EndEntityProfileEjbcaService endEntityProfileEjbcaService;

    @RequestMapping(method = RequestMethod.GET, produces = {"application/json"})
    public List<ObjectAttributeContentV2> listEntityProfiles(@PathVariable String uuid) throws NotFoundException {
        List<ObjectAttributeContentV2> listJsonContent = new ArrayList<>();
        List<NameAndIdDto> dataList = endEntityProfileEjbcaService.listEndEntityProfiles(uuid);
        for (NameAndIdDto data : dataList) {
            ObjectAttributeContentV2 content = new ObjectAttributeContentV2();
            content.setReference(data.getName());
            content.setData(data);
            listJsonContent.add(content);
        }
        return listJsonContent;
    }

    @RequestMapping(path = "/{endEntityProfileId}/certificateprofiles", method = RequestMethod.GET, produces = {"application/json"})
    public List<ObjectAttributeContentV2> listCertificateProfiles(@PathVariable String uuid, @PathVariable Integer endEntityProfileId) throws NotFoundException {
        List<ObjectAttributeContentV2> listJsonContent = new ArrayList<>();
        List<NameAndIdDto> dataList = endEntityProfileEjbcaService.listCertificateProfiles(uuid, endEntityProfileId);
        for (NameAndIdDto data : dataList) {
            ObjectAttributeContentV2 content = new ObjectAttributeContentV2();
            content.setReference(data.getName());
            content.setData(data);
            listJsonContent.add(content);
        }
        return listJsonContent;
    }

    @RequestMapping(path = "/{endEntityProfileId}/cas", method = RequestMethod.GET, produces = {"application/json"})
    public List<ObjectAttributeContentV2> listCAsInProfile(@PathVariable String uuid, @PathVariable Integer endEntityProfileId) throws NotFoundException {
        List<ObjectAttributeContentV2> listJsonContent = new ArrayList<>();
        List<NameAndIdDto> dataList = endEntityProfileEjbcaService.listCAsInProfile(uuid, endEntityProfileId);
        for (NameAndIdDto data : dataList) {
            ObjectAttributeContentV2 content = new ObjectAttributeContentV2();
            content.setReference(data.getName());
            content.setData(data);
            listJsonContent.add(content);
        }
        return listJsonContent;
    }
}
