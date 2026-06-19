package com.czertainly.ca.connector.ejbca.service.impl;

import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.exception.ValidationException;
import com.czertainly.api.model.client.attribute.RequestAttribute;
import com.czertainly.api.model.client.attribute.RequestAttributeV2;
import com.czertainly.api.model.common.NameAndIdDto;
import com.czertainly.api.model.connector.v2.CertificateIdentificationRequestDto;
import com.czertainly.api.model.connector.v2.CertificateIdentificationResponseDto;
import com.czertainly.ca.connector.ejbca.api.AuthorityInstanceControllerImpl;
import com.czertainly.ca.connector.ejbca.dto.ejbca.response.CertificateRestResponseV2;
import com.czertainly.ca.connector.ejbca.dto.ejbca.response.SearchCertificatesRestResponseV2;
import com.czertainly.ca.connector.ejbca.service.AuthorityInstanceService;
import com.czertainly.ca.connector.ejbca.service.EjbcaService;
import com.czertainly.ca.connector.ejbca.util.LocalAttributeUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class CertificateEjbcaServiceImplTest {

    @InjectMocks
    private CertificateEjbcaServiceImpl certificateEjbcaServiceImpl;

    @Mock
    private AuthorityInstanceService authorityInstanceService;

    @Mock
    private EjbcaService ejbcaService;

    @Test
    public void identifyCertificate_NotKnown() throws Exception {
        String uuid = "dde2cccc-616f-11ec-90d6-0242ac120003";
        given(authorityInstanceService.getRestApiUrl(eq(uuid))).willReturn("https://ejbca.czertainly.com:8443/ejbca/rest");
        given(ejbcaService.searchCertificates(eq(uuid), any(), any())).willReturn(getSearchCertificatesRestResponseV2_NoCertificates());

        Assertions.assertThrows(NotFoundException.class, () -> certificateEjbcaServiceImpl.identifyCertificate(uuid, getCertificateIdentificationRequestDto(12345, "Test")));
    }

    @Test
    public void identifyCertificate_WrongProfile() throws Exception {
        String uuid = "dde2cccc-616f-11ec-90d6-0242ac120003";
        given(authorityInstanceService.getRestApiUrl(eq(uuid))).willReturn("https://ejbca.czertainly.com:8443/ejbca/rest");
        given(ejbcaService.searchCertificates(eq(uuid), any(), any())).willReturn(getSearchCertificatesRestResponseV2_Certificate(12345, "TestProfile"));

        Assertions.assertThrows(ValidationException.class, () -> certificateEjbcaServiceImpl.identifyCertificate(uuid, getCertificateIdentificationRequestDto(98765, "TestProfile")));
    }

    @Test
    public void identifyCertificate_Ok() throws Exception {
        String uuid = "dde2cccc-616f-11ec-90d6-0242ac120003";
        given(authorityInstanceService.getRestApiUrl(eq(uuid))).willReturn("https://ejbca.czertainly.com:8443/ejbca/rest");
        given(ejbcaService.searchCertificates(eq(uuid), any(), any())).willReturn(getSearchCertificatesRestResponseV2_Certificate(123456, "Test"));

        CertificateIdentificationResponseDto dto = certificateEjbcaServiceImpl.identifyCertificate(uuid, getCertificateIdentificationRequestDto(123456, "Test"));
        Assertions.assertEquals(dto.getMeta().size(), 1);
    }

    private CertificateIdentificationRequestDto getCertificateIdentificationRequestDto(int profileId, String profileName) {
        CertificateIdentificationRequestDto dto = new CertificateIdentificationRequestDto();
        List<RequestAttribute> attrs = new java.util.ArrayList<>();
        attrs.add(getEndEntityProfileRequestAttributeV2(profileId, profileName));
        attrs.add(getCertificateProfileRequestAttributeV2(profileId, profileName));
        dto.setRaProfileAttributes(attrs);
        dto.setCertificate("MIIC1TCCAb2gAwIBAgIJANQeIhz8h9A3MA0GCSqGSIb3DQEBBQUAMBoxGDAWBgNVBAMTD3d3dy5leGFtcGxlLmNvbTAeFw0yMjAxMDEwOTUwMDhaFw0zMTEyMzAwOTUwMDhaMBoxGDAWBgNVBAMTD3d3dy5leGFtcGxlLmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALG+wuvOrdMjD5nwhLwmd2+FcO0htFcMi4/Ciu1/9NlHjy55JO+poBih+3JnaJ+u+BY/GCTjbn3RGvC8y2J+1RuAalU0252R0lSWOC2SqUOvUMtOJTufbr/jW0xYk2UePqPj4FX3h3zK3Byw8UaQuUmr9n9acTwyD0oYcxutFm4FqjRZ88eCm7EqNZm+52DmJBHokZPd/z+PLuN6X+Yog5DHS9E1VodHLVVcf3/9KTb3jFhKfNM9y/4pwclRKU1KbjSLStVZmGP3etYYYcFjPswy7zPgWtE8waprQxSJo+Cdqb7+16m69UjaJ1B507xhN8LUjdzZfRJVjSjiP3VKOtMCAwEAAaMeMBwwGgYDVR0RBBMwEYIPd3d3LmV4YW1wbGUuY29tMA0GCSqGSIb3DQEBBQUAA4IBAQAiUmsCNTv/pAxbAB8R9xlarMV/dL42slWJ7bI2e3e03GycVP3eajCfkEKG6XB7aaX4Epn0/jRpEPfplRXkXrxNZ8/bwkwlNN5CiziUcyqVANFC8r/GVlcg+n2+hvu7ZLXmGqBvAJBsbLuvdBKo2iqF4R3BklScDVAHhuXTYwPXd3n7iHEYnuxnGo5yshm6vZ7FKPyIroN9bFc0llJ/n5r4h8WNqaN77M6TycZm4Dlw6EGGM8Bk+IrcRoNE1JLdhIOm3YI5g1zwCprXJ4L+3X6IC20tJUK4PpMGAAdS6ak4/Sq3UM+JxF7oZ2fRCIJrKyfsN3rridYJe0tg5bQnkqmQ");
        return dto;
    }

    private RequestAttributeV2 getEndEntityProfileRequestAttributeV2(int profileId, String profileName) {
        RequestAttributeV2 dto = new RequestAttributeV2();
        dto.setName(AuthorityInstanceControllerImpl.ATTRIBUTE_END_ENTITY_PROFILE);
        NameAndIdDto nameAndIdDto = new NameAndIdDto(profileId, profileName);
        dto.setContent(LocalAttributeUtil.convertFromNameAndIdToBase(List.of(nameAndIdDto)));
        return dto;
    }

    private RequestAttributeV2 getCertificateProfileRequestAttributeV2(int profileId, String profileName) {
        RequestAttributeV2 dto = new RequestAttributeV2();
        dto.setName(AuthorityInstanceControllerImpl.ATTRIBUTE_CERTIFICATE_PROFILE);
        NameAndIdDto nameAndIdDto = new NameAndIdDto(profileId, profileName);
        dto.setContent(LocalAttributeUtil.convertFromNameAndIdToBase(List.of(nameAndIdDto)));
        return dto;
    }

    private SearchCertificatesRestResponseV2 getSearchCertificatesRestResponseV2_NoCertificates() {
        SearchCertificatesRestResponseV2 dto = new SearchCertificatesRestResponseV2();
        dto.setCertificates(List.of());
        return dto;
    }

    private SearchCertificatesRestResponseV2 getSearchCertificatesRestResponseV2_Certificate(int profileId, String username) {
        SearchCertificatesRestResponseV2 dto = new SearchCertificatesRestResponseV2();
        CertificateRestResponseV2 certificateRestResponseV2 = CertificateRestResponseV2.builder()
            .setCertificateProfileId(profileId)
            .setEndEntityProfileId(profileId)
            .setUsername(username)
            .build();
        dto.setCertificates(List.of(certificateRestResponseV2));
        return dto;
    }

}
