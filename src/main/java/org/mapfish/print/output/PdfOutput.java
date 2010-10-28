package org.mapfish.print.output;

import com.lowagie.text.DocumentException;
import org.apache.log4j.Logger;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.TimeLogger;
import org.mapfish.print.utils.PJsonObject;

import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

/**
 * User: jeichar
 * Date: Oct 18, 2010
 * Time: 2:00:30 PM
 */
public class PdfOutput implements OutputFormat, OutputFormatFactory {

    public String contentType() {
        return "application/pdf";
    }

    public String fileSuffix() {
        return "pdf";
    }

    public List<String> formats() {
        return Collections.singletonList("pdf");
    }

    public String enablementStatus() {
        return null;
    }

    public OutputFormat create(String format) {
        return this;
    }

    public RenderingContext print(MapPrinter printer, PJsonObject jsonSpec, OutputStream out, String referer) throws DocumentException {
        final TimeLogger timeLog = TimeLogger.info(Logger.getLogger(PdfOutput.class), "PDF Creation");
        final RenderingContext context = printer.print(jsonSpec, out, referer);
        timeLog.done();
        
        return context;

    }
}
