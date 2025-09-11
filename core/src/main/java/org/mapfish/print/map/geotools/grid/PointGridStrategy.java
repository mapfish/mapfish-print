package org.mapfish.print.map.geotools.grid;

import java.awt.geom.AffineTransform;
import jakarta.annotation.Nonnull;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.style.Style;
import org.geotools.data.collection.CollectionFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.mapfish.print.Constants.Style.Grid;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.FeatureSourceSupplier;

/** Strategy for creating the style and features for the grid when the grid consists of lines. */
class PointGridStrategy implements GridType.GridTypeStrategy {

  @Override
  public Style defaultStyle(final Template template, final GridParam layerData) {
    return PointGridStyle.get(layerData);
  }

  @Override
  public FeatureSourceSupplier createFeatureSource(
      final Template template, final GridParam layerData, final LabelPositionCollector labels) {
    return new FeatureSourceSupplier() {
      @Nonnull
      @Override
      public FeatureSource load(
          @Nonnull final MfClientHttpRequestFactory requestFactory,
          @Nonnull final MapfishMapContext mapContext) {
        SimpleFeatureType featureType = GridUtils.createGridFeatureType(mapContext, Point.class);
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

  private DefaultFeatureCollection createFeaturesFromSpacing(
      final MapfishMapContext mapContext,
      final SimpleFeatureBuilder featureBuilder,
      final GridParam layerData,
      final LabelPositionCollector labels) {
    GeometryFactory geometryFactory = new GeometryFactory();
    ReferencedEnvelope bounds = mapContext.toReferencedEnvelope();

    CoordinateReferenceSystem mapCrs = bounds.getCoordinateReferenceSystem();
    String unit = layerData.calculateLabelUnit(mapCrs);
    MathTransform labelTransform = layerData.calculateLabelTransform(mapCrs);

    final double incrementX = layerData.spacing[0];
    final double incrementY = layerData.spacing[1];
    double minX = GridUtils.calculateFirstLine(bounds, layerData, 0);
    double minY = GridUtils.calculateFirstLine(bounds, layerData, 1);

    MapfishMapContext rootContext = mapContext.getRootContext();
    Polygon rotatedBounds = GridUtils.calculateBounds(rootContext);
    AffineTransform worldToScreenTransform = GridUtils.getWorldToScreenTransform(mapContext);

    DefaultFeatureCollection features = new DefaultFeatureCollection();
    int i = 0;
    int j;

    boolean addBorderFeatures = true;
    for (double x = minX; x < bounds.getMaxX(); x += incrementX) {
      i++;
      j = 0;

      if (!onRightBorder(bounds, x)) { // don't add the border features twice.
        Geometry intersectionsBB =
            GridUtils.computeBottomBorderIntersections(rotatedBounds, geometryFactory, x);
        GridUtils.bottomBorderLabel(
            labels,
            unit,
            worldToScreenTransform,
            labelTransform,
            layerData.getGridLabelFormat(),
            intersectionsBB);
        Geometry intersectionsTB =
            GridUtils.computeTopBorderIntersections(rotatedBounds, geometryFactory, x);
        GridUtils.topBorderLabel(
            labels,
            unit,
            worldToScreenTransform,
            labelTransform,
            layerData.getGridLabelFormat(),
            intersectionsTB);
      }
      for (double y = minY; y < bounds.getMaxY(); y += incrementY) {
        j++;

        if (addBorderFeatures && !onRightBorder(bounds, x) && !onTopBorder(bounds, y)) {
          Geometry intersectionsLB =
              GridUtils.computeLeftBorderIntersections(rotatedBounds, geometryFactory, y);
          GridUtils.leftBorderLabel(
              labels,
              unit,
              worldToScreenTransform,
              labelTransform,
              layerData.getGridLabelFormat(),
              intersectionsLB);
          Geometry intersectionsRB =
              GridUtils.computeRightBorderIntersections(rotatedBounds, geometryFactory, y);
          GridUtils.rightBorderLabel(
              labels,
              unit,
              worldToScreenTransform,
              labelTransform,
              layerData.getGridLabelFormat(),
              intersectionsRB);
        }
        if (!onTopBorder(bounds, y)
            && !onBottomBorder(bounds, y)
            && !onLeftBorder(bounds, x)
            && !onRightBorder(bounds, x)) { // don't add the border features twice.
          featureBuilder.reset();
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
    return x >= bounds.getMaxX();
  }

  private boolean onLeftBorder(final ReferencedEnvelope bounds, final double x) {
    return x <= bounds.getMinX();
  }

  private boolean onBottomBorder(final ReferencedEnvelope bounds, final double y) {
    return y <= bounds.getMinY();
  }

  private boolean onTopBorder(final ReferencedEnvelope bounds, final double y) {
    return y >= bounds.getMaxY();
  }

  private DefaultFeatureCollection createFeaturesFromNumberOfLines(
      final MapfishMapContext mapContext,
      final SimpleFeatureBuilder featureBuilder,
      final GridParam layerData,
      final LabelPositionCollector labels) {
    GeometryFactory geometryFactory = new GeometryFactory();
    ReferencedEnvelope bounds = mapContext.toReferencedEnvelope();

    MapfishMapContext rootContext = mapContext.getRootContext();
    Polygon rotatedBounds = GridUtils.calculateBounds(rootContext);
    AffineTransform worldToScreenTransform = GridUtils.getWorldToScreenTransform(mapContext);

    CoordinateReferenceSystem mapCrs = bounds.getCoordinateReferenceSystem();
    String unit = layerData.calculateLabelUnit(mapCrs);
    MathTransform labelTransform = layerData.calculateLabelTransform(mapCrs);

    double incrementX = bounds.getWidth() / (layerData.numberOfLines[0] + 1);
    double incrementY = bounds.getHeight() / (layerData.numberOfLines[1] + 1);

    double x = bounds.getMinX();
    DefaultFeatureCollection features = new DefaultFeatureCollection();
    for (int i = 0; i < layerData.numberOfLines[0] + 2; i++) {
      double y = bounds.getMinY();
      for (int j = 0; j < layerData.numberOfLines[1] + 2; j++) {
        String fid = "grid." + i + "." + j;
        if ((i != 0 || j != 0)
            && (i != layerData.numberOfLines[0] + 1 || j != layerData.numberOfLines[1] + 1)
            && (i != 0 || j != layerData.numberOfLines[1] + 1)
            && (i != layerData.numberOfLines[0] + 1 || j != 0)) {

          if (i == 0) {
            Geometry intersectionsLB =
                GridUtils.computeLeftBorderIntersections(rotatedBounds, geometryFactory, y);
            GridUtils.leftBorderLabel(
                labels,
                unit,
                worldToScreenTransform,
                labelTransform,
                layerData.getGridLabelFormat(),
                intersectionsLB);
          } else if (i == layerData.numberOfLines[0] + 1) {
            Geometry intersectionsRB =
                GridUtils.computeRightBorderIntersections(rotatedBounds, geometryFactory, y);
            GridUtils.rightBorderLabel(
                labels,
                unit,
                worldToScreenTransform,
                labelTransform,
                layerData.getGridLabelFormat(),
                intersectionsRB);
          } else if (j == 0) {
            Geometry intersectionsBB =
                GridUtils.computeBottomBorderIntersections(rotatedBounds, geometryFactory, x);
            GridUtils.bottomBorderLabel(
                labels,
                unit,
                worldToScreenTransform,
                labelTransform,
                layerData.getGridLabelFormat(),
                intersectionsBB);
          } else if (j == layerData.numberOfLines[1] + 1) {
            Geometry intersectionsTB =
                GridUtils.computeTopBorderIntersections(rotatedBounds, geometryFactory, x);
            GridUtils.topBorderLabel(
                labels,
                unit,
                worldToScreenTransform,
                labelTransform,
                layerData.getGridLabelFormat(),
                intersectionsTB);
          } else {
            featureBuilder.reset();
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
}
