package com.otilm.ca.connector.ejbca.util;

import com.otilm.api.exception.ValidationException;
import com.otilm.api.model.core.authority.EndEntityDto;
import com.otilm.ca.connector.ejbca.ws.ExtendedInformationWS;
import com.otilm.ca.connector.ejbca.ws.MatchType;
import com.otilm.ca.connector.ejbca.ws.MatchWith;
import com.otilm.ca.connector.ejbca.ws.UserDataVOWS;
import com.otilm.ca.connector.ejbca.ws.UserMatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    public void prepareUsernameMatch_setsCorrectFields() {
        UserMatch match = EjbcaUtils.prepareUsernameMatch("alice");

        assertEquals(MatchWith.MATCH_WITH_USERNAME.getCode(), match.getMatchwith());
        assertEquals(MatchType.MATCH_TYPE_EQUALS.getCode(), match.getMatchtype());
        assertEquals("alice", match.getMatchvalue());
    }

    @Test
    public void prepareEndEntityProfileMatch_setsCorrectFields() {
        UserMatch match = EjbcaUtils.prepareEndEntityProfileMatch("EMPTY");

        assertEquals(MatchWith.MATCH_WITH_ENDENTITYPROFILE.getCode(), match.getMatchwith());
        assertEquals(MatchType.MATCH_TYPE_EQUALS.getCode(), match.getMatchtype());
        assertEquals("EMPTY", match.getMatchvalue());
    }

    @Test
    public void mapToUserDetailDTO_withoutExtendedInfo_setsBasicFields() {
        userData.setEmail("alice@example.com");
        userData.setSubjectAltName("dNSName=alice.example.com");
        // status 10 == NEW
        userData.setStatus(10);

        EndEntityDto dto = EjbcaUtils.mapToUserDetailDTO(userData);

        assertEquals(username, dto.getUsername());
        assertEquals(subjectDn, dto.getSubjectDN());
        assertEquals("alice@example.com", dto.getEmail());
        assertEquals("dNSName=alice.example.com", dto.getSubjectAltName());
        assertNotNull(dto.getStatus());
        // extensionData is set (to an empty list) whenever getExtendedInformation() is non-null.
        // UserDataVOWS lazily initialises extendedInformation to an empty list, so the result
        // is an empty list, not null.
        assertTrue(dto.getExtensionData() == null || dto.getExtensionData().isEmpty());
    }

    @Test
    public void mapToUserDetailDTO_withExtendedInfo_mapsExtensionData() {
        ExtendedInformationWS eiWs = new ExtendedInformationWS();
        eiWs.setName("custom_key");
        eiWs.setValue("custom_value");
        // getExtendedInformation() lazily initialises the list
        userData.getExtendedInformation().add(eiWs);
        userData.setStatus(40); // GENERATED

        EndEntityDto dto = EjbcaUtils.mapToUserDetailDTO(userData);

        assertNotNull(dto.getExtensionData());
        assertEquals(1, dto.getExtensionData().size());
        assertEquals("custom_key", dto.getExtensionData().get(0).getName());
        assertEquals("custom_value", dto.getExtensionData().get(0).getValue());
    }

    @Test
    public void setUserExtensions_blank_doesNothing() {
        // blank/empty string should be a no-op (isNotBlank guard)
        Assertions.assertDoesNotThrow(() -> EjbcaUtils.setUserExtensions(userData, ""));
        Assertions.assertDoesNotThrow(() -> EjbcaUtils.setUserExtensions(userData, "   "));
        Assertions.assertDoesNotThrow(() -> EjbcaUtils.setUserExtensions(userData, null));
    }

}
