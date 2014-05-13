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
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;

/**
 * Processes all {@link org.mapfish.print.servlet.fileloader.ConfigFileLoaderPlugin}s and loads the files.
 *
 * @author Jesse on 4/28/2014.
 */
public final class ConfigFileLoaderManager implements ConfigFileLoaderPlugin {
    @Autowired
    private List<ConfigFileLoaderPlugin> loaderPlugins;

    private Iterable<ConfigFileLoaderPlugin> getLoaderPlugins() {
        return Iterables.filter(this.loaderPlugins, new Predicate<ConfigFileLoaderPlugin>() {
            @Override
            public boolean apply(@Nullable final ConfigFileLoaderPlugin input) {
                return !(input instanceof ConfigFileLoaderManager);
            }
        });
    }

    /**
     * Method is called by spring and verifies that there is only one plugin per URI scheme.
     */
    @PostConstruct
    public void checkUniqueSchemes() {
        Multimap<String, ConfigFileLoaderPlugin> schemeToPluginMap = HashMultimap.create();

        for (ConfigFileLoaderPlugin plugin : getLoaderPlugins()) {
            schemeToPluginMap.put(plugin.getUriScheme(), plugin);
        }

        StringBuilder violations = new StringBuilder();
        for (String scheme : schemeToPluginMap.keySet()) {
            final Collection<ConfigFileLoaderPlugin> plugins = schemeToPluginMap.get(scheme);
            if (plugins.size() > 1) {
                violations.append("\n\n* ").append("There are  has multiple ").append(ConfigFileLoaderPlugin.class.getSimpleName())
                        .append(" plugins that support the scheme: '").append(scheme).append('\'').append(":\n\t").append(plugins);
            }
        }

        if (violations.length() > 0) {
            throw new IllegalStateException(violations.toString());
        }
    }

    @Override
    public String getUriScheme() {
        throw new UnsupportedOperationException("This method should not be called on the manager since it supports all schemas " +
                                                "available in the plugins");
    }

    /**
     * Return all URI schemes that are supported in the system.
     */
    public Set<String> getSupportedUriSchemes() {
        Set<String> schemes = Sets.newHashSet();

        for (ConfigFileLoaderPlugin loaderPlugin : this.getLoaderPlugins()) {
            schemes.add(loaderPlugin.getUriScheme());
        }

        return schemes;
    }

    @Override
    public Optional<Long> lastModified(final URI fileURI) {
        for (ConfigFileLoaderPlugin plugin : getLoaderPlugins()) {
            if (plugin.isAccessible(fileURI)) {
                return plugin.lastModified(fileURI);
            }
        }
        throw new NoSuchElementException("No file found with uri: " + fileURI);
    }

    @Override
    public boolean isAccessible(final URI fileURI) {
        for (ConfigFileLoaderPlugin plugin : getLoaderPlugins()) {
            if (plugin.isAccessible(fileURI)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public byte[] loadFile(final URI fileURI) throws IOException {
        for (ConfigFileLoaderPlugin plugin : getLoaderPlugins()) {
            if (plugin.isAccessible(fileURI)) {
                return plugin.loadFile(fileURI);
            }
        }
        throw new NoSuchElementException("No file found with uri: " + fileURI);
    }

    @Override
    public boolean isAccessible(final URI configFileUri, final String pathToSubResource) throws IOException {
        for (ConfigFileLoaderPlugin plugin : getLoaderPlugins()) {
            if (plugin.isAccessible(configFileUri, pathToSubResource)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public byte[] loadFile(final URI configFileUri, final String pathToSubResource) throws IOException {
        for (ConfigFileLoaderPlugin plugin : getLoaderPlugins()) {
            if (plugin.isAccessible(configFileUri, pathToSubResource)) {
                return plugin.loadFile(configFileUri, pathToSubResource);
            }
        }

        throw new NoSuchElementException("No resource found : " + pathToSubResource + " for configuration file: " + configFileUri);
    }
}
