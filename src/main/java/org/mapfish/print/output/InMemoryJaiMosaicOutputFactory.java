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

package org.mapfish.print.output;
import org.mapfish.print.utils.PJsonObject;
import org.mapfish.print.RenderingContext;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDDocument;

import org.apache.log4j.Logger;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;
import javax.media.jai.operator.MosaicDescriptor;

import com.itextpdf.text.DocumentException;

/**
 * An output factory that uses pdf box to parse the pdf and create a collection of BufferedImages.
 *
 * Then using JAI Mosaic operation the buffered images are combined into one RenderableImage (virtual image)
 * and that is written to a file using ImageIO
 *
 * User: jeichar
 * Date: Oct 18, 2010
 * Time: 2:00:30 PM
 */
public class InMemoryJaiMosaicOutputFactory implements OutputFormatFactory {

    public List<String> formats() {
        try {
            String[] formats = ImageIO.getWriterFormatNames();
            return Arrays.asList(formats);
        } catch (Throwable t) {
            return new ArrayList<String>();
        }
    }

    public OutputFormat create(String format) {
        return new ImageOutput(format);
    }

    public String enablementStatus() {
         try {
            MosaicDescriptor.class.getSimpleName();
        } catch (Throwable e) {
            return "JAI required";
        }

        try {
            MosaicDescriptor.class.getSimpleName();
        } catch (Throwable e) {
            return "JAI MosaicDescriptor not available on classpath";
        }
        return null;
    }

    public static class ImageOutput extends AbstractImageFormat {

        public static final Logger LOGGER = Logger.getLogger(ImageOutput.class);

        public ImageOutput(String format) {
            super(format);
        }

        public RenderingContext print(PrintParams params) throws DocumentException {
            File tmpFile = null;
            try {
                tmpFile = File.createTempFile("mapfishprint", ".pdf");
                FileOutputStream tmpOut = new FileOutputStream(tmpFile);
                RenderingContext context;
                try {
                    context =  doPrint(params.withOutput(tmpOut));
                } finally {
                    tmpOut.close();
                }

                List<BufferedImage> images = createImages(params.jsonSpec, tmpFile, context);
                drawImage(params.outputStream, images);

                return context;
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (tmpFile != null) {
                    if (!tmpFile.delete()) {
                        LOGGER.warn(tmpFile+" was not able to be deleted for unknown reason.  Will try again on shutdown");
                    }
                    tmpFile.deleteOnExit();
                }
            }
        }

        private void drawImage(OutputStream out, List<? extends RenderedImage> images) throws IOException {

            ParameterBlock pbMosaic = new ParameterBlock();
            float height = 0;
            float width = 0;

            int i = 0;
            for (RenderedImage source : images) {
                i++;
                LOGGER.debug("Adding page image " + i + " bounds: [" + 0 + "," + height + " " + source.getWidth() + "," + (height + source.getHeight()) + "]");
                ParameterBlock pbTranslate = new ParameterBlock();
                pbTranslate.addSource(source);
                pbTranslate.add(0f);
                pbTranslate.add(height);
                RenderedOp translated = JAI.create("translate", pbTranslate);

                pbMosaic.addSource(translated);

                height += source.getHeight() + MARGIN;
                if (width < source.getWidth()) width = source.getWidth();
            }

            TileCache cache = JAI.createTileCache((long) (height * width * 400));
            RenderingHints hints = new RenderingHints(JAI.KEY_TILE_CACHE, cache);

            RenderedOp mosaic = JAI.create("mosaic", pbMosaic, hints);
            ImageIO.write(mosaic, format, out);
        }

        private List<BufferedImage> createImages(PJsonObject jsonSpec, File tmpFile, RenderingContext context) throws IOException {
            List<BufferedImage> images = new ArrayList<BufferedImage>();
            PDDocument pdf = PDDocument.load(tmpFile);
            try {
                @SuppressWarnings("unchecked")
				List<PDPage> pages = pdf.getDocumentCatalog().getAllPages();

                for (PDPage page : pages) {
                    BufferedImage img = page.convertToImage(BufferedImage.TYPE_4BYTE_ABGR, calculateDPI(context, jsonSpec));
                    images.add(img);
                }
            } finally {
                pdf.close();
            }
            return images;
        }
    }
}
