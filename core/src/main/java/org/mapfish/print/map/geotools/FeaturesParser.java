package org.mapfish.print.map.geotools;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.IOUtils;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.mapfish.print.Constants;
import org.mapfish.print.FileUtils;
import org.mapfish.print.PrintException;
import org.mapfish.print.URIUtils;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Parser for GeoJson features collection.
 *
 * Created by Stéphane Brunner on 16/4/14.
 */
public class FeaturesParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesParser.class);
    private final MfClientHttpRequestFactory httpRequestFactory;
    private final boolean forceLongitudeFirst;

    /**
     * Construct.
     *
     * @param httpRequestFactory the HTTP request factory
     * @param forceLongitudeFirst if true then force longitude coordinate as first coordinate
     */
    public FeaturesParser(
            final MfClientHttpRequestFactory httpRequestFactory, final boolean forceLongitudeFirst) {
        this.httpRequestFactory = httpRequestFactory;
        this.forceLongitudeFirst = forceLongitudeFirst;
    }

    @VisibleForTesting
    static final CoordinateReferenceSystem parseCoordinateReferenceSystem(
            final MfClientHttpRequestFactory requestFactory,
            final JSONObject geojson,
            final boolean forceLongitudeFirst) {
        CoordinateReferenceSystem crs = DefaultEngineeringCRS.GENERIC_2D;
        StringBuilder code = new StringBuilder();
        try {
            if (geojson.has("crs")) {
                JSONObject crsJson = geojson.getJSONObject("crs");
                String type = crsJson.optString("type", "");

                if (type.equalsIgnoreCase("EPSG") || type.equalsIgnoreCase("CRS")) {
                    code.append(type);
                    String propCode = getProperty(crsJson, "code");
                    if (propCode != null) {
                        code.append(":").append(propCode);
                    }
                } else if (type.equalsIgnoreCase("name")) {
                    String propCode = getProperty(crsJson, "name");
                    if (propCode != null) {
                        code.append(propCode);
                    }
                } else if (type.equals("link")) {
                    String linkType = getProperty(crsJson, "type");
                    if (linkType != null &&
                            (linkType.equalsIgnoreCase("esriwkt") || linkType.equalsIgnoreCase("ogcwkt"))) {
                        String uri = getProperty(crsJson, "href");
                        if (uri != null) {
                            ClientHttpRequest request =
                                    requestFactory.createRequest(new URI(uri), HttpMethod.GET);
                            try (ClientHttpResponse response = request.execute()) {

                                if (response.getStatusCode() == HttpStatus.OK) {
                                    final String wkt = IOUtils.toString(response.getBody(),
                                                                        Constants.DEFAULT_ENCODING);
                                    try {
                                        return CRS.parseWKT(wkt);
                                    } catch (FactoryException e) {
                                        LOGGER.warn(
                                                "Unable to load linked CRS from geojson: \n{}\n\nWKT loaded" +
                                                        " from:\n{}", crsJson, wkt);
                                    }
                                }
                            }
                        }
                    } else {
                        LOGGER.warn("Unable to load linked CRS from geojson: \n{}", crsJson);
                    }
                } else {
                    code.append(getProperty(crsJson, "code"));
                }

            }
        } catch (JSONException | IOException | URISyntaxException e) {
            LOGGER.warn("Error reading the required elements to parse crs of the geojson: \n{}", geojson, e);
        }
        try {
            if (code.length() > 0) {
                crs = CRS.decode(code.toString(), forceLongitudeFirst);
            }
        } catch (NoSuchAuthorityCodeException e) {
            LOGGER.warn("No CRS with code: {}.\nRead from geojson: \n{}", code, geojson);
        } catch (FactoryException e) {
            LOGGER.warn("Error loading CRS with code: {}.\nRead from geojson: \n{}", code, geojson);
        }
        return crs;
    }

    private static String getProperty(final JSONObject crsJson, final String nameCode) throws JSONException {
        if (crsJson.has("properties")) {
            final JSONObject propertiesJson = crsJson.getJSONObject("properties");
            if (propertiesJson.has(nameCode)) {
                return propertiesJson.getString(nameCode);
            }
        }
        return null;
    }

    /**
     * Get the features collection from a GeoJson inline string or URL.
     *
     * @param template the template
     * @param features what to parse
     * @return the feature collection
     * @throws IOException
     */
    public final SimpleFeatureCollection autoTreat(final Template template, final String features)
            throws IOException {
        SimpleFeatureCollection featuresCollection = treatStringAsURL(template, features);
        if (featuresCollection == null) {
            featuresCollection = treatStringAsGeoJson(features);
        }
        return featuresCollection;
    }

    /**
     * Get the features collection from a GeoJson URL.
     *
     * @param template the template
     * @param geoJsonUrl what to parse
     * @return the feature collection
     */
    public final SimpleFeatureCollection treatStringAsURL(final Template template, final String geoJsonUrl)
            throws IOException {
        URL url;
        try {
            url = FileUtils.testForLegalFileUrl(template.getConfiguration(), new URL(geoJsonUrl));
        } catch (MalformedURLException e) {
            return null;
        }

        final String geojsonString;
        if (url.getProtocol().equalsIgnoreCase("file")) {
            geojsonString = IOUtils.toString(url, Constants.DEFAULT_CHARSET.name());
        } else {
            geojsonString = URIUtils.toString(this.httpRequestFactory, url);
        }

        return treatStringAsGeoJson(geojsonString);
    }

    /**
     * Get the features collection from a GeoJson inline string.
     *
     * @param geoJsonString what to parse
     * @return the feature collection
     * @throws IOException
     */
    public final SimpleFeatureCollection treatStringAsGeoJson(final String geoJsonString) throws IOException {
        return readFeatureCollection(geoJsonString);
    }

    private SimpleFeatureCollection readFeatureCollection(final String geojsonData) throws IOException {
        String convertedGeojsonObject = convertToGeoJsonCollection(geojsonData);

        FeatureJSON geoJsonReader = new FeatureJSON();
        final SimpleFeatureType featureType = createFeatureType(convertedGeojsonObject);
        if (featureType != null) {
            geoJsonReader.setFeatureType(featureType);
        }
        ByteArrayInputStream input =
                new ByteArrayInputStream(convertedGeojsonObject.getBytes(Constants.DEFAULT_CHARSET));

        return (SimpleFeatureCollection) geoJsonReader.readFeatureCollection(input);
    }

    private String convertToGeoJsonCollection(final String geojsonData) {
        String convertedGeojsonObject = geojsonData.trim();
        if (convertedGeojsonObject.startsWith("[")) {
            convertedGeojsonObject =
                    "{\"type\": \"FeatureCollection\", \"features\": " + convertedGeojsonObject + "}";
        }
        return convertedGeojsonObject;
    }

    private SimpleFeatureType createFeatureType(@Nonnull final String geojsonData) {
        try {
            JSONObject geojson = new JSONObject(geojsonData);
            if (geojson.has("type") && geojson.getString("type").equalsIgnoreCase("FeatureCollection")) {
                CoordinateReferenceSystem crs =
                        parseCoordinateReferenceSystem(this.httpRequestFactory, geojson,
                                                       this.forceLongitudeFirst);
                SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
                builder.setName("GeosjonFeatureType");
                final JSONArray features = geojson.getJSONArray("features");

                if (features.length() == 0) {
                    // do not try to build the feature type if there are no features
                    return null;
                }

                Set<String> allAttributes = new HashSet<>();
                Class<Geometry> geomType = null;
                for (int i = 0; i < features.length(); i++) {
                    final JSONObject feature = features.getJSONObject(i);
                    final JSONObject properties = feature.getJSONObject("properties");
                    final Iterator keys = properties.keys();
                    while (keys.hasNext()) {
                        String nextKey = (String) keys.next();
                        if (!allAttributes.contains(nextKey)) {
                            allAttributes.add(nextKey);
                            builder.add(nextKey, Object.class);
                        }
                    }
                    if (geomType != Geometry.class) {
                        Class<Geometry> thisGeomType = parseGeometryType(feature);
                        if (thisGeomType != null) {
                            if (geomType == null) {
                                geomType = thisGeomType;
                            } else if (!geomType.equals(thisGeomType)) {
                                geomType = Geometry.class;
                            }
                        }
                    }
                }

                builder.add("geometry", geomType, crs);
                builder.setDefaultGeometry("geometry");
                return builder.buildFeatureType();
            } else {
                return null;
            }
        } catch (JSONException e) {
            throw new PrintException("Invalid geoJSON: \n" + geojsonData + ": " + e.getMessage(), e);
        }

    }

    @SuppressWarnings("unchecked")
    private Class<Geometry> parseGeometryType(@Nonnull final JSONObject featureJson) throws JSONException {
        JSONObject geomJson = featureJson.optJSONObject("geometry");
        if (geomJson == null) {
            return null;
        }
        String geomTypeString = geomJson.optString("type", "Geometry");
        if (geomTypeString.equalsIgnoreCase("Positions")) {
            return Geometry.class;
        } else {
            try {
                return (Class<Geometry>) Class.forName("org.locationtech.jts.geom." + geomTypeString);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unrecognized geometry type in geojson: " + geomTypeString);
            }
        }
    }
}
