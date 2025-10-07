package org.mapfish.print.processor.map;

import static org.mapfish.print.Constants.PDF_DPI;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.Attribute;
import org.mapfish.print.attribute.DataSourceAttribute;
import org.mapfish.print.attribute.DataSourceAttribute.DataSourceAttributeValue;
import org.mapfish.print.attribute.map.AreaOfInterest;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.attribute.map.MapAttribute.MapAttributeValues;
import org.mapfish.print.attribute.map.PagingAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.map.geotools.FeatureLayer;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.InputOutputValue;
import org.mapfish.print.processor.ProvideAttributes;
import org.mapfish.print.processor.RequireAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Processor used to display a map on multiple pages.
 *
 * <p>This processor will take the defined <a href="attributes.html#!map">map attribute</a> and
 * using the geometry defined in the <a href="attributes.html#!map">map attribute's</a> area of
 * interest, will create an Iterable&lt;Values&gt; each of which contains:
 *
 * <ul>
 *   <li>a new definition of a <a href="attributes.html#!map">map attribute</a>
 *   <li>name value which is a string that roughly describes which part of the main map this sub-map
 *       is
 *   <li>left value which is the name of the sub-map to the left of the current map
 *   <li>right value which is the name of the sub-map to the right of the current map
 *   <li>top value which is the name of the sub-map to the top of the current map
 *   <li>bottom value which is the name of the sub-map to the bottom of the current map
 * </ul>
 *
 * <p>It will also create a paging overview layer for the main map if you have set
 * renderPagingOverview to true in <a href="attributes.html#!paging">!paging</a> attribute
 *
 * <p>
 *
 * <p>The iterable of values can be consumed by a <a
 * href="processors.html#!createDataSource">!createDataSource</a> processor and as a result be put
 * in the report (or one of the sub-reports) table. One must be careful as this can result in truly
 * giant reports.
 *
 * <p>See also: <a href="attributes.html#!paging">!paging</a> attribute
 * [[examples=paging,paging_with_overview_layer]]
 */
