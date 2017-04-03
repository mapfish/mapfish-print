package org.mapfish.print.map.geotools;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
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
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

public class FeaturesParserTest extends AbstractMapfishSpringTest {


    private static final String EXAMPLE_GEOJSONFILE = "geojson/geojson-inconsistent-attributes-2.json";
    @Autowired
    private TestHttpClientFactory requestFactory;
    @Autowired
    private ConfigurationFactory configurationFactory;


    @Test
    public void testParseCRSBackwardCompat() throws Exception {
        JSONObject crsJSON = new JSONObject("{\"crs\":{\n"
                                            + "  \"type\":\"EPSG\",\n"
                                            + "  \"properties\" : {\n"
                                            + "     \"code\":\"4326\"\n"
                                            + "  }\n"
                                            + "}\n}");

        CoordinateReferenceSystem crs = FeaturesParser.parseCoordinateReferenceSystem(this.requestFactory, crsJSON, false);
        assertNotNull(crs);
        assertEquals("EPSG:4326", CRS.lookupIdentifier(crs, false));
    }

    @Test
    public void testParseCRSNameCode() throws Exception {
        JSONObject crsJSON = new JSONObject("{\"crs\":{\n"
                                            + "  \"type\":\"name\",\n"
                                            + "  \"properties\" : {\n"
                                            + "     \"name\":\"EPSG:4326\"\n"
                                            + "  }\n"
                                            + "}}");

        CoordinateReferenceSystem crs = FeaturesParser.parseCoordinateReferenceSystem(this.requestFactory, crsJSON, false);
        assertNotNull(crs);
        assertEquals("EPSG:4326", CRS.lookupIdentifier(crs, false));
    }


    @Test
    public void testParseCRSNameURI() throws Exception {
        JSONObject crsJSON = new JSONObject("{\"crs\":{\n"
                                            + "  \"type\":\"name\",\n"
                                            + "  \"properties\" : {\n"
                                            + "     \"name\":\"urn:ogc:def:crs:EPSG::4326\"\n"
                                            + "  }\n"
                                            + "}}");

        CoordinateReferenceSystem crs = FeaturesParser.parseCoordinateReferenceSystem(this.requestFactory, crsJSON, false);
        assertNotNull(crs);
        assertEquals("EPSG:4326", CRS.lookupIdentifier(crs, false));
    }

    @Test @DirtiesContext
    public void testParseCRSLinkOgcWkt() throws Exception {
        requestFactory.registerHandler(new Predicate<URI>() {
            @Override
            public boolean apply(@Nullable URI input) {
                return input != null && input.getHost().equals("spatialreference.org");
            }
        }, new TestHttpClientFactory.Handler() {
            @Override
            public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
                String wkt = "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\","
                             + "\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],"
                             + "UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]";
                MockClientHttpRequest mockClientHttpRequest = new MockClientHttpRequest();
                mockClientHttpRequest.setResponse(new MockClientHttpResponse(wkt.getBytes(), HttpStatus.OK));
                return mockClientHttpRequest;

            }
        });
        JSONObject crsJSON = new JSONObject("{\"crs\":{\n"
                                            + "  \"type\":\"link\",\n"
                                            + "  \"properties\" : {\n"
                                            + "     \"href\":\"http://spatialreference.org/ref/epsg/4326/ogcwkt/\",\n"
                                            + "     \"type\":\"ogcwkt\"\n"
                                            + "  }\n"
                                            + "}}");

