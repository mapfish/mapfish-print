package org.mapfish.print.output;

import static java.util.Map.entry;
import static org.mapfish.print.Constants.PDF_DPI;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import org.mapfish.print.ImageUtils;

/** An PDF output format that uses Jasper reports to generate the result. */
public final class JasperReportImageOutputFormat extends AbstractJasperReportOutputFormat
    implements OutputFormat {

  // Use to get the image type from the output format
  public static final Map<String, Integer> IMAGE_TYPES =
      Map.ofEntries(
          entry("png", BufferedImage.TYPE_4BYTE_ABGR),
          entry("jpg", BufferedImage.TYPE_3BYTE_BGR),
          entry("jpeg", BufferedImage.TYPE_3BYTE_BGR),
          entry("tif", BufferedImage.TYPE_4BYTE_ABGR),
          entry("tiff", BufferedImage.TYPE_4BYTE_ABGR),
          entry("gif", BufferedImage.TYPE_4BYTE_ABGR),
          entry("bmp", BufferedImage.TYPE_3BYTE_BGR));

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
    JasperPrint jasperPrint = print.print();
    final int numPages = jasperPrint.getPages().size();

    final float dpiRatio = (float) (print.dpi() / PDF_DPI);
    final int pageHeightOnImage = (int) (jasperPrint.getPageHeight() * dpiRatio);
    final int pageWidthOnImage = (int) (jasperPrint.getPageWidth() * dpiRatio);
    final int separatorHeight = 1;
    final int separatorHeightOnImage = (int) (separatorHeight * dpiRatio);

    final int imageType = IMAGE_TYPES.get(getFileSuffix().toLowerCase());
    BufferedImage reportImage =
        new BufferedImage(
            pageWidthOnImage,
            numPages * pageHeightOnImage + (numPages - 1) * separatorHeightOnImage,
            imageType);
    Graphics2D graphics2D = reportImage.createGraphics();
    if (imageType != BufferedImage.TYPE_4BYTE_ABGR) {
      graphics2D.setColor(Color.WHITE);
      graphics2D.fillRect(0, 0, pageWidthOnImage, pageHeightOnImage);
    }

    try {
      JasperPrintManager printManager = JasperPrintManager.getInstance(print.context());

      for (int pageIndex = 0; pageIndex < numPages; pageIndex++) {
        Image pageImage = printManager.printToImage(jasperPrint, pageIndex, dpiRatio);

        graphics2D.drawImage(
            pageImage,
            0,
            (pageHeightOnImage + separatorHeight) * pageIndex,
            pageWidthOnImage,
            (pageHeightOnImage + separatorHeight) * pageIndex + pageHeightOnImage,
            0,
            0,
            pageWidthOnImage,
            pageHeightOnImage,
            null);
      }

      // draw separator line between the pages
      final Stroke stroke = new BasicStroke(separatorHeightOnImage);
      for (int pageIndex = 0; pageIndex < numPages - 1; pageIndex++) {
        graphics2D.setColor(Color.black);
        graphics2D.setStroke(stroke);
        int y = (pageHeightOnImage + separatorHeight) * pageIndex + pageHeightOnImage;
        graphics2D.drawLine(0, y, pageWidthOnImage, y);
      }
    } finally {
      graphics2D.dispose();
    }

    ImageUtils.writeImage(reportImage, getFileSuffix(), outputStream);
  }
}
