package org.mapfish.print.processor.map;

import org.mapfish.print.attribute.map.GenericMapAttribute.GenericMapAttributeValues;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.map.image.wms.WmsLayer;
import org.mapfish.print.map.tiled.wms.TiledWmsLayer;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.InputOutputValue;

import java.util.List;

/**
 * <p>Processor to set a param on WMS layers.</p>
 */
public class SetWmsCustomParamProcessor extends AbstractProcessor<SetWmsCustomParamProcessor.Input, Void> {

    /**
     * The parameter name.
     */
    private String paramName;


    /**
     * Constructor.
     */
    protected SetWmsCustomParamProcessor() {
        super(Void.class);
    }

    @Override
    public final Input createInputParameter() {
        return new Input();
    }

    @Override
    public final Void execute(final Input values, final ExecutionContext context) {
        for (MapLayer layer: values.map.getLayers()) {
            context.stopIfCanceled();
            if (layer instanceof WmsLayer) {
                ((WmsLayer) layer).getParams().setCustomParam(this.paramName, values.value);
            } else if (layer instanceof TiledWmsLayer) {
                ((TiledWmsLayer) layer).getParams().setCustomParam(this.paramName, values.value);
            }
        }
        return null;
    }

    @Override
    protected final void extraValidation(
            final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.paramName == null) {
            validationErrors
                    .add(new ConfigurationException("No paramName defined in " + getClass().getName()));
        }
    }

    /**
     * Set the parameter name.
     *
     * @param paramName the parameter name
     */
    public final void setParamName(final String paramName) {
        this.paramName = paramName;
    }

    /**
     * The input parameter object for {@link SetFeaturesProcessor}.
     */
    public static final class Input {

        /**
         * The map to update.
         */
        @InputOutputValue
        public GenericMapAttributeValues map;

        /**
         * The value.
         */
        public String value;
    }
}
