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

import org.mapfish.print.RenderingContext;

import com.itextpdf.text.DocumentException;

/**
 * Interface for exporting the generated PDF from MapPrinter.
 *
 * User: jeichar
 * Date: Oct 18, 2010
 * Time: 1:49:41 PM
 */
public interface OutputFormat {
    RenderingContext print(PrintParams params) throws DocumentException, InterruptedException;
    String getContentType();
    String getFileSuffix();
}
