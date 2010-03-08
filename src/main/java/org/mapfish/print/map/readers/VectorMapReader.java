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

package org.mapfish.print.map.readers;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfContentByte;
import org.json.JSONException;
import org.mapfish.geo.MfGeo;
import org.mapfish.geo.MfGeoJSONReader;
import org.mapfish.print.InvalidJsonValueException;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.MapTileTask;
import org.mapfish.print.map.ParallelMapTileLoader;
import org.mapfish.print.map.renderers.vector.FeaturesRenderer;
import org.mapfish.print.map.renderers.vector.StyledMfGeoFactory;
import org.mapfish.print.utils.PJsonObject;

import java.util.List;

/**
 * Render vector layers. The geometries and the styling comes directly from the spec JSON.
 * It expects the following attributes from the JSON spec:
 * <ul>
 * <li>style: 'Vector'
 * <li>geoJson: the geoJson to render
 * <li>styleProperties: Name of the property within the features to use as style name (defaults
 * to '_style'). The given property may contain a style object directly.
 * <li>styles: dictonary of styles. One style is defined as in OpenLayers.Feature.Vector.style
 * <li>name: the layer name.
 * </ul>
 */
public class VectorMapReader extends MapReader {
    private final MfGeo geo;
    private final RenderingContext context;
    private final String name;

    public VectorMapReader(RenderingContext context, PJsonObject params) {
        super(params);
        this.context = context;

        final PJsonObject geoJson = params.getJSONObject("geoJson");
        final String styleProperty = params.optString("styleProperty", "_style");
        final PJsonObject styles = params.optJSONObject("styles");
        try {
            final MfGeoJSONReader reader = new MfGeoJSONReader(new StyledMfGeoFactory(styles, styleProperty));
            //noinspection deprecation
            geo = reader.decode(geoJson.getInternalObj());
        } catch (JSONException e) {
            throw new InvalidJsonValueException(params, "geoJson", geoJson.toString(), e);
        }
        name = params.optString("name", "vector");
    }

    public static void create(List<MapReader> target, RenderingContext context, PJsonObject params) {
        target.add(new VectorMapReader(context, params));
    }

    public void render(final Transformer transformer, ParallelMapTileLoader parallelMapTileLoader, String srs, boolean first) {
        parallelMapTileLoader.addTileToLoad(new MapTileTask.RenderOnly() {
            public void renderOnPdf(PdfContentByte dc) throws DocumentException {
                dc.transform(transformer.getGeoTransform(false));
                float styleFactor = context.getStyleFactor();
                context.setStyleFactor(styleFactor * transformer.getGeoW() / transformer.getPaperW());  //protected by the PDFLock acquired in the caller (ParallelMapTileLoader.handle)
                FeaturesRenderer.render(context, dc, geo);
                context.setStyleFactor(styleFactor);
            }
        });
    }

    public boolean testMerge(MapReader other) {
        return false;
    }

    public String toString() {
        return name;
    }
}
