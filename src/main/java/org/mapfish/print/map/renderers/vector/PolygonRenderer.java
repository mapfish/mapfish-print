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

import com.itextpdf.awt.geom.AffineTransform;

import org.mapfish.print.RenderingContext;
import org.mapfish.print.config.ColorWrapper;
import org.mapfish.print.utils.PJsonObject;

import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfGState;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

class PolygonRenderer extends GeometriesRenderer<Polygon> {
    protected static void applyStyle(RenderingContext context, PdfContentByte dc, PJsonObject style, PdfGState state) {
        if (style == null) return;
        LineStringRenderer.applyStyle(context, dc, style, state);

        if (style.optString("fillColor") != null) {
            dc.setColorFill(ColorWrapper.convertColor(style.getString("fillColor")));
        }
        if (style.optString("fillOpacity") != null) {
            state.setFillOpacity(style.getFloat("fillOpacity"));
        }
    }

    protected void renderImpl(RenderingContext context, PdfContentByte dc, PJsonObject style, Polygon geometry, AffineTransform affineTransform) {
        PdfGState state = new PdfGState();
        applyStyle(context, dc, style, state);
        dc.setGState(state);

        final LineString ring = geometry.getExteriorRing();
        renderRing(dc, ring, affineTransform);
        for (int i = 0; i < geometry.getNumInteriorRing(); ++i) {
            renderRing(dc, geometry.getInteriorRingN(i), affineTransform);
        }
        renderStrokeAndFill(dc, style.optBool("stroke", true), style.optBool("fill", true));
    }

    private void renderRing(PdfContentByte dc, LineString ring, AffineTransform affineTransform) {
        Coordinate[] coords = ring.getCoordinates();
        if (coords.length < 3) return;
        Coordinate coord = (Coordinate) coords[0].clone();
        transformCoordinate(coord, affineTransform);
        dc.moveTo((float) coord.x, (float) coord.y);
        for (int i = 1; i < coords.length - 1; i++) {
            coord = (Coordinate) coords[i].clone();
            transformCoordinate(coord, affineTransform);
            dc.lineTo((float) coord.x, (float) coord.y);
        }
        dc.closePath();
    }

    private void renderStrokeAndFill(PdfContentByte dc, boolean stroke, boolean fill) {
        if (stroke && fill) dc.eoFillStroke();
        else if (stroke) dc.stroke();
        else if (fill) dc.eoFill();
    }
}
