package org.mapfish.print.output;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import org.mapfish.print.ImageUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import static org.mapfish.print.Constants.PDF_DPI;

/**
 * An PDF output format that uses Jasper reports to generate the result.
 */
public final class JasperReportImageOutputFormat extends AbstractJasperReportOutputFormat
        implements OutputFormat {

    private int imageType = BufferedImage.TYPE_INT_ARGB;

    private String fileSuffix;

    @Override
    public String getContentType() {
        return "image/" + this.fileSuffix;
    }

    @Override
    public String getFileSuffix() {
        return this.fileSuffix;
    }

    public void setFileSuffix(final String fileSuffix) {
        this.fileSuffix = fileSuffix;
    }

    @Override
    protected void doExport(final OutputStream outputStream, final Print print)
            throws JRException, IOException {
        JasperPrint jasperPrint = print.print;
        final int numPages = jasperPrint.getPages().size();

        final float dpiRatio = (float) (print.dpi / PDF_DPI);
        final int pageHeightOnImage = (int) (jasperPrint.getPageHeight() * dpiRatio);
        final int pageWidthOnImage = (int) (jasperPrint.getPageWidth() * dpiRatio);
        final int separatorHeight = 1;
        final int separatorHeightOnImage = (int) (separatorHeight * dpiRatio);

        BufferedImage reportImage = new BufferedImage(
                pageWidthOnImage, numPages * pageHeightOnImage + (numPages - 1) * separatorHeightOnImage,
                this.imageType);

        Graphics2D graphics2D = reportImage.createGraphics();
        try {
            JasperPrintManager printManager = JasperPrintManager.getInstance(print.context);

            for (int pageIndex = 0; pageIndex < numPages; pageIndex++) {
                Image pageImage = printManager.printToImage(jasperPrint, pageIndex, dpiRatio);

                graphics2D.drawImage(pageImage,
                                     0, (pageHeightOnImage + separatorHeight) * pageIndex,
                                     pageWidthOnImage,
                                     (pageHeightOnImage + separatorHeight) * pageIndex + pageHeightOnImage,
                                     0, 0,
                                     pageWidthOnImage, pageHeightOnImage, null);
            }

            // draw separator line between the pages
            final Stroke stroke = new BasicStroke(separatorHeightOnImage);
            for (int pageIndex = 0; pageIndex < numPages - 1; pageIndex++) {
                graphics2D.setColor(Color.black);
                graphics2D.setStroke(stroke);
                int y = (pageHeightOnImage + separatorHeight) * pageIndex + pageHeightOnImage;
                graphics2D.drawLine(
                        0, y,
                        pageWidthOnImage, y);
            }
        } finally {
            graphics2D.dispose();
        }

        ImageUtils.writeImage(reportImage, getFileSuffix(), outputStream);
    }

    /**
     * One of {@link java.awt.image.BufferedImage} TYPE_ values.
     *
     * @param imageType the buffered image type to create.
     */
    public void setImageType(final int imageType) {
        this.imageType = imageType;
    }
}
