package org.mapfish.print.map.style;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.io.Files;
import org.geotools.styling.Style;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.attribute.map.BBoxMapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.annotation.DirtiesContext;

import java.awt.Dimension;
import java.net.URI;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Jesse on 4/8/2014.
 */
public class URLSLDParserPluginTest extends AbstractMapfishSpringTest {

    @Autowired
    private URLSLDParserPlugin parserPlugin;
    @Autowired
    private TestHttpClientFactory clientHttpRequestFactory;
    private MapfishMapContext mapContext = new MapfishMapContext(new BBoxMapBounds(null, 0,0,10,10), new Dimension(20,20), 0, 72, null);

    @Test
    @DirtiesContext
    public void testParseStyle() throws Throwable {
        final String host = "URLSLDParserPluginTest.com";
        clientHttpRequestFactory.registerHandler(new Predicate<URI>() {
                                           @Override
                                           public boolean apply(URI input) {
                                               return (("" + input.getHost()).contains(host)) || input.getAuthority().contains(host);
                                           }
                                       }, new TestHttpClientFactory.Handler() {
                                           @Override
                                           public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
                                               try {
                                                   byte[] bytes = Files.toByteArray(getFile(uri.getPath()));
                                                   return ok(uri, bytes, httpMethod);
                                               } catch (AssertionError e) {
                                                   return error404(uri, httpMethod);
                                               }
                                           }
                                       }
        );

        Configuration configuration = new Configuration();
        configuration.setConfigurationFile(getFile("/org/mapfish/print/processor/map/center_wmts_fixedscale/thinline.sld"));
        final Optional<Style> styleOptional = parserPlugin.parseStyle(configuration,
                clientHttpRequestFactory, "http://" + host + "/org/mapfish/print/processor/map/center_wmts_fixedscale/thinline.sld", mapContext);

        assertTrue(styleOptional.isPresent());

        final Optional<Style> styleOptional2 = parserPlugin.parseStyle(configuration,
                clientHttpRequestFactory, "file://thinline.sld", mapContext);

        assertTrue(styleOptional2.isPresent());

        final Optional<Style> styleOptional3 = parserPlugin.parseStyle(configuration,
                clientHttpRequestFactory, "file://config.yaml", mapContext);

        assertFalse(styleOptional3.isPresent());

    }
}
