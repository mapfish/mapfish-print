package org.mapfish.print.map.style.json;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.Optional;
import org.geotools.styling.Style;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.ConfigFileResolvingHttpRequestFactory;
import org.mapfish.print.servlet.fileloader.ConfigFileLoaderManager;
import org.mapfish.print.servlet.fileloader.ServletConfigFileLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.annotation.DirtiesContext;

public class MapfishJsonFileResolverTest extends AbstractMapfishSpringTest {
  final TestHttpClientFactory httpClient = new TestHttpClientFactory();

  @Autowired private MapfishStyleParserPlugin parser;

  @Autowired private ConfigFileLoaderManager fileLoaderManager;
  @Autowired private ServletConfigFileLoader configFileLoader;

  @Test
  public void testLoadFromFile() throws Throwable {
    final String rootFile =
        getFile("/test-http-request-factory-application-context.xml")
            .getParentFile()
            .getAbsolutePath();
    configFileLoader.setServletContext(new MockServletContext(rootFile));

    final String configFile =
        "/org/mapfish/print/map/style/json/requestData-style-json-v1-style.json";
    final String styleString = "v2-style-symbolizers-default-values.json";
    final Optional<Style> styleOptional = loadStyle(configFile, styleString);
    assertTrue(styleOptional.isPresent());
    assertNotNull(styleOptional.get());
  }

  @Test
  public void testLoadFromServlet() throws Throwable {
    final File rootFile =
        getFile("/test-http-request-factory-application-context.xml").getParentFile();
    configFileLoader.setServletContext(
        new MockServletContext(
            new ResourceLoader() {
              @Override
              public Resource getResource(String location) {
                final File file = new File(rootFile, location);
                if (file.exists()) {
                  return new FileSystemResource(file);
                }
                throw new IllegalArgumentException(file + " not found");
              }

              @Override
              public ClassLoader getClassLoader() {
                return MapfishJsonFileResolverTest.class.getClassLoader();
              }
            }));

    final String configFile =
        "/org/mapfish/print/map/style/json/requestData-style-json-v1-style.json";
    final String styleString =
        "servlet:///org/mapfish/print/map/style/json/v2-style-symbolizers-default-values.json";
    final Optional<Style> styleOptional = loadStyle(configFile, styleString);

    assertTrue(styleOptional.isPresent());
    assertNotNull(styleOptional.get());
  }

  @Test
  @DirtiesContext
  public void testLoadFromURL() throws Throwable {
    final String rootFile =
        getFile("/test-http-request-factory-application-context.xml")
            .getParentFile()
            .getAbsolutePath();
    configFileLoader.setServletContext(new MockServletContext(rootFile));

    final String host = "URLSLDParserPluginTest.com";
    httpClient.registerHandler(
        input -> (("" + input.getHost()).contains(host)) || input.getAuthority().contains(host),
        createFileHandler(URI::getPath));

    Configuration configuration = new Configuration();
    configuration.setFileLoaderManager(this.fileLoaderManager);
    final String path =
        "/org/mapfish/print/map/style/json/v2-style-symbolizers-default-values.json";
    configuration.setConfigurationFile(getFile(path));

    final Optional<Style> styleOptional =
        parser.parseStyle(
            configuration, this.httpClient, "http://URLSLDParserPluginTest.com" + path);

    assertTrue(styleOptional.isPresent());
    assertNotNull(styleOptional.get());
  }

  @Test
  public void testLoadFromClasspath() throws Throwable {

    final String rootFile =
        getFile("/test-http-request-factory-application-context.xml")
            .getParentFile()
            .getAbsolutePath();
    configFileLoader.setServletContext(new MockServletContext(rootFile));

    final String configFile =
        "/org/mapfish/print/map/style/json/v2-style-symbolizers-default-values.json";
    final String styleString = "classpath://" + configFile;
    final Optional<Style> styleOptional = loadStyle(configFile, styleString);

    assertTrue(styleOptional.isPresent());
    assertNotNull(styleOptional.get());
  }

  private Optional<Style> loadStyle(String configFile, String styleString) throws Throwable {
    Configuration configuration = new Configuration();
    configuration.setFileLoaderManager(this.fileLoaderManager);
    configuration.setConfigurationFile(getFile(configFile));

    ConfigFileResolvingHttpRequestFactory requestFactory =
        new ConfigFileResolvingHttpRequestFactory(this.httpClient, configuration, "test");

    return parser.parseStyle(configuration, requestFactory, styleString);
  }
}
