package org.mapfish.print.map.geotools.grid;

import org.geotools.data.FeatureSource;
import org.geotools.data.collection.CollectionFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.styling.Style;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.mapfish.print.Constants.Style.Grid;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.FeatureSourceSupplier;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.operation.MathTransform;

import java.awt.geom.AffineTransform;
import javax.annotation.Nonnull;

/**
 * Strategy for creating the style and features for the grid when the grid consists of lines.
 */
final class LineGridStrategy implements GridType.GridTypeStrategy {
    @Override
    public Style defaultStyle(final Template template, final GridParam layerData) {
        return LineGridStyle.get(layerData);
    }

    @Override
    public FeatureSourceSupplier createFeatureSource(
            final Template template,
            final GridParam layerData,
            final LabelPositionCollector labels) {
        return new FeatureSourceSupplier() {
            @Nonnull
            @Override
            public FeatureSource load(
                    @Nonnull final MfClientHttpRequestFactory requestFactory,
                    @Nonnull final MapfishMapContext mapContext) {
                SimpleFeatureType featureType = GridUtils.createGridFeatureType(mapContext, LineString.class);
                SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
                final DefaultFeatureCollection features;
                if (layerData.numberOfLines != null) {
                    features = createFeaturesFromNumberOfLines(mapContext, featureBuilder, layerData, labels);
                } else {
                    features = createFeaturesFromSpacing(mapContext, featureBuilder, layerData, labels);
                }
                return new CollectionFeatureSource(features);
            }
        };
    }

    @Nonnull
    private DefaultFeatureCollection createFeaturesFromNumberOfLines(
            @Nonnull final MapfishMapContext mapContext,
            @Nonnull final SimpleFeatureBuilder featureBuilder,
            @Nonnull final GridParam layerData,
            @Nonnull final LabelPositionCollector labels) {

        ReferencedEnvelope bounds = mapContext.toReferencedEnvelope();

        final double xSpace = bounds.getWidth() / (layerData.numberOfLines[0] + 1);
        final double ySpace = bounds.getHeight() / (layerData.numberOfLines[1] + 1);
        double minX = bounds.getMinimum(0) + xSpace;
        double minY = bounds.getMinimum(1) + ySpace;

        return sharedCreateFeatures(labels, featureBuilder, layerData, mapContext, xSpace, ySpace, minX,
                                    minY);
    }

    @Nonnull
    private DefaultFeatureCollection createFeaturesFromSpacing(
            @Nonnull final MapfishMapContext mapContext,
            @Nonnull final SimpleFeatureBuilder featureBuilder,
            @Nonnull final GridParam layerData,
            @Nonnull final LabelPositionCollector labels) {

        ReferencedEnvelope bounds = mapContext.toReferencedEnvelope();

        final double xSpace = layerData.spacing[0];
        final double ySpace = layerData.spacing[1];
        double minX = GridUtils.calculateFirstLine(bounds, layerData, 0);
        double minY = GridUtils.calculateFirstLine(bounds, layerData, 1);

        return sharedCreateFeatures(labels, featureBuilder, layerData, mapContext, xSpace, ySpace, minX,
                                    minY);
    }

    // CSOFF: ParameterNumber
    private DefaultFeatureCollection sharedCreateFeatures(
            final LabelPositionCollector labels,
            final SimpleFeatureBuilder featureBuilder,
            final GridParam layerData,
            final MapfishMapContext mapContext,
            final double xSpace, final double ySpace,
            final double minX, final double minY) {
        // CSON: ParameterNumber
        GeometryFactory geometryFactory = new GeometryFactory();

        ReferencedEnvelope bounds = mapContext.toReferencedEnvelope();

        CoordinateReferenceSystem mapCrs = bounds.getCoordinateReferenceSystem();
        String unit = layerData.calculateLabelUnit(mapCrs);
        MathTransform labelTransform = layerData.calculateLabelTransform(mapCrs);

        final AxisDirection direction =
                bounds.getCoordinateReferenceSystem().getCoordinateSystem().getAxis(0).getDirection();
        int numDimensions = bounds.getCoordinateReferenceSystem().getCoordinateSystem().getDimension();
        Polygon rotatedBounds = GridUtils.calculateBounds(mapContext);
        AffineTransform worldToScreenTransform = GridUtils.getWorldToScreenTransform(mapContext);

        DefaultFeatureCollection features = new DefaultFeatureCollection();

        double pointSpacing = bounds.getSpan(1) / layerData.pointsInLine;
        int i = 0;
        for (double x = minX; x < bounds.getMaxX(); x += xSpace) {
            i++;
            final SimpleFeature feature = createFeature(featureBuilder, geometryFactory, layerData,
                                                        direction, numDimensions, pointSpacing, x,
                                                        bounds.getMinimum(1), i, 1);
            features.add(feature);
            GridUtils.topBorderLabel(labels, geometryFactory, rotatedBounds, unit, x,
                                     worldToScreenTransform, labelTransform, layerData.getGridLabelFormat());
            GridUtils.bottomBorderLabel(labels, geometryFactory, rotatedBounds, unit, x,
                                        worldToScreenTransform, labelTransform,
                                        layerData.getGridLabelFormat());
        }

        pointSpacing = bounds.getSpan(0) / layerData.pointsInLine;
        int j = 0;
        for (double y = minY; y < bounds.getMaxY(); y += ySpace) {
            j++;
            final SimpleFeature feature = createFeature(featureBuilder, geometryFactory, layerData,
                                                        direction, numDimensions, pointSpacing,
                                                        bounds.getMinimum(0), y, j, 0);
            features.add(feature);
            GridUtils.rightBorderLabel(labels, geometryFactory, rotatedBounds, unit, y,
                                       worldToScreenTransform, labelTransform,
                                       layerData.getGridLabelFormat());
            GridUtils.leftBorderLabel(labels, geometryFactory, rotatedBounds, unit, y,
                                      worldToScreenTransform, labelTransform, layerData.getGridLabelFormat());

        }

        return features;
    }

    // CSOFF: ParameterNumber
    private SimpleFeature createFeature(
            final SimpleFeatureBuilder featureBuilder,
            final GeometryFactory geometryFactory,
            final GridParam layerData,
            final AxisDirection direction,
            final int numDimensions,
            final double spacing,
            final double x, final double y,
            final int i,
            final int ordinate) {
        // CSON: ParameterNumber

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
        featureBuilder.set(Grid.ATT_GEOM, geom);

        return featureBuilder.buildFeature("grid." + (ordinate == 1 ? 'x' : 'y') + "." + i);
    }
}
