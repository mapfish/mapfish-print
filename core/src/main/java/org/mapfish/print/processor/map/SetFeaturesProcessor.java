package org.mapfish.print.processor.map;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.mapfish.print.attribute.FeaturesAttribute.FeaturesAttributeValues;
import org.mapfish.print.attribute.map.GenericMapAttribute.GenericMapAttributeValues;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.map.geotools.AbstractFeatureSourceLayer;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.InputOutputValue;
import org.mapfish.print.processor.http.MfClientHttpRequestFactoryProvider;

import java.util.List;


/**
 * <p>Processor to set features on vector layers.</p>
 * [[examples=report]]
 */
public class SetFeaturesProcessor extends
        AbstractProcessor<SetFeaturesProcessor.Input, Void> {

    /**
     * Constructor.
     */
    protected SetFeaturesProcessor() {
        super(Void.class);
    }

    @Override
    public final Input createInputParameter() {
        return new Input();
    }

    @Override
    public final Void execute(final Input values, final ExecutionContext context) throws Exception {
        for (MapLayer layer: values.map.getLayers()) {
            context.stopIfCanceled();
            if (layer instanceof AbstractFeatureSourceLayer) {
                final SimpleFeatureCollection features = values.features.getFeatures(
                        values.clientHttpRequestFactoryProvider.get());
                ((AbstractFeatureSourceLayer) layer).setFeatureCollection(features);
            }
        }

        return null;
    }

    @Override
    protected void extraValidation(
            final List<Throwable> validationErrors, final Configuration configuration) {
        // no checks needed
    }

    /**
     * The input parameter object for {@link SetFeaturesProcessor}.
     */
    public static final class Input {
        /**
         * The factory to use for making http requests.
         */
        public MfClientHttpRequestFactoryProvider clientHttpRequestFactoryProvider;

        /**
         * The map to update.
         */
        @InputOutputValue
        public GenericMapAttributeValues map;

        /**
         * The features.
         */
        public FeaturesAttributeValues features;
    }
}
