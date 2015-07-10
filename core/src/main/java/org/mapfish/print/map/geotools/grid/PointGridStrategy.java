package org.mapfish.print.map.geotools.grid;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
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

import javax.annotation.Nonnull;

/**
 * Strategy for creating the style and features for the grid when the grid consists of lines.
 *
 * @author Jesse on 6/29/2015.
 */
class PointGridStrategy implements GridType.GridTypeStrategy {

    public static final int NINTY_DEGREES = 90;
    public static final int TEXT_DISPLACEMENT = 10;

    @Override
    public Style defaultStyle(final Template template, final GridParam layerData) {
        return template.getConfiguration().getDefaultStyle(Grid.NAME_POINTS);
    }

    @Override
    public FeatureSourceSupplier createFeatureSource(final Template template, final GridParam layerData) {
        return new FeatureSourceSupplier() {
            @Nonnull
            @Override
            public FeatureSource load(@Nonnull final MfClientHttpRequestFactory requestFactory,
                                      @Nonnull final MapfishMapContext mapContext) {
                SimpleFeatureType featureType = GridUtils.createGridFeatureType(mapContext, Point.class);
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

    private DefaultFeatureCollection createFeaturesFromSpacing(final MapfishMapContext mapContext,
                                                               final SimpleFeatureBuilder featureBuilder,
                                                               final GridParam layerData) {
        GeometryFactory geometryFactory = new GeometryFactory();
        ReferencedEnvelope bounds = mapContext.toReferencedEnvelope();
        String unit = bounds.getCoordinateReferenceSystem().getCoordinateSystem().getAxis(0).getUnit().toString();

        final double incrementX = layerData.spacing[0];
        final double incrementY = layerData.spacing[1];
        double minX = GridUtils.calculateFirstLine(bounds, layerData, 0, TEXT_DISPLACEMENT);
        double minY = GridUtils.calculateFirstLine(bounds, layerData, 1, TEXT_DISPLACEMENT);

        DefaultFeatureCollection features = new DefaultFeatureCollection();
        int i = 0;
        int j;

        boolean addBorderFeatures = true;
        for (double x = minX; x < bounds.getMaxX(); x += incrementX) {
            i++;
            j = 0;

            if (!onRightBorder(bounds, x)) { // don't add the border features twice.
                features.add(bottomBorderFeature(featureBuilder, geometryFactory, "grid." + i + ".top", unit, x, bounds.getMinY()));
                features.add(topBorderFeature(featureBuilder, geometryFactory, "grid." + i + ".bottom", unit, x, bounds.getMaxY()));
            }
            for (double y = minY; y < bounds.getMaxY(); y += incrementY) {
                j++;

                if (addBorderFeatures && !onRightBorder(bounds, x) && !onTopBorder(bounds, y)) {
                    features.add(leftBorderFeature(featureBuilder, geometryFactory, "grid.left." + j, unit, bounds.getMinX(), y));
                    features.add(rightBorderFeature(featureBuilder, geometryFactory, "grid.right." + j, unit, bounds.getMaxX(), y));
                }
                if (!onTopBorder(bounds, y) && !onBottomBorder(bounds, y) &&
                    !onLeftBorder(bounds, x) && !onRightBorder(bounds, x)) { // don't add the border features twice.
                    zeroFeatureBuilder(featureBuilder);
                    Point geom = geometryFactory.createPoint(new Coordinate(x, y));
                    featureBuilder.set(Grid.ATT_GEOM, geom);
                    features.add(featureBuilder.buildFeature("grid." + i + "." + j));
                }
            }
            addBorderFeatures = false;
        }
        return features;
    }

    private boolean onRightBorder(final ReferencedEnvelope bounds, final double x) {
        return x >= bounds.getMaxX() - TEXT_DISPLACEMENT;
    }

    private boolean onLeftBorder(final ReferencedEnvelope bounds, final double x) {
        return x <= bounds.getMinX() + TEXT_DISPLACEMENT;
    }

    private boolean onBottomBorder(final ReferencedEnvelope bounds, final double y) {
        return y <= bounds.getMinY() + TEXT_DISPLACEMENT;
    }

    private boolean onTopBorder(final ReferencedEnvelope bounds, final double y) {
        return y >= bounds.getMaxY() - TEXT_DISPLACEMENT;
    }

    private DefaultFeatureCollection createFeaturesFromNumberOfLines(final MapfishMapContext mapContext,
                                                                     final SimpleFeatureBuilder featureBuilder,
                                                                     final GridParam layerData) {
        GeometryFactory geometryFactory = new GeometryFactory();
        ReferencedEnvelope bounds = mapContext.toReferencedEnvelope();
        String unit = bounds.getCoordinateReferenceSystem().getCoordinateSystem().getAxis(0).getUnit().toString();
        double incrementX = bounds.getWidth() / (layerData.numberOfLines[0] + 1);
        double incrementY = bounds.getHeight() / (layerData.numberOfLines[1] + 1);

        double x = bounds.getMinX();
        DefaultFeatureCollection features = new DefaultFeatureCollection();
        for (int i = 0; i < layerData.numberOfLines[0] + 2; i++) {
            double y = bounds.getMinY();
            for (int j = 0; j < layerData.numberOfLines[1] + 2; j++) {
                String fid = "grid." + i + "." + j;
                if ((i != 0 || j != 0) && (i != layerData.numberOfLines[0] + 1 || j != layerData.numberOfLines[1] + 1) &&
                    (i != 0 || j != layerData.numberOfLines[1] + 1) && (i != layerData.numberOfLines[0] + 1 || j != 0)) {

                    if (i == 0) {
                        features.add(leftBorderFeature(featureBuilder, geometryFactory, fid, unit, bounds.getMinX(), y));
                    } else if (i == layerData.numberOfLines[0] + 1) {
                        features.add(rightBorderFeature(featureBuilder, geometryFactory, fid, unit, bounds.getMaxX(), y));
                    } else if (j == 0) {
                        features.add(bottomBorderFeature(featureBuilder, geometryFactory, fid, unit, x, bounds.getMinY()));
                    } else if (j == layerData.numberOfLines[1] + 1) {
                        features.add(topBorderFeature(featureBuilder, geometryFactory, fid, unit, x, bounds.getMaxY()));
                    } else {
                        zeroFeatureBuilder(featureBuilder);
                        Point geom = geometryFactory.createPoint(new Coordinate(x, y));
                        featureBuilder.set(Grid.ATT_GEOM, geom);
                        features.add(featureBuilder.buildFeature(fid));
                    }
                }
                y += incrementY;
            }
            x += incrementX;
        }
        return features;
    }

    private SimpleFeature topBorderFeature(final SimpleFeatureBuilder featureBuilder, final GeometryFactory geometryFactory,
                                           final String fid, final String unit, final double x, final double maxY) {
        zeroFeatureBuilder(featureBuilder);
        featureBuilder.set(Grid.ATT_ROTATION, -NINTY_DEGREES);
        featureBuilder.set(Grid.ATT_ANCHOR_X, 1.0);
        featureBuilder.set(Grid.ATT_X_DISPLACEMENT, -TEXT_DISPLACEMENT);
        featureBuilder.set(Grid.ATT_LABEL, GridUtils.createLabel(x, unit));
        Point geom = geometryFactory.createPoint(new Coordinate(x, maxY - TEXT_DISPLACEMENT));
        featureBuilder.set(Grid.ATT_GEOM, geom);
        return featureBuilder.buildFeature(fid);
    }

    private SimpleFeature bottomBorderFeature(final SimpleFeatureBuilder featureBuilder, final GeometryFactory geometryFactory,
                                              final String fid, final String unit, final double x, final double minY) {
        zeroFeatureBuilder(featureBuilder);
        featureBuilder.set(Grid.ATT_ROTATION, -NINTY_DEGREES);
        featureBuilder.set(Grid.ATT_ANCHOR_X, 0.0);
        featureBuilder.set(Grid.ATT_X_DISPLACEMENT, TEXT_DISPLACEMENT);
        featureBuilder.set(Grid.ATT_LABEL, GridUtils.createLabel(x, unit));
        Point geom = geometryFactory.createPoint(new Coordinate(x, minY + TEXT_DISPLACEMENT));
        featureBuilder.set(Grid.ATT_GEOM, geom);
        return featureBuilder.buildFeature(fid);
    }

    private SimpleFeature rightBorderFeature(final SimpleFeatureBuilder featureBuilder, final GeometryFactory geometryFactory,
                                             final String fid, final String unit, final double maxX, final double y) {
        zeroFeatureBuilder(featureBuilder);
        featureBuilder.set(Grid.ATT_LABEL, GridUtils.createLabel(y, unit));
        featureBuilder.set(Grid.ATT_X_DISPLACEMENT, -TEXT_DISPLACEMENT);
        featureBuilder.set(Grid.ATT_ANCHOR_X, 1);
        Point geom = geometryFactory.createPoint(new Coordinate(maxX - TEXT_DISPLACEMENT, y));
        featureBuilder.set(Grid.ATT_GEOM, geom);
        return featureBuilder.buildFeature(fid);
    }

    private SimpleFeature leftBorderFeature(final SimpleFeatureBuilder featureBuilder, final GeometryFactory geometryFactory,
                                            final String fid, final String unit, final double minX, final double y) {
        zeroFeatureBuilder(featureBuilder);
        featureBuilder.set(Grid.ATT_LABEL, GridUtils.createLabel(y, unit));
        featureBuilder.set(Grid.ATT_X_DISPLACEMENT, TEXT_DISPLACEMENT);
        featureBuilder.set(Grid.ATT_ANCHOR_X, 0);
        Point geom = geometryFactory.createPoint(new Coordinate(minX + TEXT_DISPLACEMENT, y));
        featureBuilder.set(Grid.ATT_GEOM, geom);
        return featureBuilder.buildFeature(fid);
    }

    private void zeroFeatureBuilder(final SimpleFeatureBuilder featureBuilder) {
        featureBuilder.reset();
        featureBuilder.set(Grid.ATT_ANCHOR_X, 0);
        featureBuilder.set(Grid.ATT_X_DISPLACEMENT, 0);
        featureBuilder.set(Grid.ATT_Y_DISPLACEMENT, 0);
        featureBuilder.set(Grid.ATT_ROTATION, 0);
        featureBuilder.set(Grid.ATT_LABEL, "");
    }
}
