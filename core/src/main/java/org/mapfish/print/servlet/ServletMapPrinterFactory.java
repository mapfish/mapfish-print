package org.mapfish.print.servlet;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.util.Assert;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.MapPrinterFactory;
import org.mapfish.print.servlet.fileloader.ConfigFileLoaderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;

/**
 * A {@link org.mapfish.print.MapPrinterFactory} that reads configuration from files and uses servlet's
 * methods for resolving the paths to the files.
 *
 */
public class ServletMapPrinterFactory implements MapPrinterFactory {

    /**
     * The name of the default app.  This is always required to be one of the apps that are registered.
     */
    public static final String DEFAULT_CONFIGURATION_FILE_KEY = "default";
    private static final String CONFIG_YAML = "config.yaml";
    private static final Logger LOGGER = LoggerFactory.getLogger(ServletMapPrinterFactory.class);
    private static final int MAX_DEPTH = 2;
    private final Map<String, MapPrinter> printers = new HashMap<>();
    private final Map<String, Long> configurationFileLastModifiedTimes = new HashMap<>();
    private final Map<String, URI> configurationFiles = new HashMap<>();
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private ConfigFileLoaderManager configFileLoader;
    private String appsRootDirectory = null;

    @PostConstruct
    private void validateConfigurationFiles() {
        for (URI file: this.configurationFiles.values()) {
            Assert.isTrue(this.configFileLoader.isAccessible(file),
                          file + " does not exist or is not accessible.");
        }
    }

    @Override
    public final synchronized MapPrinter create(@Nullable final String app) throws NoSuchAppException {
        String finalApp = app;
        if (app == null) {
            finalApp = DEFAULT_CONFIGURATION_FILE_KEY;
        }
        URI configFile = this.configurationFiles.get(finalApp);

        if (configFile == null) {
            configFile = checkForAddedApp(finalApp);
        }

        if (configFile == null) {
            throw new NoSuchAppException(
                    "There is no configurationFile registered in the " + getClass().getName() +
                            " bean with the id: '" + finalApp + "'");
        }

        final long lastModified = this.configurationFileLastModifiedTimes.getOrDefault(finalApp, 0L);

        MapPrinter printer = this.printers.get(finalApp);

        final Optional<Long> configFileLastModified;
        try {
            configFileLastModified = this.configFileLoader.lastModified(configFile);
        } catch (NoSuchElementException e) {
            // the app has been removed
            this.configurationFiles.remove(finalApp);
            this.configurationFileLastModifiedTimes.remove(finalApp);
            this.printers.remove(finalApp);
            if (finalApp.equals(DEFAULT_CONFIGURATION_FILE_KEY)) {
                pickDefaultApp();
            }
            throw new NoSuchAppException(
                    "There is no configurationFile registered in the " + getClass().getName() +
                            " bean with the id: '" + finalApp + "'");
        }
        if (configFileLastModified.isPresent() && configFileLastModified.get() > lastModified) {
            // file modified, reload it
            LOGGER.info("Configuration file modified. Reloading...");

            this.printers.remove(finalApp);
            printer = null;
        }

        if (printer == null) {
            if (configFileLastModified.isPresent()) {
                this.configurationFileLastModifiedTimes.put(finalApp, configFileLastModified.get());
            }

            try {
                LOGGER.info("Loading configuration file: {}", configFile);
                printer = this.applicationContext.getBean(MapPrinter.class);
                byte[] bytes = this.configFileLoader.loadFile(configFile);
                printer.setConfiguration(configFile, bytes);

                this.printers.put(finalApp, printer);
            } catch (ClosedByInterruptException e) {
                // because of a bug in the JDK, the interrupted status might not be set
                // when throwing a ClosedByInterruptException. so, we do it manually.
                // see also http://bugs.java.com/view_bug.do?bug_id=7043425
                Thread.currentThread().interrupt();
                LOGGER.error("Error occurred while reading configuration file '{}'", configFile);
                throw new RuntimeException(String.format(
                        "Error occurred while reading configuration file '%s': ", configFile),
                                           e);
            } catch (Throwable e) {
                LOGGER.error("Error occurred while reading configuration file '{}'", configFile);
                throw new RuntimeException(String.format(
                        "Error occurred while reading configuration file '%s'", configFile), e);
            }
        }

        return printer;
    }

