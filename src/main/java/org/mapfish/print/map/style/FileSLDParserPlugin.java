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

import com.google.common.base.Optional;
import com.vividsolutions.jts.util.Assert;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.mapfish.print.config.Configuration;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Style parser plugin that loads styles from a file containing SLD xml.
 * <p/>
 * Since an SLD can have multiple styles, the string can end with ##&lt;number>.  if that number is there
 * then the identified style will be retrieved from the parsed file.
 *
 * @author Jesse on 3/26/14.
 */
public final class FileSLDParserPlugin implements StyleParserPlugin {
    @Override
    public Optional<Style> parseStyle(final Configuration configuration, final String ref) throws FileNotFoundException {
        String styleString = ref;
        int styleIndex = 0;
        int styleIdentifier = ref.lastIndexOf("##");
        if (styleIdentifier > 0) {
            styleString = ref.substring(0, styleIdentifier);
            styleIndex = Integer.parseInt(ref.substring(styleIdentifier + 2));
        }
        final File configDirectory = configuration.getDirectory();
        File file = new File(configDirectory, styleString);
        Style style = null;
        if (file.exists()) {
            style = tryLoadSLD(file, styleIndex);
        }
        if (style == null) {
            file = new File(styleString);
            if (file.exists()) {
                if (file.getAbsolutePath().startsWith(configDirectory.getAbsolutePath())) {
                    style = tryLoadSLD(file, styleIndex);
                } else {
                    throw new IllegalArgumentException("Files must always be within the configuration directory.  File was " +
                                                       file.getAbsolutePath());
                }
            }
        }
        return Optional.fromNullable(style);
    }

    private Style tryLoadSLD(final File file, final int styleIndex) throws FileNotFoundException {
        Assert.isTrue(styleIndex > -1, "styleIndex must be > -1 but was: " + styleIndex);

        final SLDParser sldParser = new SLDParser(CommonFactoryFinder.getStyleFactory());
        sldParser.setInput(file);
        final Style[] styles = sldParser.readXML();

        Assert.isTrue(styleIndex < styles.length, "There where " + styles.length + " styles in file but requested index was: " +
                                                  styleIndex);

        return styles[styleIndex];
    }
}
