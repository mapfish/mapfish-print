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
import com.google.common.io.Resources;
import org.mapfish.print.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * A plugin that loads the config resources from urls starting with prefix:
 * {@value org.mapfish.print.servlet.fileloader.ClasspathConfigFileLoader#PREFIX}://.
 *
 * @author Jesse on 4/28/2014.
 */
public final class ClasspathConfigFileLoader implements ConfigFileLoaderPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClasspathConfigFileLoader.class);

    private static final String PREFIX = "classpath";
    private static final int PREFIX_LENGTH = (PREFIX + "://").length();

    @Override
    public Optional<File> toFile(final URI fileUri) {
        final Optional<URL> urlOptional = loadResources(fileUri);
        if (urlOptional.isPresent() && urlOptional.get().getProtocol().equalsIgnoreCase(FileConfigFileLoader.PREFIX)) {
            try {
                return Optional.of(new File(urlOptional.get().toURI()));
            } catch (URISyntaxException e) {
                return Optional.absent();
            }
        }
        return Optional.absent();
    }

    @Override
    public String getUriScheme() {
        return PREFIX;
    }

    @Override
    public Optional<Long> lastModified(final URI fileURI) {
        final Optional<URL> resources = loadResources(fileURI);

        if (resources.isPresent()) {
            final URL url = resources.get();
            if (url.getProtocol().equalsIgnoreCase(FileConfigFileLoader.PREFIX)) {
                try {
                    return Optional.of(new File(url.toURI()).lastModified());
                } catch (URISyntaxException e) {
                    throw ExceptionUtils.getRuntimeException(e);
                }
            } else {
                return Optional.absent();
            }
        }

        throw new NoSuchElementException(fileURI + " does not exist");
    }

    @Override
    public boolean isAccessible(final URI fileURI) {
        final Optional<URL> resources = loadResources(fileURI);

        return resources.isPresent();

    }

    @Override
    public byte[] loadFile(final URI fileURI) throws IOException {
        final Optional<URL> resources = loadResources(fileURI);
        if (resources.isPresent()) {
            return Resources.toByteArray(resources.get());
        }

        throw new NoSuchElementException(fileURI + " does not exist");
    }

    @Override
    public boolean isAccessible(final URI configFileUri, final String pathToSubResource) {
        try {
            Optional<URL> child = resolveChild(configFileUri, pathToSubResource);
            return child.isPresent();
        } catch (IllegalArgumentException e) {
            return false;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    @Override
    public byte[] loadFile(final URI configFileUri, final String pathToSubResource) throws IOException {
        Optional<URL> child = resolveChild(configFileUri, pathToSubResource);
        if (child.isPresent()) {
            return Resources.toByteArray(child.get());
        }
        throw new NoSuchElementException("No file is found for parameters: '" + configFileUri + "' and subresource: '" +
                                         pathToSubResource + "'");
    }

    private Optional<URL> resolveChild(final URI configFileUri, final String pathToSubResource) {
        Optional<URL> urlOptional = loadResources(configFileUri);
        if (!urlOptional.isPresent()) {
            throw new NoSuchElementException("Configuration file '" + configFileUri + "' does not exist");
        }
        final String configUriAsString = configFileUri.toString();
        String configFileName = configUriAsString.substring(configUriAsString.lastIndexOf('/') + 1);
        String configFileDir = urlOptional.get().toString();
        configFileDir = configFileDir.substring(0, configFileDir.indexOf(configFileName));

        if (pathToSubResource.startsWith(PREFIX)) {
            Optional<URL> found = resolveChildAsUri(configFileUri, pathToSubResource, configFileDir);
            if (found.isPresent()) {
                return found;
            }
        }

        try {
            if (pathToSubResource.contains(":/")) {
                final URI uri = new URI(pathToSubResource);
                throw new IllegalArgumentException("Only uris with prefix " + PREFIX + " are supported.  Found: " + uri);
            }
        } catch (URISyntaxException e) {
            // good it should not be a non-classpath uri.
        }

        final String subResourceRelativeToConfigFileDir = configUriAsString.substring(0, configUriAsString.indexOf(configFileName)) +
                                                          pathToSubResource;
        return resolveChildAsUri(configFileUri, subResourceRelativeToConfigFileDir, configFileDir);
    }

    private Optional<URL> resolveChildAsUri(final URI configFileUri, final String pathToSubResource, final String configFileDir) {
        try {
            final Optional<URL> subResource = loadResources(new URI(pathToSubResource));
            if (subResource.isPresent()) {
                if (!subResource.get().toString().startsWith(configFileDir)) {
                    throw new IllegalArgumentException("'" + pathToSubResource + "' is not a child of '" + configFileUri + "'");
                }
                return Optional.of(subResource.get());
            }
        } catch (URISyntaxException e) {
            return Optional.absent();
        }

        return Optional.absent();
    }

    private Optional<URL> loadResources(final URI fileURI) {
        if (fileURI == null) {
            return Optional.absent();
        }
        if (fileURI.getScheme() != null && fileURI.getScheme().equals("file")) {
            File file;
            try {
                file = new File(fileURI);
            } catch (IllegalArgumentException e) {
                file = new File(fileURI.toString().substring("file://".length()));
            }
            if (file.exists()) {
                try {
                    return Optional.of(file.toURI().toURL());
                } catch (MalformedURLException e) {
                    throw ExceptionUtils.getRuntimeException(e);
                }
            } else {
                return Optional.absent();
            }
        }
        if (!fileURI.toString().startsWith(PREFIX)) {
            return Optional.absent();
        }
        String path = fileURI.toString().substring(PREFIX_LENGTH);
        if (path.charAt(0) == '/') {
            path = path.substring(1);
        }
        try {
            final Enumeration<URL> resources = FileConfigFileLoader.class.getClassLoader().getResources(path);
            if (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                return Optional.of(resource);
            }
        } catch (IOException e) {
            LOGGER.warn("Unable to find resources on the path: " + fileURI);
        }
        return Optional.absent();
    }
}
