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

package org.mapfish.print.map.geotools.grid;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import jsr166y.ForkJoinPool;
import org.geotools.data.FeatureSource;
import org.geotools.data.collection.CollectionFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.styling.Style;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.AbstractFeatureSourceLayerPlugin;
import org.mapfish.print.map.geotools.FeatureSourceSupplier;
import org.mapfish.print.map.geotools.StyleSupplier;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;

/**
 * The plugin for creating the grid layer.
 */
public final class GridLayerPlugin extends AbstractFeatureSourceLayerPlugin<GridParam> {

    private static final String TYPE = "grid";
    @Autowired
    private ForkJoinPool pool;

    /**
     * Constructor.
     */
    public GridLayerPlugin() {
        super(TYPE);
    }

    @Override
    public GridParam createParameter() {
        return new GridParam();
    }

    @Nonnull
    @Override
    public GridLayer parse(@Nonnull final Template template, @Nonnull final GridParam layerData) throws Throwable {

        FeatureSourceSupplier featureSource = createFeatureSourceFunction(template, layerData);
        final StyleSupplier<FeatureSource> styleFunction = createStyleSupplier(template, layerData);
        return new GridLayer(this.pool, featureSource, styleFunction,
                template.getConfiguration().renderAsSvg(layerData.renderAsSvg));
    }

    private StyleSupplier<FeatureSource> createStyleSupplier(final Template template, final GridParam layerData) {

        return new StyleSupplier<FeatureSource>() {
            @Override
            public Style load(final MfClientHttpRequestFactory requestFactory,
                              final FeatureSource featureSource,
                              final MapfishMapContext mapContext) {
                String styleRef = layerData.style;
                return template.getStyle(styleRef, mapContext)
                        .or(GridLayerPlugin.super.parser.loadStyle(
                                template.getConfiguration(),
                                requestFactory, styleRef, mapContext))
                        .or(template.getConfiguration().getDefaultStyle(Constants.Style.Grid.NAME));
            }
        };
    }

    private FeatureSourceSupplier createFeatureSourceFunction(final Template template,
                                                              final GridParam layerData) {

        return new FeatureSourceSupplier() {
            @Nonnull
            @Override
            public FeatureSource load(@Nonnull final MfClientHttpRequestFactory requestFactory,
                                      @Nonnull final MapfishMapContext mapContext) {
                final SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
                CoordinateReferenceSystem projection = mapContext.getBounds().getProjection();
                typeBuilder.add(Constants.Style.Grid.ATT_GEOM, LineString.class, projection);
                typeBuilder.add(Constants.Style.Grid.ATT_LABEL, String.class);
                typeBuilder.add(Constants.Style.Grid.ATT_ROTATION, Double.class);
                typeBuilder.add(Constants.Style.Grid.ATT_X_DISPLACEMENT, Double.class);
                typeBuilder.add(Constants.Style.Grid.ATT_Y_DISPLACEMENT, Double.class);
                typeBuilder.setName(Constants.Style.Grid.NAME);

                SimpleFeatureType featureType = typeBuilder.buildFeatureType();
                SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
                final DefaultFeatureCollection features;
                if (layerData.numberOfLines != null) {
                    features = createFeaturesFromNumberOfLines(mapContext, featureBuilder, layerData);
                } else {
                    features = createFeaturesFromSpacing(mapContext, featureBuilder, layerData);
                }
                return new CollectionFeatureSource(features);
            }
        };
    }

    @Nonnull
    private DefaultFeatureCollection createFeaturesFromNumberOfLines(@Nonnull final MapfishMapContext mapContext,
                                                                     @Nonnull final SimpleFeatureBuilder featureBuilder,
                                                                     @Nonnull final GridParam layerData) {

        ReferencedEnvelope bounds = mapContext.toReferencedEnvelope();

        final double xSpace = bounds.getWidth() / (layerData.numberOfLines[0] + 1);
        final double ySpace = bounds.getHeight() / (layerData.numberOfLines[1] + 1);
        double minX = bounds.getMinimum(0) + xSpace;
        double minY = bounds.getMinimum(1) + ySpace;

        return sharedCreateFeatures(featureBuilder, layerData, mapContext, xSpace, ySpace, minX, minY);
    }

    @Nonnull
    private DefaultFeatureCollection createFeaturesFromSpacing(@Nonnull final MapfishMapContext mapContext,
                                                               @Nonnull final SimpleFeatureBuilder featureBuilder,
                                                               @Nonnull final GridParam layerData) {

        ReferencedEnvelope bounds = mapContext.toReferencedEnvelope();

        final double xSpace = layerData.spacing[0];
        final double ySpace = layerData.spacing[1];
        double minX = calculateFirstLine(bounds, layerData, 0);
        double minY = calculateFirstLine(bounds, layerData, 1);

        return sharedCreateFeatures(featureBuilder, layerData, mapContext, xSpace, ySpace, minX, minY);
    }

