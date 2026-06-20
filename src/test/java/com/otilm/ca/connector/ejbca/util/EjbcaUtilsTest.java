package com.otilm.ca.connector.ejbca.util;

import com.otilm.api.exception.ValidationException;
import com.otilm.ca.connector.ejbca.ws.UserDataVOWS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class EjbcaUtilsTest {

    private static final String username = "test";
    private static final String password = "test";
    private static final String subjectDn = "CN=test";

    private UserDataVOWS userData;

    @BeforeEach
    public void setUp() {
        userData = new UserDataVOWS();
        userData.setUsername(username);
        userData.setPassword(password);
        userData.setSubjectDN(subjectDn);
    }

    @Test
    public void setUserExtensions_WrongData() {
        String extensions = "wrong_extensions";
        ValidationException ex = Assertions.assertThrows(
                ValidationException.class,
                () -> EjbcaUtils.setUserExtensions(userData, extensions)
        );
        Assertions.assertEquals("Invalid extension format: " + extensions, ex.getMessage());
    }

    @Test
    public void setUserExtensions_Ok() {
        String extensions = "1.1.1.1.1=sample extension";
        Assertions.assertDoesNotThrow(() -> EjbcaUtils.setUserExtensions(userData, extensions));
    }

    @Test
    public void setUserExtensions_NotOk() {
        String extensions = "my extension=sample extension";
        ValidationException ex = Assertions.assertThrows(
                ValidationException.class,
                () -> EjbcaUtils.setUserExtensions(userData, extensions)
        );
        Assertions.assertEquals("OID should be a series of integers separated by dots", ex.getMessage());
    }

    @Test
    public void setUserExtensions_Ok_Multiple() {
        String extensions = "1.1.1.1.1=sample extension,2.2.2.2=something, 3.3.3=third one";
        Assertions.assertDoesNotThrow(() -> EjbcaUtils.setUserExtensions(userData, extensions));
    }

    @Test
    public void setUserExtensions_NotOk_Multiple() {
        String extensions = "1.1.1.1.1=sample extension,2.2.2.2=something,=third one";
        ValidationException ex = Assertions.assertThrows(
                ValidationException.class,
                () -> EjbcaUtils.setUserExtensions(userData, extensions)
        );
        Assertions.assertEquals("OID cannot be empty", ex.getMessage());
    }

}
