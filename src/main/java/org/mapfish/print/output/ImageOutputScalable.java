package org.mapfish.print.output;

import com.lowagie.text.DocumentException;
import com.sun.media.jai.codec.FileSeekableStream;
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
public class ImageOutputScalable extends OutputFormat {

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
            List<ImageInfo> images = createImages(jsonSpec, tmpFile, context);
            timeLog.done();

            timeLog = TimeLogger.info(LOGGER, "Write Mosaiced Image");            
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

    private void drawImage(OutputStream out, List<ImageInfo> images) throws IOException {

        ParameterBlock pbMosaic=new ParameterBlock();
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
            LOGGER.debug("Adding page image "+i+" bounds: ["+0+","+height+" "+source.getWidth()+","+(height + source.getHeight())+"]");
            RenderedOp translated = translateImage(height, source);

            pbMosaic.addSource(translated);

            height += imageinfo.height + MARGIN;
            if(width < imageinfo.width) width = imageinfo.width;
        }

        RenderedOp mosaic = JAI.create("mosaic", pbMosaic);
        ImageIO.write(mosaic, "png",out);
    }

    private RenderedOp translateImage(float height, RenderedImage source) {
        ParameterBlock pbTranslate=new ParameterBlock();
        pbTranslate.addSource(source);
        pbTranslate.add(0f);
        pbTranslate.add(height);
        RenderedOp translated = JAI.create("translate", pbTranslate);
        return translated;
    }

    private List<ImageInfo> createImages(PJsonObject jsonSpec, File tmpFile, RenderingContext context) throws IOException {
       List<ImageInfo> images = new ArrayList<ImageInfo>();
        PDDocument pdf = PDDocument.load(tmpFile);
        try {
            List<PDPage> pages = pdf.getDocumentCatalog().getAllPages();

            for (PDPage page : pages) {
                BufferedImage img = page.convertToImage(BufferedImage.TYPE_4BYTE_ABGR, context.calculateDPI(jsonSpec));
                File file = File.createTempFile("pdfToImage", "tiff");
                ImageIO.write(img, "TIFF", file);
                images.add(new ImageInfo(file,img.getWidth(), img.getHeight()));
            }
        } finally {
            pdf.close();
        }
       return images;
    }
    
    public boolean accepts(String id) {
        return id.equalsIgnoreCase("png");
    }

    class ImageInfo {
        final File imageFile;
        final int width,height;

        ImageInfo(File imageFile, int width, int height) {
            this.imageFile = imageFile;
            
            this.width = width;
            this.height = height;
        }
    }
}
