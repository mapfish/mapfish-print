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
import org.mapfish.print.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Abstract implementation for files that are on the local file system.
 *
 * @author Jesse on 4/28/2014.
 */
public abstract class AbstractFileConfigFileLoader implements ConfigFileLoaderPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileConfigFileLoader.class);

    /**
     * Load the files referenced by the id (in the case of a classpath uri it could references several files, although normally it will
     * only reference one).
     *
     * @param fileURI the uri identifying the config file.
     */
    protected abstract Iterator<File> resolveFiles(URI fileURI);

    @Override
    public final Optional<File> toFile(final URI fileUri) {
        try {
            final Iterator<File> fileIterator = resolveFiles(fileUri);
            while (fileIterator.hasNext()) {
                File next = fileIterator.next();
                if (next.exists()) {
                    return Optional.of(next);
                }
            }
        } catch (IllegalArgumentException e) {
            // ignore because it just means that this can't handle the uri
        }
        return Optional.absent();
    }

    @Override
    public final Optional<Long> lastModified(final URI fileURI) {
        Optional<File> file = findFile(resolveFiles(fileURI));

        if (file.isPresent()) {
            return Optional.of(file.get().lastModified());
        }

        throw new NoSuchElementException("No config file found at: " + fileURI);
    }

    @Override
    public final boolean isAccessible(final URI fileURI) {
        if (!fileURI.getScheme().equalsIgnoreCase(getUriScheme())) {
            return false;
        }
        Optional<File> file = findFile(resolveFiles(fileURI));


        if (file.isPresent() && file.get().isDirectory()) {
            throw new IllegalArgumentException(fileURI + " does not refer to a file, it is a directory");
        }

        return file.isPresent() && file.get().exists();
    }

    @Override
    public final byte[] loadFile(final URI fileURI) throws IOException {
        Optional<File> file = findFile(resolveFiles(fileURI));

        if (file.isPresent() && file.get().exists()) {
            return Files.toByteArray(file.get());
        }
        throw new NoSuchElementException("No config file found at: " + fileURI);
    }

    @Override
    public final boolean isAccessible(final URI configFileUri, final String pathToSubResource) throws IOException {
        try {
            final Optional<File> childFile = resolveChildFile(configFileUri, pathToSubResource);
            return childFile.isPresent() && childFile.get().exists();
        } catch (NoSuchElementException nsee) {
            return false;
        }
    }


    @Override
    public final byte[] loadFile(final URI configFileUri, final String pathToSubResource) throws IOException {
        Optional<File> childFile = resolveChildFile(configFileUri, pathToSubResource);
        if (childFile.isPresent() && childFile.get().exists()) {
            return Files.toByteArray(childFile.get());
        }
        throw new NoSuchElementException("File does not exist: " + childFile);
    }

    private Optional<File> findFile(final Iterator<File> files) {
        while (files.hasNext()) {
            File next = files.next();
            if (next.isFile()) {
                return Optional.of(next);
            }
        }
        return Optional.absent();
    }

    private Optional<File> resolveChildFile(final URI configFileUri, final String pathToSubResource) throws IOException {
        final Optional<File> configFileOptional = findFile(resolveFiles(configFileUri));
        if (!configFileOptional.isPresent()) {
            throw new NoSuchElementException("No configuration file found at: " + configFileUri);
        }
        File configFile = configFileOptional.get();
        try {
            final URI uri = new URI(pathToSubResource);

            final File configDir = configFile.getParentFile();
            if (pathToSubResource.startsWith(getUriScheme())) {
                final Iterator<File> fileIterator = resolveFiles(uri);

                while (fileIterator.hasNext()) {
                    File next = fileIterator.next();
                    if (next.exists()) {
                        FileUtils.assertIsSubDirectory("configuration", configDir, next);
                        return Optional.of(next);
                    }
                }

                final File childFile = new File(configDir, platformIndependentUriToFile(uri).getPath());
                if (childFile.exists()) {
                    FileUtils.assertIsSubDirectory("configuration", configDir, childFile);
                    return Optional.of(childFile);
                }
            }

            try {
                final File childFile = platformIndependentUriToFile(uri);

                if (childFile.exists()) {
                    FileUtils.assertIsSubDirectory("configuration", configDir, childFile);
                    return Optional.of(childFile);
                } else {
                    return Optional.absent();
                }
            } catch (IllegalArgumentException e) {
                return resolveFileAssumingPathIsFile(pathToSubResource, configFile);
            }

        } catch (URISyntaxException e) {
            return resolveFileAssumingPathIsFile(pathToSubResource, configFile);
        }

    }

    private Optional<File> resolveFileAssumingPathIsFile(final String pathToSubResource, final File configFile) throws IOException {
        // not a uri
        File childFile = new File(configFile.getParentFile(), pathToSubResource);
        if (childFile.exists()) {
            return Optional.of(childFile);
        } else {
            childFile = new File(pathToSubResource);
            if (childFile.exists()) {
                FileUtils.assertIsSubDirectory("configuration", configFile.getParentFile(), childFile);
                return Optional.of(childFile);
            }
        }

        return Optional.absent();
    }

    /**
     * Convert a url to a file object.  No checks are made to see if file exists but there are some hacks that are needed
     * to convert uris to files across platforms.
     * @param fileURI the uri to convert
     */
    protected static File platformIndependentUriToFile(final URI fileURI) {
        File file;
        try {
            file = new File(fileURI);
        } catch (IllegalArgumentException e) {
            if (fileURI.toString().startsWith("file://")) {
                file = new File(fileURI.toString().substring("file://".length()));
            } else {
                throw e;
            }
        }
        return file;
    }
}
