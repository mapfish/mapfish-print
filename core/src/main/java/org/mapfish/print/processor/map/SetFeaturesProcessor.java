package org.mapfish.print.processor.map;

import org.geotools.data.simple.SimpleFeatureCollection;

import org.mapfish.print.attribute.FeaturesAttribute.FeaturesAttributeValues;
import org.mapfish.print.attribute.map.GenericMapAttribute;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.AbstractFeatureSourceLayer;
import org.mapfish.print.processor.AbstractProcessor;

import java.util.List;


/**
 * <p>Processor to set features on vector layers.</p>
 * [[examples=report]]
 */
public class SetFeaturesProcessor extends
        AbstractProcessor<SetFeaturesProcessor.Input, SetFeaturesProcessor.Output> {

    /**
     * Constructor.
     */
    protected SetFeaturesProcessor() {
        super(Output.class);
    }

    @Override
    public final Input createInputParameter() {
        return new Input();
    }

    @Override
    public final Output execute(final Input values, final ExecutionContext context) throws Exception {
        for (MapLayer layer : values.map.getLayers()) {
            checkCancelState(context);
            if (layer instanceof AbstractFeatureSourceLayer) {
                final SimpleFeatureCollection features = values.features.getFeatures(values.clientHttpRequestFactory);
                ((AbstractFeatureSourceLayer) layer).setFeatureCollection(features);
            }
        }

        return new Output(values.map);
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors, final Configuration configuration) {
        // no checks needed
    }

    /**
     * The input parameter object for {@link SetFeaturesProcessor}.
     */
    public static final class Input {
        /**
         * The factory to use for making http requests.
         */
        public MfClientHttpRequestFactory clientHttpRequestFactory;
        /**
         * The map to update.
         */
        public GenericMapAttribute<?>.GenericMapAttributeValues map;

        /**
         * The features.
         */
        public FeaturesAttributeValues features;
    }

    /**
     * The object containing the output for this processor.
     */
    public static class Output {

        /**
         * The map to update with the static layers.
         */
        public GenericMapAttribute<?>.GenericMapAttributeValues map;

        Output(final GenericMapAttribute<?>.GenericMapAttributeValues map) {
            this.map = map;
        }
    }
}
