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

package org.mapfish.print.map.style;

import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;

import org.mapfish.print.config.Configuration;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Style parser plugin that loads styles from a file containing SLD xml.
 * <p/>
 * Since an SLD can have multiple styles, the string can end with ##&lt;number>.  if that number is there
 * then the identified style will be retrieved from the parsed file.
 *
 * @author Jesse on 3/26/14.
 */
public final class StringSLDParserPlugin extends AbstractSLDParserPlugin {

    @Override
    protected List<ByteSource> getInputStreamSuppliers(final Configuration configuration, final String styleString) {
        List<ByteSource> options = Lists.newArrayList();
        try {
            options.add(ByteSource.wrap(styleString.getBytes("UTF8")));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return options;
    }
}
