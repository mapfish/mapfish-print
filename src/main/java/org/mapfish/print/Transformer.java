/*
 * Copyright (C) 2013  Camptocamp
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

package org.mapfish.print;

import com.itextpdf.awt.geom.AffineTransform;

import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.mapfish.print.config.Config;
import org.geotools.referencing.GeodeticCalculator;
import org.mapfish.print.utils.DistanceUnit;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.itextpdf.text.pdf.PdfContentByte;

/**
 * Class that deals with the geometric tranformation between the geographic,
 * bitmap, and paper space for a map rendering.
 */
public class Transformer implements Cloneable {
    private static final String GOOGLE_WKT = "PROJCS[\"Google Mercator\","
                                             + "GEOGCS[\"WGS 84\","
                                             + "DATUM[\"World Geodetic System 1984\","
                                             + "SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]],"
                                             + "AUTHORITY[\"EPSG\",\"6326\"]],"
                                             + "PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]],"
                                             + "UNIT[\"degree\", 0.017453292519943295],"
                                             + "AXIS[\"Geodetic latitude\", NORTH],"
                                             + "AXIS[\"Geodetic longitude\", EAST],"
                                             + "AUTHORITY[\"EPSG\",\"4326\"]],"
                                             + "PROJECTION[\"Mercator_1SP\"],"
                                             + "PARAMETER[\"semi_minor\", 6378137.0],"
                                             + "PARAMETER[\"latitude_of_origin\", 0.0],"
                                             + "PARAMETER[\"central_meridian\", 0.0],"
                                             + "PARAMETER[\"scale_factor\", 1.0],"
                                             + "PARAMETER[\"false_easting\", 0.0],"
                                             + "PARAMETER[\"false_northing\", 0.0]," + "UNIT[\"m\", 1.0],"
                                             + "AXIS[\"Easting\", EAST]," + "AXIS[\"Northing\", NORTH],"
                                             + "AUTHORITY[\"EPSG\",\"900913\"]]";

    private float svgFactor = 1.0f;
    public double minGeoX;
    public double minGeoY;
    public double maxGeoX;
    public double maxGeoY;
    private final double scale;
    private final float paperWidth;
    private final float paperHeight;
    private double pixelPerGeoUnit;
    private float paperPosX;
    private float paperPosY;
    private final int dpi;

    /**
     * angle in radian
     */
    private double rotation;
    private final boolean strictEpsg4326;

    private void adjustSvgFactor(int dpi, boolean isIntegerSvg) {
        /**
         * The following code has been changed due to the fact that it seems
         * wrong. However, I'm not sure if my "correction" solves the problem
         * for the better. So, please review.
         *
         * The ('wrong') code below can be reformed: (600 + dpi - 1) / dpi = 1 +
         * 599/dpi and is never smaller than 1, since dpi>1. Also, it does not
         * make sense, as this factor gets smaller with increasing values for
         * dpi.
         */
        // target at least 600DPI for the SVG precision
        // svgFactor = Math.max((600 + dpi - 1) / dpi, 1);

        /**
         * svgFactor seems to not matter so much so we set it to 1 (above) needs
         * to get bigger if DPI increases and at standard 72 DPI needs to be 1.0
         */
        if (isIntegerSvg) { // integerSvg: true # in yaml
            // config file
            if (dpi < 600) { // target at least 600 DPI, this is a hack and only
                // needed for MapServer <= 5.6 where integers
                // are put into SVG
                svgFactor = 600f / 72.0f;
                /**
                 * = 8.33 so almost 9 as before with svgFactor being (600 + dpi
                 * -1)/dpi = ~9 if dpi = 72
                 */
            } else {
                svgFactor = dpi / 72.0f; // gets greater than 8.33
            }
        } // else defaults to 1.0 as it should with MapServer >= 6 and CAIRO SVG
        // rendering with floating point values
    }

