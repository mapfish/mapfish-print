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

package org.mapfish.print.attribute;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.referencing.CRS;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.geotools.FeaturesParser;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.wrapper.PObject;
import org.mapfish.print.wrapper.yaml.PYamlObject;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Attribute for GeoJson features collection.
 * <p/>
 * Created by Stéphane Brunner on 16/4/14.
 */
public class FeaturesAttribute extends AttributeWithDefaultConfig<FeaturesAttribute.FeaturesAttributeValues> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesAttribute.class);

    private PYamlObject defaults = new PYamlObject(Collections.<String, Object>emptyMap(), "featuresAttribute");

    /**
     * A http request factory for making http requests.
     */
    @Autowired
    protected ClientHttpRequestFactory httpRequestFactory;

    public final void setDefaults(final Map<String, Object> defaults) {
        this.defaults = new PYamlObject(defaults, "featuresAttribute");
    }

    @Override
    public final PObject getDefaultValues() {
        return this.defaults;
    }

    @Override
    public final FeaturesAttributeValues createValue(final Template template) {
        FeaturesAttributeValues result = new FeaturesAttributeValues(template);
        return result;
    }


    @Override
    public void validate(final List<Throwable> validationErrors) {
        // no checks required
    }

    /**
     * The value of {@link FeaturesAttribute}.
     */
    public final class FeaturesAttributeValues {
        private final Template template;
        private SimpleFeatureCollection featuresCollection;

        /**
         * The geojson features.
         */
        public String features;

        /**
         * The projection of the features.
         */
        @HasDefaultValue
        public String projection = "EPSG:3857";

        /**
         * Constructor.
         *
         * @param template the template this map is part of.
         */
        public FeaturesAttributeValues(final Template template) {
            this.template = template;
        }

        /**
         * Validate the values provided by the request data and construct MapBounds and parse the layers.
         */
        public void postConstruct() throws FactoryException {
            final FeaturesParser parser = new FeaturesParser(FeaturesAttribute.this.httpRequestFactory);
            try {
                this.featuresCollection = parser.autoTreat(this.template, this.features);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Validate the values provided by the request data and construct MapBounds and parse the layers.
         */
        public SimpleFeatureCollection getFeatures() throws FactoryException {
            return this.featuresCollection;

        }

        private CoordinateReferenceSystem parseProjection() {
            try {
                return CRS.decode(this.projection);
            } catch (NoSuchAuthorityCodeException e) {
                throw new RuntimeException(this.projection + "was not recognized as a crs code", e);
            } catch (FactoryException e) {
                throw new RuntimeException("Error occurred while parsing: " + this.projection, e);
            }
        }
    }
}