public class CreateMapPagesProcessor
    extends AbstractProcessor<CreateMapPagesProcessor.Input, CreateMapPagesProcessor.Output>
    implements ProvideAttributes, RequireAttributes {
  private static final Logger LOGGER = LoggerFactory.getLogger(CreateMapPagesProcessor.class);
  private static final int DO_NOT_RENDER_BBOX_INDEX = -1;
  private static final String MAP_KEY = "map";

  private final GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
  private MapAttribute mapAttribute;

  @Autowired FeatureLayer.Plugin featureLayerPlugin;

  /** Name of features generated for paging. */
  public static final String OVERVIEW_PAGING_FEATURE_NAME = "overviewPaging";

  /** Name of the text attribut to be rendered in overviewPaging features. */
  public static final String OVERVIEW_PAGING_ATTRIBUT_TEXT = "name";

  /** Constructor. */
  protected CreateMapPagesProcessor() {
    super(Output.class);
  }

  @Override
  protected void extraValidation(
      final List<Throwable> validationErrors, final Configuration configuration) {}

  @Override
  public final Input createInputParameter() {
    return new Input();
  }

  @SuppressWarnings("unchecked")
  @Override
  public final Output execute(final Input values, final ExecutionContext context)
      throws IOException {
    final MapAttributeValues map = values.map;
    final PagingAttribute.PagingProcessorValues paging = values.paging;
    CoordinateReferenceSystem projection = map.getMapBounds().getProjection();
    final Rectangle paintArea = new Rectangle(map.getMapSize());

    // Compute aoiBBox
    AreaOfInterest areaOfInterest = this.getOrCreateAreaOfInterest(map, paintArea);
    Envelope aoiBBox = areaOfInterest.getArea().getEnvelopeInternal();

    // Compute paging dimensions
    PagingDimensions pagingDimensions = this.calculatePagingDimensions(values, aoiBBox, paintArea);

    // Compute map grid and collects overview features
    LOGGER.info(
        "Paging generate a grid of {}x{} potential maps.",
        pagingDimensions.nbWidth(),
        pagingDimensions.nbHeight());
    final int[][] mapIndexes = new int[pagingDimensions.nbWidth()][pagingDimensions.nbHeight()];
    final Envelope[][] mapsBounds =
        new Envelope[pagingDimensions.nbWidth()][pagingDimensions.nbHeight()];
    final boolean renderPagingOverview = Boolean.TRUE.equals(paging.renderPagingOverview);
    final DefaultFeatureCollection featuresOverviewPaging =
        this.populateMapGrid(
            mapIndexes,
            mapsBounds,
            pagingDimensions,
            projection,
            areaOfInterest,
            renderPagingOverview);

    // Generate the map definitions
    final List<Map<String, Object>> mapList =
        this.createMapDefinitions(mapIndexes, mapsBounds, pagingDimensions, values);

    this.addPagingOverviewLayer(featuresOverviewPaging, paging, map);

    LOGGER.info("Paging generate {} maps definitions.", mapList.size());
    DataSourceAttributeValue datasourceAttributes = new DataSourceAttributeValue();
    datasourceAttributes.attributesValues = mapList.toArray(new Map[0]);
    return new Output(datasourceAttributes);
  }

  private static SimpleFeatureType simpleFeatureTypeBuilder(
      @Nonnull final CoordinateReferenceSystem crs) {
    final SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
    typeBuilder.setName(OVERVIEW_PAGING_FEATURE_NAME);
    typeBuilder.add("geom", Polygon.class, crs);
    typeBuilder.add(OVERVIEW_PAGING_ATTRIBUT_TEXT, String.class);
    return typeBuilder.buildFeatureType();
  }

  private static SimpleFeature buildOverviewPagingFeature(
      @Nonnull final SimpleFeatureType typeOverviewPaging,
      @Nonnull final Polygon bbox,
      final int mapIndex) {
    return SimpleFeatureBuilder.build(
        typeOverviewPaging,
        new Object[] {bbox, Integer.toString(mapIndex)},
        Integer.toString(mapIndex));
  }

  private void addPagingOverviewLayer(
      @Nonnull final DefaultFeatureCollection featuresOverviewPaging,
      @Nonnull final PagingAttribute.PagingProcessorValues paging,
      @Nonnull final MapAttributeValues map)
      throws IOException {
    if (featuresOverviewPaging.getCount() > 0) {
      FeatureLayer.FeatureLayerParam param = new FeatureLayer.FeatureLayerParam();
      param.defaultStyle = Constants.Style.PagingOverviewLayer.NAME;
      param.style = paging.pagingOverviewStyle;
      param.renderAsSvg = true;
      param.features = featuresOverviewPaging;
      final FeatureLayer featureLayer = this.featureLayerPlugin.parse(map.getTemplate(), param);
      map.setPagingOverviewLayer(featureLayer);
    }
  }

  /**
   * Set the map attribute.
   *
   * @param name the attribute name
   * @param attribute the attribute
   */
  public void setAttribute(final String name, final Attribute attribute) {
    if (name.equals(MAP_KEY)) {
      this.mapAttribute = (MapAttribute) attribute;
    }
  }

  /**
   * Gets the attributes provided by the processor.
   *
   * @return the attributes
   */
  public Map<String, Attribute> getAttributes() {
    Map<String, Attribute> result = new HashMap<>();
    DataSourceAttribute datasourceAttribute = new DataSourceAttribute();
    Map<String, Attribute> dsResult = new HashMap<>();
    dsResult.put(MAP_KEY, this.mapAttribute);
    datasourceAttribute.setAttributes(dsResult);
    result.put("datasource", datasourceAttribute);
    return result;
  }

  /** Handles area of interest initialization. */
  public AreaOfInterest getOrCreateAreaOfInterest(
      final MapAttribute.MapAttributeValues map, final Rectangle paintArea) {
    AreaOfInterest areaOfInterest = map.areaOfInterest;

    if (areaOfInterest == null) {
      areaOfInterest = new AreaOfInterest();
      areaOfInterest.display = AreaOfInterest.AoiDisplay.NONE;
      ReferencedEnvelope mapBBox = map.getMapBounds().toReferencedEnvelope(paintArea);

      areaOfInterest.setPolygon(this.geometryFactory.toGeometry(mapBBox));
    }

    return areaOfInterest;
  }

  /** Calculates layout dimensions. */
  public PagingDimensions calculatePagingDimensions(
      final Input values, final Envelope aoiBBox, final Rectangle paintArea) {
    final PagingAttribute.PagingProcessorValues paging = values.paging;
    CoordinateReferenceSystem projection = values.map.getMapBounds().getProjection();
    final DistanceUnit projectionUnit = DistanceUnit.fromProjection(projection);

    final double paintAreaWidthIn = paintArea.getWidth() * paging.scale / PDF_DPI;
    final double paintAreaHeightIn = paintArea.getHeight() * paging.scale / PDF_DPI;

    final double paintAreaWidth = DistanceUnit.IN.convertTo(paintAreaWidthIn, projectionUnit);
    final double paintAreaHeight = DistanceUnit.IN.convertTo(paintAreaHeightIn, projectionUnit);

    final double overlapProj =
        DistanceUnit.IN.convertTo(paging.overlap * paging.scale / PDF_DPI, projectionUnit);

    final int nbWidth =
        (int) Math.ceil((aoiBBox.getWidth() + overlapProj) / (paintAreaWidth - overlapProj));
    final int nbHeight =
        (int) Math.ceil((aoiBBox.getHeight() + overlapProj) / (paintAreaHeight - overlapProj));

    final double marginWidth =
        (paintAreaWidth * nbWidth - (nbWidth - 1) * overlapProj - aoiBBox.getWidth()) / 2;
    final double marginHeight =
        (paintAreaHeight * nbHeight - (nbHeight - 1) * overlapProj - aoiBBox.getHeight()) / 2;

    final double minX = aoiBBox.getMinX() - marginWidth - overlapProj / 2;
    final double minY = aoiBBox.getMinY() - marginHeight - overlapProj / 2;

    return new PagingDimensions(
        nbWidth, nbHeight, minX, minY, paintAreaWidth, paintAreaHeight, overlapProj);
  }

  /** Populates the map grid and collects overview features. */
  public DefaultFeatureCollection populateMapGrid(
      final int[][] mapIndexes,
      final Envelope[][] mapsBounds,
      final PagingDimensions pagingD,
      final CoordinateReferenceSystem projection,
      final AreaOfInterest areaOfInterest,
      final boolean renderPagingOverview) {
    int mapIndex = 0;

    final SimpleFeatureType typeOverviewPaging = simpleFeatureTypeBuilder(projection);
    final DefaultFeatureCollection featuresOverviewPaging = new DefaultFeatureCollection();

    for (int j = 0; j < pagingD.nbHeight(); j++) {
      for (int i = 0; i < pagingD.nbWidth(); i++) {
        final double x1 = pagingD.minX() + i * (pagingD.paintAreaWidth() - pagingD.overlapProj());
        final double x2 = x1 + pagingD.paintAreaWidth();
        final double y1 = pagingD.minY() + j * (pagingD.paintAreaHeight() - pagingD.overlapProj());
        final double y2 = y1 + pagingD.paintAreaHeight();
        Coordinate[] coords =
            new Coordinate[] {
              new Coordinate(x1, y1),
              new Coordinate(x1, y2),
              new Coordinate(x2, y2),
              new Coordinate(x2, y1),
              new Coordinate(x1, y1)
            };

        LinearRing ring = this.geometryFactory.createLinearRing(coords);
        final Polygon bbox = this.geometryFactory.createPolygon(ring);

        if (areaOfInterest.getArea().intersects(bbox)) {
          mapsBounds[i][j] = bbox.getEnvelopeInternal();
          mapIndexes[i][j] = mapIndex;
          if (renderPagingOverview) {
            featuresOverviewPaging.add(
                buildOverviewPagingFeature(typeOverviewPaging, bbox, mapIndex));
          }
          mapIndex++;
        } else {
          mapIndexes[i][j] = DO_NOT_RENDER_BBOX_INDEX;
        }
      }
    }
    return featuresOverviewPaging;
  }

  /** Creates map definitions for each page. */
  public List<Map<String, Object>> createMapDefinitions(
      final int[][] mapIndexes,
      final Envelope[][] mapsBounds,
      final PagingDimensions pagingD,
      final Input values) {
    final MapAttributeValues map = values.map;
    final PagingAttribute.PagingProcessorValues paging = values.paging;

    final List<Map<String, Object>> mapList = new ArrayList<>();

    for (int j = 0; j < pagingD.nbHeight(); j++) {
      for (int i = 0; i < pagingD.nbWidth(); i++) {
        if (mapIndexes[i][j] != DO_NOT_RENDER_BBOX_INDEX) {
          Map<String, Object> mapValues = new HashMap<>();
          mapValues.put("name", mapIndexes[i][j]);
          mapValues.put("left", i != 0 ? mapIndexes[i - 1][j] : DO_NOT_RENDER_BBOX_INDEX);
          mapValues.put("bottom", j != 0 ? mapIndexes[i][j - 1] : DO_NOT_RENDER_BBOX_INDEX);
          mapValues.put(
              "right",
              i != pagingD.nbWidth() - 1 ? mapIndexes[i + 1][j] : DO_NOT_RENDER_BBOX_INDEX);
          mapValues.put(
              "top", j != pagingD.nbHeight() - 1 ? mapIndexes[i][j + 1] : DO_NOT_RENDER_BBOX_INDEX);

          final Envelope mapsBound = mapsBounds[i][j];
          MapAttributeValues theMap = this.createSingleMapDefinition(map, mapsBound, paging);
          mapValues.put(MAP_KEY, theMap);
          mapList.add(mapValues);
        }
      }
    }
    return mapList;
  }

  /** Handles creation of a single map definition. */
  public MapAttributeValues createSingleMapDefinition(
      final MapAttribute.MapAttributeValues map,
      final Envelope mapsBound,
      final PagingAttribute.PagingProcessorValues paging) {
    return map.copy(
        map.getWidth(),
        map.getHeight(),
        (@Nonnull final MapAttributeValues input) -> {
          // Setting zoomToFeatures to null to ensure that the map does not automatically
          // zoom to any features, as the bounding box (bbox) is explicitly defined below.
          input.zoomToFeatures = null;
          input.center = null;
          input.bbox =
              new double[] {
                mapsBound.getMinX(), mapsBound.getMinY(), mapsBound.getMaxX(), mapsBound.getMaxY()
              };

          if (paging.aoiDisplay != null) {
            input.areaOfInterest.display = paging.aoiDisplay;
          }
          if (paging.aoiStyle != null) {
            input.areaOfInterest.style = paging.aoiStyle;
          }
          return null;
        });
  }

  public record PagingDimensions(
      int nbWidth,
      int nbHeight,
      double minX,
      double minY,
      double paintAreaWidth,
      double paintAreaHeight,
      double overlapProj) {}

  /** The Input object for processor. */
  public static class Input {

    /** The required parameters for the map. */
    @InputOutputValue public MapAttribute.MapAttributeValues map;

    /**
     * Attributes that define how each page/sub-map will be generated. It defines the scale and how
     * to render the area of interest, etc...
     */
    public PagingAttribute.PagingProcessorValues paging;
  }

  /**
   * Output of processor.
   *
   * @param datasource Resulting list of values for the maps.
   */
  public record Output(DataSourceAttributeValue datasource) {}
}
