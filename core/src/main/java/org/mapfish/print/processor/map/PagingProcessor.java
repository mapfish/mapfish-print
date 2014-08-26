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
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mapfish.print.attribute.map.AreaOfInterest;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.attribute.map.MapAttribute.MapAttributeValues;
import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.map.Scale;
import org.mapfish.print.output.Values;
import org.mapfish.print.processor.AbstractProcessor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processor used to display a geometry on multiple pages.
 *
 * @author Stéphane Brunner
 */
public class PagingProcessor extends AbstractProcessor<PagingProcessor.Input, PagingProcessor.Output> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PagingProcessor.class);
    private static final char DO_NOT_RENDER_BBOX_CHAR = ' ';

    private GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
    private Scale scale;
    private double overlap;

    /**
     * Constructor.
     */
    protected PagingProcessor() {
        super(Output.class);
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors) {
    }

    @Override
    public final Input createInputParameter() {
        return new Input();
    }

    /**
     * Set the destination scale.
     *
     * @param scale the scale
     */
    public final void setScale(final double scale) {
        this.scale = new Scale(scale);
    }

    /**
     * Set the pages overlap.
     *
     * @param overlap the pages overlap [projection unit]
     */
    public final void setOverlap(final double overlap) {
        this.overlap = overlap;
    }

    @Override
    public final Output execute(final Input values, final ExecutionContext context) throws Exception {

        CoordinateReferenceSystem projection = values.map.getMapBounds().getProjection();
        final Rectangle paintArea = new Rectangle(values.map.getMapSize());
        final double dpi = values.map.getRequestorDPI();
        final DistanceUnit projectionUnit = DistanceUnit.fromProjection(projection);

        AreaOfInterest areaOfInterest = values.map.areaOfInterest;
        if (areaOfInterest == null) {
            areaOfInterest = new AreaOfInterest();
            areaOfInterest.display = AreaOfInterest.AoiDisplay.NONE;
            ReferencedEnvelope mapBBox = values.map.getMapBounds().toReferencedEnvelope(paintArea, dpi);

            areaOfInterest.setPolygon((Polygon) this.geometryFactory.toGeometry(mapBBox));
        }

        Envelope aoiBBox = areaOfInterest.getArea().getEnvelopeInternal();

        final double paintAreaWidthIn = paintArea.getWidth() * this.scale.getDenominator() / dpi;
        final double paintAreaHeightIn = paintArea.getHeight() * this.scale.getDenominator() / dpi;

        final double paintAreaWidth = DistanceUnit.IN.convertTo(paintAreaWidthIn, projectionUnit);
        final double paintAreaHeight = DistanceUnit.IN.convertTo(paintAreaHeightIn, projectionUnit);

        final int nbWidth = (int) Math.ceil(aoiBBox.getWidth() / (paintAreaWidth - this.overlap));
        final int nbHeight = (int) Math.ceil(aoiBBox.getHeight() / (paintAreaHeight - this.overlap));

        final double marginWidth = (paintAreaWidth * nbWidth - aoiBBox.getWidth()) / 2;
        final double marginHeight = (paintAreaHeight * nbHeight - aoiBBox.getHeight()) / 2;

        final double minX = aoiBBox.getMinX() - marginWidth - this.overlap / 2;
        final double minY = aoiBBox.getMinY() - marginHeight - this.overlap / 2;

        LOGGER.info("Paging generate a grid of " + nbWidth + "x" + nbHeight + " potential maps.");
        final char[][] names = new char[nbWidth][nbHeight];
        final Envelope[][] mapsBounds = new Envelope[nbWidth][nbHeight];
        char mapName = 'A';

        for (int j = 0; j < nbHeight; j++) {
            for (int i = 0; i < nbWidth; i++) {
                final double x1 = minX + i * (paintAreaWidth - this.overlap);
                final double x2 = x1 + paintAreaWidth;
                final double y1 = minY + j * (paintAreaHeight - this.overlap);
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
                    names[i][j] = mapName;
                    mapName++;
                } else {
                    names[i][j] = DO_NOT_RENDER_BBOX_CHAR;
                }
            }
        }

        final List<Values> mapList = new ArrayList<Values>();
        MapAttribute mapAttribute = (MapAttribute) values.map.getAttribute();

        for (int j = 0; j < nbHeight; j++) {
            for (int i = 0; i < nbWidth; i++) {
                if (names[i][j] != DO_NOT_RENDER_BBOX_CHAR) {
                    Map<String, Object> mapValues = new HashMap<String, Object>();
                    mapValues.put("name", "" + names[i][j]);
                    mapValues.put("left", "" + (i != 0 ? names[i - 1][j] : DO_NOT_RENDER_BBOX_CHAR));
                    mapValues.put("bottom", "" + (j != 0 ? names[i][j - 1] : DO_NOT_RENDER_BBOX_CHAR));
                    mapValues.put("right", "" + (i != nbWidth - 1 ? names[i + 1][j] : DO_NOT_RENDER_BBOX_CHAR));
                    mapValues.put("top", "" + (j != nbHeight - 1 ? names[i][j + 1] : DO_NOT_RENDER_BBOX_CHAR));

                    MapAttributeValues theMap = mapAttribute.new MapAttributeValues(
                            values.map.getTemplate(), values.map.getMapSize());
                    Coordinate center = mapsBounds[i][j].centre();
                    theMap.center = new double[]{center.x, center.y};
                    theMap.scale = this.scale.getDenominator();
                    theMap.areaOfInterest = areaOfInterest.copy();
                    theMap.layers = values.map.layers;
                    theMap.dpi = dpi;
                    theMap.longitudeFirst = true;
                    theMap.postConstruct();
                    mapValues.put("map", theMap);

                    mapList.add(new Values(mapValues));
                }
            }
        }
        LOGGER.info("Paging generate " + mapList.size() + " maps definitions.");
        return new Output(mapList);
    }

    /**
     * The Input object for processor.
     */
    public static class Input {
        /**
         * The required parameters for the map.
         */
        public MapAttribute.MapAttributeValues map;
    }

    /**
     * Output of processor.
     */
    public static final class Output {
        /**
         * Resulting list of values for the maps.
         */
        public final List<Values> maps;

        private Output(final List<Values> tableList) {
            this.maps = tableList;
        }
    }
}
