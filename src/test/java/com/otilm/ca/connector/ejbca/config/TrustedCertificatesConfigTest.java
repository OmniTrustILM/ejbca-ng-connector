package com.otilm.ca.connector.ejbca.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TrustedCertificatesConfigTest {

    private static final String certString = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGCDCCA/CgAwIBAgIUNqs50/tomsiRjWxMbSWvq+FXRjYwDQYJKoZIhvcNAQEN\n" +
            "BQAwNTEVMBMGA1UEAwwMRGVtbyBSb290IENBMRwwGgYDVQQKDBMzS2V5IENvbXBh\n" +
            "bnkgcy5yLm8uMB4XDTE5MTAyNTA4NTExM1oXDTM0MTAyMTA4NTExM1owOzEbMBkG\n" +
            "A1UEAwwSRGVtbyBDbGllbnQgU3ViIENBMRwwGgYDVQQKDBMzS2V5IENvbXBhbnkg\n" +
            "cy5yLm8uMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA1hWze2gCXG1S\n" +
            "gD/Bhi32EvHyyLQJMVVrxHXHDG1zysoL3pyrmwu5uCJ5y/9LpwMOIz3remokUg7I\n" +
            "tqHe22sMxSkZPP34Hk+IZdSqpyxoh/6miZT7kUNkyow+AjISQSSCp4eUWTHVM/uC\n" +
            "Ai/YCMHYPIW55V6CTRBQkjJF2bS5aaDS+d/xCzRh5S5OmC7/tz3P+pTKOjhfG7yE\n" +
            "bg3Zd4q9vW3HJTGFgVPVkObdx9V9FHneDgCSTOFgtAI/Gl9EpxRROmK3yfKS0shu\n" +
            "6OKvqUqXu1u5bWiXgIz9pXUKzLzpiBjzGIWFHoeyj2GTUpkJZfR/8Q9q6oEsRY+0\n" +
            "p5G5E3b4vw10OZYY/9dRiAlAQq7IuVIlmlP1aDajUdkLfVujDEGOLTMzEQd07N7J\n" +
            "Vf6xi2ckBr4DPwtbVjgZRP7ynRs2sDaMN4xIVn47DT9BwzDPsHQjOFbAdv5jnZdK\n" +
            "hWD7z+FwvFd+O8fZFZ3Dz35nmMVHYEblg75rRZLJ46NrGk3ELoReT4KHs/2KKtys\n" +
            "8+Ut24xYmcDCu3E3b2MetEaeiKEPpKlBRY9SilfKyjGN9mFyNpfmvEewjwRtuJfy\n" +
            "GheQfDGMm/S5+vWidIEzfCKKe7alXZFb9VlZe66y4rp/HoMawiOAwojQcNVYi3D6\n" +
            "hjRqHlEpwGX2b2hZCz2X+INnk8lFaI0CAwEAAaOCAQgwggEEMA8GA1UdEwEB/wQF\n" +
            "MAMBAf8wHwYDVR0jBBgwFoAUzXowKX36GdFLETw6VdX96cS/zJ4wSwYIKwYBBQUH\n" +
            "AQEEPzA9MDsGCCsGAQUFBzAChi9odHRwOi8vcGtpLjNrZXkuY29tcGFueS9jYXMv\n" +
            "ZGVtby9kZW1vcm9vdGNhLmNydDARBgNVHSAECjAIMAYGBFUdIAAwQQYDVR0fBDow\n" +
            "ODA2oDSgMoYwaHR0cDovL3BraS4za2V5LmNvbXBhbnkvY3Jscy9kZW1vL2RlbW9y\n" +
            "b290Y2EuY3JsMB0GA1UdDgQWBBRb1CkuOKhih42ufn80UCgpIuNFGDAOBgNVHQ8B\n" +
            "Af8EBAMCAYYwDQYJKoZIhvcNAQENBQADggIBAKxj9Tj4n/ukXiuxRJ55Awj44Na4\n" +
            "lCosaugGk5WaFjFWJ/VnmCB3rRR/Pj+OXBBpT++0sSSuRVb9H8z/QnC2RUIB2HcM\n" +
            "mNNjW8TQY69vG2VIBeR7naHJcjXtRuot7OCWed72jJvs5+mrndlXo8jOS26RH/hN\n" +
            "fdxFQiDp/IAGdKmX6vrlDsmcD4nVtVg16Qn4JFZU9/2I6RrppX0pWpJ+4s1HmaHQ\n" +
            "V06aoRBhCUcKvUauRXakQo9R4EXqWp/cXAUprpUQSdE1QGvBvPmoNjn6c/spi09n\n" +
            "fKmsJ0Rgle0sVfMmyO/BXL5mPVA/CpCqBHJJFdOojykKv/PNFMhqAua+1PjH1saZ\n" +
            "saBC+HmCuIAXJnBfreXSA0Ki9LT6NjDAZzEh/R2JzbPvEX88RUL0Q4g7U2PilBjx\n" +
            "2erwopF4LjfM+lwuoQHXi0O+EE3crDUguHJ5okr5XIRc7vkqwvE0L6iWh5uVRuL+\n" +
            "MFg9xvglFuJcy1bGhJPJjvjFSatVETZ2t8aprByBjYU5io3WUTawchCCY0vBLcLM\n" +
            "gEiMEymgH9AUtu9PCGx+KPZ8RzH2WB/T0s2s1+ZExd39jQGfezIOYk0keWr5FeaT\n" +
            "fDt6aM1f0OK8pfGDlzk7obGpqQRzlc8xPG4DLawUKeWMj9Cb+oCn2VamI7dA0SHm\n" +
            "bmafaPj1x+cNQ5AM\n" +
            "-----END CERTIFICATE-----\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFvTCCA6WgAwIBAgIUUnPaN2j/mEDjbEa4760q+kAW120wDQYJKoZIhvcNAQEN\n" +
            "BQAwNTEVMBMGA1UEAwwMRGVtbyBSb290IENBMRwwGgYDVQQKDBMzS2V5IENvbXBh\n" +
            "bnkgcy5yLm8uMB4XDTE5MDUxMjA4MjMzMloXDTQ5MDUwNDA4MjMzMlowNTEVMBMG\n" +
            "A1UEAwwMRGVtbyBSb290IENBMRwwGgYDVQQKDBMzS2V5IENvbXBhbnkgcy5yLm8u\n" +
            "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEArie92/QkN4mXqTN9wt8M\n" +
            "2lMfP10c6YzmvJK93fOlc9087RecZwMLNzkFXvqRG5O1dl2ONTnEJm26e+Gplt7b\n" +
            "mPfNgwICRH4P2xPJY0+/GrFuPS/DupiOI3lNoIZGWYv7I/ml7NidvDAXwUnHRzR2\n" +
            "WF2XebDt91Y5IM81GNZvTsdTG/sBNCnHB6cCDCAZd1KMAKRkcda43kfcQOf1fknl\n" +
            "YPgeY0JE0Qh8OY09l5CG13W7L8DNS59hbz0mZS0lbd7C8vR3BMch0YIDFmHU4EWr\n" +
            "iTk2q9nG/dCaqtp0+xxD1Cq2H8h33zolOTzVNrSasaLVRz7RSw1sYpbbmbXtSIpK\n" +
            "zccpPeca0KjqC06XXyvf0H/xPvRZfGlFk+EM9cur12z4951SrTEZZAH2/XA/9s5e\n" +
            "zFWk/ZtKRo6Tav3CQEjkHxx0ciFkJggJ6BvpfkHwyWm+cIfYRdw032aeFhcY9Dah\n" +
            "vhj1IUMnLeo9gg+5wLOw7hfYmLei2IiJRv4/5lDa68X4qUwYtfxjw4UwbNNNkDFG\n" +
            "YUe+Ag+Hhf+HhAtoRZRyV6ThLlXrjfnMXXRgFxLp0c9tyhSdNUw/JYjiKZa+6PS+\n" +
            "7gn0yMdxpISHSy2D6bAXSpI3wiVNZlYyuvcCqL9Sikj777x4NFwQFbgL818wMr7Z\n" +
            "BvygPSzTqFp6Q/v7C6kh4KkCAwEAAaOBxDCBwTAPBgNVHRMBAf8EBTADAQH/MB8G\n" +
            "A1UdIwQYMBaAFM16MCl9+hnRSxE8OlXV/enEv8yeMEsGCCsGAQUFBwEBBD8wPTA7\n" +
            "BggrBgEFBQcwAoYvaHR0cDovL3BraS4za2V5LmNvbXBhbnkvY2FzL2RlbW8vZGVt\n" +
            "b3Jvb3RjYS5jcnQwEQYDVR0gBAowCDAGBgRVHSAAMB0GA1UdDgQWBBTNejApffoZ\n" +
            "0UsRPDpV1f3pxL/MnjAOBgNVHQ8BAf8EBAMCAYYwDQYJKoZIhvcNAQENBQADggIB\n" +
            "AJU/KIi0aw19GDuZauQFwGAjY3GpwWuLujyDUuZKYlhHpOI7YE3heGf9s+DHqn3I\n" +
            "MEl6TAYXtH18kt/jZ05GQciPcn5tIHXXeWEAbc9ZmhZAmEuKs2cB/MUj0UuO8l0O\n" +
            "26YzNo9UeYBDkZg63G0tDLXo7IUIiARzHrXy1cXQjQQA5OcigFTuCzxsOdiF5BgZ\n" +
            "M6EN9z+Wt6L7mpRQ/rRySWVbAj1Ao0qmIfBYAAZW8eiGGQrE4NXtCbAIiBjr0Z5Y\n" +
            "rU7hlVYaS+Z5Rz+WoUbCh3WKXjvFiKSOhJxsfUKRloRjpRJ5sVnErizURWTwEhA+\n" +
            "I6uaddnlhgjYcgAj0Ppip7vzYOWvyANnamojoVXWI2Jo2tDk9SyY+yMWibTyG6nF\n" +
            "SwZu2BPrVRqLGlcNAogs9Mfu1QDsqCBjk4Eq/Hrw3CZLqHPCUdgh6PjhnPREOabj\n" +
            "IPHNeyx4e0fR0ipdQPRNqdWKNG3m3reyKac3xiK7vaqHX/15V0ZKbKJD7BV/7S9R\n" +
            "XWsNXechEq2d5lXtV14fMWyFPMYtXuLO7MkW0LHsPcAFcJ2nu/4kym6vRIOPV7JB\n" +
            "bmVrs7CGni8Gnp9FEXFhdJAAVKbgyt5y6xnKwLhVEtcb9S8FuoSA3YB7LaLaJDLn\n" +
            "zHkkiIL5Cldz+syVVqpDqB0sSgajzl60P/XJMf2tSpab\n" +
            "-----END CERTIFICATE-----\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIGEDCCA/igAwIBAgIUH4U6YvWqAUzuOTXSXCsH5TF9Ny8wDQYJKoZIhvcNAQEN\n" +
            "BQAwNTEVMBMGA1UEAwwMRGVtbyBSb290IENBMRwwGgYDVQQKDBMzS2V5IENvbXBh\n" +
            "bnkgcy5yLm8uMB4XDTIwMDcyODEyNDQxOFoXDTM1MDcyNTEyNDQxOFowQzEjMCEG\n" +
            "A1UEAwwaRGVtbyBPQ1NQIFJlc3BvbmRlciBTdWIgQ0ExHDAaBgNVBAoMEzNLZXkg\n" +
            "Q29tcGFueSBzLnIuby4wggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQC4\n" +
            "R1XWFTSAXWP/I4H2jtiKW0MmV2gDF2envWtzy5DbmV2ZFBixRSh64v+5plHZPI53\n" +
            "RbeVGKxdY1eMKdO62pS0SBvar87oXl/ohhBDyfjtVY+NuNYE6dHVt582pXMhqFqX\n" +
            "vPuu+BZXf3TzP+puQSTL5Syst/l9bDLcoDi7DWQqj1bb/uVw1e7dHwHlU02fcW2G\n" +
            "Dkwu6D8tjy/MLR77E7K5IJRtKCviX/MmtYK/AYw2TLpOxks8VV2gDC3rYoRgaEgN\n" +
            "EfQd4QsB1BUXNV/ccnXFuiFCFhBGMNzoZMk8SJXWw2s/PpAkR87bUFfWWl7CCjaQ\n" +
            "jm8YfGPqnsVo7z7JgdbaifYyC1R9hwhTdwFPKr5hihGpEEDXAk/nzz6irdow5cs/\n" +
            "sPfQ8quHQhjziI6JpxYuAtEONTQU/Mm8oOS5yfIKS9hiWurr3eO+V5+j8fSpkaIr\n" +
            "sdL9xN/yAJP6s3KlCQxurhk2Gq7fhUXzJPSuEUyKBO/pKiNoxmOc3KILMte+Dbmz\n" +
            "Le9rwcYD3XhojZVCZabeNU04eOO/B3Ihdi7lXr9yAUGzdyqlgBTK6ZMXZHTVYluu\n" +
            "WGq4lHe/51mJPXAbl5X65QIPzezrMHBGgRSt2ZSJs6yBC/1XPpZb89OXgGWqo0cZ\n" +
            "G0SbhoNIZLopqMotL1FexnFBek0lkNPh2b1Q1FwWpwIDAQABo4IBCDCCAQQwDwYD\n" +
            "VR0TAQH/BAUwAwEB/zAfBgNVHSMEGDAWgBTNejApffoZ0UsRPDpV1f3pxL/MnjBL\n" +
            "BggrBgEFBQcBAQQ/MD0wOwYIKwYBBQUHMAKGL2h0dHA6Ly9wa2kuM2tleS5jb21w\n" +
            "YW55L2Nhcy9kZW1vL2RlbW9yb290Y2EuY3J0MBEGA1UdIAQKMAgwBgYEVR0gADBB\n" +
            "BgNVHR8EOjA4MDagNKAyhjBodHRwOi8vcGtpLjNrZXkuY29tcGFueS9jcmxzL2Rl\n" +
            "bW8vZGVtb3Jvb3RjYS5jcmwwHQYDVR0OBBYEFHAHwIu+Mzuh6xIX3v/WtQuyC2G3\n" +
            "MA4GA1UdDwEB/wQEAwIBhjANBgkqhkiG9w0BAQ0FAAOCAgEAJ/54dn1wuIK0ZaqL\n" +
            "nLlOLluZ2CwvJutI4bGmKmOIrgCCwyDxjb0nmSiCIii1I+JOKWL553PdxTRwpvBa\n" +
            "9EGJ1iMuItaeHioYIXvsIfx+NIOY3d8ab6nMLKxdpIBgiOZvM8v1xaiEb4hV3pyX\n" +
            "TsCb8LMOzjjcXl/dfo6szlmFg3ThMcFZ/dAoFR/41B911A5KymMxDs42/7zlm2fT\n" +
            "CTIaOFqbJQw0xY+NAH6LJTnADvYKMI1zIX1GdOkMfn3oHEnT6wgN0FtUqzaSmg2n\n" +
            "aKiiW4r0ZBPbgYjpXOQ6vjilb4/Htu32UpKXpq9hUgT/7m2M9m2oxU/+Rh/VlVL+\n" +
            "xLyOCstlpGsyKyVwiiAHsWhcoN3pr7spdYRUILucpMw/tB1oOnlTCuQqeihv9+Xb\n" +
            "vC2j+eIrqXyVjp3hUp1cEgsKoK6ebJ58TYC4Aa3sFxliJALNWbseeXMnEDsQ4k+f\n" +
            "J2TIn3gmqRpp6/denB7rgIF1W78qzUvTmP/AeelWc1dO3fGg+8ygP8ziFZ71vet5\n" +
            "GXyAYQXuVu0+O/KBHJQG7APi76oXjDDx6EuHaswZBQSq9DZCtmfI3l2zcTolC9Lg\n" +
            "vo6PuZ/FAc2Ss0V6Dqz5D7ay7DOl9KBv/ThSI+MEujRuAnNG2T+EXbx+w6NNpfYz\n" +
            "uvksXnGBwnCWocfj9eiVblp7OKA=\n" +
            "-----END CERTIFICATE-----\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIGCDCCA/CgAwIBAgIUP8UHtBOrxQykX1mbYdeqcsS6Ph4wDQYJKoZIhvcNAQEN\n" +
            "BQAwNTEVMBMGA1UEAwwMRGVtbyBSb290IENBMRwwGgYDVQQKDBMzS2V5IENvbXBh\n" +
            "bnkgcy5yLm8uMB4XDTE5MDUxMjA4NDIwMloXDTM0MDUwODA4NDIwMlowOzEbMBkG\n" +
            "A1UEAwwSRGVtbyBTZXJ2ZXIgU3ViIENBMRwwGgYDVQQKDBMzS2V5IENvbXBhbnkg\n" +
            "cy5yLm8uMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA6rioH4qF4CDb\n" +
            "ieNIAzHne9Jmm/lgkJR5DlU1n5lm/yf6kn973n0ZJqeQZVXl3+GRf0cuh2UfkoEF\n" +
            "sdAiWyjXf0jB0p1GdR8ft2Hs7jq9b0HBf/16KNjdwT/L/o8nnBS6aGoHsp/byCfp\n" +
            "jwy9ebalruv/l6591eJ9wDJ1c53tFMDP5Wng8nWrRvrMkPhosViOYu+e52xgw5Wg\n" +
            "EYxIuqi4jSefAAivPCes6Z2ylbljvS8Mb8c0cmA6I+dLPEQ++mepxZGIQkiMsaT6\n" +
            "a9G0i2fKPJBJ7tZTG2UudLtU1QRxopx3oeKKd3ShsCy8r4K0Gy3l07RdWutFZvCi\n" +
            "TYZoYSsq6T8EWgQndkaJWdNeJMnbW1FoaPY6n6zJMr5uyRLSsXjLlCGZsbfkL6ZB\n" +
            "9ScaFysdKA/htxDWkRZgTWoTKKa6K4l411NYoFkP/s0B/ZqMLJXNBbQ/VJhROXGe\n" +
            "7k7EuuCIuQksJIwYS7OR86MqcUnWK8SzZl0JIIYu9L+snemKaAvJoEK/Nw1/5NrU\n" +
            "9Ek2BVf6+y8nU21cEuEpe1u4CHD6BmcAkAcyaCd9XfZtDRsO/bSUOq26Tetxi402\n" +
            "IMj+RdspSBTDOszM3jTRI5FSB+JJwTmiso2AP8AWsIE7DmCvUyvSyEcn6+T+de/Q\n" +
            "Tizgzo57bRycu7vBMb+TiIikWvIrbWkCAwEAAaOCAQgwggEEMA8GA1UdEwEB/wQF\n" +
            "MAMBAf8wHwYDVR0jBBgwFoAUzXowKX36GdFLETw6VdX96cS/zJ4wSwYIKwYBBQUH\n" +
            "AQEEPzA9MDsGCCsGAQUFBzAChi9odHRwOi8vcGtpLjNrZXkuY29tcGFueS9jYXMv\n" +
            "ZGVtby9kZW1vcm9vdGNhLmNydDARBgNVHSAECjAIMAYGBFUdIAAwQQYDVR0fBDow\n" +
            "ODA2oDSgMoYwaHR0cDovL3BraS4za2V5LmNvbXBhbnkvY3Jscy9kZW1vL2RlbW9y\n" +
            "b290Y2EuY3JsMB0GA1UdDgQWBBQb/FXk1AOCzd40P27IA82xTCrutTAOBgNVHQ8B\n" +
            "Af8EBAMCAYYwDQYJKoZIhvcNAQENBQADggIBAChnt005/XfZwbSambonrcaSGcuP\n" +
            "8gWrOZVJ9IYSTbvj8HRJhhvHqLb707y5rdNcKxAozF344CBpaVtK5HGgQ0Jyiiia\n" +
            "XhRtfkYBj55OV7vcy3rESsOxOUXHC1YF7AViq4gxh27q8wWU3bAmix2FIOpf0lXb\n" +
            "UNi3rmclm4czbC15IKpQdYVoyb0dYP34HiiyQB7fA9hMIxDAw1kvmN4CAQrztdm9\n" +
            "dQ7zcOJmUnjOMtkFo9G+vwjXODp+35Bg66FlCurcvmn2S7dUANOFzO3BrjRHuRpu\n" +
            "GDFBpsbRSPQ6d2SYb/wQgDkkfxbYbA5hIs29mG2skOuGLmZSW+H56Vx6Qv8I92pm\n" +
            "VY7SANXpMDJjs3cZCRDCdP/b+Vwp82hdRl4iiCEiDxkahZ4CtlguKMXsyP+rygi9\n" +
            "9tj9wtkLdSMz/hmZlLHoXq8xIe/iCQzvtYfV7Dh5GU9aBN+/r+7KlLTvXYz0tlh0\n" +
            "C2FYigoh7uMrf7CYdSzjCDe2eITdK6awrrBBZ00zNvjGuY3us8dXcX6ixI2EwKu5\n" +
            "fraDqbPk006YculFKHgiuV4NJwkIuzVuXBbNsVWCm+3tuiqsyvYpUZ44c93ikq1s\n" +
            "ciT6ldMXEapkv2ZI5Sbm1a1Vd1LFQ0A7I7hwOvMw6J9j2Sjg5/MELrtyOov6tjZ0\n" +
            "x9vUlRQ6MmxpCnL3\n" +
            "-----END CERTIFICATE-----\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFcDCCA1igAwIBAgIUQ8qEcLshApVCEh1+wrszGBW9mHEwDQYJKoZIhvcNAQEL\n" +
            "BQAwNjEWMBQGA1UEAwwNTWFuYWdlbWVudCBDQTEcMBoGA1UECgwTM0tleSBDb21w\n" +
            "YW55IHMuci5vLjAeFw0xOTA1MDgxMjUxNTZaFw0yOTA1MDUxMjUxNTZaMDYxFjAU\n" +
            "BgNVBAMMDU1hbmFnZW1lbnQgQ0ExHDAaBgNVBAoMEzNLZXkgQ29tcGFueSBzLnIu\n" +
            "by4wggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQDZ3+k1GPIGkimNd9ME\n" +
            "w9cWCYYzUh11mv5NVJsw+uRbzxQsP7oqeQQdMxHqhSP0eGg6UxqQ6PQSCCkHvMtr\n" +
            "pFGX+eMZuB/z1vFodHdBCUTNOUr6c0c+wMGuhJoYPAwY/OW5YDrw6r1oxtKbbD7H\n" +
            "MOPE1tt5twGQTuy4BBQEWp/SSHs39UoDtLqbLwzuekv1GcQxCjWpHjX5fIbIf7JZ\n" +
            "fXrQ8WnMkHInbow9tQIAOtKyDJ32Y2DUSgK4v1zp0UtnABXBaVyih3am/Sghv6RL\n" +
            "jP0xEnWpRtAt4HqtSFFMVCEkgqIEU7Ev9eTuGJvy76lvvrPpr5dow35FWz8tZcPw\n" +
            "PwXKMqaHRPPIjS3HDKi/BOmqOGpnANcJyn4X+/NsOTNkIXLAj9H5t4deQR4jckSw\n" +
            "NcPeh/5zXgqmPU5GLhhis+zu0TxfMKuEovXHOWlfiyU4Xzx6MAsjb9dPRqjR2ku7\n" +
            "TNc9megR7yDV809eOa6M5roVZB2L71/rKKkzLS/u9kImShhtqlxQ9XckN0Op2Gpp\n" +
            "PYIO8rzmZHzCv+gb/NIeVZ+UbTIbANA3NYXWjJlMzgyf/Dub0vZbhRxLpKRcziOZ\n" +
            "N8STB8p+d6kChct1u10SZwQglMUM8WPo8t+CHPw/AvApdwD2z45CktFiR6V83gqv\n" +
            "qGdDkaCg/CdpnzltHN/Zr4NDpwIDAQABo3YwdDAPBgNVHRMBAf8EBTADAQH/MB8G\n" +
            "A1UdIwQYMBaAFNSuk5EY8MsHAKr7hYgWlF7vvWIzMBEGA1UdIAQKMAgwBgYEVR0g\n" +
            "ADAdBgNVHQ4EFgQU1K6TkRjwywcAqvuFiBaUXu+9YjMwDgYDVR0PAQH/BAQDAgGG\n" +
            "MA0GCSqGSIb3DQEBCwUAA4ICAQDZZN0oD9kIZ3puvGAcxwGKZ1DNCNtnNut5k4E+\n" +
            "hBJc5TnMskfyM2drMi0vFvFXXJlu5NBprS0jw+x6XHYTC6BXwe4LwZib/Yr/6ROv\n" +
            "73nC/ecJK2w2n6PIWehs7qwigakzh1tf8iypj8kTl2taMMEgzO7bF9CgLQnm0eVj\n" +
            "uPzvRRSsZ0dbBisKwkOTpt9aYG/WsuLZ7LmFKBvpVSvJJBYmgGmeqkx0Pijdg9PE\n" +
            "kUP3Ek2tN5DoOFQzlPicujZ5p3akfTG4L2PNuZWf52zmY5sXmxEr0zoyeuXrIM42\n" +
            "4c5qvh82yvP2M2AXty8s2O1jW11snemSLPVhxsnUlfMqic8rfO0QTPJg3WU/SmHp\n" +
            "vmcyzUGpwYfm1wdOS2e9Ow5fSxR6TyHc14Lox4yXCLzGkPdBPcj8jG1qJ9Pqkwpa\n" +
            "z9hGC3elTd3TDCocDRlrz6OQfE4j1OKCeaiuHCjZO3v3e0VxWh7T9Synez/thxi/\n" +
            "UPKBL8Gh103AUCOQYGJkIdIKSyQusnfxCj93YE5cJUC9rPfrqe1Rjct3nGNj5E5w\n" +
            "nVwU4HtvosPw6mGtPuUNA3fhvntZN+P+1hi9+322s1s0ttdBYjIYNKVRqiuGB8NH\n" +
            "4xX8R6husmmRbOI1OFdtaruc30rSo1Y3/iYWdWaA1zokkYOztcjbffoznZlIBawm\n" +
            "if4oKw==\n" +
            "-----END CERTIFICATE-----\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIDmzCCAoOgAwIBAgIUTZTcPaZUhaK++sPtcZnv5Ae6bgIwDQYJKoZIhvcNAQEL\n" +
            "BQAwXTELMAkGA1UEBhMCQ1oxEDAOBgNVBAgMB0N6ZWNoaWExCzAJBgNVBAcMAkNC\n" +
            "MQ0wCwYDVQQKDAQzS0VZMQwwCgYDVQQLDANERVYxEjAQBgNVBAMMCWxvY2FsaG9z\n" +
            "dDAeFw0yMDA5MjUxMTUyMDVaFw0zMDA5MjMxMTUyMDVaMF0xCzAJBgNVBAYTAkNa\n" +
            "MRAwDgYDVQQIDAdDemVjaGlhMQswCQYDVQQHDAJDQjENMAsGA1UECgwEM0tFWTEM\n" +
            "MAoGA1UECwwDREVWMRIwEAYDVQQDDAlsb2NhbGhvc3QwggEiMA0GCSqGSIb3DQEB\n" +
            "AQUAA4IBDwAwggEKAoIBAQC0bbMHkeqyu8VbgUJ3NsEY8775YFO6mZQf6tq+R2jf\n" +
            "EZlYBEiXynHXuI89WAJzIBZm4wtZNKCmPYQD+McRgybBf/Fb1DtWD+uBuoae2Vej\n" +
            "d6fHwtRb4dOlulIw0W6FpjHgNTBPX4sNQOM0x0+Ci9U9CVsYnaqp4Gd2+7dI4Dw5\n" +
            "ZoZ7bYVhU8NVh7ZNBKST+V5rVrVQFcERfsSBfMoGZqDRypZot5PHZi9szIZMfOlP\n" +
            "ru3JC6Jmn22fJ+yywSow10v8/S5cvq1rlqZlRL8FE8hYW3Q4UH266Vnp0JW6aRwc\n" +
            "NryS1HYj492tosw3tRI3hNVhy9h2K4yfXa3jOqoEyPllAgMBAAGjUzBRMB0GA1Ud\n" +
            "DgQWBBTKncrv3CcEYfrs3s50dJSnqwxtRzAfBgNVHSMEGDAWgBTKncrv3CcEYfrs\n" +
            "3s50dJSnqwxtRzAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQBe\n" +
            "2uOg1EkI7NAzz4kEHHfSlK9aDfkzkYdsez9lD5HP5GYILC66pkthts4UqThu4hBy\n" +
            "6Lr/ElVq1neQuX2OFlx/2h6uVV1GTkLWYWqIbYO0TAAtoMA6XArwluRgzN799tDq\n" +
            "ty+FhDWC9OE7O7R3PApnT9+apKc0CQ3zpG4pGS4XEmVyoZp9Q0JXkCxQG7sA0e1K\n" +
            "t0rHDqEPgGzk2MPUfIEyCzZW4mBqPlTxM+rLb1+bpOtOmjSdGnNdlOSbg6ZzinF+\n" +
            "ZONtKUAokwmTWeljMykyEEFIO+9Gcudkzy9Ck1IIEd+qPWsdeh+MqVpWNZZ0xuv6\n" +
            "sfMC1iy43V54jA1W6ZQN\n" +
            "-----END CERTIFICATE-----";

    @BeforeEach
    public void setUp() {
        System.setProperty("trusted.certificates", certString);
    }

    @Test
    public void testConfigureGlobalTrustStore_ok() throws Exception {
        TrustedCertificatesConfig config =  new TrustedCertificatesConfig();
        Assertions.assertDoesNotThrow(config::configureGlobalTrustStore);
    }
}
