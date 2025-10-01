package org.mapfish.print.map.geotools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.Constants;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.http.ConfigFileResolvingHttpRequestFactory;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.annotation.DirtiesContext;

public class FeaturesParserTest extends AbstractMapfishSpringTest {

  private static final String EXAMPLE_GEOJSONFILE =
      "geojson/geojson-inconsistent-attributes-2.json";
  private static final String WKT =
      "GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\""
          + ",SPHEROID[\"WGS_1984\",6378137,298.257223563]]"
          + ",PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]]";
  @Autowired private TestHttpClientFactory requestFactory;
  @Autowired private ConfigurationFactory configurationFactory;

  @Test
  public void testParseCRSBackwardCompat() throws Exception {
    JSONObject crsJSON =
        new JSONObject(
            """
            {"crs":{
              "type":"EPSG",
              "properties" : {
                 "code":"4326"
              }
            }
            }\
            """);

    CoordinateReferenceSystem crs =
        FeaturesParser.parseCoordinateReferenceSystem(this.requestFactory, crsJSON, false);
    assertNotNull(crs);
    assertEquals("EPSG:4326", CRS.lookupIdentifier(crs, false));
  }

  @Test
  public void testParseCRSNameCode() throws Exception {
    JSONObject crsJSON =
        new JSONObject(
            """
            {"crs":{
              "type":"name",
              "properties" : {
                 "name":"EPSG:4326"
              }
            }}\
            """);

    CoordinateReferenceSystem crs =
        FeaturesParser.parseCoordinateReferenceSystem(this.requestFactory, crsJSON, false);
    assertNotNull(crs);
    assertEquals("EPSG:4326", CRS.lookupIdentifier(crs, false));
  }

  @Test
  public void testParseCRSNameURI() throws Exception {
    JSONObject crsJSON =
        new JSONObject(
            """
            {"crs":{
              "type":"name",
              "properties" : {
                 "name":"urn:ogc:def:crs:EPSG::4326"
              }
            }}\
            """);

    CoordinateReferenceSystem crs =
        FeaturesParser.parseCoordinateReferenceSystem(this.requestFactory, crsJSON, false);
    assertNotNull(crs);
    assertEquals("EPSG:4326", CRS.lookupIdentifier(crs, false));
  }

  @Test
  @DirtiesContext
  public void testParseCRSLinkOgcWkt() throws Exception {
    requestFactory.registerHandler(
        input -> input != null && input.getHost().equals("spatialreference.org"),
        new TestHttpClientFactory.Handler() {
          @Override
          public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) {
            String wkt =
                "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,"
                    + "AUTHORITY[\"EPSG\","
                    + "\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,"
                    + "AUTHORITY[\"EPSG\",\"8901\"]],"
                    + "UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],"
                    + "AUTHORITY[\"EPSG\",\"4326\"]]";
            MockClientHttpRequest mockClientHttpRequest = new MockClientHttpRequest();
            mockClientHttpRequest.setResponse(
                new MockClientHttpResponse(wkt.getBytes(Constants.DEFAULT_CHARSET), HttpStatus.OK));
            return mockClientHttpRequest;
          }
        });
    JSONObject crsJSON =
        new JSONObject(
            """
            {"crs":{
              "type":"link",
              "properties" : {
                 "href":"http://spatialreference\
            .org/ref/epsg/4326/ogcwkt/",
                 "type":"ogcwkt"
              }
            }}\
            """);

