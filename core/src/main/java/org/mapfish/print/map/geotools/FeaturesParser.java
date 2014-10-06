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

import com.google.common.io.CharSource;
import com.google.common.io.Closer;
import com.google.common.io.Files;
import org.geotools.data.crs.ForceCoordinateSystemFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.CRS;
import org.mapfish.print.Constants;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.FileUtils;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
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

/**
 * Parser for GeoJson features collection.
 * <p/>
 * Created by Stéphane Brunner on 16/4/14.
 */
public class FeaturesParser {
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

            return readFeatureCollection(input);
        } catch (URISyntaxException e) {
            throw ExceptionUtils.getRuntimeException(e);
        } finally {
            closer.close();
        }
    }

    /**
     * Get the features collection from a GeoJson inline string.
     *
     * @param geoJsonString what to parse
     * @return the feature collection
     * @throws IOException
     */
    public final SimpleFeatureCollection treatStringAsGeoJson(final String geoJsonString) throws IOException {
        final byte[] bytes = geoJsonString.getBytes(Constants.DEFAULT_ENCODING);
        ByteArrayInputStream input = null;
        try {
            input = new ByteArrayInputStream(bytes);
            return readFeatureCollection(input);
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    private SimpleFeatureCollection readFeatureCollection(final Object input) throws IOException {
        FeatureJSON geoJsonReader = new FeatureJSON();
        SimpleFeatureCollection simpleFeatureCollection = (SimpleFeatureCollection) geoJsonReader.readFeatureCollection(input);
        if (this.forceLongitudeFirst) {
            CoordinateReferenceSystem crs = simpleFeatureCollection.getSchema().getCoordinateReferenceSystem();

            final String code;
            try {
                code = CRS.lookupIdentifier(crs, false);

                if (code != null) {
                    crs = CRS.decode(code, true);
                    simpleFeatureCollection = new ForceCoordinateSystemFeatureResults(simpleFeatureCollection, crs);
                }
            } catch (FactoryException e) {
                throw ExceptionUtils.getRuntimeException(e);
            } catch (SchemaException e) {
                throw ExceptionUtils.getRuntimeException(e);
            }
        }

        return simpleFeatureCollection;
    }
}
