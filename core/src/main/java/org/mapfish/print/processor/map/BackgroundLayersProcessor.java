package org.mapfish.print.processor.map;

import org.mapfish.print.attribute.map.BackgroundLayersAttribute;
import org.mapfish.print.attribute.map.MapAttribute.MapAttributeValues;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PJoinedArray;

import java.util.List;
import javax.annotation.Nullable;

/**
 * This processor adds the configured set of layers to the map as the bottom set of layers.  This is useful when all maps
 * should have a default set of background layers added to those that the client sends for printing.  This can simplify the
 * client so the client only needs to be concerned with the overlay layers.
 *
 * @author Jesse on 4/18/2015.
 */
public final class BackgroundLayersProcessor extends AbstractProcessor<BackgroundLayersProcessor.Input, Void> {
    /**
     * Constructor.
     */
    protected BackgroundLayersProcessor() {
        super(Void.class);
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors, final Configuration configuration) {
        // no checks to do
    }

    @Nullable
    @Override
    public Input createInputParameter() {
        return new Input();
    }

    @Nullable
    @Override
    public Void execute(final Input values, final ExecutionContext context) throws Exception {
        values.map.layers = new PJoinedArray(new PArray[]{values.map.layers, values.backgroundLayers.layers});
        values.map.postConstruct();
        return null;
    }

    /**
     * The object containing the values required for this processor.
     */
    public static class Input {

        /**
         * The map to update with the background layers.
         */
        public MapAttributeValues map;

        /**
         * The attribute containing the background layers to add to the map.
         */
        public BackgroundLayersAttribute.BackgroundLayersAttributeValue backgroundLayers;
    }
}
