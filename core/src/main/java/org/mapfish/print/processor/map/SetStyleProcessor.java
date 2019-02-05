package org.mapfish.print.processor.map;

import org.geotools.styling.Style;
import org.mapfish.print.attribute.StyleAttribute;
import org.mapfish.print.attribute.map.GenericMapAttribute.GenericMapAttributeValues;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.geotools.AbstractFeatureSourceLayer;
import org.mapfish.print.map.style.StyleParserPlugin;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.InputOutputValue;
import org.mapfish.print.processor.http.MfClientHttpRequestFactoryProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * <p>Processor to set a style on vector layers from the attributes.</p>
 * [[examples=report]]
 */
public class SetStyleProcessor extends
        AbstractProcessor<SetStyleProcessor.Input, Void> {

    @Autowired
    private StyleParserPlugin mapfishJsonParser;

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
        final Style style = this.mapfishJsonParser.parseStyle(
                values.template.getConfiguration(),
                values.clientHttpRequestFactoryProvider.get(),
                values.style.style
        ).get();
        for (MapLayer layer: values.map.getLayers()) {
            context.stopIfCanceled();
            if (layer instanceof AbstractFeatureSourceLayer) {
                ((AbstractFeatureSourceLayer) layer).setStyle((requestFactory, featureSource) -> style);
            }
        }

        return null;
    }

    @Override
    protected void extraValidation(
            final List<Throwable> validationErrors, final Configuration configuration) {
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
        public MfClientHttpRequestFactoryProvider clientHttpRequestFactoryProvider;

        /**
         * The template containing this table processor.
         */
        public Template template;

        /**
         * The map to update.
         */
        @InputOutputValue
        public GenericMapAttributeValues map;

        /**
         * The style.
         */
        public StyleAttribute.StylesAttributeValues style;
    }
}
