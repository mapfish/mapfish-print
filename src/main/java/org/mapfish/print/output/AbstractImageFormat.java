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
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;

/**
 * User: jeichar
 * Date: 10/21/10
 * Time: 11:18 AM
 */
abstract class AbstractImageFormat extends AbstractOutputFormat implements OutputFormat {
    protected static final float MARGIN = 20;

    protected final String format;

    protected AbstractImageFormat(String format) {
        this.format = format;
    }

    public String getContentType() {

        return "image/" + format;
    }

    public String getFileSuffix() {
        return format;
    }

    protected int calculateDPI(RenderingContext context, PJsonObject jsonSpec) {
        final int MISSING_VALUE = -1;
        int dpi = jsonSpec.optInt("dpi", MISSING_VALUE);
        dpi = Math.max(dpi, context.getGlobalParams().optInt("dpi", MISSING_VALUE));
        PJsonArray pages = jsonSpec.optJSONArray("pages");
        if (pages != null) {
            for (int i = 0; i < pages.size(); i++) {
                PJsonObject page = pages.getJSONObject(i);
                dpi = Math.max(dpi, page.optInt("dpi", MISSING_VALUE));
            }
        }
        if (dpi < 0) {
            throw new IllegalArgumentException("unable to calculation DPI of maps");
        }
        return dpi;
    }
}
