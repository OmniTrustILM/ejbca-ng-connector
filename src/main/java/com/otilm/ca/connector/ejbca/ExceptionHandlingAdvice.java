package com.otilm.ca.connector.ejbca;

import com.otilm.api.exception.AlreadyExistException;
import com.otilm.api.exception.CertificateOperationException;
import com.otilm.api.exception.NotDeletableException;
import com.otilm.api.exception.NotFoundException;
import com.otilm.api.exception.ValidationError;
import com.otilm.api.exception.ValidationException;
import com.otilm.api.model.common.ErrorMessageDto;
import com.otilm.ca.connector.ejbca.exception.CertificateRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ExceptionHandlingAdvice {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandlingAdvice.class);

    /**
     * Handler for {@link NotFoundException}.
     *
     * @param ex Caught {@link NotFoundException}.
     * @return
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorMessageDto handleNotFoundException(NotFoundException ex) {
        LOG.warn("HTTP 404: {}", ex.getMessage());
        return ErrorMessageDto.getInstance(ex.getMessage());
    }

    /**
     * Handler for {@link AlreadyExistException}.
     *
     * @return
     */
    @ExceptionHandler(AlreadyExistException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessageDto handleAlreadyExistException(AlreadyExistException ex) {
        LOG.info("HTTP 400: {}", ex.getMessage());
        return ErrorMessageDto.getInstance(ex.getMessage());
    }

    /**
     * Handler for {@link NotDeletableException}.
     *
     * @return
     */
    @ExceptionHandler(NotDeletableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessageDto handleNotDeletableException(NotDeletableException ex) {
        LOG.info("HTTP 400: {}", ex.getMessage());
        return ErrorMessageDto.getInstance(ex.getMessage());
    }

    /**
     * Handler for {@link ValidationException}.
     *
     * @return
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public List<String> handleValidationException(ValidationException ex) {
        LOG.info("HTTP 422: {}", ex.getMessage());

        return ex.getErrors().stream()
                .map(ValidationError::getErrorDescription)
                .toList();
    }

    /**
     * Handler for {@link HttpMessageNotReadableException}.
     *
     * @return
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessageDto handleMessageNotReadable(HttpMessageNotReadableException ex) {
        LOG.info("HTTP 400: {}", ex.getMessage());
        return ErrorMessageDto.getInstance(ex.getMessage());
    }

//    /**
//     * Handler for {@link AccessDeniedException}.
//     */
//    @ExceptionHandler(AccessDeniedException.class)
//    public void handleAccessDeniedException(AccessDeniedException ex) {
//        LOG.warn("Access denied: {}", ex.getMessage());
//        // re-throw to let the Spring Security handle it
//        throw ex;
//    }

    /**
     * Handler for EJBCA Web Service exceptions.
     *
     * @return
     */
    @ExceptionHandler({
            EjbcaException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessageDto handleMessageNotReadable(EjbcaException ex) {
        LOG.info("HTTP 400: {}", ex.getMessage());
        return ErrorMessageDto.getInstance(ex.getMessage());
    }

    /**
     * Handler for EJBCA Web Service exceptions.
     *
     * @return
     */
    @ExceptionHandler({
            CertificateOperationException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessageDto handleMessageNotReadable(CertificateOperationException ex) {
        LOG.info("HTTP 400: {}", ex.getMessage());
        return ErrorMessageDto.getInstance(ex.getMessage());
    }

    /**
     * Handler for {@link CertificateRequestException}.
     *
     * @return
     */
    @ExceptionHandler({
            CertificateRequestException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessageDto handleMessageNotReadable(CertificateRequestException ex) {
        LOG.info("HTTP 400: {}", ex.getMessage());
        return ErrorMessageDto.getInstance(ex.getMessage());
    }

    /**
     * Handler for {@link RuntimeException}.
     *
     * @return
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessageDto handleRuntimeException(RuntimeException ex) {
        LOG.info("HTTP 400: {}", ex.getMessage());
        return ErrorMessageDto.getInstance(ex.getMessage());
    }

    /**
     * Handler for {@link Exception}.
     *
     * @return
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorMessageDto handleException(Exception ex) {
        LOG.error("General error occurred: {}", ex.getMessage(), ex);
        return ErrorMessageDto.getInstance("Internal server error.");
    }
}
