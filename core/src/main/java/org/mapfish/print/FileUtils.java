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

package org.mapfish.print;

import org.mapfish.print.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

/**
 * Methods for interacting with files.  Such things and verifying the files are in the correct directory,
 * Converting URLs to file objects.
 *
 * @author Jesse on 4/8/2014.
 */
public final class FileUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    private FileUtils() {
        // intentionally empty
    }
    /**
     * Check if the url is a file url which is either relative to the configuration directory or refers to a file in the
     * configuration directory.
     * <p/>
     * The correct file url is returned or the original if not a file url.  If an illegal file url (one that does not refer to a
     * file within the configuration directory or is relative to the configuration directory) then an exception is thrown.
     *
     * @param url           the url to test
     * @param configuration the configuration to test relativity.
     */
    public static URL testForLegalFileUrl(final Configuration configuration, final URL url) {
        final String protocol = url.getProtocol();
        if (protocol.equalsIgnoreCase("file")) {
            try {

                File file = new File(configuration.getDirectory(), url.toExternalForm().substring("file://".length()));
                if (file.exists() && file.isFile()) {
                    URL tmpUrl = file.getAbsoluteFile().toURI().toURL();
                    assertFileIsInConfigDir(configuration, file);
                    return tmpUrl;
                } else {
                    file = new File(url.getFile());
                    if (file.exists() && file.isFile()) {
                        URL tmpUrl = file.getAbsoluteFile().toURI().toURL();
                        assertFileIsInConfigDir(configuration, file);
                        return tmpUrl;
                    } else {
                        throw new IllegalArgumentException("File urls must refer to a file within the configuration directory");
                    }
                }
            } catch (MalformedURLException e) {
                throw ExceptionUtils.getRuntimeException(e);
            }
        }
        return url;
    }

    /**
     * Verify that the file is within the configuration directory.
     *
     * @param configuration the configuration to test relativity.
     * @param file the file to test
     */
    public static void assertFileIsInConfigDir(final Configuration configuration, final File file) {
        assertIsSubDirectory("configuration", file, configuration.getDirectory());
    }


    /**
     * Verify that the file is within the base directory. {@link org.mapfish.print.IllegalFileAccessException} will be thrown
     * if the assertion does not hold.
     *  @param descriptorOfBase a simple description of the base file, for example: configuration
     * @param child the file to test that is is a child of base.
     * @param baseFiles the directories that can legally contain the child.
     */
    public static boolean assertIsSubDirectory(final String descriptorOfBase, final File child, final File... baseFiles) {
        File canonicalChild;
        try {
            canonicalChild = child.getCanonicalFile();
        } catch (IOException e) {
            throw new Error("Unable to get the canonical file of '" + child + "'.  Therefore it is not possible to verify if it is a " +

                            "child of '" + Arrays.toString(baseFiles) + "'.");
        }
        for (File base : baseFiles) {
            File canonicalBase;
            try {
                canonicalBase = base.getCanonicalFile();
            } catch (IOException e) {
                throw new Error("Unable to get the canonical file of '" + base + "'.  Therefore it is not possible to verify if '" + child
                                + "' is a child of it.");
            }
            File parentFile = canonicalChild;
            while (parentFile != null) {
                if (canonicalBase.equals(parentFile)) {
                    return true;
                }
                parentFile = parentFile.getParentFile();
            }
        }
        LOGGER.warn("A user attempted to access a file not within the '" + descriptorOfBase + "' directories (" +
                    Arrays.toString(baseFiles) + "). " + "Attempted access to :" + canonicalChild);
        throw new IllegalFileAccessException("'" + canonicalChild + "' identifies a file that is not within the '" +
                                             descriptorOfBase +
                                             "' directories: " + Arrays.toString(baseFiles));
    }


}