    /**
     * @param centerX       geographic center in projection - x
     * @param centerY       geographic center in projection - y
     * @param paperWidth    e.g. map width in pt on the PDF
     * @param paperHeight   e.g. map height in pt on the PDF
     * @param scale         e.g. 10000 if scale is 1:10,000
     * @param dpi           as selected in request
     * @param unitEnum      the distance unit of the map e.g. DistanceUnit.M for meters
     * @param rotation      the rotation of the map per the request
     * @param geodeticSRS   if not null then it is a the srs to use with the geodetic
     * @param strictEpsg4326 if true then EPSG:4326 should interpretted as lat/long otherwise use the "incorrect" long/lat
     */
    public Transformer(double centerX, double centerY, float paperWidth,
                       float paperHeight, double scale, int dpi, DistanceUnit unitEnum,
                       double rotation, String geodeticSRS, boolean isIntegerSvg, boolean strictEpsg4326) {
        this.strictEpsg4326 = strictEpsg4326;
        this.dpi = dpi;
        pixelPerGeoUnit = (unitEnum.convertTo(dpi, DistanceUnit.IN) / scale);

        double geoWidth = paperWidth * dpi / 72.0f / pixelPerGeoUnit;
        double geoHeight = paperHeight * dpi / 72.0f / pixelPerGeoUnit;

        adjustSvgFactor(dpi, isIntegerSvg);

        this.paperWidth = paperWidth;
        this.paperHeight = paperHeight;
        this.scale = scale;
        this.rotation = rotation;

        if (geodeticSRS != null) {
            computeGeodeticBBox(geoWidth, geoHeight, centerX, centerY, dpi,
                    geodeticSRS);
        } else {
            this.minGeoX = centerX - (geoWidth / 2.0f);
            this.minGeoY = centerY - (geoHeight / 2.0f);
            this.maxGeoX = minGeoX + geoWidth;
            this.maxGeoY = minGeoY + geoHeight;
        }

    }

