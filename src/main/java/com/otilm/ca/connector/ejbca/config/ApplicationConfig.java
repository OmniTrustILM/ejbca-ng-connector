package com.otilm.ca.connector.ejbca.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import jakarta.xml.ws.BindingProvider;
import java.net.URL;

@Configuration
@EnableJpaAuditing
@PropertySource(value = ApplicationConfig.EXTERNAL_PROPERTY_SOURCE, ignoreResourceNotFound = true)
public class ApplicationConfig {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

    public static final String EXTERNAL_PROPERTY_SOURCE =
            "file:${raprofiles-ejbca-connector.config.dir:/etc/raprofiles-ejbca-connector}/raprofiles-ejbca-connector.properties";
    /**
     * Set this property on the {@link BindingProvider#getRequestContext()} to
     * enable {HttpURLConnection#setConnectTimeout(int)}
     *
     * <p>
     * int timeout = ...; Map<String, Object> ctxt =
     * ((BindingProvider)proxy).getRequestContext(); ctxt.put(CONNECT_TIMEOUT,
     * timeout);
     * </p>
     */
    public static final String CONNECT_TIMEOUT = "com.sun.xml.ws.connect.timeout";

    /**
     * Set this property on the {@link BindingProvider#getRequestContext()} to
     * enable {HttpURLConnection#setReadTimeout(int)}
     *
     * <p>
     * int timeout = ...; Map<String, Object> ctxt =
     * ((BindingProvider)proxy).getRequestContext(); ctxt.put(REQUEST_TIMEOUT,
     * timeout);
     * </p>
     */
    public static final String REQUEST_TIMEOUT = "com.sun.xml.ws.request.timeout";
    public static final String REQUEST_SSL_SOCKET_FACTORY = "com.sun.xml.ws.transport.https.client.SSLSocketFactory";

    public static final URL WSDL_URL = ApplicationConfig.class.getClassLoader().getResource("ejbcaws.wsdl");
}
