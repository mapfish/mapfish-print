package org.mapfish.print.output;

import java.util.ArrayList;
import java.util.List;

/**
 * User: jeichar
 * Date: Oct 18, 2010
 * Time: 2:07:54 PM
 */
public class OutputFactory {
    private static List<OutputFormat> formats = new ArrayList<OutputFormat>();
    static {
        formats.add(new PdfOutput());
        formats.add(new ImageOutputScalable());
//        formats.add(new ImageOutput());
    }
    public static OutputFormat create(String id) {
        if(id == null) {
            id = "pdf";
        }

        for (OutputFormat format : formats) {
            if(format.accepts(id)){
                return format;
            }
        }

        if(id.equalsIgnoreCase("png")) {
            throw new Error("There must be a format that can output PDF");
        } else {
            throw new IllegalArgumentException(id+" is not a supported format");
        }
    }
}
