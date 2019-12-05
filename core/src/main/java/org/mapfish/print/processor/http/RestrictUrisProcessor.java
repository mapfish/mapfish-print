package org.mapfish.print.processor.http;

import org.mapfish.print.http.AbstractMfClientHttpRequestFactoryWrapper;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;

import java.io.IOException;
import java.net.URI;

/**
 * <p>This processor check urls against a set of url matchers to see if the request should be allowed or
 * rejected.</p>
 * <p>
 * Usage of processor is as follows:
 * </p>
 * <pre><code>
 * - !restrictUris
 *   matchers:
 *     - !localMatch {}
 *     - !ipMatch
 *       ip: www.camptocamp.org
 *     - !dnsMatch
 *       host: mapfish-geoportal.demo-camptocamp.com
 *       port: 80
 *     - !dnsMatch
 *       host: labs.metacarta.com
 *       port: 80
 *     - !dnsMatch
 *       host: terraservice.net
 *       port: 80
 *     - !dnsMatch
 *       host: tile.openstreetmap.org
 *       port: 80
 *     - !dnsMatch
 *       host: www.geocat.ch
 *       port: 80
 * </code></pre>
 *
 * <p>
 * By default a matcher allows the URL, but it can be setup to reject the URL (by setting reject to true). The
 * first matcher that matches will be the one picking the final outcome. If no matcher matches, the URI is
 * rejected. So, for example, you can allow every URLs apart from the internal URLs like that:
 * </p>
 * <pre><code>
 * - !restrictUris
 *   matchers:
 *     - !ipMatch
 *       ip : 192.178.0.0
 *       mask : 255.255.0.0
 *       reject: true
 *     - !acceptAll
 * </code></pre>
 *
 * <p>
 * If the Print service is in your DMZ and needs to allow access to any WMS server, it is strongly recommended
 * to have a configuration like the previous one in order to avoid having the Print service being used as a
 * proxy to access your internal servers.
 * </p>
 *
 * <p>
 * <strong>Note:</strong> if this class is part of a CompositeClientHttpRequestFactoryProcessor
 * (!configureHttpRequests) then it should be the last one so that the checks are done after all changes to
 * the URIs
 * </p>
 * [[examples=http_processors]]
 *
 * @see org.mapfish.print.processor.http.matcher.AcceptAllMatcher
 * @see org.mapfish.print.processor.http.matcher.AddressHostMatcher
 * @see org.mapfish.print.processor.http.matcher.DnsHostMatcher
 * @see org.mapfish.print.processor.http.matcher.LocalHostMatcher
 */
public final class RestrictUrisProcessor extends AbstractClientHttpRequestFactoryProcessor {
    @Override
    public MfClientHttpRequestFactory createFactoryWrapper(
            final ClientHttpFactoryProcessorParam clientHttpFactoryProcessorParam,
            final MfClientHttpRequestFactory requestFactory) {
        return new AbstractMfClientHttpRequestFactoryWrapper(requestFactory, matchers, true) {
            @Override
            protected ClientHttpRequest createRequest(
                    final URI uri,
                    final HttpMethod httpMethod,
                    final MfClientHttpRequestFactory requestFactory) throws IOException {
                // Everything is already done by the caller
                return requestFactory.createRequest(uri, httpMethod);
            }
        };
    }
}
