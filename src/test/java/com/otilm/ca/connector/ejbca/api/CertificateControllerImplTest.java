package com.otilm.ca.connector.ejbca.api;

import com.otilm.api.exception.NotFoundException;
import com.otilm.api.model.connector.v2.CertificateIdentificationResponseDto;
import com.otilm.ca.connector.ejbca.service.CertificateEjbcaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(CertificateControllerImpl.class)
@ExtendWith(SpringExtension.class)
class CertificateControllerImplTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CertificateEjbcaService certificateEjbcaService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void identifyCertificate() throws Exception {
        String uuid = "dde2cccc-616f-11ec-90d6-0242ac120003";
        String serialized = mapper.writeValueAsString(getCertificateIdentificationResponseDto());
        given(certificateEjbcaService.identifyCertificate(eq(uuid), any())).willReturn(
                getCertificateIdentificationResponseDto()
        );
        mvc.perform(
            MockMvcRequestBuilders.post("/v2/authorityProvider/authorities/"+uuid+"/certificates/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialized)
            ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonPath("$.meta", Matchers.hasSize(0)));
    }

    @Test
    void issueCertificate_serviceThrowsNotFoundException_returns404() throws Exception {
        String uuid = "dde2cccc-616f-11ec-90d6-0242ac120003";
        willThrow(new NotFoundException("authority", uuid))
                .given(certificateEjbcaService).issueCertificate(eq(uuid), any());

        mvc.perform(
            MockMvcRequestBuilders.post("/v2/authorityProvider/authorities/" + uuid + "/certificates/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void renewCertificate_serviceThrowsNotFoundException_returns404() throws Exception {
        String uuid = "dde2cccc-616f-11ec-90d6-0242ac120003";
        willThrow(new NotFoundException("authority", uuid))
                .given(certificateEjbcaService).renewCertificate(eq(uuid), any());

        mvc.perform(
            MockMvcRequestBuilders.post("/v2/authorityProvider/authorities/" + uuid + "/certificates/renew")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    private CertificateIdentificationResponseDto getCertificateIdentificationResponseDto() {
        CertificateIdentificationResponseDto dto = new CertificateIdentificationResponseDto();
        dto.setMeta(List.of());
        return dto;
    }

}
