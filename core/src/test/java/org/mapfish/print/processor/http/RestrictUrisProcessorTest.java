package org.mapfish.print.processor.http;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.processor.http.matcher.AcceptAllMatcher;
import org.mapfish.print.processor.http.matcher.AddressHostMatcher;
import org.mapfish.print.processor.http.matcher.LocalHostMatcher;
import org.mapfish.print.processor.http.matcher.URIMatcher;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.mock.http.client.MockClientHttpRequest;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RestrictUrisProcessorTest {
    static final TestHttpClientFactory requestFactory = new TestHttpClientFactory();

    @BeforeClass
    public static void setUp() {
        requestFactory.registerHandler(input -> true, new TestHttpClientFactory.Handler() {
            @Override
            public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) {
                assertEquals("localhost", uri.getHost());
                return null;
            }
        });
    }

    @Test
    public void testCreateFactoryWrapperLegalRequest() throws Exception {
        final RestrictUrisProcessor restrictUrisProcessor = createLocalhostOnly();
        ClientHttpFactoryProcessorParam params = new ClientHttpFactoryProcessorParam();
        final MfClientHttpRequestFactory factoryWrapper =
                restrictUrisProcessor.createFactoryWrapper(params, requestFactory);
        factoryWrapper.createRequest(new URI("http://localhost:8080/geoserver/wms"), HttpMethod.GET);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateFactoryWrapperIllegalRequest() throws Exception {
        final RestrictUrisProcessor restrictUrisProcessor = createLocalhostOnly();
        ClientHttpFactoryProcessorParam params = new ClientHttpFactoryProcessorParam();
        final ClientHttpRequestFactory factoryWrapper =
                restrictUrisProcessor.createFactoryWrapper(params, requestFactory);
        factoryWrapper.createRequest(new URI("http://www.google.com/q"), HttpMethod.GET);
    }

    @Test
    public void testRejectWithLegalRequest() throws Exception {
        final RestrictUrisProcessor restrictUrisProcessor = createDenyInternal();
        ClientHttpFactoryProcessorParam params = new ClientHttpFactoryProcessorParam();
        final ClientHttpRequestFactory factoryWrapper =
                restrictUrisProcessor.createFactoryWrapper(params, requestFactory);
        factoryWrapper.createRequest(new URI("http://localhost/q"), HttpMethod.GET);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectWithIllegalRequest() throws Exception {
        final RestrictUrisProcessor restrictUrisProcessor = createDenyInternal();
        ClientHttpFactoryProcessorParam params = new ClientHttpFactoryProcessorParam();
        final ClientHttpRequestFactory factoryWrapper =
                restrictUrisProcessor.createFactoryWrapper(params, requestFactory);
        factoryWrapper.createRequest(new URI("http://192.168.12.23/q"), HttpMethod.GET);
    }

    private RestrictUrisProcessor createLocalhostOnly() {
        final RestrictUrisProcessor processor = new RestrictUrisProcessor();
        processor.setMatchers(Collections.singletonList(new LocalHostMatcher()));
        return processor;
    }

    private RestrictUrisProcessor createDenyInternal() {
        final RestrictUrisProcessor processor = new RestrictUrisProcessor();
        List<URIMatcher> matchers = new ArrayList<>();
        AddressHostMatcher addressHostMatcher = new AddressHostMatcher();
        addressHostMatcher.setIp("192.168.12.0");
        addressHostMatcher.setMask("255.255.255.0");
        addressHostMatcher.setReject(true);
        matchers.add(addressHostMatcher);
        matchers.add(new AcceptAllMatcher());
        processor.setMatchers(matchers);
        return processor;
    }
}
