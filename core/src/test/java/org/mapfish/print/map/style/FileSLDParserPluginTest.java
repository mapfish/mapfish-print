package org.mapfish.print.map.style;

import com.google.common.base.Optional;

import org.geotools.styling.Style;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.ConfigFileResolvingHttpRequestFactory;
import org.mapfish.print.servlet.fileloader.ConfigFileLoaderManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test loading an style from a file.
 */
public class FileSLDParserPluginTest extends AbstractMapfishSpringTest {
    @Autowired
    private SLDParserPlugin parser;
    @Autowired
    private TestHttpClientFactory clientHttpRequestFactory;
    @Autowired
    private ConfigFileLoaderManager fileLoaderManager;

    @Test
    public void testParseStyle_SingleStyleRelativeToConfig() throws Throwable {
        final String fileName = "singleStyle.sld";
        final Optional<Style> styleOptional = loadStyle(fileName, fileName);

        assertTrue (styleOptional.isPresent());
        assertTrue(styleOptional.get() instanceof Style);
        assertEquals(1, styleOptional.get().featureTypeStyles().size());
        assertEquals(2, styleOptional.get().featureTypeStyles().get(0).rules().size());
        assertEquals(1, styleOptional.get().featureTypeStyles().get(0).rules().get(0).symbolizers().size());
        assertEquals(1, styleOptional.get().featureTypeStyles().get(0).rules().get(1).symbolizers().size());
    }

    @Test
    public void testParseStyle_SingleStyleRelativeToConfig_HasStyleIndex() throws Throwable {
        final String fileName = "singleStyle.sld";
        final Optional<Style> styleOptional = loadStyle(fileName, fileName + "##1");
        assertTrue (styleOptional.isPresent());
        assertTrue(styleOptional.get() instanceof Style);
        assertEquals(1, styleOptional.get().featureTypeStyles().size());
        assertEquals(2, styleOptional.get().featureTypeStyles().get(0).rules().size());
        assertEquals(1, styleOptional.get().featureTypeStyles().get(0).rules().get(0).symbolizers().size());
        assertEquals(1, styleOptional.get().featureTypeStyles().get(0).rules().get(1).symbolizers().size());
    }

    @Test
    public void testParseStyle_SingleStyleAbsoluteFile() throws Throwable {
        File file = getFile(FileSLDParserPluginTest.class, "singleStyle.sld");
        final Optional<Style> styleOptional = loadStyle(file.getName(), file.getAbsolutePath());

        assertTrue (styleOptional.isPresent());
        assertTrue(styleOptional.get() instanceof Style);
        assertEquals(1, styleOptional.get().featureTypeStyles().size());
        assertEquals(2, styleOptional.get().featureTypeStyles().get(0).rules().size());
        assertEquals(1, styleOptional.get().featureTypeStyles().get(0).rules().get(0).symbolizers().size());
        assertEquals(1, styleOptional.get().featureTypeStyles().get(0).rules().get(1).symbolizers().size());
    }

    @Test(expected = Exception.class)
    public void testParseStyle_MultipleStyles_NoIndex() throws Throwable {
        final String fileName = "multipleStyles.sld";
        loadStyle(fileName, fileName);
    }

    @Test
    public void testParseStyle_MultipleStyles() throws Throwable {
        final String fileName = "multipleStyles.sld";
        Optional<Style> styleOptional = loadStyle(fileName, fileName + "##1");

        assertTrue (styleOptional.isPresent());
        assertTrue(styleOptional.get() instanceof Style);
        assertEquals(1, styleOptional.get().featureTypeStyles().size());
        assertEquals(2, styleOptional.get().featureTypeStyles().get(0).rules().size());
        assertEquals(1, styleOptional.get().featureTypeStyles().get(0).rules().get(0).symbolizers().size());
        assertEquals(1, styleOptional.get().featureTypeStyles().get(0).rules().get(1).symbolizers().size());

        styleOptional = loadStyle(fileName, fileName + "##2");
        assertTrue (styleOptional.isPresent());
        assertTrue(styleOptional.get() instanceof Style);
        assertEquals(1, styleOptional.get().featureTypeStyles().size());
        assertEquals(1, styleOptional.get().featureTypeStyles().get(0).rules().size());
        assertEquals(2, styleOptional.get().featureTypeStyles().get(0).rules().get(0).symbolizers().size());

    }

    @Test(expected = Exception.class)
    public void testIndexOutOfBounds() throws Throwable {
        final String fileName = "singleStyle.sld";
        loadStyle(fileName, fileName + "##3");
    }

    @Test(expected = Exception.class)
    public void testIndexTooLow() throws Throwable {
        final String fileName = "singleStyle.sld";
        loadStyle(fileName, fileName + "##-1");
    }

    @Test
    public void testFileNotInConfigDir() throws Throwable {
        final File tempFile = File.createTempFile("config", ".yaml");
        File file = getFile(FileSLDParserPluginTest.class, "singleStyle.sld");
        Configuration config = new Configuration();
        config.setConfigurationFile(tempFile);
        config.setFileLoaderManager(this.fileLoaderManager);

        ConfigFileResolvingHttpRequestFactory requestFactory = new ConfigFileResolvingHttpRequestFactory(
                this.clientHttpRequestFactory, config, "test");

        assertFalse(this.parser.parseStyle(config, requestFactory, file.getAbsolutePath()).isPresent());
    }

    private Optional<Style> loadStyle(String fileName, String styleString) throws Throwable {
        File file = getFile(FileSLDParserPluginTest.class, fileName);
        Configuration config = new Configuration();
        config.setConfigurationFile(file);
        config.setFileLoaderManager(this.fileLoaderManager);

        ConfigFileResolvingHttpRequestFactory requestFactory = new ConfigFileResolvingHttpRequestFactory(
                this.clientHttpRequestFactory, config, "test");

        return this.parser.parseStyle(config, requestFactory, styleString);
    }
}
