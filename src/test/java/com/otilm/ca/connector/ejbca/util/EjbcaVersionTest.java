package com.otilm.ca.connector.ejbca.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EjbcaVersionTest {

    String ejbcaVersionTech = "EJBCA 8 Enterprise (5045c834d0c99315db96d88d32cc449ff334b07f)";
    String ejbcaVersionTechMajor = "EJBCA 7.11 Enterprise (5045c834d0c99315db96d88d32cc449ff334b07f)";
    String ejbcaVersionTechMajorMinor = "EJBCA 8.0.47 Enterprise (5045c834d0c99315db96d88d32cc449ff334b07f)";
    String ejbcaVersionTechMajorMinorPatch = "EJBCA 7.12.1.52 Enterprise (5045c834d0c99315db96d88d32cc449ff334b07f)";

    @Test
    void testVersionTech_ok() {
        Assertions.assertDoesNotThrow(() -> new EjbcaVersion(ejbcaVersionTech));

        EjbcaVersion version = new EjbcaVersion(ejbcaVersionTech);

        Assertions.assertEquals(8, version.getTechVersion());
        Assertions.assertEquals(0, version.getMajorVersion());
        Assertions.assertEquals(0, version.getMinorVersion());
        Assertions.assertEquals("Enterprise", version.getVersion());

        System.out.println(version);
    }

    @Test
    void testVersionTechMajor_ok() {
        Assertions.assertDoesNotThrow(() -> new EjbcaVersion(ejbcaVersionTechMajor));

        EjbcaVersion version = new EjbcaVersion(ejbcaVersionTechMajor);

        Assertions.assertEquals(7, version.getTechVersion());
        Assertions.assertEquals(11, version.getMajorVersion());
        Assertions.assertEquals(0, version.getMinorVersion());
        Assertions.assertEquals("Enterprise", version.getVersion());

        System.out.println(version);
    }

    @Test
    void testVersionTechMajorMinor_ok() {
        Assertions.assertDoesNotThrow(() -> new EjbcaVersion(ejbcaVersionTechMajorMinor));

        EjbcaVersion version = new EjbcaVersion(ejbcaVersionTechMajorMinor);

        Assertions.assertEquals(8, version.getTechVersion());
        Assertions.assertEquals(0, version.getMajorVersion());
        Assertions.assertEquals(47, version.getMinorVersion());
        Assertions.assertEquals("Enterprise", version.getVersion());

        System.out.println(version);
    }

    @Test
    void testVersionTechMajorMinorPatch_ok() {
        Assertions.assertDoesNotThrow(() -> new EjbcaVersion(ejbcaVersionTechMajorMinorPatch));

        EjbcaVersion version = new EjbcaVersion(ejbcaVersionTechMajorMinorPatch);

        Assertions.assertEquals(7, version.getTechVersion());
        Assertions.assertEquals(12, version.getMajorVersion());
        Assertions.assertEquals(1, version.getMinorVersion());
        Assertions.assertEquals("Enterprise", version.getVersion());

        System.out.println(version);
    }

}
