package org.mapfish.print.output;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

import java.io.OutputStream;

/**
 * An PDF output format that uses Jasper reports to generate the result.
 */
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
