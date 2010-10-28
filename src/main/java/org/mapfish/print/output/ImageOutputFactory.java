package org.mapfish.print.output;

import com.lowagie.text.DocumentException;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.TimeLogger;
import org.mapfish.print.utils.PJsonObject;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;
import javax.media.jai.operator.MosaicDescriptor;
import java.awt.*;
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

/**
 * User: jeichar
 * Date: Oct 18, 2010
 * Time: 2:00:30 PM
 */
public class ImageOutputFactory implements OutputFormatFactory {

    public List<String> formats() {
        String[] formats = ImageIO.getWriterFormatNames();
        return Arrays.asList(formats);
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

        public RenderingContext print(MapPrinter printer, PJsonObject jsonSpec, OutputStream out, String referer) throws DocumentException {
            File tmpFile = null;
            try {
                tmpFile = File.createTempFile("mapfishprint", ".pdf");
                FileOutputStream tmpOut = new FileOutputStream(tmpFile);
                RenderingContext context;
                try {
                    TimeLogger timeLog = TimeLogger.info(LOGGER, "PDF Creation");
                    context = printer.print(jsonSpec, tmpOut, referer);
                    timeLog.done();
                } finally {
                    tmpOut.close();
                }

                TimeLogger timeLog = TimeLogger.info(LOGGER, "Pdf to image conversion");
                List<BufferedImage> images = createImages(jsonSpec, tmpFile, context);
                timeLog.done();

                timeLog = TimeLogger.info(LOGGER, "Write Image");
                drawImage(out, images);
                timeLog.done();

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
            ImageIO.write(mosaic, "TIFF", out);
        }

        private List<BufferedImage> createImages(PJsonObject jsonSpec, File tmpFile, RenderingContext context) throws IOException {
            List<BufferedImage> images = new ArrayList<BufferedImage>();
            PDDocument pdf = PDDocument.load(tmpFile);
            try {
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
