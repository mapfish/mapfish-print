package org.mapfish.print.map.geotools.grid;

import java.awt.geom.AffineTransform;
import jakarta.annotation.Nonnull;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.cs.AxisDirection;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.style.Style;
import org.geotools.data.collection.CollectionFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.mapfish.print.Constants.Style.Grid;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.geotools.FeatureSourceSupplier;

/** Strategy for creating the style and features for the grid when the grid consists of lines. */
final class LineGridStrategy implements GridType.GridTypeStrategy {
  @Override
  public Style defaultStyle(final Template template, final GridParam layerData) {
    return LineGridStyle.get(layerData);
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
        SimpleFeatureType featureType =
            GridUtils.createGridFeatureType(mapContext, LineString.class);
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

    SpacesAndMins fromLinesNbr = fromNumberOfLines(mapContext, layerData);

    return sharedCreateFeatures(labels, featureBuilder, layerData, mapContext, fromLinesNbr);
  }

  private SpacesAndMins fromNumberOfLines(
      final MapfishMapContext mapContext, final GridParam layerData) {
    ReferencedEnvelope bounds = mapContext.toReferencedEnvelope();

    final double xSpace = bounds.getWidth() / (layerData.numberOfLines[0] + 1);
    final double ySpace = bounds.getHeight() / (layerData.numberOfLines[1] + 1);
    double minX = bounds.getMinimum(0) + xSpace;
    double minY = bounds.getMinimum(1) + ySpace;
    return new SpacesAndMins(xSpace, ySpace, minX, minY);
  }

  @Nonnull
  private DefaultFeatureCollection createFeaturesFromSpacing(
      @Nonnull final MapfishMapContext mapContext,
      @Nonnull final SimpleFeatureBuilder featureBuilder,
      @Nonnull final GridParam layerData,
      @Nonnull final LabelPositionCollector labels) {

    SpacesAndMins fromSpacing = fromSpacing(mapContext, layerData);

    return sharedCreateFeatures(labels, featureBuilder, layerData, mapContext, fromSpacing);
  }

  private SpacesAndMins fromSpacing(final MapfishMapContext mapContext, final GridParam layerData) {
    ReferencedEnvelope bounds = mapContext.toReferencedEnvelope();

    final double xSpace = layerData.spacing[0];
    final double ySpace = layerData.spacing[1];
    double minX = GridUtils.calculateFirstLine(bounds, layerData, 0);
    double minY = GridUtils.calculateFirstLine(bounds, layerData, 1);
    return new SpacesAndMins(xSpace, ySpace, minX, minY);
  }

  private DefaultFeatureCollection sharedCreateFeatures(
      final LabelPositionCollector labels,
      final SimpleFeatureBuilder featureBuilder,
      final GridParam layerData,
      final MapfishMapContext mapContext,
      final SpacesAndMins spacesAndMins) {
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
    for (double x = spacesAndMins.minX; x < bounds.getMaxX(); x += spacesAndMins.xSpace) {
      i++;
      featureBuilder.reset();
      LinearCoordinateSequence coordinateSequence =
          getLinearCoordinateSequence(
              layerData, numDimensions, bounds, x, pointSpacing, direction, 1);
      final SimpleFeature feature =
          createFeature(featureBuilder, geometryFactory, i, 1, coordinateSequence);
      features.add(feature);
      Geometry intersectionsTB =
          GridUtils.computeTopBorderIntersections(rotatedBounds, geometryFactory, x);
      GridUtils.topBorderLabel(
          labels,
          unit,
          worldToScreenTransform,
          labelTransform,
          layerData.getGridLabelFormat(),
          intersectionsTB);
      Geometry intersectionsBB =
          GridUtils.computeBottomBorderIntersections(rotatedBounds, geometryFactory, x);
      GridUtils.bottomBorderLabel(
          labels,
          unit,
          worldToScreenTransform,
          labelTransform,
          layerData.getGridLabelFormat(),
          intersectionsBB);
    }

    pointSpacing = bounds.getSpan(0) / layerData.pointsInLine;
    int j = 0;
    for (double y = spacesAndMins.minY; y < bounds.getMaxY(); y += spacesAndMins.ySpace) {
      j++;
      featureBuilder.reset();
      LinearCoordinateSequence coordinateSequence =
          getLinearCoordinateSequence(
              layerData, numDimensions, bounds, y, pointSpacing, direction, 0);
      final SimpleFeature feature =
          createFeature(featureBuilder, geometryFactory, j, 0, coordinateSequence);
      features.add(feature);
      Geometry intersectionsRB =
          GridUtils.computeRightBorderIntersections(rotatedBounds, geometryFactory, y);
      GridUtils.rightBorderLabel(
          labels,
          unit,
          worldToScreenTransform,
          labelTransform,
          layerData.getGridLabelFormat(),
          intersectionsRB);
      Geometry intersectionsLB =
          GridUtils.computeLeftBorderIntersections(rotatedBounds, geometryFactory, y);
      GridUtils.leftBorderLabel(
          labels,
          unit,
          worldToScreenTransform,
          labelTransform,
          layerData.getGridLabelFormat(),
          intersectionsLB);
    }

    return features;
  }

  private static LinearCoordinateSequence getLinearCoordinateSequence(
      final GridParam layerData,
      final int numDimensions,
      final ReferencedEnvelope bounds,
      final double pointCoordinate,
      final double pointSpacing,
      final AxisDirection direction,
      final int variableAxis) {
    final int numPoints = layerData.pointsInLine + 1; // add 1 for the last point
    double originX;
    double originY;
    if (variableAxis == 0) {
      originX = bounds.getMinimum(0);
      originY = pointCoordinate;
    } else if (variableAxis == 1) {
      originX = pointCoordinate;
      originY = bounds.getMinimum(1);
    } else {
      throw new RuntimeException("Unsupported variableAxis =" + variableAxis);
    }
    return new LinearCoordinateSequence()
        .setDimension(numDimensions)
        .setOrigin(originX, originY)
        .setVariableAxis(variableAxis)
        .setNumPoints(numPoints)
        .setSpacing(pointSpacing)
        .setOrdinate0AxisDirection(direction);
  }

  private SimpleFeature createFeature(
      final SimpleFeatureBuilder featureBuilder,
      final GeometryFactory geometryFactory,
      final int i,
      final int ordinate,
      final LinearCoordinateSequence coordinateSequence) {

    LineString geom = geometryFactory.createLineString(coordinateSequence);
    featureBuilder.set(Grid.ATT_GEOM, geom);

    return featureBuilder.buildFeature("grid." + (ordinate == 1 ? 'x' : 'y') + "." + i);
  }

  private static final class SpacesAndMins {
    public final double xSpace;
    public final double ySpace;
    public final double minX;
    public final double minY;

    SpacesAndMins(final double xSpace, final double ySpace, final double minX, final double minY) {
      this.xSpace = xSpace;
      this.ySpace = ySpace;
      this.minX = minX;
      this.minY = minY;
    }
  }
}
