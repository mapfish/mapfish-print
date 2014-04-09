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
import com.google.common.io.Files;
import org.mapfish.print.FileUtils;
import org.mapfish.print.config.Configuration;

import java.io.File;
import java.util.List;

/**
 * Style parser plugin that loads styles from a file containing SLD xml.
 * <p/>
 * Since an SLD can have multiple styles, the string can end with ##&lt;number>.  if that number is there
 * then the identified style will be retrieved from the parsed file.
 *
 * @author Jesse on 3/26/14.
 */
public final class FileSLDParserPlugin extends AbstractSLDParserPlugin {

    @Override
    protected List<ByteSource> getInputStreamSuppliers(final Configuration configuration, final String styleString) {
        List<ByteSource> options = Lists.newArrayList();

        addRelativeFile(styleString, options, configuration);
        addNonRelativeFile(styleString, options, configuration);
        return options;
    }

    private void addRelativeFile(final String styleString, final List<ByteSource> options, final Configuration configuration) {
        final File file = new File(configuration.getDirectory(), styleString);
        if (file.exists()) {
            FileUtils.assertFileIsInConfigDir(configuration, file);
            options.add(Files.asByteSource(file));
        }
    }

    private void addNonRelativeFile(final String styleString, final List<ByteSource> options,
                                    final Configuration configuration) {
        final File file = new File(styleString);
        if (file.exists()) {
            FileUtils.assertFileIsInConfigDir(configuration, file);
            if (file.getAbsolutePath().startsWith(configuration.getDirectory().getAbsolutePath())) {
                options.add(Files.asByteSource(file));
            } else {
                throw new IllegalArgumentException("Files must always be within the configuration directory.  File was " +
                                                   file.getAbsolutePath());
            }
        }
    }
}
