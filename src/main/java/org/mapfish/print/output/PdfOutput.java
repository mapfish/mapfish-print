package org.mapfish.print.output;

import com.lowagie.text.DocumentException;
import org.apache.log4j.Logger;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.TimeLogger;
import org.mapfish.print.utils.PJsonObject;

import java.io.OutputStream;

/**
 * User: jeichar
 * Date: Oct 18, 2010
 * Time: 2:00:30 PM
 */
public class PdfOutput extends OutputFormat {

    public RenderingContext print(MapPrinter printer, PJsonObject jsonSpec, OutputStream out, String referer) throws DocumentException {
        final TimeLogger timeLog = TimeLogger.info(Logger.getLogger(PdfOutput.class), "Pdf to image conversion");
        final RenderingContext context = printer.print(jsonSpec, out, referer);
        timeLog.done();
        
        return context;

    }

    public boolean accepts(String id) {
        return id.equalsIgnoreCase("pdf");
    }
}
