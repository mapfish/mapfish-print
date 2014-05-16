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

package org.mapfish.print.servlet.fileloader;

import com.google.common.base.Optional;
import com.google.common.io.Files;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.net.URI;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileConfigFileLoaderTest extends AbstractMapfishSpringTest {
    private static final File CONFIG_FILE = getFile(FileConfigFileLoaderTest.class, "config.yaml");

    @Autowired
    private FileConfigFileLoader loader;
    @Test
    public void testToFile() throws Exception {
        assertFalse(loader.toFile(new URI("servlet:///blahblahblah")).isPresent());
        assertTrue(loader.toFile(CONFIG_FILE.toURI()).isPresent());
        assertTrue(loader.toFile(CONFIG_FILE.getParentFile().toURI()).isPresent());
    }

    @Test
    public void testLastModified() throws Exception {

        Optional<Long> lastModified = this.loader.lastModified(CONFIG_FILE.toURI());
        assertTrue(lastModified.isPresent());
        assertEquals(CONFIG_FILE.lastModified(), lastModified.get().longValue());
    }

    @Test
    public void testAccessible() throws Exception {
        assertTrue(CONFIG_FILE.toURI().toString(), loader.isAccessible(CONFIG_FILE.toURI()));
        assertFalse(loader.isAccessible(new URI(CONFIG_FILE.toURI() + "xzy")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAccessible_RelativePath() throws Exception {
        final URI fileURI = new URI("file://relativePath/config.yaml");
        loader.isAccessible(fileURI);
    }

    @Test
    public void testLoadFile() throws Exception {
        byte[] loaded = this.loader.loadFile(CONFIG_FILE.toURI());
        assertArrayEquals(Files.toByteArray(CONFIG_FILE), loaded);
    }

    @Test(expected = NoSuchElementException.class)
    public void testLoadFileMissingFile() throws Exception {
        this.loader.loadFile(new URI("file:/c:/doesnotexist"));
    }

    @Test(expected = NoSuchElementException.class)
    public void testLastModifiedMissingFile() throws Exception {
        this.loader.lastModified(new URI("file:/c:/doesnotexist"));
    }

    @Test
    public void testAccessibleChildResource() throws Exception {
        final URI configFileUri = CONFIG_FILE.toURI();
        final String resourceFileName = "resourceFile.txt";
        assertTrue(this.loader.isAccessible(configFileUri, resourceFileName));
        assertTrue(this.loader.isAccessible(configFileUri, getFile(FileConfigFileLoader.class, resourceFileName).toURI().toString()));
        assertTrue(this.loader.isAccessible(configFileUri, getFile(FileConfigFileLoader.class, resourceFileName).getAbsolutePath()));
        assertTrue(this.loader.isAccessible(configFileUri, getFile(FileConfigFileLoader.class, resourceFileName).getPath()));

        assertFalse(this.loader.isAccessible(configFileUri, getFile(FileConfigFileLoader.class,
                "/test-http-request-factory-application-context.xml")
                .getAbsolutePath()));
        assertFalse(this.loader.isAccessible(configFileUri, getFile(FileConfigFileLoader.class,
                "../../../../../test-http-request-factory-application-context.xml").getAbsolutePath()));
    }

    @Test
    public void testLoadFileChildResource() throws Exception {
        final URI configFileUri = CONFIG_FILE.toURI();
        final String resourceFileName = "resourceFile.txt";
        final byte[] bytes = Files.toByteArray(getFile(FileConfigFileLoader.class, resourceFileName));

        assertArrayEquals(bytes, this.loader.loadFile(configFileUri, resourceFileName));
        assertArrayEquals(bytes, this.loader.loadFile(configFileUri, getFile(FileConfigFileLoader.class,
                resourceFileName).getAbsolutePath()));
        assertArrayEquals(bytes, this.loader.loadFile(configFileUri, getFile(FileConfigFileLoader.class, resourceFileName).getPath()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadFileChildResource_NotInConfigDir() throws Exception {
        final URI configFileUri = CONFIG_FILE.toURI();

        this.loader.loadFile(configFileUri, getFile(FileConfigFileLoader.class, "/test-http-request-factory-application-context.xml")
                .getAbsolutePath());
    }

    @Test(expected = NoSuchElementException.class)
    public void testLoadFileChildResource_DoesNotExist() throws Exception {
        final URI configFileUri = CONFIG_FILE.toURI();

        this.loader.loadFile(configFileUri, "doesNotExist");
    }
}