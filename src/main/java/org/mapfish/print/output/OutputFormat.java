package org.mapfish.print.output;

import com.lowagie.text.DocumentException;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.utils.PJsonObject;

import java.io.OutputStream;

/**
 * Interface for exporting the generated PDF from MapPrinter.
 * 
 * User: jeichar
 * Date: Oct 18, 2010
 * Time: 1:49:41 PM
 */
public abstract class OutputFormat {
    public final RenderingContext print(MapPrinter printer, String spec, OutputStream out, String referer) throws DocumentException {
        final PJsonObject jsonSpec = printer.parseSpec(spec);
        return print(printer,jsonSpec,out, referer);
    }
    public abstract RenderingContext print(MapPrinter printer, PJsonObject jsonSpec, OutputStream out, String referer) throws DocumentException;
    public abstract boolean accepts(String id);
}