    private double calculateFirstLine(final ReferencedEnvelope bounds,
                                      final GridParam layerData,
                                      final int ordinal) {
        double spaceFromOrigin = bounds.getMinimum(ordinal) - layerData.origin[ordinal];
        double linesBetweenOriginAndMap = Math.ceil(spaceFromOrigin / layerData.spacing[ordinal]);

        return linesBetweenOriginAndMap * layerData.spacing[ordinal] + layerData.origin[ordinal];
    }

    private DefaultFeatureCollection sharedCreateFeatures(final SimpleFeatureBuilder featureBuilder,
                                                          final GridParam layerData,
                                                          final MapfishMapContext mapContext,
                                                          final double xSpace,
                                                          final double ySpace,
                                                          final double minX,
                                                          final double minY) {
        GeometryFactory geometryFactory = new GeometryFactory();

        ReferencedEnvelope bounds = mapContext.toReferencedEnvelope();

        String unit = bounds.getCoordinateReferenceSystem().getCoordinateSystem().getAxis(0).getUnit().toString();
        final AxisDirection direction = bounds.getCoordinateReferenceSystem().getCoordinateSystem().getAxis(0).getDirection();
        int numDimensions = bounds.getCoordinateReferenceSystem().getCoordinateSystem().getDimension();

        DefaultFeatureCollection features = new DefaultFeatureCollection();

        double pointSpacing = bounds.getSpan(1) / layerData.pointsInLine;
        int i = 0;
        for (double x = minX; x < bounds.getMaxX(); x += xSpace) {
            i++;
            final SimpleFeature feature = createFeature(mapContext, featureBuilder, geometryFactory, layerData,
                    unit, direction, numDimensions, pointSpacing, x, bounds.getMinimum(1), i, 1);
            features.add(feature);
        }

        pointSpacing = bounds.getSpan(0) / layerData.pointsInLine;
        int j = 0;
        for (double y = minY; y < bounds.getMaxY(); y += ySpace) {
            j++;
            final SimpleFeature feature = createFeature(mapContext, featureBuilder, geometryFactory, layerData,
                    unit, direction, numDimensions, pointSpacing, bounds.getMinimum(0), y, j, 0);
            features.add(feature);
        }

        return features;
    }

    // CHECKSTYLE:OFF
    private org.opengis.feature.simple.SimpleFeature createFeature(final MapfishMapContext mapContext,
                                                                   final SimpleFeatureBuilder featureBuilder,
                                                                   final GeometryFactory geometryFactory,
                                                                   final GridParam layerData,
                                                                   final String unit,
                                                                   final AxisDirection direction,
                                                                   final int numDimensions,
                                                                   final double spacing,
                                                                   final double x,
                                                                   final double y,
                                                                   final int i,
                                                                   final int ordinate) {
        featureBuilder.reset();
        final int numPoints = layerData.pointsInLine + 1; // add 1 for the last point
        final LinearCoordinateSequence coordinateSequence = new LinearCoordinateSequence().
                setDimension(numDimensions).
                setOrigin(x, y).
                setVariableAxis(ordinate).
                setNumPoints(numPoints).
                setSpacing(spacing).
                setOrdinate0AxisDirection(direction);
        LineString geom = geometryFactory.createLineString(coordinateSequence);
        featureBuilder.add(geom);
        featureBuilder.add(createLabel(ordinate == 1 ? x : y, unit));

        int indentAmount = (int) (mapContext.getDPI() / 8); // 1/8 inch indent
        if (ordinate == 0) {
            featureBuilder.add(0); // rotation
            featureBuilder.add(-(mapContext.getMapSize().width / 2) + indentAmount); // x displacement
            featureBuilder.add(0); // y displacement
        } else {
            featureBuilder.add(0); // rotation
            featureBuilder.add(0); // x displacement
            featureBuilder.add(-(mapContext.getMapSize().height / 2) + indentAmount); // y1displacement
        }

        return featureBuilder.buildFeature("grid." + (ordinate == 1 ? 'x' : 'y') + "." + i);
    }
    // CHECKSTYLE:ON

    private String createLabel(final double x, final String unit) {
        final double zero = 0.000000001;
        final int maxBeforeNoDecimals = 1000000;
        final double minBeforeScientific = 0.0001;
        final int maxWithDecimals = 1000;

        if (Math.abs(x - Math.round(x)) < zero) {
            return String.format("%d %s", Math.round(x), unit);
        } else {
            if (x > maxBeforeNoDecimals || x < minBeforeScientific) {
                return String.format("%1.0f %s", x, unit);
            } else if (x < maxWithDecimals) {
                return String.format("%f1.2 %s", x, unit);
            } else if (x > minBeforeScientific) {
                return String.format("%1.4f %s", x, unit);
            } else {
                return String.format("%e %s", x, unit);
            }
        }
    }

}
