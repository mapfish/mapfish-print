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
                SimpleFeatureType featureType = GridType.createGridFeatureType(mapContext, Point.class);
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
        return null;
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
                featureBuilder.reset();
                if ((i != 0 || j != 0) && (i != layerData.numberOfLines[0] + 1 || j != layerData.numberOfLines[1] + 1) &&
                    (i != 0 || j != layerData.numberOfLines[1] + 1) && (i != layerData.numberOfLines[0] + 1 || j != 0)) {
                    featureBuilder.set(Grid.ATT_ANCHOR_X, 0);
                    featureBuilder.set(Grid.ATT_X_DISPLACEMENT, 0);
                    featureBuilder.set(Grid.ATT_Y_DISPLACEMENT, 0);
                    featureBuilder.set(Grid.ATT_ROTATION, 0);
                    featureBuilder.set(Grid.ATT_LABEL, "");

                    Point geom;
                    if (i == 0) {
                        featureBuilder.set(Grid.ATT_LABEL, GridType.createLabel(y, unit));
                        featureBuilder.set(Grid.ATT_X_DISPLACEMENT, TEXT_DISPLACEMENT);
                        featureBuilder.set(Grid.ATT_ANCHOR_X, 0);
                        geom = geometryFactory.createPoint(new Coordinate(x + TEXT_DISPLACEMENT, y));
                    } else if (i == layerData.numberOfLines[0] + 1) {
                        featureBuilder.set(Grid.ATT_LABEL, GridType.createLabel(y, unit));
                        featureBuilder.set(Grid.ATT_X_DISPLACEMENT, -TEXT_DISPLACEMENT);
                        featureBuilder.set(Grid.ATT_ANCHOR_X, 1);
                        geom = geometryFactory.createPoint(new Coordinate(x - TEXT_DISPLACEMENT, y));
                    } else if (j == 0) {
                        featureBuilder.set(Grid.ATT_ROTATION, -NINTY_DEGREES);
                        featureBuilder.set(Grid.ATT_ANCHOR_X, 0.0);
                        featureBuilder.set(Grid.ATT_X_DISPLACEMENT, TEXT_DISPLACEMENT);
                        featureBuilder.set(Grid.ATT_LABEL, GridType.createLabel(x, unit));
                        geom = geometryFactory.createPoint(new Coordinate(x, y + TEXT_DISPLACEMENT));
                    } else if (j == layerData.numberOfLines[1] + 1) {
                        featureBuilder.set(Grid.ATT_ROTATION, -NINTY_DEGREES);
                        featureBuilder.set(Grid.ATT_ANCHOR_X, 1.0);
                        featureBuilder.set(Grid.ATT_X_DISPLACEMENT, -TEXT_DISPLACEMENT);
                        featureBuilder.set(Grid.ATT_LABEL, GridType.createLabel(x, unit));
                        geom = geometryFactory.createPoint(new Coordinate(x, y - TEXT_DISPLACEMENT));
                    } else {
                        geom = geometryFactory.createPoint(new Coordinate(x, y));
                    }
                    featureBuilder.set(Grid.ATT_GEOM, geom);
                    features.add(featureBuilder.buildFeature("grid." + i + "." + j));
                }
                y += incrementY;
            }
            x += incrementX;
        }
        return features;
    }
}
