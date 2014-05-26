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
import com.sun.media.jai.codec.FileSeekableStream;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import com.itextpdf.text.DocumentException;

/**
 * Similar to {@link InMemoryJaiMosaicOutputFactory} in that it uses pdf box to parse pdf.  However it writes
 * each page to disk as an image before combining them using JAI mosaic.
 *
 * @author jeichar
 */
public class FileCachingJaiMosaicOutputFactory extends InMemoryJaiMosaicOutputFactory {

    @Override
    public OutputFormat create(String format) {
        return new ImageOutputScalable(format);
    }

    public String enablementStatus() {
        if(super.enablementStatus() != null) {
            return super.enablementStatus();
        }
        if(!formats().contains("TIFF")) {
            return "TIFF not supported by ImageIO";
        }
        return null;
    }

    public static class ImageOutputScalable extends AbstractImageFormat {

        public static final Logger LOGGER = Logger.getLogger(ImageOutputScalable.class);

        public ImageOutputScalable(String format) {
            super(format);
        }

        public RenderingContext print(PrintParams params) throws DocumentException {
            File tmpFile = null;
            try {
                tmpFile = File.createTempFile("mapfishprint", ".pdf");
                FileOutputStream tmpOut = new FileOutputStream(tmpFile);
                RenderingContext context;
                try {
                    context = doPrint(params.withOutput(tmpOut));
                } finally {
                    tmpOut.close();
                }

                List<ImageInfo> images = createImages(params.jsonSpec, tmpFile, context);

                drawImage(params.outputStream, images);

                return context;
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (tmpFile != null) {
                    if(!tmpFile.delete()) {
                        LOGGER.warn(tmpFile+" was not able to be deleted for unknown reason.  Will try again on shutdown");
                    }
                    tmpFile.deleteOnExit();
                }
            }
        }

        private void drawImage(OutputStream out, List<ImageInfo> images) throws IOException {

            ParameterBlock pbMosaic = new ParameterBlock();
            float height = 0;
            float width = 0;

            int i = 0;
            for (ImageInfo imageinfo : images) {
                ParameterBlock pb = new ParameterBlock();
                pb.add(new FileSeekableStream(imageinfo.imageFile));
                pb.add(null);
                pb.add(null);
                RenderedOp source = JAI.create("TIFF", pb);
                i++;
                LOGGER.debug("Adding page image " + i + " bounds: [" + 0 + "," + height + " " + source.getWidth() + "," + (height + source.getHeight()) + "]");
                RenderedOp translated = translateImage(height, source);

                pbMosaic.addSource(translated);

                height += imageinfo.height + MARGIN;
                if (width < imageinfo.width) width = imageinfo.width;
            }

            RenderedOp mosaic = JAI.create("mosaic", pbMosaic);
            ImageIO.write(mosaic, format, out);
        }

        private RenderedOp translateImage(float height, RenderedImage source) {
            ParameterBlock pbTranslate = new ParameterBlock();
            pbTranslate.addSource(source);
            pbTranslate.add(0f);
            pbTranslate.add(height);
            return JAI.create("translate", pbTranslate);
        }

        private List<ImageInfo> createImages(PJsonObject jsonSpec, File tmpFile, RenderingContext context) throws IOException {
            List<ImageInfo> images = new ArrayList<ImageInfo>();
            PDDocument pdf = PDDocument.load(tmpFile);
            try {
                @SuppressWarnings("unchecked")
				List<PDPage> pages = pdf.getDocumentCatalog().getAllPages();

                for (PDPage page : pages) {
                    BufferedImage img = page.convertToImage(BufferedImage.TYPE_INT_RGB, calculateDPI(context, jsonSpec));
                    File file = File.createTempFile("pdfToImage", "tiff");
                    ImageIO.write(img, "TIFF", file);
                    images.add(new ImageInfo(file, img.getWidth(), img.getHeight()));
                }
            } finally {
                pdf.close();
            }
            return images;
        }

    }

    static class ImageInfo {
        final File imageFile;
        final int width, height;

        ImageInfo(File imageFile, int width, int height) {
            this.imageFile = imageFile;

            this.width = width;
            this.height = height;
        }
    }
}
