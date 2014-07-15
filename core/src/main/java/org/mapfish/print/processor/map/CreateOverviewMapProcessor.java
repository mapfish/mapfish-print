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
import org.mapfish.print.attribute.map.MapAttribute.OverridenMapAttributeValues;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.OverviewMapAttribute;
import org.mapfish.print.map.geotools.FeatureLayer;
import org.mapfish.print.map.geotools.FeatureLayer.FeatureLayerParam;
import org.mapfish.print.processor.AbstractProcessor;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Processor to create overview maps. Internally {@link CreateMapProcessor} is used.
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
        
        MapAttribute.OverridenMapAttributeValues mapParams = values.map.getWithOverrides(values.overviewMap);
        mapProcessorValues.map = mapParams;

        // TODO validate parameters (dpi? mapParams.postConstruct())
        
        MapBounds originalBounds = mapParams.getOriginalBounds();
        setZoomedOutBounds(mapParams, originalBounds, values);
        setOriginalMapExtentLayer(originalBounds, values, mapParams);

        CreateMapProcessor.Output output = this.mapProcessor.execute(mapProcessorValues, context);
        return new Output(output.layerGraphics, output.mapSubReport);
    }

    private void setOriginalMapExtentLayer(final MapBounds originalBounds,
            final Input values,
            final MapAttribute.OverridenMapAttributeValues mapParams)
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
            final OverridenMapAttributeValues mapParams, final String style,
            final CoordinateReferenceSystem crs) throws IOException {
        FeatureLayerParam layerParams = new FeatureLayerParam();
        layerParams.style = style;
        layerParams.defaultStyle = Constants.OVERVIEWMAP_STYLE_NAME;
        // TODO make this configurable?
        layerParams.renderAsSvg = false;
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
            final MapAttribute.OverridenMapAttributeValues mapParams,
            final MapBounds originalBounds, final Input values) {
        // zoom-out the bounds by the given factor
        MapBounds overviewMapBounds = originalBounds.zoomOut(values.overviewMap.getZoomFactor());

        // adjust the bounds to size of the overview map, because the overview map
        // might have a different aspect ratio than the main map
        overviewMapBounds = overviewMapBounds.adjustedEnvelope(new Rectangle(values.overviewMap.getMapSize()));
        mapParams.setZoomedOutBounds(overviewMapBounds);
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
