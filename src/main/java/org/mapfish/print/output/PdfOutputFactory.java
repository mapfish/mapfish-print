/*
 * Copyright (C) 2013  Camptocamp
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

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.TimeLogger;

import com.lowagie.text.DocumentException;

/**
 * OutputFormat and factory that Outputs PDF objects
 * 
 * User: jeichar
 * Date: Oct 18, 2010
 * Time: 2:00:30 PM
 */
public class PdfOutputFactory extends AbstractOutputFormat implements OutputFormatFactory {

    public String getContentType() {
        return "application/pdf";
    }

    public String getFileSuffix() {
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
