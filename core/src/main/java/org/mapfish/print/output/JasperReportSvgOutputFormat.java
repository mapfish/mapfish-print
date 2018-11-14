package org.mapfish.print.output;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.export.JRGraphics2DExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleGraphics2DExporterOutput;
import net.sf.jasperreports.export.SimpleGraphics2DReportConfiguration;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.mapfish.print.Constants;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * An SVG output format that uses Jasper reports to generate the result.
 */
public class JasperReportSvgOutputFormat extends AbstractJasperReportOutputFormat {

    @Override
    public String getContentType() {
        return "image/svg+xml";
    }

    @Override
    public String getFileSuffix() {
        return "svg";
    }

    @Override
    protected void doExport(final OutputStream outputStream, final Print print)
            throws JRException, IOException {

        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        Document document = domImpl.createDocument(null, "svg", null);
        SVGGraphics2D grx = new SVGGraphics2D(document);

        SimpleGraphics2DReportConfiguration configuration = new SimpleGraphics2DReportConfiguration();
        configuration.setStartPageIndex(0);
        SimpleGraphics2DExporterOutput output = new SimpleGraphics2DExporterOutput();
        output.setGraphics2D(grx);

        JRGraphics2DExporter exporter = new JRGraphics2DExporter(print.context);
        exporter.setExporterInput(new SimpleExporterInput(print.print));
        exporter.setExporterOutput(output);
        exporter.setConfiguration(configuration);
        exporter.exportReport();

        grx.stream(new OutputStreamWriter(outputStream, Constants.DEFAULT_CHARSET), true);
    }
}
