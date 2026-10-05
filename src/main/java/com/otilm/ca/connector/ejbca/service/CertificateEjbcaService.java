package com.otilm.ca.connector.ejbca.service;

import com.otilm.api.exception.AlreadyExistException;
import com.otilm.api.exception.NotFoundException;
import com.otilm.api.exception.ValidationException;
import com.otilm.api.model.connector.v2.CertRevocationDto;
import com.otilm.api.model.connector.v2.CertificateDataResponseDto;
import com.otilm.api.model.connector.v2.CertificateIdentificationRequestDto;
import com.otilm.api.model.connector.v2.CertificateIdentificationResponseDto;
import com.otilm.api.model.connector.v2.CertificateRenewRequestDto;
import com.otilm.api.model.connector.v2.CertificateSignRequestDto;
import com.otilm.ca.connector.ejbca.EjbcaException;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;

public interface CertificateEjbcaService {

    CertificateDataResponseDto issueCertificate(String uuid, CertificateSignRequestDto request)
            throws IOException, EjbcaException, NotFoundException, AlreadyExistException;

    CertificateDataResponseDto renewCertificate(String uuid, CertificateRenewRequestDto request)
            throws IOException, EjbcaException, NotFoundException, AlreadyExistException;

    void revokeCertificate(String uuid, CertRevocationDto request) throws NotFoundException, AccessDeniedException;

    CertificateIdentificationResponseDto identifyCertificate(String uuid, CertificateIdentificationRequestDto request)
            throws NotFoundException, ValidationException;
}
