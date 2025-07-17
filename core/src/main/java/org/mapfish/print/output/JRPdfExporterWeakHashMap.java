package org.mapfish.print.output;

import java.util.WeakHashMap;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.util.ExifOrientationEnum;
import net.sf.jasperreports.engine.util.Pair;
import net.sf.jasperreports.pdf.JRPdfExporter;
import net.sf.jasperreports.pdf.common.PdfImage;

public class JRPdfExporterWeakHashMap extends JRPdfExporter {

  public JRPdfExporterWeakHashMap(final JasperReportsContext jasperReportsContext) {
    super(jasperReportsContext);
  }

  @Override
  protected void initReport() {
    super.initReport();
    // We use a WeakHashMap as an image cache to allow it to be automatically cleared
    // when memory becomes low, while preserving this functionality of JRPdfExporter
    // which helps produce a smaller PDF
    this.loadedImagesMap =
        new WeakHashMap<>() {
          @Override
          public Pair<PdfImage, ExifOrientationEnum> put(
              final String key, final Pair<PdfImage, ExifOrientationEnum> value) {
            return super.put(new String(key.toCharArray()), value);
          }
        };
  }
}