    @Override
    public final Set<String> getAppIds() {
        return this.configurationFiles.keySet();
    }

    /**
     * The setter for setting configuration file.  It will convert the value to a URI.
     *
     * @param configurationFiles the configuration file map.
     */
    public final void setConfigurationFiles(final Map<String, String> configurationFiles)
            throws URISyntaxException {
        this.configurationFiles.clear();
        this.configurationFileLastModifiedTimes.clear();
        for (Map.Entry<String, String> entry: configurationFiles.entrySet()) {
            if (!entry.getValue().contains(":/")) {
                // assume is a file
                this.configurationFiles.put(entry.getKey(), new File(entry.getValue()).toURI());
            } else {
                this.configurationFiles.put(entry.getKey(), new URI(entry.getValue()));
            }
        }

        if (this.configFileLoader != null) {
            this.validateConfigurationFiles();
        }
    }

    /**
     * Set a single directory that contains one or more subdirectories, each one that contains a config.yaml
     * file will be considered a print app.
     * <p>
     * This can be called multiple times and each directory will add to the apps found in the other
     * directories.  However the appId is based on the directory names so if there are 2 directories with the
     * same name the second will overwrite the first encounter.
     *
     * @param directory the root directory containing the sub-app-directories.  This must resolve to a
     *         file with the
     */
    public final void setAppsRootDirectory(final String directory) throws URISyntaxException, IOException {
        this.appsRootDirectory = directory;

        final File realRoot;
        if (!directory.contains(":/")) {
            realRoot = new File(directory);
        } else {
            final Optional<File> fileOptional = this.configFileLoader.toFile(new URI(directory));
            if (fileOptional.isPresent()) {
                realRoot = fileOptional.get();
            } else {
                throw new IllegalArgumentException(
                        directory + " does not refer to a file on the current system.");
            }
        }

        final AppWalker walker = new AppWalker();
        for (File child: walker.getAppDirs(realRoot)) {
            final File configFile = new File(child, CONFIG_YAML);
            String appName = realRoot.toURI().relativize(child.toURI()).getPath().replace('/', ':');
            if (appName.endsWith(":")) {
                appName = appName.substring(0, appName.length() - 1);
            }
            this.configurationFiles.put(appName, configFile.toURI());
        }
        if (this.configurationFiles.isEmpty()) {
            return;
        }

        // ensure there is a "default" app
        if (!this.configurationFiles.containsKey(DEFAULT_CONFIGURATION_FILE_KEY)) {
            pickDefaultApp();
        }
    }

    private void pickDefaultApp() {
        final Iterator<Map.Entry<String, URI>> iterator = this.configurationFiles.entrySet().iterator();
        if (iterator.hasNext()) {
            final Map.Entry<String, URI> next = iterator.next();
            final URI uri = next.getValue();
            this.configurationFiles.put(DEFAULT_CONFIGURATION_FILE_KEY, uri);
        }
    }

    @Nullable
    private URI checkForAddedApp(@Nonnull final String app) {
        if (this.appsRootDirectory == null) {
            return null;
        }

        if (StringUtils.countMatches(app, ":") > MAX_DEPTH) {
            return null;
        }
        final Optional<File> child;
        try {
            child = this.configFileLoader.toFile(new URI(this.appsRootDirectory + "/" +
                                                                 app.replace(':', '/')));
        } catch (URISyntaxException e) {
            return null;
        }
        if (child.isPresent()) {
            final File configFile = new File(child.get(), CONFIG_YAML);
            if (configFile.exists()) {
                final URI uri = configFile.toURI();
                this.configurationFiles.put(app, uri);
                if (!this.configurationFiles.containsKey(DEFAULT_CONFIGURATION_FILE_KEY)) {
                    this.configurationFiles.put(DEFAULT_CONFIGURATION_FILE_KEY, uri);
                }
                return uri;
            }
        }
        return null;
    }

    private static class AppWalker extends DirectoryWalker<File> {
        public List<File> getAppDirs(final File base) throws IOException {
            List<File> results = new ArrayList<>();
            walk(base, results);
            return results;
        }

        @Override
        protected boolean handleDirectory(
                final File directory, final int depth,
                final Collection<File> results) {
            final File configFile = new File(directory, CONFIG_YAML);
            if (configFile.exists()) {
                results.add(directory);
            }
            return depth < MAX_DEPTH;
        }
    }
}
