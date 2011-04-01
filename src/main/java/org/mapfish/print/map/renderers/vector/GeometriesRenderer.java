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

import java.util.HashMap;
import java.util.Map;

import org.mapfish.print.RenderingContext;
import org.mapfish.print.utils.PJsonObject;

import com.lowagie.text.pdf.PdfContentByte;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * iText renderer for JTS geometries.
 */
public abstract class GeometriesRenderer<T extends Geometry> {
    private static final Map<Class<? extends Geometry>, GeometriesRenderer<?>> RENDERERS =
            new HashMap<Class<? extends Geometry>, GeometriesRenderer<?>>();

    static {
        RENDERERS.put(LineString.class, new LineStringRenderer());
        RENDERERS.put(LinearRing.class, new LineStringRenderer());
        RENDERERS.put(GeometryCollection.class, new GeometryCollectionRenderer());
        RENDERERS.put(MultiLineString.class, new GeometryCollectionRenderer());
        RENDERERS.put(MultiPoint.class, new GeometryCollectionRenderer());
        RENDERERS.put(MultiPolygon.class, new GeometryCollectionRenderer());
        RENDERERS.put(Polygon.class, new PolygonRenderer());
        RENDERERS.put(Point.class, new PointRenderer());
    }

    @SuppressWarnings({"RawUseOfParameterizedType", "unchecked"})
    protected static void render(RenderingContext context, PdfContentByte dc, PJsonObject style, Geometry geometry) {
        GeometriesRenderer renderer = RENDERERS.get(geometry.getClass());
        if (renderer == null) {
            throw new RuntimeException("Rendering of " + geometry.getClass().getName() + " not supported");
        }
        dc.saveState();
        try {
            renderer.renderImpl(context, dc, style, geometry);
            LabelRenderer.applyStyle(context, dc, style, geometry);
        } finally {
            dc.restoreState();
        }
    }

    protected abstract void renderImpl(RenderingContext context, PdfContentByte dc, PJsonObject style, T geometry);

    private static class GeometryCollectionRenderer extends GeometriesRenderer<GeometryCollection> {
        protected void renderImpl(RenderingContext context, PdfContentByte dc, PJsonObject style, GeometryCollection geometry) {
            for (int i = 0; i < geometry.getNumGeometries(); ++i) {
                render(context, dc, style, geometry.getGeometryN(i));
            }
        }
    }

}
