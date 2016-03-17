package org.mapfish.print.processor.map;

import org.geotools.styling.Style;
import org.mapfish.print.attribute.StyleAttribute;
import org.mapfish.print.attribute.map.GenericMapAttribute;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.AbstractFeatureSourceLayer;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.mapfish.print.processor.AbstractProcessor;

import java.util.List;

/**
 * <p>Processor to set a style on vector layers from the attributes.
 * </p>
 * @author Stéphane Brunner
 */
public class SetStyleProcessor extends
        AbstractProcessor<SetStyleProcessor.Input, Void> {

    /**
     * Constructor.
     */
    protected SetStyleProcessor() {
        super(Void.class);
    }

    @Override
    public final Input createInputParameter() {
        return new Input();
    }

    @Override
    public final Void execute(final Input values, final ExecutionContext context) {
        for (MapLayer layer : values.map.getLayers()) {
            checkCancelState(context);
            if (layer instanceof AbstractFeatureSourceLayer) {
                ((AbstractFeatureSourceLayer) layer).setStyle(new StyleSupplier() {
                    @Override
                    public Style load(final MfClientHttpRequestFactory requestFactory,
                                      final Object featureSource,
                                      final MapfishMapContext mapContext) throws Exception {
                        return values.style.getStyle(values.clientHttpRequestFactory, mapContext);
                    }
                });
            }
        }

        return null;
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors, final Configuration configuration) {
        // no validation needed
    }

    /**
     * The input parameter object for {@link SetStyleProcessor}.
     */
    public static final class Input {
        /**
         * A factory for making http requests.  This is added to the values by the framework and therefore
         * does not need to be set in configuration
         */
        public MfClientHttpRequestFactory clientHttpRequestFactory;
        /**
         * The map to update.
         */
        public GenericMapAttribute<?>.GenericMapAttributeValues map;

        /**
         * The style.
         */
        public StyleAttribute.StylesAttributeValues style;
    }
}
