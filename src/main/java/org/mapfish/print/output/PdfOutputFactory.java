/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

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
 * OutputFormat and factory that Outputs PDF objects
 * 
 * User: jeichar
 * Date: Oct 18, 2010
 * Time: 2:00:30 PM
 */
public class PdfOutputFactory extends AbstractOutputFormat implements OutputFormatFactory {

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

    public RenderingContext print(PrintParams params) throws DocumentException {
        final TimeLogger timeLog = TimeLogger.info(Logger.getLogger(PdfOutputFactory.class), "PDF Creation");
        final RenderingContext context = doPrint(params);
        timeLog.done();
        
        return context;

    }
}
