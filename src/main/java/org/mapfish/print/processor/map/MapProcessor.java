/*
 * Copyright (C) 2014  Camptocamp
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

package org.mapfish.print.processor.map;

import org.mapfish.print.processor.AbstractProcessor;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jesseeichar on 3/17/14.
 * @author sbrunner
 */
public class MapProcessor extends AbstractProcessor {
    private static final int TEMPORARY_MAP_SIZE = 200;
//    private static final String MAP_INPUT = "map";
    private static final String MAP_OUTPUT = "map";

    @Override
    public final Map<String, Object> execute(final Map<String, Object> values) throws Exception {
        final Map<String, Object> output = new HashMap<String, Object>();
        output.put(MAP_OUTPUT, new BufferedImage(TEMPORARY_MAP_SIZE, TEMPORARY_MAP_SIZE, BufferedImage.TYPE_INT_ARGB_PRE));
        return output;
    }
}