    private void computeGeodeticBBox(double geoWidth, double geoHeight,
                                     double centerX, double centerY, float dpi, String srsCode) {
        try {
            CoordinateReferenceSystem crs;
            if (srsCode.equalsIgnoreCase("EPSG:900913")) {
                crs = CRS.parseWKT(GOOGLE_WKT);
            } else {
                crs = CRS.decode(srsCode, !strictEpsg4326);
            }
            GeodeticCalculator calc = new GeodeticCalculator(crs);
            DirectPosition2D directPosition2D = new DirectPosition2D(centerX,
                    centerY);
            directPosition2D.setCoordinateReferenceSystem(crs);
            calc.setStartingPosition(directPosition2D);

            calc.setDirection(-90, geoWidth / 2.0f);
            minGeoX = (float) calc.getDestinationPosition().getOrdinate(0);

            calc.setDirection(90, geoWidth / 2.0f);
            maxGeoX = (float) calc.getDestinationPosition().getOrdinate(0);

            calc.setDirection(180, geoHeight / 2.0f);
            minGeoY = (float) calc.getDestinationPosition().getOrdinate(1);

            calc.setDirection(0, geoHeight / 2.0f);
            maxGeoY = (float) calc.getDestinationPosition().getOrdinate(1);

            pixelPerGeoUnit = (paperWidth * dpi) / 72.0f / (maxGeoX - minGeoX);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public double getGeoW() {
        return maxGeoX - minGeoX;
    }

    public double getGeoH() {
        return (maxGeoY - minGeoY);
    }

    public double getStraightBitmapW() {
        return getGeoW() * pixelPerGeoUnit;
    }

    public double getStraightBitmapH() {
        return getGeoH() * pixelPerGeoUnit;
    }

    public long getRotatedBitmapW() {
        double width = getStraightBitmapW();
        if (rotation != 0.0) {
            double height = getStraightBitmapH();
            width = Math.abs(width * Math.cos(rotation))
                    + Math.abs(height * Math.sin(rotation));
        }
        return Math.round(width);
    }

    public long getRotatedBitmapH() {
        double height = getStraightBitmapH();
        if (rotation != 0.0) {
            double width = getStraightBitmapW();
            height = Math.abs(height * Math.cos(rotation))
                     + Math.abs(width * Math.sin(rotation));
        }
        return Math.round(height);
    }

    public double getRotatedGeoW() {
        double width = getGeoW();
        if (rotation != 0.0) {
            double height = getGeoH();
            width = (float) (Math.abs(width * Math.cos(rotation)) + Math
                    .abs(height * Math.sin(rotation)));
        }
        return width;
    }

    public double getRotatedGeoH() {
        double height = getGeoH();
        if (rotation != 0.0) {
            double width = getGeoW();
            height = (float) (Math.abs(height * Math.cos(rotation)) + Math
                    .abs(width * Math.sin(rotation)));
        }
        return height;
    }

    public float getRotatedPaperW() {
        float width = getPaperW();
        if (rotation != 0.0) {
            float height = getPaperH();
            width = (float) (Math.abs(width * Math.cos(rotation)) + Math
                    .abs(height * Math.sin(rotation)));
        }
        return width;
    }

    public float getRotatedPaperH() {
        float height = getPaperH();
        if (rotation != 0.0) {
            float width = getPaperW();
            height = (float) (Math.abs(height * Math.cos(rotation)) + Math
                    .abs(width * Math.sin(rotation)));
        }
        return height;
    }

    public double getRotatedMinGeoX() {
        return minGeoX - (getRotatedGeoW() - getGeoW()) / 2.0F;
    }

    public double getRotatedMaxGeoX() {
        return maxGeoX + (getRotatedGeoW() - getGeoW()) / 2.0F;
    }

    public double getRotatedMinGeoY() {
        return minGeoY - (getRotatedGeoH() - getGeoH()) / 2.0F;
    }

    public double getRotatedMaxGeoY() {
        return maxGeoY + (getRotatedGeoH() - getGeoH()) / 2.0F;
    }

    public long getRotatedSvgW() {
        return (long) (getRotatedBitmapW() * svgFactor);
    }

    public long getRotatedSvgH() {
        return (long) (getRotatedBitmapH() * svgFactor);
    }

    public long getStraightSvgW() {
        return (long) (getStraightBitmapW() * svgFactor);
    }

    public long getStraightSvgH() {
        return (long) (getStraightBitmapH() * svgFactor);
    }

    public float getPaperW() {
        return paperWidth;
    }

    public float getPaperH() {
        return paperHeight;
    }

    public void setMapPos(float x, float y) {
        paperPosX = x;
        paperPosY = y;
    }

    public float getPaperPosX() {
        return paperPosX;
    }

    public float getPaperPosY() {
        return paperPosY;
    }

    /**
     * @return a transformer with paper dimensions, but that takes into account
     * the position of the map and its rotation.
     */
    public AffineTransform getBaseTransform() {
        final AffineTransform result = AffineTransform.getTranslateInstance(
                paperPosX, paperPosY);
        if (rotation != 0.0F) {
            result.translate(getPaperW() / 2, getPaperH() / 2);
            result.rotate(rotation);
            result.translate(-getRotatedPaperW() / 2, -getRotatedPaperH() / 2);
        }
        return result;
    }

    /**
     * @param reverseRotation True to do the rotation in the other direction
     * @return The affine transformation to go from geographic coordinated to
     * paper coordinates
     */
    public AffineTransform getGeoTransform(boolean reverseRotation) {
        final AffineTransform result = AffineTransform.getTranslateInstance(
                paperPosX, paperPosY);
        if (rotation != 0.0F) {
            result.rotate((reverseRotation ? -1 : 1) * rotation,
                    getPaperW() / 2, getPaperH() / 2);
        }
        result.scale(getPaperW() / getGeoW(), getPaperH() / getGeoH());
        result.translate(-minGeoX, -minGeoY);
        return result;
    }

    public AffineTransform getSvgTransform() {
        final AffineTransform result = getBaseTransform();
        result.scale(getPaperW() / getStraightSvgW(), getPaperH()
                                                      / getStraightSvgH());
        return result;
    }

    public AffineTransform getPdfTransform() {
        final AffineTransform result = getBaseTransform();
        result.scale(getPaperW() / getStraightBitmapW(), getPaperH()
                                                         / getStraightBitmapH());
        return result;
    }

    public AffineTransform getBitmapTransform() {
        return getPdfTransform();
    }

    public double getScale() {
        return scale;
    }

    public void zoom(Transformer mainTransformer, float factor) {
        double destW = mainTransformer.getGeoW() / factor;
        double destH = mainTransformer.getGeoH() / factor;

        // fix aspect ratio
        if (destW / destH > getGeoW() / getGeoH()) {
            destH = getGeoH() * destW / getGeoW();
        } else {
            destW = getGeoW() * destH / getGeoH();
        }

        double cX = (minGeoX + maxGeoX) / 2.0f;
        double cY = (minGeoY + maxGeoY) / 2.0f;
        pixelPerGeoUnit = pixelPerGeoUnit * getGeoW() / destW;
        minGeoX = cX - destW / 2.0f;
        maxGeoX = cX + destW / 2.0f;
        minGeoY = cY - destH / 2.0f;
        maxGeoY = cY + destH / 2.0f;
    }

    public Transformer clone() {
        try {
            return (Transformer) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public double getMinGeoX() {
        return minGeoX;
    }

    public double getMinGeoY() {
        return minGeoY;
    }

    public double getMaxGeoX() {
        return maxGeoX;
    }

    public double getMaxGeoY() {
        return maxGeoY;
    }

    public float getSvgFactor() {
        return svgFactor;
    }

    public double getRotation() {
        return rotation;
    }

    public void setClipping(PdfContentByte dc) {
        dc.rectangle(paperPosX, paperPosY, paperWidth, paperHeight);
        dc.clip();
        dc.newPath();
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    public double getResolution() {
        return 1 / pixelPerGeoUnit;
    }

    public void setResolution(double resolution) {
        this.pixelPerGeoUnit = 1 / resolution;
    }

    public int getDpi() {
        return dpi;
    }

    public boolean strictEpsg4326() {
        return this.strictEpsg4326;
    }
}
