package org.mapfish.print.processor.map;


import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.OverviewMapAttribute;
import org.mapfish.print.processor.AbstractProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.awt.Rectangle;
import java.io.File;
import java.net.URI;
import java.util.List;

public class CreateOverviewMapProcessor extends AbstractProcessor<CreateOverviewMapProcessor.Input, CreateOverviewMapProcessor.Output> {

    @Autowired
    private CreateMapProcessor mapProcessor;
    
    /**
     * Constructor.
     */
    public CreateOverviewMapProcessor() {
        super(Output.class);
    }
    
    @Override
    public final Input createInputParameter() {
        return new Input();
    }

    @Override
    public final Output execute(final Input values, final ExecutionContext context)
            throws Exception {
        CreateMapProcessor.Input mapProcessorValues = this.mapProcessor.createInputParameter();
        mapProcessorValues.clientHttpRequestFactory = values.clientHttpRequestFactory;
        mapProcessorValues.tempTaskDirectory = values.tempTaskDirectory;
        
        MapAttribute.OverridenMapAttributeValues mapParams = values.map.getWithOverrides(values.overviewMap);
        mapProcessorValues.map = mapParams;

        // TODO validate parameters (dpi? mapParams.postConstruct())
        
        // zoom-out the bounds by the given factor
        final MapBounds originalBounds = mapParams.getOriginalBounds();
        MapBounds overviewMapBounds = originalBounds.zoomOut(values.overviewMap.getZoomFactor());
        
        // adjust the bounds to size of the overview map, because the overview map
        // might have a different aspect ratio than the main map
        overviewMapBounds = overviewMapBounds.adjustedEnvelope(new Rectangle(values.overviewMap.getMapSize()));
        mapParams.setZoomedOutBounds(overviewMapBounds);
        
        // TODO reset rotation
        // TODO add layer with box

        CreateMapProcessor.Output output = this.mapProcessor.execute(mapProcessorValues, context);
        return new Output(output.layerGraphics, output.mapSubReport);
    }

    @Override
    protected final void extraValidation(final List<Throwable> validationErrors) {
        this.mapProcessor.extraValidation(validationErrors);
    }

    /**
     * The Input object for the processor.
     */
    public static final class Input {
        /**
         * A factory for making http requests.  This is added to the values by the framework and therefore
         * does not need to be set in configuration
         */
        public ClientHttpRequestFactory clientHttpRequestFactory;

        /**
         * The required parameters for the main map for which the overview
         * will be created.
         */
        public MapAttribute.MapAttributeValues map;

        /**
         * Optional parameters for the overview map which allow to override
         * parameters of the main map.
         */
        public OverviewMapAttribute.OverviewMapAttributeValues overviewMap;
        
        /**
         * The path to the temporary directory for the print task.
         */
        public File tempTaskDirectory;
    }

    /**
     * Output for the processor.
     */
    public static final class Output {

        /**
         * The paths to a graphic for each layer.
         */
        public final List<URI> layerGraphics;
        
        /**
         * The path to the compiled sub-report for the overview map.
         */
        public final String overviewMapSubReport;

        private Output(final List<URI> layerGraphics, final String overviewMapSubReport) {
            this.layerGraphics = layerGraphics;
            this.overviewMapSubReport = overviewMapSubReport;
        }
    }
}
