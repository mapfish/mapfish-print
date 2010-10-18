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
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import org.apache.batik.ext.awt.RenderingHintsKeyExt;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.print.PrintTranscoder;
import org.apache.log4j.Logger;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.mapfish.print.InvalidValueException;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.map.MapTileTask;
import org.mapfish.print.map.ParallelMapTileLoader;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

public class SVGTileRenderer extends TileRenderer {
    public static final Logger LOGGER = Logger.getLogger(SVGTileRenderer.class);

    private static final Document svgZoomOut;

    static {
        DOMParser parser = new DOMParser();
        String svgZoomFileName = "svgZoomOut.xsl";
        final InputStream stream = SVGTileRenderer.class.getResourceAsStream(svgZoomFileName);
        if (stream == null) {
            String file = SVGTileRenderer.class.getResource(".").getPath() + svgZoomFileName;
            throw new RuntimeException("Cannot find the SVG transformation XSLT: expected it to be in: "+file);
        }
        try {
            final InputSource inputSource = new InputSource(stream);
            inputSource.setSystemId(".");
            parser.parse(inputSource);

            svgZoomOut = parser.getDocument();

        } catch (Exception e) {
            throw new RuntimeException("Cannot parse the SVG transformation XSLT", e);
        } finally {
            if(stream != null) {
                try {
                    stream.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    public void render(final Transformer transformer, java.util.List<URI> uris, ParallelMapTileLoader parallelMapTileLoader, final RenderingContext context, final float opacity, int nbTilesHorizontal, float offsetX, float offsetY, long bitmapTileW, long bitmapTileH) throws IOException {
        if (uris.size() != 1) {
            //tiling not supported in SVG
            throw new InvalidValueException("format", "application/x-pdf");
        }

        final URI uri = uris.get(0);

        parallelMapTileLoader.addTileToLoad(new MapTileTask() {
            public PrintTranscoder pt;

            @Override
            protected void readTile() throws IOException, DocumentException {
                LOGGER.debug(uri);
                final TranscoderInput ti = getTranscoderInput(uri.toURL(), transformer, context);
                if (ti != null) {
                    pt = new PrintTranscoder();
                    pt.transcode(ti, null);
                }
            }

            @Override
            protected void renderOnPdf(PdfContentByte dc) throws DocumentException {
                dc.transform(transformer.getSvgTransform());

                if (opacity < 1.0) {
                    PdfGState gs = new PdfGState();
                    gs.setFillOpacity(opacity);
                    gs.setStrokeOpacity(opacity);
                    //gs.setBlendMode(PdfGState.BM_SOFTLIGHT);
                    dc.setGState(gs);
                }

                Graphics2D g2 = dc.createGraphics(transformer.getRotatedSvgW(), transformer.getRotatedSvgH());

                //avoid a warning from Batik
                System.setProperty("org.apache.batik.warn_destination", "false");
                g2.setRenderingHint(RenderingHintsKeyExt.KEY_TRANSCODING, RenderingHintsKeyExt.VALUE_TRANSCODING_PRINTING);
                g2.setRenderingHint(RenderingHintsKeyExt.KEY_AVOID_TILE_PAINTING, RenderingHintsKeyExt.VALUE_AVOID_TILE_PAINTING_ON);

                Paper paper = new Paper();
                paper.setSize(transformer.getRotatedSvgW(), transformer.getRotatedSvgH());
                paper.setImageableArea(0, 0, transformer.getRotatedSvgW(), transformer.getRotatedSvgH());
                PageFormat pf = new PageFormat();
                pf.setPaper(paper);
                pt.print(g2, pf, 0);
                g2.dispose();
            }
        });
    }

    private TranscoderInput getTranscoderInput(URL url, Transformer transformer, RenderingContext context) {
        final float zoomFactor = transformer.getSvgFactor() * context.getStyleFactor();
        if (svgZoomOut != null && zoomFactor != 1.0f) {
            try {
                DOMResult transformedSvg = new DOMResult();
                final TransformerFactory factory = TransformerFactory.newInstance();
                javax.xml.transform.Transformer xslt = factory.newTransformer(new DOMSource(svgZoomOut));

                //TODO: may want a different zoom factor in function of the layer and the type (symbol, line or font)
                xslt.setParameter("zoomFactor", zoomFactor);

                final URLConnection urlConnection = url.openConnection();
                if (context.getReferer() != null) {
                    urlConnection.setRequestProperty("Referer", context.getReferer());
                }
                final InputStream inputStream = urlConnection.getInputStream();

                Document doc;
                try {
                    xslt.transform(new StreamSource(inputStream), transformedSvg);
                    doc = (Document) transformedSvg.getNode();

                    if (LOGGER.isDebugEnabled()) {
                        printDom(doc);
                    }
                } finally {
                    inputStream.close();
                }
                return new TranscoderInput(doc);

            } catch (Exception e) {
                context.addError(e);
                return null;
            }
        } else {
            return new TranscoderInput(url.toString());
        }
    }

    /**
     * Just for debugging XML.
     */
    public static void printDom(Document doc) throws IOException {
        OutputFormat format = new OutputFormat(doc);
        format.setLineWidth(65);
        format.setIndenting(true);
        format.setIndent(2);

        OutputStream out = new ByteArrayOutputStream();

        XMLSerializer serializer = new XMLSerializer(out, format);
        serializer.serialize(doc);

        LOGGER.trace(out.toString());
        out.close();
    }

}
