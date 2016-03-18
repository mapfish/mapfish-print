package org.mapfish.print.processor.map;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mapfish.print.attribute.DataSourceAttribute.DataSourceAttributeValue;
import org.mapfish.print.attribute.map.AreaOfInterest;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.attribute.map.MapAttribute.MapAttributeValues;
import org.mapfish.print.attribute.map.PagingAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.processor.AbstractProcessor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * <p>Processor used to display a map on multiple pages.</p>
 * <p>
 *     This processor will take the defined <a href="attributes.html#!map">map attribute</a> and using the geometry defined in the
 *     <a href="attributes.html#!map">map attribute's</a> area of interest, will create an Iterable&lt;Values&gt; each of which
 *     contains:
 * </p>
 *     <ul>
 *         <li>a new definition of a <a href="attributes.html#!map">map attribute</a></li>
 *         <li>name value which is a string that roughly describes which part of the main map this sub-map is</li>
 *         <li>left value which is the name of the sub-map to the left of the current map</li>
 *         <li>right value which is the name of the sub-map to the right of the current map</li>
 *         <li>top value which is the name of the sub-map to the top of the current map</li>
 *         <li>bottom value which is the name of the sub-map to the bottom of the current map</li>
 *     </ul>
 * <p>
 *     The iterable of values can be consumed by a <a href="processors.html#!createDataSource">!createDataSource</a> processor
 *     and as a result be put in the report (or one of the sub-reports) table.  One must be careful as
 *     this can result in truly giant reports.
 * </p>
 * <p>See also: <a href="attributes.html#!paging">!paging</a> attribute</p>
 * [[examples=paging]]
 *
 * @author Stéphane Brunner
 */
