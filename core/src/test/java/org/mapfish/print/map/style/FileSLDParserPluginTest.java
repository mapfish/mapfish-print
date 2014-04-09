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
import org.geotools.styling.Style;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test loading an style from a file.
 *
 * @author Jesse on 3/26/14.
 */
public class FileSLDParserPluginTest extends AbstractMapfishSpringTest {
    @Autowired
    private FileSLDParserPlugin parser;

    @Test
    public void testParseStyle_SingleStyleRelativeToConfig() throws Throwable {
        File file = getFile(FileSLDParserPluginTest.class, "singleStyle.sld");
        Configuration config = new Configuration();
        config.setConfigurationFile(file);

        final Optional<Style> styleOptional = this.parser.parseStyle(config, file.getName());
        assertTrue (styleOptional.isPresent());
        assertTrue(styleOptional.get() instanceof Style);
        assertEquals(1, styleOptional.get().featureTypeStyles().size());
        assertEquals(2, styleOptional.get().featureTypeStyles().get(0).rules().size());
        assertEquals(1, styleOptional.get().featureTypeStyles().get(0).rules().get(0).symbolizers().size());
        assertEquals(1, styleOptional.get().featureTypeStyles().get(0).rules().get(1).symbolizers().size());
    }

    @Test
    public void testParseStyle_SingleStyleRelativeToConfig_HasStyleIndex() throws Throwable {
        File file = getFile(FileSLDParserPluginTest.class, "singleStyle.sld");
        Configuration config = new Configuration();
        config.setConfigurationFile(file);

        final Optional<Style> styleOptional = this.parser.parseStyle(config, file.getName()+"##1");
        assertTrue (styleOptional.isPresent());
        assertTrue(styleOptional.get() instanceof Style);
        assertEquals(1, styleOptional.get().featureTypeStyles().size());
        assertEquals(2, styleOptional.get().featureTypeStyles().get(0).rules().size());
        assertEquals(1, styleOptional.get().featureTypeStyles().get(0).rules().get(0).symbolizers().size());
        assertEquals(1, styleOptional.get().featureTypeStyles().get(0).rules().get(1).symbolizers().size());
    }

    @Test
    public void testParseStyle_SingleStyleAbsoluteFile() throws Throwable {
        File file = getFile(FileSLDParserPluginTest.class, "singleStyle.sld");
        Configuration config = new Configuration();
        config.setConfigurationFile(file);

        final Optional<Style> styleOptional = this.parser.parseStyle(config, file.getAbsolutePath());
        assertTrue (styleOptional.isPresent());
        assertTrue(styleOptional.get() instanceof Style);
        assertEquals(1, styleOptional.get().featureTypeStyles().size());
        assertEquals(2, styleOptional.get().featureTypeStyles().get(0).rules().size());
        assertEquals(1, styleOptional.get().featureTypeStyles().get(0).rules().get(0).symbolizers().size());
        assertEquals(1, styleOptional.get().featureTypeStyles().get(0).rules().get(1).symbolizers().size());
    }

    @Test(expected = Exception.class)
    public void testParseStyle_MultipleStyles_NoIndex() throws Throwable {
        File file = getFile(FileSLDParserPluginTest.class, "multipleStyles.sld");
        Configuration config = new Configuration();
        config.setConfigurationFile(file);

        this.parser.parseStyle(config, file.getName());
    }

    @Test
    public void testParseStyle_MultipleStyles() throws Throwable {
        File file = getFile(FileSLDParserPluginTest.class, "multipleStyles.sld");
        Configuration config = new Configuration();
        config.setConfigurationFile(file);

        Optional<Style> styleOptional = this.parser.parseStyle(config, file.getName()+"##1");
        assertTrue (styleOptional.isPresent());
        assertTrue(styleOptional.get() instanceof Style);
        assertEquals(1, styleOptional.get().featureTypeStyles().size());
        assertEquals(2, styleOptional.get().featureTypeStyles().get(0).rules().size());
        assertEquals(1, styleOptional.get().featureTypeStyles().get(0).rules().get(0).symbolizers().size());
        assertEquals(1, styleOptional.get().featureTypeStyles().get(0).rules().get(1).symbolizers().size());


        styleOptional = this.parser.parseStyle(config, file.getName()+"##2");
        assertTrue (styleOptional.isPresent());
        assertTrue(styleOptional.get() instanceof Style);
        assertEquals(1, styleOptional.get().featureTypeStyles().size());
        assertEquals(1, styleOptional.get().featureTypeStyles().get(0).rules().size());
        assertEquals(2, styleOptional.get().featureTypeStyles().get(0).rules().get(0).symbolizers().size());

    }

    @Test(expected = Exception.class)
    public void testIndexOutOfBounds() throws Throwable {
        File file = getFile(FileSLDParserPluginTest.class, "singleStyle.sld");
        Configuration config = new Configuration();
        config.setConfigurationFile(file);

        this.parser.parseStyle(config, file.getName()+"##3");
    }

    @Test(expected = Exception.class)
    public void testIndexTooLow() throws Throwable {
        File file = getFile(FileSLDParserPluginTest.class, "singleStyle.sld");
        Configuration config = new Configuration();
        config.setConfigurationFile(file);

        this.parser.parseStyle(config, file.getName()+"##-1");
    }

    @Test(expected = Exception.class)
    public void testFileNotInConfigDir() throws Throwable {
        final File tempFile = File.createTempFile("config", ".yaml");
        File file = getFile(FileSLDParserPluginTest.class, "singleStyle.sld");
        Configuration config = new Configuration();
        config.setConfigurationFile(tempFile);

        this.parser.parseStyle(config, file.getAbsolutePath());
    }
}
