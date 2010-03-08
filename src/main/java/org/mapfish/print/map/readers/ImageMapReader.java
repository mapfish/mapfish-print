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
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mapfish.print.InvalidJsonValueException;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.MapTileTask;
import org.mapfish.print.map.ParallelMapTileLoader;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;

import java.awt.geom.AffineTransform;
import java.net.URI;
import java.util.List;

/**
 * Renders using a georeferenced image directly.
 */
public class ImageMapReader extends MapReader {
    private static final Log LOGGER = LogFactory.getLog(ImageMapReader.class);

    private final String name;
    private final RenderingContext context;
    private final URI baseUrl;
    private final float extentMinX;
    private final float extentMinY;
    private final float extentMaxX;
    private final float extentMaxY;

    protected ImageMapReader(RenderingContext context, PJsonObject params) {
        super(params);
        name = params.getString("name");
        this.context = context;
        try {
            baseUrl = new URI(params.getString("baseURL"));
        } catch (Exception e) {
            throw new InvalidJsonValueException(params, "baseURL", params.getString("baseURL"), e);
        }
        PJsonArray extent = params.getJSONArray("extent");
        extentMinX = extent.getFloat(0);
        extentMinY = extent.getFloat(1);
        extentMaxX = extent.getFloat(2);
        extentMaxY = extent.getFloat(3);

        //we don't really care about the pixel size
//        PJsonArray size = params.getJSONArray("pixelSize");
//        pixelW = size.getInt(0);
//        pixelH = size.getInt(1);

        checkSecurity(context, params);
    }

    private void checkSecurity(RenderingContext context, PJsonObject params) {
        try {
            if (!context.getConfig().validateUri(baseUrl)) {
                throw new InvalidJsonValueException(params, "baseURL", baseUrl);
            }
        } catch (Exception e) {
            throw new InvalidJsonValueException(params, "baseURL", baseUrl, e);
        }
    }

    public void render(final Transformer transformer, ParallelMapTileLoader parallelMapTileLoader, String srs, boolean first) {
        LOGGER.debug(baseUrl);

        parallelMapTileLoader.addTileToLoad(new MapTileTask() {
            public Image image;

            public void readTile() throws DocumentException {
                image = PDFUtils.createImage(context, extentMaxX - extentMinX, extentMaxY - extentMinY, baseUrl, 0);
                image.setAbsolutePosition(extentMinX, extentMinY);
            }

            public void renderOnPdf(PdfContentByte dc) throws DocumentException {
                //add the image using a geo->paper transformer
                final AffineTransform geoTransform = transformer.getGeoTransform(false);
                dc.transform(geoTransform);
                if (opacity < 1.0) {
                    PdfGState gs = new PdfGState();
                    gs.setFillOpacity(opacity);
                    gs.setStrokeOpacity(opacity);
                    dc.setGState(gs);
                }
                dc.addImage(image);
            }
        });
    }

    public boolean testMerge(MapReader other) {
        return false;
    }

    @Override
    protected boolean canMerge(MapReader other) {
        return false;
    }

    public String toString() {
        return name;
    }

    public static void create(List<MapReader> target, RenderingContext context, PJsonObject params) {
        target.add(new ImageMapReader(context, params));
    }
}
