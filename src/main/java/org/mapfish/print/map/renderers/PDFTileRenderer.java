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

package org.mapfish.print.map.renderers;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.apache.log4j.Logger;
import org.mapfish.print.InvalidValueException;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.MapTileTask;
import org.mapfish.print.map.ParallelMapTileLoader;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;

public class PDFTileRenderer extends TileRenderer {
    public static final Logger LOGGER = Logger.getLogger(PDFTileRenderer.class);

    public void render(final Transformer transformer, List<URI> uris, ParallelMapTileLoader parallelMapTileLoader, final RenderingContext context, final float opacity, int nbTilesHorizontal, double offsetX, double offsetY, long bitmapTileW, long bitmapTileH) throws IOException {
        if (uris.size() != 1) {
            //tiling not supported in PDF
            throw new InvalidValueException("format", "application/x-pdf");
        }
        final URI uri = uris.get(0);

        parallelMapTileLoader.addTileToLoad(new MapTileTask() {
            public PdfImportedPage pdfMap;

            protected void readTile() throws IOException, DocumentException {
                LOGGER.debug(uri);
                PdfReader reader = new PdfReader(uri.toURL());
                synchronized (context.getPdfLock()) {
                    pdfMap = context.getWriter().getImportedPage(reader, 1);

                    if (opacity < 1.0) {
                        PdfGState gs = new PdfGState();
                        gs.setFillOpacity(opacity);
                        gs.setStrokeOpacity(opacity);
                        //gs.setBlendMode(PdfGState.BM_SOFTLIGHT);
                        pdfMap.setGState(gs);
                    }
                }
            }

            protected void renderOnPdf(PdfContentByte dc) throws DocumentException {
                dc.transform(transformer.getPdfTransform());
                dc.addTemplate(pdfMap, 0, 0);
            }
        });
    }
}
