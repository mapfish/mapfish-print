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

package org.mapfish.print.map.readers;

import com.itextpdf.awt.geom.AffineTransform;
import java.net.URI;
import java.util.Collections;
import java.util.List;

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

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfGState;

/**
 * Renders using a georeferenced image directly.
 */
public class ImageMapReader extends MapReader {
    public static class Factory implements MapReaderFactory {

        @Override
        public List<? extends MapReader> create(String type, RenderingContext context,
                PJsonObject params) {
            return Collections.singletonList(new ImageMapReader(context, params));
        }
    }

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
}
