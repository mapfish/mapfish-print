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
public interface OutputFormat {
    RenderingContext print(MapPrinter printer, PJsonObject jsonSpec, OutputStream out, String referer) throws DocumentException;
    String contentType();
    String fileSuffix();
}
