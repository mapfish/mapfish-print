package org.mapfish.print.map.style;

import org.geotools.styling.Style;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.ConfigFileResolvingHttpRequestFactory;
import org.mapfish.print.servlet.fileloader.ConfigFileLoaderManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class URLSLDParserPluginTest extends AbstractMapfishSpringTest {

    @Autowired
    private SLDParserPlugin parserPlugin;
    @Autowired
    private TestHttpClientFactory clientHttpRequestFactory;
    @Autowired
    private ConfigFileLoaderManager fileLoaderManager;

    @Test
    @DirtiesContext
    public void testParseStyle() throws Throwable {
        final String host = "URLSLDParserPluginTest.com";
        clientHttpRequestFactory.registerHandler(
                input -> (("" + input.getHost()).contains(host)) || input.getAuthority().contains(host),
                createFileHandler(URI::getPath)
        );

        Configuration configuration = new Configuration();
        configuration.setFileLoaderManager(this.fileLoaderManager);
        configuration.setConfigurationFile(
                getFile("/org/mapfish/print/processor/map/center_wmts_fixedscale/thinline.sld"));

        ConfigFileResolvingHttpRequestFactory requestFactory =
                new ConfigFileResolvingHttpRequestFactory(this.clientHttpRequestFactory,
                                                          configuration, "test");
        final Optional<Style> styleOptional = parserPlugin.parseStyle(
                configuration, requestFactory,
                "http://" + host + "/org/mapfish/print/processor/map/center_wmts_fixedscale/thinline.sld");

        assertTrue(styleOptional.isPresent());

        final Optional<Style> styleOptional2 = parserPlugin.parseStyle(
                configuration, requestFactory, "file://thinline.sld");

        assertTrue(styleOptional2.isPresent());

        final Optional<Style> styleOptional3 = parserPlugin.parseStyle(
                configuration, requestFactory, "file://config.yaml");

        assertFalse(styleOptional3.isPresent());

    }
}
