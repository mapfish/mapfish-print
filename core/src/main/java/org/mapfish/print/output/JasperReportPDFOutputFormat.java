package org.mapfish.print.output;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.PrintPageFormat;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import net.sf.jasperreports.export.type.PdfVersionEnum;
import org.mapfish.print.config.PDFConfig;
import org.mapfish.print.processor.ExecutionStats;

import java.io.OutputStream;

/**
 * An PDF output format that uses Jasper reports to generate the result.
 */
public final class JasperReportPDFOutputFormat extends AbstractJasperReportOutputFormat
        implements OutputFormat {

    @Override
    public String getContentType() {
        return "application/pdf";
    }

    @Override
    public String getFileSuffix() {
        return "pdf";
    }

    @Override
    protected void doExport(final OutputStream outputStream, final Print print) throws JRException {

        JRPdfExporter exporter = new JRPdfExporter(print.context);

        exporter.setExporterInput(new SimpleExporterInput(print.print));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));

        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
        configuration.setPdfVersion(PdfVersionEnum.VERSION_1_7);

        final PDFConfig pdfConfig = print.values.getObject(Values.PDF_CONFIG_KEY, PDFConfig.class);

        configuration.setCompressed(pdfConfig.isCompressed());
        configuration.setMetadataAuthor(pdfConfig.getAuthor());
        configuration.setMetadataCreator(pdfConfig.getCreator());
        configuration.setMetadataSubject(pdfConfig.getSubject());
        configuration.setMetadataTitle(pdfConfig.getTitle());
        configuration.setMetadataKeywords(pdfConfig.getKeywordsAsString());

        exporter.setConfiguration(configuration);

        exporter.exportReport();

        final JasperPrint jasperPrint = exporter.getCurrentJasperPrint();
        final ExecutionStats stats = print.executionContext.getStats();
        for (int i = 0; i < jasperPrint.getPages().size(); ++i) {
            final PrintPageFormat pageFormat = jasperPrint.getPageFormat(i);
            stats.addPageStats(pageFormat);
        }
    }
}
