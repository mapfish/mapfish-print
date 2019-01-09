package org.mapfish.print.servlet.fileloader;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

/**
 * Processes all {@link ConfigFileLoaderPlugin}s and loads the files.
 */
public final class ConfigFileLoaderManager implements ConfigFileLoaderPlugin {
    @Autowired
    private List<ConfigFileLoaderPlugin> loaderPlugins;

    private Iterable<ConfigFileLoaderPlugin> getLoaderPlugins() {
        return this.loaderPlugins.stream().filter(input -> !(input instanceof ConfigFileLoaderManager))
                .collect(Collectors.toList());
    }

    /**
     * Method is called by spring and verifies that there is only one plugin per URI scheme.
     */
    @PostConstruct
    public void checkUniqueSchemes() {
        Multimap<String, ConfigFileLoaderPlugin> schemeToPluginMap = HashMultimap.create();

        for (ConfigFileLoaderPlugin plugin: getLoaderPlugins()) {
            schemeToPluginMap.put(plugin.getUriScheme(), plugin);
        }

        StringBuilder violations = new StringBuilder();
        for (String scheme: schemeToPluginMap.keySet()) {
            final Collection<ConfigFileLoaderPlugin> plugins = schemeToPluginMap.get(scheme);
            if (plugins.size() > 1) {
                violations.append("\n\n* ").append("There are  has multiple ")
                        .append(ConfigFileLoaderPlugin.class.getSimpleName())
                        .append(" plugins that support the scheme: '").append(scheme).append('\'')
                        .append(":\n\t").append(plugins);
            }
        }

        if (violations.length() > 0) {
            throw new IllegalStateException(violations.toString());
        }
    }

    @Override
    public Optional<File> toFile(final URI fileUri) {
        for (ConfigFileLoaderPlugin configFileLoaderPlugin: getLoaderPlugins()) {
            final Optional<File> fileOptional = configFileLoaderPlugin.toFile(fileUri);
            if (fileOptional.isPresent()) {
                return fileOptional;
            }
        }
        return Optional.empty();
    }

    @Override
    public String getUriScheme() {
        throw new UnsupportedOperationException(
                "This method should not be called on the manager since it supports all schemas " +
                        "available in the plugins");
    }

    /**
     * Return all URI schemes that are supported in the system.
     */
    public Set<String> getSupportedUriSchemes() {
        Set<String> schemes = new HashSet<>();

        for (ConfigFileLoaderPlugin loaderPlugin: this.getLoaderPlugins()) {
            schemes.add(loaderPlugin.getUriScheme());
        }

        return schemes;
    }

    @Override
    public Optional<Long> lastModified(final URI fileURI) {
        for (ConfigFileLoaderPlugin plugin: getLoaderPlugins()) {
            if (plugin.isAccessible(fileURI)) {
                return plugin.lastModified(fileURI);
            }
        }
        throw new NoSuchElementException("No file found with uri: " + fileURI);
    }

    @Override
    public boolean isAccessible(final URI fileURI) {
        for (ConfigFileLoaderPlugin plugin: getLoaderPlugins()) {
            if (plugin.isAccessible(fileURI)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public byte[] loadFile(final URI fileURI) throws IOException {
        for (ConfigFileLoaderPlugin plugin: getLoaderPlugins()) {
            if (plugin.isAccessible(fileURI)) {
                return plugin.loadFile(fileURI);
            }
        }
        throw new NoSuchElementException("No file found with uri: " + fileURI);
    }

    @Override
    public boolean isAccessible(final URI configFileUri, final String pathToSubResource) throws IOException {
        for (ConfigFileLoaderPlugin plugin: getLoaderPlugins()) {
            if (plugin.isAccessible(configFileUri, pathToSubResource)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public byte[] loadFile(final URI configFileUri, final String pathToSubResource) throws IOException {
        for (ConfigFileLoaderPlugin plugin: getLoaderPlugins()) {
            if (plugin.isAccessible(configFileUri, pathToSubResource)) {
                return plugin.loadFile(configFileUri, pathToSubResource);
            }
        }

        throw new NoSuchElementException(
                "No resource found : " + pathToSubResource + " for configuration file: " + configFileUri);
    }
}
