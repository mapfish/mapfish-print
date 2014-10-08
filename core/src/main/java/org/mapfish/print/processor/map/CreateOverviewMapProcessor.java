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

package org.mapfish.print.processor.map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.mapfish.print.Constants;
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
 * <p/>
 * Example Configuration:
 * <pre><code>
 * attributes:
 *    ...
 *    overviewMapDef: !overviewMap
 *      width: 300
 *      height: 200
 *      maxDpi: 400
 * processors:
 *    ...
 *    - !createOverviewMap
 *      inputMapper: {
 *        mapDef: map,
 *        overviewMapDef: overviewMap
 *      }
 *      outputMapper: {
 *        overviewMapSubReport: overviewMapOut,
 *        layerGraphics: overviewMapLayerGraphics
 *      }
 * </code></pre>
 * <p/>
 * <strong>Features:</strong>
 * <p/>
 * The attribute overviewMap allows to overwrite all properties of the main map, for example to use different layers.
 * The overview map can have a different rotation than the main map. For example the main map is rotated and the overview map faces
 * north. But the overview map can also be rotated.
 * <p/>
 * The style of the bbox rectangle can be changed.
 */
public class CreateOverviewMapProcessor extends AbstractProcessor<CreateOverviewMapProcessor.Input, CreateOverviewMapProcessor.Output> {

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
        mapProcessorValues.clientHttpRequestFactory = values.clientHttpRequestFactory;
        mapProcessorValues.tempTaskDirectory = values.tempTaskDirectory;
        
        MapAttribute.OverriddenMapAttributeValues mapParams = values.map.getWithOverrides(values.overviewMap);
        mapProcessorValues.map = mapParams;

        // TODO validate parameters (dpi? mapParams.postConstruct())

        // NOTE: Original map is the map that is the "subject/target" of this overview map
        MapBounds boundsOfOriginalMap = mapParams.getOriginalBounds();
        setOriginalMapExtentLayer(boundsOfOriginalMap, values, mapParams);
        setZoomedOutBounds(mapParams, boundsOfOriginalMap, values);

        CreateMapProcessor.Output output = this.mapProcessor.execute(mapProcessorValues, context);
        return new Output(output.layerGraphics, output.mapSubReport);
    }

    private void setOriginalMapExtentLayer(final MapBounds originalBounds,
            final Input values,
            final MapAttribute.OverriddenMapAttributeValues mapParams)
            throws IOException {
        Rectangle originalPaintArea = new Rectangle(values.map.getMapSize());
        MapBounds adjustedBounds =
                CreateMapProcessor.adjustBoundsToScaleAndMapSize(values.map, values.map.getDpi(), originalPaintArea, originalBounds);
        ReferencedEnvelope originalEnvelope =
                adjustedBounds.toReferencedEnvelope(originalPaintArea, values.map.getDpi());

        Geometry mapExtent = JTS.toGeometry(originalEnvelope);
        if (values.map.getRotation() != 0.0) {
            mapExtent = rotateExtent(mapExtent, values.map.getRotation(), originalEnvelope);
        }
        
        FeatureLayer layer = createOrignalMapExtentLayer(mapExtent, mapParams,
                values.overviewMap.getStyle(), originalEnvelope.getCoordinateReferenceSystem());
        mapParams.setMapExtentLayer(layer);
    }

    private Geometry rotateExtent(final Geometry mapExtent, final double rotation,
            final ReferencedEnvelope originalEnvelope) {
        final Coordinate center = originalEnvelope.centre();
        final AffineTransform affineTransform = AffineTransform.getRotateInstance(
                Math.toRadians(rotation), center.x, center.y);
        final MathTransform mathTransform = new AffineTransform2D(affineTransform);

        try {
            return JTS.transform(mapExtent, mathTransform);
        } catch (TransformException e) {
            throw new RuntimeException("Failed to rotate map extent", e);
        }
    }

    private FeatureLayer createOrignalMapExtentLayer(final Geometry mapExtent,
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

    private void setZoomedOutBounds(
            final MapAttribute.OverriddenMapAttributeValues mapParams,
            final MapBounds originalBounds, final Input values) {
        // zoom-out the bounds by the given factor
        MapBounds overviewMapBounds = originalBounds.zoomOut(values.overviewMap.getZoomFactor());

        // adjust the bounds to size of the overview map, because the overview map
        // might have a different aspect ratio than the main map
        overviewMapBounds = overviewMapBounds.adjustedEnvelope(new Rectangle(values.overviewMap.getMapSize()));
        mapParams.setZoomedOutBounds(overviewMapBounds);
    }

    @Override
    protected final void extraValidation(final List<Throwable> validationErrors, final Configuration configuration) {
        this.mapProcessor.extraValidation(validationErrors, configuration);
    }

    /**
     * The Input object for the processor.
     */
    public static final class Input extends CreateMapProcessor.Input {
        /**
         * Optional parameters for the overview map which allow to override
         * parameters of the main map.
         */
        public OverviewMapAttribute.OverviewMapAttributeValues overviewMap;
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