    CoordinateReferenceSystem crs =
        FeaturesParser.parseCoordinateReferenceSystem(this.requestFactory, crsJSON, false);
    assertNotNull(crs);
    assertEquals("EPSG:4326", CRS.lookupIdentifier(crs, false));
  }

  @Test
  @DirtiesContext
  public void testParseCRSLinkEsriWkt() {
    final MockClientHttpResponse clientHttpResponse =
        new MockClientHttpResponse(WKT.getBytes(Constants.DEFAULT_CHARSET), HttpStatus.OK);

    CoordinateReferenceSystem crs = parseCoordinateReferenceSystemFromResponse(clientHttpResponse);
    assertNotSame(DefaultEngineeringCRS.GENERIC_2D, crs);
  }

  @Test
  @DirtiesContext
  public void testParseCRSLinkEsriWktWithUnsupportedErrorCode() {
    final MockClientHttpResponse clientHttpResponse =
        new MockClientHttpResponse(WKT.getBytes(Constants.DEFAULT_CHARSET), 999);

    CoordinateReferenceSystem crs = parseCoordinateReferenceSystemFromResponse(clientHttpResponse);
    assertSame(DefaultEngineeringCRS.GENERIC_2D, crs);
  }

  private CoordinateReferenceSystem parseCoordinateReferenceSystemFromResponse(
      final MockClientHttpResponse clientHttpResponse) {
    requestFactory.registerHandler(
        input -> input != null && input.getHost().equals("spatialreference.org"),
        new TestHttpClientFactory.Handler() {
          @Override
          public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) {
            MockClientHttpRequest mockClientHttpRequest = new MockClientHttpRequest();
            mockClientHttpRequest.setResponse(clientHttpResponse);
            return mockClientHttpRequest;
          }
        });
    JSONObject crsJSON =
        new JSONObject(
            """
            {"crs":{
              "type":"link",
              "properties" : {
                 "href":"http://spatialreference\
            .org/ref/epsg/4326/esriwkt/",
                 "type":"esriwkt"
              }
            }}\
            """);

    CoordinateReferenceSystem crs =
        FeaturesParser.parseCoordinateReferenceSystem(this.requestFactory, crsJSON, false);
    assertNotNull(crs);
    return crs;
  }

  @Test
  public void testUnsupportedLinkTypeProj4() {
    JSONObject crsJSON =
        new JSONObject(
            """
            {"crs":{
              "type":"link",
              "properties" : {
                 "href":"http://spatialreference\
            .org/ref/epsg/4326/proj4/",
                 "type":"proj4"
              }
            }}\
            """);

    CoordinateReferenceSystem crs =
        FeaturesParser.parseCoordinateReferenceSystem(this.requestFactory, crsJSON, false);
    assertNotNull(crs);
    assertSame(DefaultEngineeringCRS.GENERIC_2D, crs);
  }

  @Test
  public void testTreatStringAsGeoJson() throws Exception {
    Configuration configuration = configurationFactory.getConfig(getFile("geojson/config.yaml"));
    MfClientHttpRequestFactory configRequestFactory =
        new ConfigFileResolvingHttpRequestFactory(
            requestFactory,
            configuration,
            new HashMap<String, String>(),
            HTTP_REQUEST_MAX_NUMBER_FETCH_RETRY,
            HTTP_REQUEST_FETCH_RETRY_INTERVAL_MILLIS);
    FeaturesParser featuresParser = new FeaturesParser(configRequestFactory, false);
    for (File geojsonExample : getGeoJsonExamples()) {
      try {
        int numFeatures = getNumExpectedFeatures(geojsonExample);
        final String geojson =
            java.nio.file.Files.readString(geojsonExample.toPath(), Constants.DEFAULT_CHARSET);
        final SimpleFeatureCollection simpleFeatureCollection =
            featuresParser.treatStringAsGeoJson(geojson);
        assertEquals(geojsonExample.getName(), numFeatures, simpleFeatureCollection.size());
      } catch (RuntimeException t) {
        throw new RuntimeException(
            "Exception raised when processing: " + geojsonExample.getName() + "\n" + t.getMessage(),
            t);
      }
    }
  }

  @Test
  public void testTreatStringAsGeoJsonEmptyCollection() throws Exception {
    Configuration configuration = configurationFactory.getConfig(getFile("geojson/config.yaml"));
    MfClientHttpRequestFactory configRequestFactory =
        new ConfigFileResolvingHttpRequestFactory(
            requestFactory,
            configuration,
            new HashMap<String, String>(),
            HTTP_REQUEST_MAX_NUMBER_FETCH_RETRY,
            HTTP_REQUEST_FETCH_RETRY_INTERVAL_MILLIS);
    FeaturesParser featuresParser = new FeaturesParser(configRequestFactory, false);

    final String geojson = "{\"type\": \"FeatureCollection\", \"features\": []}";
    final SimpleFeatureCollection simpleFeatureCollection =
        featuresParser.treatStringAsGeoJson(geojson);
    assertEquals(0, simpleFeatureCollection.size());
  }

  private int getNumExpectedFeatures(File geojsonExample) {
    final Pattern numExpectedFilesPattern = Pattern.compile(".*-(\\d+)\\.json");

    final Matcher matcher = numExpectedFilesPattern.matcher(geojsonExample.getName());
    assertTrue(matcher.find());
    final String numFeatures = matcher.group(1);
    return Integer.parseInt(numFeatures);
  }

  private Iterable<File> getGeoJsonExamples() {
    final File file = getFile(EXAMPLE_GEOJSONFILE);
    File directory = file.getParentFile();
    final File[] files = directory.listFiles();
    assertNotNull(files);
    return Arrays.stream(files)
        .filter(input -> input.getName().endsWith(".json"))
        .collect(Collectors.toList());
  }
}
