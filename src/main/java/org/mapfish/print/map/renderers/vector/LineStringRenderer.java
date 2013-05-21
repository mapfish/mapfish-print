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

package org.mapfish.print.map.renderers.vector;

import java.awt.geom.AffineTransform;

import org.mapfish.print.InvalidValueException;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.config.ColorWrapper;
import org.mapfish.print.utils.PJsonObject;

import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

public class LineStringRenderer extends GeometriesRenderer<LineString> {
    protected static void applyStyle(RenderingContext context, PdfContentByte dc, PJsonObject style, PdfGState state) {
        if (style == null) return;
        if (style.optString("strokeColor") != null) {
            dc.setColorStroke(ColorWrapper.convertColor(style.getString("strokeColor")));
        }
        if (style.optString("strokeOpacity") != null) {
            state.setStrokeOpacity(style.getFloat("strokeOpacity"));
        }
        final float width = style.optFloat("strokeWidth", 1) * context.getStyleFactor();
        dc.setLineWidth(width);
        final String linecap = style.optString("strokeLinecap");
        if (linecap != null) {
            if (linecap.equalsIgnoreCase("butt")) {
                dc.setLineCap(PdfContentByte.LINE_CAP_BUTT);
            } else if (linecap.equalsIgnoreCase("round")) {
                dc.setLineCap(PdfContentByte.LINE_CAP_ROUND);
            } else if (linecap.equalsIgnoreCase("square")) {
                dc.setLineCap(PdfContentByte.LINE_CAP_PROJECTING_SQUARE);
            } else {
                throw new InvalidValueException("strokeLinecap", linecap);
            }
        }
        final String dashStyle = style.optString("strokeDashstyle");
        if (dashStyle != null) {
            if (dashStyle.equalsIgnoreCase("dot")) {
                final float[] def = new float[]{0.1f, 2 * width};
                dc.setLineDash(def, 0);
            } else if (dashStyle.equalsIgnoreCase("dash")) {
                final float[] def = new float[]{2 * width, 2 * width};
                dc.setLineDash(def, 0);
            } else if (dashStyle.equalsIgnoreCase("dashdot")) {
                final float[] def = new float[]{3 * width, 2 * width, 0.1f, 2 * width};
                dc.setLineDash(def, 0);
            } else if (dashStyle.equalsIgnoreCase("longdash")) {
                final float[] def = new float[]{4 * width, 2 * width};
                dc.setLineDash(def, 0);
            } else if (dashStyle.equalsIgnoreCase("longdashdot")) {
                final float[] def = new float[]{5 * width, 2 * width, 0.1f, 2 * width};
                dc.setLineDash(def, 0);
            } else if (dashStyle.equalsIgnoreCase("solid")) {

            } else {
                throw new InvalidValueException("strokeDashstyle", dashStyle);
            }
        }
    }

    protected void renderImpl(RenderingContext context, PdfContentByte dc, PJsonObject style, LineString geometry, AffineTransform affineTransform) {
        PdfGState state = new PdfGState();
        applyStyle(context, dc, style, state);
        dc.setGState(state);
        Coordinate[] coords = geometry.getCoordinates();
        if (coords.length < 2) return;
        Coordinate coord = (Coordinate) coords[0].clone();
        transformCoordinate(coord, affineTransform);
        dc.moveTo((float) coord.x, (float) coord.y);
        for (int i = 1; i < coords.length; i++) {
            coord = (Coordinate) coords[i].clone();
            transformCoordinate(coord, affineTransform);
            dc.lineTo((float) coord.x, (float) coord.y);
        }
        if (style.optBool("stroke", true)) dc.stroke();
    }
}
