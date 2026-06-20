package com.otilm.ca.connector.ejbca.service.impl;

import com.otilm.ca.connector.ejbca.config.ApplicationConfig;
import io.netty.channel.ChannelOption;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorityInstanceServiceImplTest {

    // Values distinct from the production defaults (5000/30000) so the tests fail if the code
    // ever reverts to hardcoding instead of reading the injected fields.
    private AuthorityInstanceServiceImpl serviceWithTimeouts() {
        AuthorityInstanceServiceImpl service = new AuthorityInstanceServiceImpl();
        ReflectionTestUtils.setField(service, "connectionTimeout", 1234);
        ReflectionTestUtils.setField(service, "requestTimeout", 5678);
        return service;
    }

    @Test
    void applyTimeouts_usesConfiguredConnectAndRequestTimeouts() {
        Map<String, Object> requestContext = new HashMap<>();
        serviceWithTimeouts().applyTimeouts(requestContext);

        assertEquals(1234, requestContext.get(ApplicationConfig.CONNECT_TIMEOUT));
        assertEquals(5678, requestContext.get(ApplicationConfig.REQUEST_TIMEOUT));
    }

    @Test
    void withTimeouts_appliesConfiguredConnectAndResponseTimeoutsToHttpClient() {
        HttpClient client = serviceWithTimeouts().withTimeouts(HttpClient.create());

        assertEquals(Duration.ofMillis(5678), client.configuration().responseTimeout());
        assertEquals(1234, client.configuration().options().get(ChannelOption.CONNECT_TIMEOUT_MILLIS));
    }
}
