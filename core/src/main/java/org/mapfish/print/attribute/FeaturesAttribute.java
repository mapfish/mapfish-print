package org.mapfish.print.attribute;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.FeaturesParser;
import org.mapfish.print.parser.HasDefaultValue;

import java.io.IOException;
import java.util.List;

/**
 * Attribute for GeoJson feature collection. [[examples=report]]
 */
public final class FeaturesAttribute extends ReflectiveAttribute<FeaturesAttribute.FeaturesAttributeValues> {

    @Override
    public Class<FeaturesAttributeValues> getValueType() {
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
        /**
         * The geojson features.
         */
        public String features;
        /**
         * By default the normal axis order as specified in EPSG code will be used when parsing projections.
         * However the requestor can override this by explicitly declaring that longitude axis is first.
         */
        @HasDefaultValue
        public Boolean longitudeFirst = null;
        private SimpleFeatureCollection featuresCollection;

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
        public synchronized SimpleFeatureCollection getFeatures(
                final MfClientHttpRequestFactory httpRequestFactory) throws
                IOException {
            if (this.featuresCollection == null) {
                final boolean forceLongitudeFirst = this.longitudeFirst == null ? false : this.longitudeFirst;
                final FeaturesParser parser = new FeaturesParser(httpRequestFactory, forceLongitudeFirst);
                this.featuresCollection = parser.autoTreat(this.template, this.features);
            }
            return this.featuresCollection;
        }
    }
}
