package org.mapfish.print.map.geotools.grid;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import org.geotools.data.FeatureSource;
import org.geotools.data.collection.CollectionFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.styling.Style;
import org.mapfish.print.Constants.Style.Grid;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.FeatureSourceSupplier;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.cs.AxisDirection;

import javax.annotation.Nonnull;

/**
 * Strategy for creating the style and features for the grid when the grid consists of lines.
 *
 * @author Jesse on 6/29/2015.
 */
final class LineGridStrategy implements GridType.GridTypeStrategy {
    @Override
    public Style defaultStyle(final Template template, final GridParam layerData) {
        return template.getConfiguration().getDefaultStyle(Grid.NAME_LINES);
    }

    @Override
    public FeatureSourceSupplier createFeatureSource(final Template template, final GridParam layerData) {
        return new FeatureSourceSupplier() {
            @Nonnull
            @Override
            public FeatureSource load(@Nonnull final MfClientHttpRequestFactory requestFactory,
                                      @Nonnull final MapfishMapContext mapContext) {
                SimpleFeatureType featureType = GridType.createGridFeatureType(mapContext, LineString.class);
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

    static double calculateFirstLine(final ReferencedEnvelope bounds,
                                      final GridParam layerData,
                                      final int ordinal) {
        return calculateFirstLine(bounds, layerData, ordinal, 0);
    }
    static double calculateFirstLine(final ReferencedEnvelope bounds,
                                     final GridParam layerData,
                                     final int ordinal,
                                     final int indent) {
        double spaceFromOrigin = bounds.getMinimum(ordinal) + indent - layerData.origin[ordinal];
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
        featureBuilder.set(Grid.ATT_GEOM, geom);
        featureBuilder.set(Grid.ATT_LABEL, GridType.createLabel(ordinate == 1 ? x : y, unit));

        int indentAmount = (int) (mapContext.getDPI() / 8); // 1/8 inch indent
        if (ordinate == 0) {
            featureBuilder.set(Grid.ATT_ROTATION, 0);
            featureBuilder.set(Grid.ATT_X_DISPLACEMENT,  - (mapContext.getMapSize().width / 2) + indentAmount);
            featureBuilder.set(Grid.ATT_Y_DISPLACEMENT, 0);
        } else {
            featureBuilder.set(Grid.ATT_ROTATION, 270);
            featureBuilder.set(Grid.ATT_X_DISPLACEMENT, -(mapContext.getMapSize().height / 2) + indentAmount);
            featureBuilder.set(Grid.ATT_Y_DISPLACEMENT, 0);
        }
        featureBuilder.set(Grid.ATT_ANCHOR_X, 0);

        return featureBuilder.buildFeature("grid." + (ordinate == 1 ? 'x' : 'y') + "." + i);
    }
    // CHECKSTYLE:ON

}
