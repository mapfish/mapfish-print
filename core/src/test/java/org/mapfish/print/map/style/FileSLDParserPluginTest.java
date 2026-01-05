package org.mapfish.print.map.style;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import org.geotools.api.style.Style;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.util.Assert;
import org.locationtech.jts.util.AssertionFailedException;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.ConfigFileResolvingHttpRequestFactory;
import org.mapfish.print.servlet.fileloader.ConfigFileLoaderManager;
import org.springframework.beans.factory.annotation.Autowired;

/** Test loading a style from a file. */
public class FileSLDParserPluginTest extends AbstractMapfishSpringTest {
  @Autowired private SLDParserPlugin parser;
  @Autowired private TestHttpClientFactory clientHttpRequestFactory;
  @Autowired private ConfigFileLoaderManager fileLoaderManager;

  @Test
  public void testParseStyle_SingleStyleRelativeToConfig() {
    final String fileName = "singleStyle.sld";
    final Optional<Style> styleOptional = loadStyle(fileName, fileName);

    assertTrue(styleOptional.isPresent());
    assertEquals(1, styleOptional.get().featureTypeStyles().size());
    assertEquals(2, styleOptional.get().featureTypeStyles().getFirst().rules().size());
    assertEquals(
        1, styleOptional.get().featureTypeStyles().getFirst().rules().get(0).symbolizers().size());
    assertEquals(
        1, styleOptional.get().featureTypeStyles().getFirst().rules().get(1).symbolizers().size());
  }

  @Test
  public void testParseStyle_SingleStyleRelativeToConfig_HasStyleIndex() {
    final String fileName = "singleStyle.sld";
    final Optional<Style> styleOptional = loadStyle(fileName, fileName + "##1");
    assertTrue(styleOptional.isPresent());
    assertEquals(1, styleOptional.get().featureTypeStyles().size());
    assertEquals(2, styleOptional.get().featureTypeStyles().getFirst().rules().size());
    assertEquals(
        1, styleOptional.get().featureTypeStyles().getFirst().rules().get(0).symbolizers().size());
    assertEquals(
        1, styleOptional.get().featureTypeStyles().getFirst().rules().get(1).symbolizers().size());
  }

  @Test
  public void testParseStyle_SingleStyleAbsoluteFile() {
    File file = getFile(FileSLDParserPluginTest.class, "singleStyle.sld");
    final Optional<Style> styleOptional = loadStyle(file.getName(), file.getAbsolutePath());

    assertTrue(styleOptional.isPresent());
    assertEquals(1, styleOptional.get().featureTypeStyles().size());
    assertEquals(2, styleOptional.get().featureTypeStyles().getFirst().rules().size());
    assertEquals(
        1, styleOptional.get().featureTypeStyles().getFirst().rules().get(0).symbolizers().size());
    assertEquals(
        1, styleOptional.get().featureTypeStyles().getFirst().rules().get(1).symbolizers().size());
  }

  @Test
  public void testParseStyle_MultipleStyles_NoIndex() {
    final String fileName = "multipleStyles.sld";
    try {
      loadStyle(fileName, fileName);
      Assert.shouldNeverReachHere();
    } catch (AssertionFailedException e) {
      assertEquals(
          "There are 2 therefore the styleRef must contain an index identifying the style.  The"
              + " index starts at 1 for the first style.\n"
              + "\tExample: thinline.sld##1",
          e.getMessage());
    }
  }

  @Test
  public void testParseStyle_MultipleStyles() {
    final String fileName = "multipleStyles.sld";
    Optional<Style> styleOptional = loadStyle(fileName, fileName + "##1");

    assertTrue(styleOptional.isPresent());
    assertEquals(1, styleOptional.get().featureTypeStyles().size());
    assertEquals(2, styleOptional.get().featureTypeStyles().getFirst().rules().size());
    assertEquals(
        1, styleOptional.get().featureTypeStyles().getFirst().rules().get(0).symbolizers().size());
    assertEquals(
        1, styleOptional.get().featureTypeStyles().getFirst().rules().get(1).symbolizers().size());

    styleOptional = loadStyle(fileName, fileName + "##2");
    assertTrue(styleOptional.isPresent());
    assertEquals(1, styleOptional.get().featureTypeStyles().size());
    assertEquals(1, styleOptional.get().featureTypeStyles().getFirst().rules().size());
    assertEquals(
        2,
        styleOptional.get().featureTypeStyles().getFirst().rules().getFirst().symbolizers().size());
  }

  @Test
  public void testIndexOutOfBounds() {
    final String fileName = "singleStyle.sld";
    try {
      loadStyle(fileName, fileName + "##3");
      Assert.shouldNeverReachHere();
    } catch (AssertionFailedException e) {
      assertEquals("There where 1 styles in file but requested index was: 3", e.getMessage());
    }
  }

  @Test
  public void testIndexTooLow() {
    final String fileName = "singleStyle.sld";
    try {
      loadStyle(fileName, fileName + "##-1");
      Assert.shouldNeverReachHere();
    } catch (AssertionFailedException e) {
      assertEquals("styleIndex must be > -1 but was: -2", e.getMessage());
    }
  }

  @Test
  public void testFileNotInConfigDir() throws IOException {
    final File tempFile = File.createTempFile("config", ".yaml");
    File file = getFile(FileSLDParserPluginTest.class, "singleStyle.sld");
    Configuration config = new Configuration();
    config.setConfigurationFile(tempFile);
    config.setFileLoaderManager(this.fileLoaderManager);

    ConfigFileResolvingHttpRequestFactory requestFactory =
        new ConfigFileResolvingHttpRequestFactory(
            this.clientHttpRequestFactory,
            config,
            new HashMap<>(),
            HTTP_REQUEST_MAX_NUMBER_FETCH_RETRY,
            HTTP_REQUEST_FETCH_RETRY_INTERVAL_MILLIS);

    assertFalse(this.parser.parseStyle(config, requestFactory, file.getAbsolutePath()).isPresent());
  }

  private Optional<Style> loadStyle(String fileName, String styleString) {
    File file = getFile(FileSLDParserPluginTest.class, fileName);
    Configuration config = new Configuration();
    config.setConfigurationFile(file);
    config.setFileLoaderManager(this.fileLoaderManager);

    ConfigFileResolvingHttpRequestFactory requestFactory =
        new ConfigFileResolvingHttpRequestFactory(
            this.clientHttpRequestFactory,
            config,
            new HashMap<>(),
            HTTP_REQUEST_MAX_NUMBER_FETCH_RETRY,
            HTTP_REQUEST_FETCH_RETRY_INTERVAL_MILLIS);

    return this.parser.parseStyle(config, requestFactory, styleString);
  }
}
