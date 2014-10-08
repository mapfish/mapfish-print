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
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.FeaturesParser;
import org.mapfish.print.parser.HasDefaultValue;
import org.opengis.referencing.FactoryException;

import java.io.IOException;
import java.util.List;

/**
 * Attribute for GeoJson features collection.
 * <p/>
 * Created by Stéphane Brunner on 16/4/14.
 */
public final class FeaturesAttribute extends ReflectiveAttribute<FeaturesAttribute.FeaturesAttributeValues> {
    @Override
    protected Class<FeaturesAttributeValues> getValueType() {
        return FeaturesAttributeValues.class;
    }

    @Override
    public FeaturesAttributeValues createValue(final Template template) {
        return new FeaturesAttributeValues(template);
    }


    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        // no checks required
    }

    /**
     * The value of {@link FeaturesAttribute}.
     */
    public static final class FeaturesAttributeValues {
        private final Template template;
        private SimpleFeatureCollection featuresCollection;

        /**
         * The geojson features.
         */
        public String features;

        /**
         * By default the normal axis order as specified in EPSG code will be used when parsing projections.  However
         * the requestor can override this by explicitly declaring that longitude axis is first.
         */
        @HasDefaultValue
        public Boolean longitudeFirst = null;

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
         *
         * @param httpRequestFactory the request factory to use for making requests
         */
        public synchronized SimpleFeatureCollection getFeatures(final MfClientHttpRequestFactory httpRequestFactory) throws
                FactoryException, IOException {
            if (this.featuresCollection == null) {
                final boolean forceLongitudeFirst = this.longitudeFirst == null ? false : this.longitudeFirst;
                final FeaturesParser parser = new FeaturesParser(httpRequestFactory, forceLongitudeFirst);
                this.featuresCollection = parser.autoTreat(this.template, this.features);
            }
            return this.featuresCollection;
        }
    }
}