        CoordinateReferenceSystem crs = FeaturesParser.parseCoordinateReferenceSystem(this.requestFactory, crsJSON, false);
        assertNotNull(crs);
        assertEquals("EPSG:4326", CRS.lookupIdentifier(crs, false));
    }

    @Test @DirtiesContext
    public void testParseCRSLinkEsriWkt() throws Exception {
        requestFactory.registerHandler(new Predicate<URI>() {
            @Override
            public boolean apply(@Nullable URI input) {
                return input != null && input.getHost().equals("spatialreference.org");
            }
        }, new TestHttpClientFactory.Handler() {
            @Override
            public MockClientHttpRequest handleRequest(URI uri, HttpMethod httpMethod) throws Exception {
                String wkt = "GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],"
                             + "PRIMEM[\"Greenwich\","
                             + "0],UNIT[\"Degree\",0.017453292519943295]]";
                MockClientHttpRequest mockClientHttpRequest = new MockClientHttpRequest();
                mockClientHttpRequest.setResponse(new MockClientHttpResponse(wkt.getBytes(), HttpStatus.OK));
                return mockClientHttpRequest;
            }
        });
        JSONObject crsJSON = new JSONObject("{\"crs\":{\n"
                                            + "  \"type\":\"link\",\n"
                                            + "  \"properties\" : {\n"
                                            + "     \"href\":\"http://spatialreference.org/ref/epsg/4326/esriwkt/\",\n"
                                            + "     \"type\":\"esriwkt\"\n"
                                            + "  }\n"
                                            + "}}");

        CoordinateReferenceSystem crs = FeaturesParser.parseCoordinateReferenceSystem(this.requestFactory, crsJSON, false);
        assertNotNull(crs);
        assertNotSame(DefaultEngineeringCRS.GENERIC_2D, crs);
    }

    public void testParseCRSLinkProj4() throws Exception {
        JSONObject crsJSON = new JSONObject("{\"crs\":{\n"
                                            + "  \"type\":\"link\",\n"
                                            + "  \"properties\" : {\n"
                                            + "     \"href\":\"http://spatialreference.org/ref/epsg/4326/proj4/\",\n"
                                            + "     \"type\":\"proj4\"\n"
                                            + "  }\n"
                                            + "}}");

        CoordinateReferenceSystem crs = FeaturesParser.parseCoordinateReferenceSystem(this.requestFactory, crsJSON, false);
        assertNotNull(crs);
        assertNotSame(DefaultEngineeringCRS.GENERIC_2D, crs);
    }

    @Test
    public void testTreatStringAsGeoJson() throws Exception {
        Configuration configuration = configurationFactory.getConfig(getFile("geojson/config.yaml"));
        MfClientHttpRequestFactory configRequestFactory = new ConfigFileResolvingHttpRequestFactory(requestFactory, configuration, "test");
        FeaturesParser featuresParser = new FeaturesParser( configRequestFactory, false);
        for (File geojsonExample : getGeoJsonExamples()) {
            try {
                int numFeatures = getNumExpectedFeatures(geojsonExample);
                final String geojson = Files.toString(geojsonExample, Constants.DEFAULT_CHARSET);
                final SimpleFeatureCollection simpleFeatureCollection = featuresParser.treatStringAsGeoJson(geojson);
                assertEquals(geojsonExample.getName(), numFeatures, simpleFeatureCollection.size());
            } catch (AssertionError e) {
                throw e;
            } catch (Throwable t) {
                t.printStackTrace();
                throw new AssertionError("Exception raised when processing: " + geojsonExample.getName() + "\n" + t.getMessage());
            }
        }
    }

    @Test
    public void testTreatStringAsGeoJsonEmptyCollection() throws Exception {
        Configuration configuration = configurationFactory.getConfig(getFile("geojson/config.yaml"));
        MfClientHttpRequestFactory configRequestFactory = new ConfigFileResolvingHttpRequestFactory(requestFactory, configuration, "test");
        FeaturesParser featuresParser = new FeaturesParser( configRequestFactory, false);

        final String geojson = "{\"type\": \"FeatureCollection\", \"features\": []}";
        final SimpleFeatureCollection simpleFeatureCollection = featuresParser.treatStringAsGeoJson(geojson);
        assertEquals(0, simpleFeatureCollection.size());
    }

    private int getNumExpectedFeatures(File geojsonExample) {
        final Pattern numExpectedFilesPattern = Pattern.compile(".*-(\\d+)\\.json");

        final Matcher matcher = numExpectedFilesPattern.matcher(geojsonExample.getName());
        matcher.find();
        final String numFeatures = matcher.group(1);
        return Integer.parseInt(numFeatures);

    }

    private Iterable<File> getGeoJsonExamples() {
        final File file = getFile(EXAMPLE_GEOJSONFILE);
        File directory = file.getParentFile();
        return Iterables.filter(Files.fileTreeTraverser().children(directory), new Predicate<File>() {
            @Override
            public boolean apply(@Nullable File input) {
                return input.getName().endsWith(".json");
            }
        });
    }
}
