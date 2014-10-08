/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.map.geotools;

import com.google.common.collect.Sets;
import com.google.common.io.CharSource;
import com.google.common.io.CharStreams;
import com.google.common.io.Closer;
import com.google.common.io.Files;
import com.vividsolutions.jts.geom.Geometry;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.Constants;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.FileUtils;
import org.mapfish.print.PrintException;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Parser for GeoJson features collection.
 * <p/>
 * Created by Stéphane Brunner on 16/4/14.
 */
public class FeaturesParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesParser.class);
    private final MfClientHttpRequestFactory httpRequestFactory;
    private final boolean forceLongitudeFirst;

    /**
     * Construct.
     *
     * @param httpRequestFactory  the HTTP request factory
     * @param forceLongitudeFirst if true then force longitude coordinate as first coordinate
     */
    public FeaturesParser(final MfClientHttpRequestFactory httpRequestFactory, final boolean forceLongitudeFirst) {
        this.httpRequestFactory = httpRequestFactory;
        this.forceLongitudeFirst = forceLongitudeFirst;
    }

    /**
     * Get the features collection from a GeoJson inline string or URL.
     *
     * @param template the template
     * @param features what to parse
     * @return the feature collection
     * @throws IOException
     */
    public final SimpleFeatureCollection autoTreat(final Template template, final String features) throws IOException {
        SimpleFeatureCollection featuresCollection = treatStringAsURL(template, features);
        if (featuresCollection == null) {
            featuresCollection = treatStringAsGeoJson(features);
        }
        return featuresCollection;
    }

    /**
     * Get the features collection from a GeoJson URL.
     *
     * @param template   the template
     * @param geoJsonUrl what to parse
     * @return the feature collection
     */
    public final SimpleFeatureCollection treatStringAsURL(final Template template, final String geoJsonUrl) throws IOException {
        URL url;
        try {
            url = FileUtils.testForLegalFileUrl(template.getConfiguration(), new URL(geoJsonUrl));
        } catch (MalformedURLException e) {
            return null;
        }

        final String geojsonString;
        Closer closer = Closer.create();
        try {
            Reader input;
            if (url.getProtocol().equalsIgnoreCase("file")) {
                final CharSource charSource = Files.asCharSource(new File(url.getFile()), Constants.DEFAULT_CHARSET);
                input = closer.register(charSource.openBufferedStream());
            } else {
                final ClientHttpResponse response = closer.register(this.httpRequestFactory.createRequest(url.toURI(),
                        HttpMethod.GET).execute());

                input = closer.register(new BufferedReader(new InputStreamReader(response.getBody(), Constants.DEFAULT_CHARSET)));
            }
            geojsonString = CharStreams.toString(input);
        } catch (URISyntaxException e) {
            throw ExceptionUtils.getRuntimeException(e);
        } finally {
            closer.close();
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
        ByteArrayInputStream input = new ByteArrayInputStream(convertedGeojsonObject.getBytes(Constants.DEFAULT_CHARSET));

        return (SimpleFeatureCollection) geoJsonReader.readFeatureCollection(input);
    }

    private String convertToGeoJsonCollection(final String geojsonData) {
        String convertedGeojsonObject = geojsonData.trim();
        if (convertedGeojsonObject.startsWith("[")) {
            convertedGeojsonObject = "{\"type\": \"FeatureCollection\", \"features\": " + convertedGeojsonObject + "}";
        }
        return convertedGeojsonObject;
    }

    private SimpleFeatureType createFeatureType(@Nonnull final String geojsonData) {
        try {
            JSONObject geojson = new JSONObject(geojsonData);
            if (geojson.has("type") && geojson.getString("type").equalsIgnoreCase("FeatureCollection")) {
                CoordinateReferenceSystem crs = parseCoordinateReferenceSystem(geojson);
                SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
                builder.setName("GeosjonFeatureType");
                final JSONArray features = geojson.getJSONArray("features");

                Set<String> allAttributes = Sets.newHashSet();
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
                        if (geomType != Geometry.class) {
                            Class<Geometry> thisGeomType = parseGeometryType(feature);
                            if (thisGeomType != null) {
                                if (geomType == null) {
                                    geomType = thisGeomType;
                                } else if (geomType != thisGeomType) {
                                    geomType = Geometry.class;
                                }
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
                return (Class<Geometry>) Class.forName("com.vividsolutions.jts.geom." + geomTypeString);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unrecognized geometry type in geojson: " + geomTypeString);
            }
        }
    }

    private CoordinateReferenceSystem parseCoordinateReferenceSystem(final JSONObject geojson) {
        CoordinateReferenceSystem crs = DefaultEngineeringCRS.GENERIC_2D;
        StringBuilder code = new StringBuilder();
        try {
            if (geojson.has("crs")) {
                JSONObject crsJson = geojson.getJSONObject("crs");
                if (crsJson.has("type")) {
                    code.append(crsJson.getString("type"));
                }

                if (crsJson.has("properties")) {
                    final JSONObject propertiesJson = crsJson.getJSONObject("properties");
                    if (propertiesJson.has("code")) {
                        if (code.length() > 0) {
                            code.append(":");
                        }
                        code.append(propertiesJson.getString("code"));
                    }
                }
            }
        } catch (JSONException e) {
            LOGGER.warn("Error reading the required elements to parse crs of the geojson: \n" + geojson, e);
        }
        try {
            if (code.length() > 0) {
                crs = CRS.decode(code.toString(), this.forceLongitudeFirst);
            }
        } catch (NoSuchAuthorityCodeException e) {
            LOGGER.warn("No CRS with code: " + code + ".\nRead from geojson: \n" + geojson);
        } catch (FactoryException e) {
            LOGGER.warn("Error loading CRS with code: " + code + ".\nRead from geojson: \n" + geojson);
        }
        return crs;
    }
}
