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

package org.mapfish.print.map.renderers;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.MapTileTask;
import org.mapfish.print.map.ParallelMapTileLoader;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.URI;
import java.util.List;

public class BitmapTileRenderer extends TileRenderer {
    private static final Log LOGGER = LogFactory.getLog(BitmapTileRenderer.class);

    public void render(Transformer transformer, List<URI> uris, ParallelMapTileLoader parallelMapTileLoader, final RenderingContext context, final float opacity, int nbTilesHorizontal, float offsetX, float offsetY, final long bitmapTileW, final long bitmapTileH) throws IOException {
        final AffineTransform bitmapTransformer = transformer.getBitmapTransform();
        final double rotation = transformer.getRotation();

        for (int i = 0; i < uris.size(); i++) {
            final URI uri = uris.get(i);
            if (uri == null) {
                continue;
            }

            final int line = i / nbTilesHorizontal;
            final int col = i % nbTilesHorizontal;
            final float posX = 0 - offsetX + col * bitmapTileW;
            final float posY = 0 - offsetY + line * bitmapTileH;

            if (rotation != 0.0 && !isTileVisible(posX, posY, bitmapTileW, bitmapTileH, bitmapTransformer, transformer)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Not needed: " + uri);
                }
                continue;
            }

            parallelMapTileLoader.addTileToLoad(new MapTileTask() {
                public Image map;

                protected void readTile() throws IOException, DocumentException {
                    map = PDFUtils.getImage(context, uri, bitmapTileW, bitmapTileH);
                    map.setAbsolutePosition(posX, posY);
                }

                protected void renderOnPdf(PdfContentByte dc) throws DocumentException {
                    dc.transform(bitmapTransformer);
                    if (opacity < 1.0) {
                        PdfGState gs = new PdfGState();
                        gs.setFillOpacity(opacity);
                        gs.setStrokeOpacity(opacity);
                        dc.setGState(gs);
                    }
                    dc.addImage(map);
                }
            });
        }
    }

    private boolean isTileVisible(float x, float y, long w, long h, AffineTransform bitmapTransformer, Transformer transformer) {
        GeometryFactory gf = new GeometryFactory();
        Polygon page = gf.createPolygon(
                gf.createLinearRing(new Coordinate[]{
                        new Coordinate(transformer.getPaperPosX(), transformer.getPaperPosY()),
                        new Coordinate(transformer.getPaperPosX() + transformer.getPaperW(), transformer.getPaperPosY()),
                        new Coordinate(transformer.getPaperPosX() + transformer.getPaperW(), transformer.getPaperPosY() + transformer.getPaperH()),
                        new Coordinate(transformer.getPaperPosX(), transformer.getPaperPosY() + transformer.getPaperH()),
                        new Coordinate(transformer.getPaperPosX(), transformer.getPaperPosY()),
                }), null);

        Point2D.Float ll = new Point2D.Float();
        Point2D.Float lr = new Point2D.Float();
        Point2D.Float ur = new Point2D.Float();
        Point2D.Float ul = new Point2D.Float();
        bitmapTransformer.transform(new Point2D.Float(x, y), ll);
        bitmapTransformer.transform(new Point2D.Float(x + w, y), lr);
        bitmapTransformer.transform(new Point2D.Float(x + w, y + h), ur);
        bitmapTransformer.transform(new Point2D.Float(x, y + h), ul);
        Polygon tile = gf.createPolygon(
                gf.createLinearRing(new Coordinate[]{
                        new Coordinate(ll.getX(), ll.getY()),
                        new Coordinate(lr.getX(), lr.getY()),
                        new Coordinate(ur.getX(), ur.getY()),
                        new Coordinate(ul.getX(), ul.getY()),
                        new Coordinate(ll.getX(), ll.getY()),
                }), null
        );

        return page.intersects(tile);
    }
}
