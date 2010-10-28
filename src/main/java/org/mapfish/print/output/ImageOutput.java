package org.mapfish.print.output;

import com.lowagie.text.DocumentException;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.util.PDFImageWriter;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.TimeLogger;
import org.mapfish.print.utils.PJsonObject;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * User: jeichar
 * Date: Oct 18, 2010
 * Time: 2:00:30 PM
 */
public class ImageOutput extends OutputFormat {

    public static final Logger LOGGER = Logger.getLogger(ImageOutput.class);

    private static final float MARGIN = 20;

    public RenderingContext print(MapPrinter printer, PJsonObject jsonSpec, OutputStream out, String referer) throws DocumentException {
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("mapfishprint",".pdf");
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
            drawImage(out,images);
            timeLog.done();

            return context;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(tmpFile != null) {
                tmpFile.delete();
                tmpFile.deleteOnExit();
            }
        }
    }

    private void drawImage(OutputStream out, List<? extends RenderedImage> images) throws IOException {

        ParameterBlock pbMosaic=new ParameterBlock();
        float height = 0;
        float width = 0;

        int i = 0;
        for (RenderedImage source : images) {
            i++;
            LOGGER.debug("Adding page image "+i+" bounds: ["+0+","+height+" "+source.getWidth()+","+(height + source.getHeight())+"]");
            ParameterBlock pbTranslate=new ParameterBlock();
            pbTranslate.addSource(source);
            pbTranslate.add(0f);
            pbTranslate.add(height);
            RenderedOp translated = JAI.create("translate", pbTranslate);

            pbMosaic.addSource(translated);

            height += source.getHeight() + MARGIN;
            if(width < source.getWidth()) width = source.getWidth();
        }

        TileCache cache = JAI.createTileCache((long) (height * width * 400) );
        RenderingHints hints = new RenderingHints(JAI.KEY_TILE_CACHE, cache);

        RenderedOp mosaic = JAI.create("mosaic", pbMosaic,hints);
        ImageIO.write(mosaic, "TIFF",out);
    }
   private List<BufferedImage> createImages(PJsonObject jsonSpec, File tmpFile, RenderingContext context) throws IOException {
       List<BufferedImage> images = new ArrayList<BufferedImage>();
        PDDocument pdf = PDDocument.load(tmpFile);
        try {
            List<PDPage> pages = pdf.getDocumentCatalog().getAllPages();

            for (PDPage page : pages) {
                BufferedImage img = page.convertToImage(BufferedImage.TYPE_4BYTE_ABGR, context.calculateDPI(jsonSpec));
                images.add(img);
            }
        } finally {
            pdf.close();
        }
       return images;
    }
    
    public boolean accepts(String id) {
        return id.equalsIgnoreCase("png");
    }
}