public class CreateMapPagesProcessor extends AbstractProcessor<CreateMapPagesProcessor.Input, CreateMapPagesProcessor.Output> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateMapPagesProcessor.class);
    private static final int DO_NOT_RENDER_BBOX_INDEX = -1;

    private final GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();


    /**
     * Constructor.
     */
    protected CreateMapPagesProcessor() {
        super(Output.class);
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors, final Configuration configuration) {
    }

    @Override
    public final Input createInputParameter() {
        return new Input();
    }

    @SuppressWarnings("unchecked")
    @Override
    public final Output execute(final Input values, final ExecutionContext context) throws Exception {

        final MapAttributeValues map = values.map;
        final PagingAttribute.PagingProcessorValues paging = values.paging;
        CoordinateReferenceSystem projection = map.getMapBounds().getProjection();
        final Rectangle paintArea = new Rectangle(map.getMapSize());
        final double dpi = map.getRequestorDPI();
        final DistanceUnit projectionUnit = DistanceUnit.fromProjection(projection);

        AreaOfInterest areaOfInterest = map.areaOfInterest;
        if (areaOfInterest == null) {
            areaOfInterest = new AreaOfInterest();
            areaOfInterest.display = AreaOfInterest.AoiDisplay.NONE;
            ReferencedEnvelope mapBBox = map.getMapBounds().toReferencedEnvelope(paintArea, dpi);

            areaOfInterest.setPolygon(this.geometryFactory.toGeometry(mapBBox));
        }

        Envelope aoiBBox = areaOfInterest.getArea().getEnvelopeInternal();

        final double paintAreaWidthIn = paintArea.getWidth() * paging.scale / dpi;
        final double paintAreaHeightIn = paintArea.getHeight() * paging.scale / dpi;

        final double paintAreaWidth = DistanceUnit.IN.convertTo(paintAreaWidthIn, projectionUnit);
        final double paintAreaHeight = DistanceUnit.IN.convertTo(paintAreaHeightIn, projectionUnit);

        final double overlapProj = DistanceUnit.IN.convertTo(paging.overlap * paging.scale / dpi, projectionUnit);

        final int nbWidth = (int) Math.ceil((aoiBBox.getWidth() + overlapProj) / (paintAreaWidth - overlapProj));
        final int nbHeight = (int) Math.ceil((aoiBBox.getHeight() + overlapProj) / (paintAreaHeight - overlapProj));

        final double marginWidth = (paintAreaWidth * nbWidth - (nbWidth - 1) * overlapProj - aoiBBox.getWidth()) / 2;
        final double marginHeight = (paintAreaHeight * nbHeight - (nbHeight - 1) * overlapProj - aoiBBox.getHeight()) / 2;

        final double minX = aoiBBox.getMinX() - marginWidth - overlapProj / 2;
        final double minY = aoiBBox.getMinY() - marginHeight - overlapProj / 2;

        LOGGER.info("Paging generate a grid of " + nbWidth + "x" + nbHeight + " potential maps.");
        final int[][] mapIndexes = new int[nbWidth][nbHeight];
        final Envelope[][] mapsBounds = new Envelope[nbWidth][nbHeight];
        int mapIndex = 0;

        for (int j = 0; j < nbHeight; j++) {
            for (int i = 0; i < nbWidth; i++) {
                final double x1 = minX + i * (paintAreaWidth - overlapProj);
                final double x2 = x1 + paintAreaWidth;
                final double y1 = minY + j * (paintAreaHeight - overlapProj);
                final double y2 = y1 + paintAreaHeight;
                Coordinate[] coords  = new Coordinate[] {
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
                    mapIndex++;
                } else {
                    mapIndexes[i][j] = DO_NOT_RENDER_BBOX_INDEX;
                }
            }
        }

        final List<Map<String, Object>> mapList = Lists.newArrayList();

        for (int j = 0; j < nbHeight; j++) {
            for (int i = 0; i < nbWidth; i++) {
                if (mapIndexes[i][j] != DO_NOT_RENDER_BBOX_INDEX) {
                    Map<String, Object> mapValues = new HashMap<String, Object>();
                    mapValues.put("name", mapIndexes[i][j]);
                    mapValues.put("left", i != 0 ? mapIndexes[i - 1][j] : DO_NOT_RENDER_BBOX_INDEX);
                    mapValues.put("bottom", j != 0 ? mapIndexes[i][j - 1] : DO_NOT_RENDER_BBOX_INDEX);
                    mapValues.put("right", i != nbWidth - 1 ? mapIndexes[i + 1][j] : DO_NOT_RENDER_BBOX_INDEX);
                    mapValues.put("top", j != nbHeight - 1 ? mapIndexes[i][j + 1] : DO_NOT_RENDER_BBOX_INDEX);

                    final Envelope mapsBound = mapsBounds[i][j];
                    MapAttributeValues theMap = map.copy(map.getMapSize(), new Function<MapAttributeValues, Void>() {
                        @Nullable
                        @Override
                        public Void apply(@Nonnull final MapAttributeValues input) {
                            input.center = null;
                            input.bbox = new double[]{
                                    mapsBound.getMinX(),
                                    mapsBound.getMinY(),
                                    mapsBound.getMaxX(),
                                    mapsBound.getMaxY()
                            };
                            input.dpi = dpi;
                            if (paging.aoiDisplay != null) {
                                input.areaOfInterest.display = paging.aoiDisplay;
                            }
                            if (paging.aoiStyle != null) {
                                input.areaOfInterest.style = paging.aoiStyle;
                            }
                            return null;
                        }
                    });
                    mapValues.put("map", theMap);

                    mapList.add(mapValues);
                }
            }
        }
        LOGGER.info("Paging generate " + mapList.size() + " maps definitions.");
        DataSourceAttributeValue datasourceAttributes = new DataSourceAttributeValue();
        datasourceAttributes.attributesValues = mapList.toArray(new Map[mapList.size()]);
        return new Output(datasourceAttributes);
    }

    /**
     * The Input object for processor.
     */
    public static class Input {
        /**
         * The required parameters for the map.
         */
        public MapAttribute.MapAttributeValues map;

        /**
         * Attributes that define how each page/sub-map will be generated.  It defines the scale and how to render the area of interest,
         * etc...
         */
        public PagingAttribute.PagingProcessorValues paging;
    }

    /**
     * Output of processor.
     */
    public static final class Output {
        /**
         * Resulting list of values for the maps.
         */
        public final DataSourceAttributeValue datasource;

        private Output(final DataSourceAttributeValue tableList) {
            this.datasource = tableList;
        }
    }
}
