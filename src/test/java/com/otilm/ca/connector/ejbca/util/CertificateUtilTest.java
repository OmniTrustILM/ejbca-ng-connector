package com.otilm.ca.connector.ejbca.util;

import com.otilm.api.exception.NotFoundException;
import com.otilm.api.model.connector.v2.CertificateDataResponseDto;
import com.otilm.api.model.core.certificate.CertificateType;
import com.otilm.ca.connector.ejbca.ws.Certificate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CertificateUtilTest {

    // A well-known valid cert (single-line base64 from existing test fixture)
    private static final String CERT_BASE64_VALID =
            "MIIC1TCCAb2gAwIBAgIJANQeIhz8h9A3MA0GCSqGSIb3DQEBBQUAMBoxGDAWBgNVBAMTD3d3dy5leGFtcGxlLmNvbTAeFw0yMjAxMDEwOTUwMDhaFw0zMTEyMzAwOTUwMDhaMBoxGDAWBgNVBAMTD3d3dy5leGFtcGxlLmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALG+wuvOrdMjD5nwhLwmd2+FcO0htFcMi4/Ciu1/9NlHjy55JO+poBih+3JnaJ+u+BY/GCTjbn3RGvC8y2J+1RuAalU0252R0lSWOC2SqUOvUMtOJTufbr/jW0xYk2UePqPj4FX3h3zK3Byw8UaQuUmr9n9acTwyD0oYcxutFm4FqjRZ88eCm7EqNZm+52DmJBHokZPd/z+PLuN6X+Yog5DHS9E1VodHLVVcf3/9KTb3jFhKfNM9y/4pwclRKU1KbjSLStVZmGP3etYYYcFjPswy7zPgWtE8waprQxSJo+Cdqb7+16m69UjaJ1B507xhN8LUjdzZfRJVjSjiP3VKOtMCAwEAAaMeMBwwGgYDVR0RBBMwEYIPd3d3LmV4YW1wbGUuY29tMA0GCSqGSIb3DQEBBQUAA4IBAQAiUmsCNTv/pAxbAB8R9xlarMV/dL42slWJ7bI2e3e03GycVP3eajCfkEKG6XB7aaX4Epn0/jRpEPfplRXkXrxNZ8/bwkwlNN5CiziUcyqVANFC8r/GVlcg+n2+hvu7ZLXmGqBvAJBsbLuvdBKo2iqF4R3BklScDVAHhuXTYwPXd3n7iHEYnuxnGo5yshm6vZ7FKPyIroN9bFc0llJ/n5r4h8WNqaN77M6TycZm4Dlw6EGGM8Bk+IrcRoNE1JLdhIOm3YI5g1zwCprXJ4L+3X6IC20tJUK4PpMGAAdS6ak4/Sq3UM+JxF7oZ2fRCIJrKyfsN3rridYJe0tg5bQnkqmQ";

    @Test
    void getX509Certificate_bytes_ok() throws CertificateException {
        byte[] der = Base64.getDecoder().decode(CERT_BASE64_VALID);
        X509Certificate cert = CertificateUtil.getX509Certificate(der);
        assertNotNull(cert);
    }

    @Test
    void getX509Certificate_bytes_invalid_throwsCertificateException() {
        assertThrows(CertificateException.class,
                () -> CertificateUtil.getX509Certificate("not-a-cert".getBytes()));
    }

    @Test
    void getX509Certificate_base64String_ok() throws CertificateException {
        X509Certificate cert = CertificateUtil.getX509Certificate(CERT_BASE64_VALID);
        assertNotNull(cert);
        assertNotNull(cert.getSubjectDN());
    }

    @Test
    void parseCertificate_withoutHeaders_ok() throws CertificateException {
        X509Certificate cert = CertificateUtil.parseCertificate(CERT_BASE64_VALID);
        assertNotNull(cert);
    }

    @Test
    void parseCertificate_withPemHeaders_ok() throws CertificateException {
        String pem = "-----BEGIN CERTIFICATE-----\n" + CERT_BASE64_VALID + "\n-----END CERTIFICATE-----";
        X509Certificate cert = CertificateUtil.parseCertificate(pem);
        assertNotNull(cert);
    }

    @Test
    void getDnFromX509Certificate_ok() throws CertificateException, NotFoundException {
        String dn = CertificateUtil.getDnFromX509Certificate(CERT_BASE64_VALID);
        assertNotNull(dn);
        assertTrue(dn.contains("www.example.com"));
    }

    @Test
    void getIssuerDnFromX509Certificate_ok() throws CertificateException, NotFoundException {
        X509Certificate cert = CertificateUtil.getX509Certificate(CERT_BASE64_VALID);
        String issuerDn = CertificateUtil.getIssuerDnFromX509Certificate(cert);
        assertNotNull(issuerDn);
    }

    @Test
    void getSerialNumberFromX509Certificate_ok() throws CertificateException {
        X509Certificate cert = CertificateUtil.getX509Certificate(CERT_BASE64_VALID);
        String serial = CertificateUtil.getSerialNumberFromX509Certificate(cert);
        assertNotNull(serial);
        assertFalse(serial.isEmpty());
    }

    @Test
    void convertWSCertificateDataToDto_single_ok() {
        Certificate wsCert = new Certificate();
        wsCert.setCertificateData(CERT_BASE64_VALID.getBytes());

        CertificateDataResponseDto dto = CertificateUtil.convertWSCertificateDataToDto(wsCert);

        assertNotNull(dto);
        assertEquals(CertificateType.X509, dto.getCertificateType());
        assertFalse(dto.getCertificateData().contains("-----BEGIN"));
        assertFalse(dto.getCertificateData().contains("\n"));
    }

    @Test
    void convertWSCertificateDataToDto_list_ok() {
        Certificate wsCert = new Certificate();
        wsCert.setCertificateData(CERT_BASE64_VALID.getBytes());

        List<CertificateDataResponseDto> dtos = CertificateUtil.convertWSCertificateDataToDto(List.of(wsCert));

        assertEquals(1, dtos.size());
        assertEquals(CertificateType.X509, dtos.get(0).getCertificateType());
    }

    @Test
    void convertWSCertificateDataToDto_stripsHeaders() {
        String withHeaders = "-----BEGIN CERTIFICATE-----\r\n" + CERT_BASE64_VALID + "\r\n-----END CERTIFICATE-----";
        Certificate wsCert = new Certificate();
        wsCert.setCertificateData(withHeaders.getBytes());

        CertificateDataResponseDto dto = CertificateUtil.convertWSCertificateDataToDto(wsCert);

        assertFalse(dto.getCertificateData().contains("BEGIN"));
        assertFalse(dto.getCertificateData().contains("END"));
        assertFalse(dto.getCertificateData().contains("\n"));
        assertFalse(dto.getCertificateData().contains("\r"));
    }
}
