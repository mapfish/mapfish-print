/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.map.renderers.vector;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Coordinate;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import org.mapfish.print.utils.PJsonObject;
import org.mapfish.print.config.ColorWrapper;
import org.mapfish.print.RenderingContext;

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

    protected void renderImpl(RenderingContext context, PdfContentByte dc, PJsonObject style, Polygon geometry) {
        PdfGState state = new PdfGState();
        applyStyle(context, dc, style, state);
        dc.setGState(state);

        final LineString ring = geometry.getExteriorRing();
        renderRing(dc, ring);
        for (int i = 0; i < geometry.getNumInteriorRing(); ++i) {
            renderRing(dc, geometry.getInteriorRingN(i));
        }
        dc.eoFillStroke();
    }

    private void renderRing(PdfContentByte dc, LineString ring) {
        Coordinate[] coords = ring.getCoordinates();
        if (coords.length < 3) return;
        dc.moveTo((float) coords[0].x, (float) coords[0].y);
        for (int i = 1; i < coords.length - 1; i++) {
            Coordinate coord = coords[i];
            dc.lineTo((float) coord.x, (float) coord.y);
        }
        dc.closePath();
    }
}
