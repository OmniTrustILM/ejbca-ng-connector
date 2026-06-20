package com.otilm.ca.connector.ejbca.api;

import com.otilm.api.exception.AlreadyExistException;
import com.otilm.api.exception.NotFoundException;
import com.otilm.api.exception.ValidationError;
import com.otilm.api.exception.ValidationException;
import com.otilm.api.model.common.NameAndIdDto;
import com.otilm.api.model.common.attribute.common.BaseAttribute;
import com.otilm.api.model.common.attribute.v2.content.BaseAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.ObjectAttributeContentV2;
import com.otilm.api.model.common.attribute.v2.content.StringAttributeContentV2;
import com.otilm.ca.connector.ejbca.dto.EjbcaVersionResponseDto;
import com.otilm.ca.connector.ejbca.service.AuthorityInstanceService;
import com.otilm.ca.connector.ejbca.service.DiscoveryAttributeService;
import com.otilm.ca.connector.ejbca.service.EjbcaService;
import com.otilm.ca.connector.ejbca.service.EndEntityProfileEjbcaService;
import com.otilm.ca.connector.ejbca.util.EjbcaVersion;
import com.otilm.ca.connector.ejbca.util.LocalAttributeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v1/discoveryProvider")
public class DiscoverySupportController {

    private EjbcaService ejbcaService;
    private EndEntityProfileEjbcaService endEntityProfileEjbcaService;
    private AuthorityInstanceService authorityInstanceService;
    private DiscoveryAttributeService discoveryAttributeService;

    @Autowired
    public void setEjbcaService(EjbcaService ejbcaService) {
        this.ejbcaService = ejbcaService;
    }

    @Autowired
    public void setEndEntityProfileEjbcaService(EndEntityProfileEjbcaService endEntityProfileEjbcaService) {
        this.endEntityProfileEjbcaService = endEntityProfileEjbcaService;
    }

    @Autowired
    public void setAuthorityInstanceService(AuthorityInstanceService authorityInstanceService) {
        this.authorityInstanceService = authorityInstanceService;
    }

    @Autowired
    public void setDiscoveryAttributeService(DiscoveryAttributeService discoveryAttributeService) {
        this.discoveryAttributeService = discoveryAttributeService;
    }

    @RequestMapping(
            path = "/{ejbcaInstanceName}/ejbcaVersion",
            method = RequestMethod.GET,
            produces = "application/json"
    )
    public EjbcaVersionResponseDto getEjbcaVersion(@PathVariable String ejbcaInstanceName) throws NotFoundException, AlreadyExistException {
        EjbcaVersion ejbcaVersion = ejbcaService.getEjbcaVersion(ejbcaInstanceName);

        if (ejbcaVersion.getMajorVersion() < 9) {
            List<ValidationError> errors = new ArrayList<>();
            errors.add(ValidationError.create("EJBCA version missing", ""));
            throw new ValidationException("EJBCA version is not supported", errors);
        }

        //List<String> list = new ArrayList<>();
        //list.add(ejbcaVersion.toString());
        //return ejbcaVersion.toString();

        return new EjbcaVersionResponseDto(ejbcaVersion.toString());
    }

    @RequestMapping(
            path = "/{ejbcaInstanceUuid}/listEndEntityProfiles",
            method = RequestMethod.GET,
            produces = "application/json"
    )
    public List<ObjectAttributeContentV2> listEndEntityProfiles(@PathVariable String ejbcaInstanceUuid) throws NotFoundException {
        checkEjbcaVersion(ejbcaInstanceUuid);

        List<NameAndIdDto> endEntityProfiles = endEntityProfileEjbcaService.listEndEntityProfiles(ejbcaInstanceUuid);
        List<ObjectAttributeContentV2> contentList = new ArrayList<>();
        for (NameAndIdDto endEntityProfile : endEntityProfiles) {
            ObjectAttributeContentV2 content = new ObjectAttributeContentV2(endEntityProfile.getName(), endEntityProfile);
            contentList.add(content);
        }
        return contentList;
    }

    @RequestMapping(
            path = "/{ejbcaInstanceUuid}/listCas",
            method = RequestMethod.GET,
            produces = "application/json"
    )
    public List<ObjectAttributeContentV2> listCas(@PathVariable String ejbcaInstanceUuid) throws NotFoundException {
        checkEjbcaVersion(ejbcaInstanceUuid);

        List<NameAndIdDto> cas = ejbcaService.getAvailableCas(ejbcaInstanceUuid);
        return LocalAttributeUtil.convertFromNameAndId(cas);
    }

    @RequestMapping(
            path = "/{ejbcaInstanceUuid}/ejbcaRestApi",
            method = RequestMethod.GET,
            produces = "application/json"
    )
    public StringAttributeContentV2 ejbcaRestApi(@PathVariable String ejbcaInstanceUuid) throws NotFoundException {
        checkEjbcaVersion(ejbcaInstanceUuid);

        String url = authorityInstanceService.getRestApiUrl(ejbcaInstanceUuid);
        return new StringAttributeContentV2(url);
    }

    @RequestMapping(
            path = "/{ejbcaInstanceUuid}/{kind}/configuration",
            method = RequestMethod.GET,
            produces = "application/json"
    )
    public List<BaseAttribute> configuration(
            @PathVariable String ejbcaInstanceUuid, @PathVariable String kind) throws NotFoundException {
        checkEjbcaVersion(ejbcaInstanceUuid);

        List<NameAndIdDto> endEntityProfiles = endEntityProfileEjbcaService.listEndEntityProfiles(ejbcaInstanceUuid);
        List<BaseAttributeContentV2<?>> eeProfilesContent = new ArrayList<>();
        for (NameAndIdDto endEntityProfile : endEntityProfiles) {
            ObjectAttributeContentV2 content = new ObjectAttributeContentV2(endEntityProfile.getName(), endEntityProfile);
            eeProfilesContent.add(content);
        }

        List<NameAndIdDto> cas = ejbcaService.getAvailableCas(ejbcaInstanceUuid);
        List<BaseAttributeContentV2<?>> casContent = LocalAttributeUtil.convertFromNameAndIdToBase(cas);

        String url = authorityInstanceService.getRestApiUrl(ejbcaInstanceUuid);
        List<BaseAttributeContentV2<?>> urlContent = new ArrayList<>();
        StringAttributeContentV2 urlAttributeContent = new StringAttributeContentV2(url);
        urlContent.add(urlAttributeContent);

        return discoveryAttributeService.getInstanceAndKindAttributes(kind, eeProfilesContent, casContent, urlContent);
    }

    private void checkEjbcaVersion(String ejbcaInstanceName) throws NotFoundException {
        EjbcaVersion ejbcaVersion = ejbcaService.getEjbcaVersion(ejbcaInstanceName);

        boolean supported = false;
        // check the EJBCA version, only from 7.8 and above the REST API for certificate searching is available
        if (ejbcaVersion.getTechVersion() > 7) {
            supported = true;
        } else if (ejbcaVersion.getTechVersion() == 7) {
            supported = ejbcaVersion.getMajorVersion() >= 8;
        }

        if (!supported) {
            List<ValidationError> errors = new ArrayList<>();
            errors.add(ValidationError.create("Unsupported version " + ejbcaVersion, ""));
            throw new ValidationException("EJBCA version is not supported", errors);
        }
    }
}
