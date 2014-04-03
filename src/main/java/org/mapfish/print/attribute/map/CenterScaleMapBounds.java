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

package org.mapfish.print.attribute.map;

import com.vividsolutions.jts.geom.Coordinate;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.GeodeticCalculator;
import org.mapfish.print.Constants;
import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.map.Scale;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;

/**
 * Represent Map Bounds with a center location and a scale of the map.
 * <p/>
 * Created by Jesse on 3/26/14.
 */
public class CenterScaleMapBounds extends MapBounds {
    private Coordinate center;
    private Scale scale;

    /**
     * Constructor.
     *
     * @param projection the projection these bounds are defined in.
     * @param centerX the x coordinate of the center point.
     * @param centerY the y coordinate of the center point.
     * @param scale   the scale of the map
     */
    public CenterScaleMapBounds(final CoordinateReferenceSystem projection, final double centerX,
                                final double centerY, final Scale scale) {
        super(projection);
        this.center = new Coordinate(centerX, centerY);
        this.scale = scale;
    }


    @Override
    public final ReferencedEnvelope toReferencedEnvelope(final Rectangle paintArea, final double dpi) {
        double pixelPerGeoUnit = (this.scale.getUnit().convertTo(dpi, DistanceUnit.IN) / this.scale.getDenominator());

        double geoWidth = paintArea.width * dpi / Constants.PDF_DPI / pixelPerGeoUnit;
        double geoHeight = paintArea.height * dpi / Constants.PDF_DPI / pixelPerGeoUnit;
        final double centerX = this.center.getOrdinate(0);
        final double centerY = this.center.getOrdinate(1);

        ReferencedEnvelope bbox;
        final boolean standardUnit = this.getProjection().getCoordinateSystem().getAxis(0).getUnit().isStandardUnit();
        if (!standardUnit) {
            bbox = computeGeodeticBBox(geoWidth, geoHeight);
        } else {
            double minGeoX = centerX - (geoWidth / 2.0f);
            double minGeoY = centerY - (geoHeight / 2.0f);
            double maxGeoX = minGeoX + geoWidth;
            double maxGeoY = minGeoY + geoHeight;
            bbox = new ReferencedEnvelope(minGeoX, maxGeoX, minGeoY, maxGeoY, getProjection());
        }

        return bbox;
    }

    private ReferencedEnvelope computeGeodeticBBox(final double geoWidth, final double geoHeight) {
        try {
            CoordinateReferenceSystem crs = getProjection();

            GeodeticCalculator calc = new GeodeticCalculator(crs);
            DirectPosition2D directPosition2D = new DirectPosition2D(this.center.x, this.center.y);
            directPosition2D.setCoordinateReferenceSystem(crs);
            calc.setStartingPosition(directPosition2D);

            final int west = -90;
            calc.setDirection(west, geoWidth / 2.0f);
            double minGeoX =  calc.getDestinationPosition().getOrdinate(0);

            final int east = 90;
            calc.setDirection(east, geoWidth / 2.0f);
            double maxGeoX = calc.getDestinationPosition().getOrdinate(0);

            final int south = 180;
            calc.setDirection(south, geoHeight / 2.0f);
            double minGeoY = calc.getDestinationPosition().getOrdinate(1);

            final int north = 0;
            calc.setDirection(north, geoHeight / 2.0f);
            double maxGeoY = calc.getDestinationPosition().getOrdinate(1);

            return new ReferencedEnvelope(
                    rollLongitude(minGeoX), rollLongitude(maxGeoX),
                    rollLatitude(minGeoY), rollLatitude(maxGeoY), crs);
        } catch (TransformException e) {
            throw new RuntimeException(e);
        }
    }

    // CSOFF: MagicNumber
    private double rollLongitude(final double x) {
        return x - (((int) (x + Math.signum(x) * 180)) / 360) * 360.0;
    }

    private double rollLatitude(final double y) {
        return y - (((int) (y + Math.signum(y) * 90)) / 180) * 180.0;
    }
    // CSON: MagicNumber
}
