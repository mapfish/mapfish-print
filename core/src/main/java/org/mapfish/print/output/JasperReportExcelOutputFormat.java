package org.mapfish.print.output;

import java.io.OutputStream;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.poi.export.JRXlsExporter;

/** An PDF output format that uses Jasper reports to generate the result. */
public final class JasperReportExcelOutputFormat extends AbstractJasperReportOutputFormat
    implements OutputFormat {

  @Override
  public String getContentType() {
    return "application/vnd.ms-excel";
  }

  @Override
  public String getFileSuffix() {
    return "xls";
  }

  @Override
  protected void doExport(final OutputStream outputStream, final Print print) throws JRException {
    JRXlsExporter exporter = new JRXlsExporter();

    exporter.setExporterInput(new SimpleExporterInput(print.print));
    exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));

    exporter.exportReport();
  }
}
