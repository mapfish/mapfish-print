package org.mapfish.print.processor.map;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.mapfish.print.Constants;
import org.mapfish.print.FloatingPointUtil;
import org.mapfish.print.attribute.map.GenericMapAttribute;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.OverviewMapAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.map.geotools.FeatureLayer;
import org.mapfish.print.map.geotools.FeatureLayer.FeatureLayerParam;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.InternalValue;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Processor to create overview maps. Internally {@link CreateMapProcessor} is used.
 *
 * Example Configuration:
 * <pre><code>
 * attributes:
 *    ...
 *    overviewMap: !overviewMap
 *      width: 300
 *      height: 200
 *      maxDpi: 400
 * processors:
 *    ...
 *    - !createOverviewMap
 *      outputMapper:
 *        layerGraphics: overviewMapLayerGraphics
 * </code></pre>
 *
 * <strong>Features:</strong>
 *
 * The attribute overviewMap allows to overwrite all properties of the main map, for example to use different
 * layers. The overview map can have a different rotation than the main map. For example the main map is
 * rotated and the overview map faces north. But the overview map can also be rotated.
 *
 * <p>The style of the bbox rectangle can be changed by setting the <code>style</code> property.</p>
 * <p>See also: <a href="attributes.html#!overviewMap">!overviewMap</a> attribute</p>
 * [[examples=verboseExample,overviewmap_tyger_ny_EPSG_3857]]
 */
public class CreateOverviewMapProcessor
        extends AbstractProcessor<CreateOverviewMapProcessor.Input, CreateOverviewMapProcessor.Output> {

    @Autowired
    private CreateMapProcessor mapProcessor;

    @Autowired
    private FeatureLayer.Plugin featureLayerParser;

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
        mapProcessorValues.clientHttpRequestFactoryProvider = values.clientHttpRequestFactoryProvider;
        mapProcessorValues.tempTaskDirectory = values.tempTaskDirectory;
        mapProcessorValues.template = values.template;

        MapAttribute.OverriddenMapAttributeValues mapParams =
                ((MapAttribute.MapAttributeValues) values.map).getWithOverrides(
                        (OverviewMapAttribute.OverviewMapAttributeValues) values.overviewMap);
        mapProcessorValues.map = mapParams;

        // TODO validate parameters (dpi? mapParams.postConstruct())

        // NOTE: Original map is the map that is the "subject/target" of this overview map
        MapBounds boundsOfOriginalMap = mapParams.getOriginalBounds();
        setOriginalMapExtentLayer(boundsOfOriginalMap, values, mapParams);
        setOverviewMapBounds(mapParams, boundsOfOriginalMap, values);

        CreateMapProcessor.Output output = this.mapProcessor.execute(mapProcessorValues, context);
        return new Output(output.layerGraphics, output.mapSubReport);
    }

    private void setOriginalMapExtentLayer(
            final MapBounds originalBounds,
            final Input values,
            final MapAttribute.OverriddenMapAttributeValues mapParams)
            throws IOException {
        Rectangle originalPaintArea = new Rectangle(values.map.getMapSize());
        MapBounds adjustedBounds = CreateMapProcessor.adjustBoundsToScaleAndMapSize(
                values.map, originalPaintArea, originalBounds, values.map.getDpi());
        ReferencedEnvelope originalEnvelope =
                adjustedBounds.toReferencedEnvelope(originalPaintArea);

        Geometry mapExtent = JTS.toGeometry(originalEnvelope);
        if (!FloatingPointUtil.equals(values.map.getRotation(), 0.0)) {
            mapExtent = rotateExtent(mapExtent, values.map.getRotation(), originalEnvelope);
        }

        FeatureLayer layer = createOrignalMapExtentLayer(
                mapExtent, mapParams,
                ((OverviewMapAttribute.OverviewMapAttributeValues) values.overviewMap).getStyle(),
                originalEnvelope.getCoordinateReferenceSystem());
        mapParams.setMapExtentLayer(layer);
    }

    private Geometry rotateExtent(
            final Geometry mapExtent, final double rotation,
            final ReferencedEnvelope originalEnvelope) {
        final Coordinate center = originalEnvelope.centre();
        final AffineTransform affineTransform = AffineTransform.getRotateInstance(
                rotation, center.x, center.y);
        final MathTransform mathTransform = new AffineTransform2D(affineTransform);

        try {
            return JTS.transform(mapExtent, mathTransform);
        } catch (TransformException e) {
            throw new RuntimeException("Failed to rotate map extent", e);
        }
    }

    private FeatureLayer createOrignalMapExtentLayer(
            final Geometry mapExtent,
            final MapAttribute.OverriddenMapAttributeValues mapParams, final String style,
            final CoordinateReferenceSystem crs) throws IOException {
        FeatureLayerParam layerParams = new FeatureLayerParam();
        layerParams.style = style;
        layerParams.defaultStyle = Constants.Style.OverviewMap.NAME;
        // TODO make this configurable?
        layerParams.renderAsSvg = null;
        layerParams.features = wrapIntoFeatureCollection(mapExtent, crs);

        return this.featureLayerParser.parse(mapParams.getTemplate(), layerParams);
    }

    private DefaultFeatureCollection wrapIntoFeatureCollection(
            final Geometry mapExtent, final CoordinateReferenceSystem crs) {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("overview-map");
        typeBuilder.setCRS(crs);
        typeBuilder.add("geom", Polygon.class);
        final SimpleFeatureType type = typeBuilder.buildFeatureType();

        DefaultFeatureCollection features = new DefaultFeatureCollection();
        features.add(SimpleFeatureBuilder.build(type, new Object[]{mapExtent}, null));

        return features;
    }

    private void setOverviewMapBounds(
            final MapAttribute.OverriddenMapAttributeValues mapParams,
            final MapBounds originalBounds, final Input values) {
        MapBounds overviewMapBounds;
        if (mapParams.getCustomBounds() != null) {
            overviewMapBounds = mapParams.getCustomBounds();
        } else {
            // zoom-out the original map bounds by the given factor
            overviewMapBounds = originalBounds.zoomOut(
                    ((OverviewMapAttribute.OverviewMapAttributeValues) values.overviewMap).getZoomFactor());
        }

        // adjust the bounds to size of the overview map, because the overview map
        // might have a different aspect ratio than the main map
        overviewMapBounds =
                overviewMapBounds.adjustedEnvelope(new Rectangle(values.overviewMap.getMapSize()));
        mapParams.setZoomedOutBounds(overviewMapBounds);
    }

    @Override
    protected final void extraValidation(
            final List<Throwable> validationErrors, final Configuration configuration) {
        this.mapProcessor.extraValidation(validationErrors, configuration);
    }

    /**
     * The Input object for the processor.
     */
    public static final class Input extends CreateMapProcessor.Input {
        /**
         * Optional parameters for the overview map which allow to override parameters of the main map.
         */
        public GenericMapAttribute.GenericMapAttributeValues overviewMap;
    }

    /**
     * Output for the processor.
     */
    public static final class Output {

        /**
         * The paths to a graphic for each layer.
         */
        @InternalValue
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
