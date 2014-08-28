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

package org.mapfish.print.config;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import javax.annotation.PostConstruct;

import static com.google.common.io.Files.getNameWithoutExtension;

/**
 * Class for configuring the working directories and ensuring they exist correctly.
 * <p/>
 * @author jesseeichar on 3/25/14.
 */
public class WorkingDirectories {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkingDirectories.class);
    
    private File working;
    private File reports;
    private File reportsOldApi;

    public final void setWorking(final File working) {
        this.working = working;
    }

    public final File getWorking() {
        return this.working;
    }

    /**
     * Called by spring after bean has been created and populated.
     */
    @PostConstruct
    public final void init() {
        this.reports = new File(this.working, "reports");
        this.reportsOldApi = new File(this.working, "reports-old-api");
    }
    /**
     * Get the directory where the compiled jasper reports should be put.
     * @param configuration the configuration for the current app.
     */
    public final File getJasperCompilation(final Configuration configuration) {
        File jasperCompilation = new File(getWorking(configuration), "jasper-bin");
        createIfMissing(jasperCompilation, "Jasper Compilation");
        return jasperCompilation;
    }

    /**
     * Get the directory where the reports are written to.  This may be a temporary location before sending the files to a
     * central repository that can better handle clustering.
     */
    public final File getReports() {
        createIfMissing(this.reports, "Reports");
        return this.reports;
    }
    
    /**
     * Creates and returns a temporary directory for a printing task.
     */
    public final File getTaskDirectory() {
        createIfMissing(this.working, "Working");
        try {
            File file = File.createTempFile("task-", "tmp", this.working);
            if (!file.delete() || !file.mkdirs()) {
                throw new IOException("Unable to make temporary directory: " + file);
            }
            return file;
        } catch (IOException e) {
            throw new AssertionError("Unable to create temporary directory in '" + this.working + "'");
        }
    }

    /**
     * Get the directory where the reports of the old API servlet are written to.
     */
    public final File getReportsOldApi() {
        createIfMissing(this.reportsOldApi, "Reports-Old-API");
        return this.reportsOldApi;
    }

    /**
     * Deletes the given directory.
     * 
     * @param directory The directory to delete.
     */
    public final void removeDirectory(final File directory) {
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            LOGGER.error("Unable to delete directory '" + directory + "'");
        }
    }

    private void createIfMissing(final File directory, final String name) {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new AssertionError("Unable to create working directory: '" + directory + "' it is the '" + name + "' directory");
        }
    }

    /**
     * Calculate the file to compile a jasper report template to.
     *
     * @param configuration the configuration for the current app.
     * @param jasperFileXml the jasper report template in xml format.
     * @param extension the extension of the compiled report template.
     * @param logger the logger to log errors to if an occur.
     */
    public final File getBuildFileFor(final Configuration configuration, final File jasperFileXml,
                                      final String extension, final Logger logger) {
        final String configurationAbsolutePath = configuration.getDirectory().getPath();
        final int prefixToConfiguration = configurationAbsolutePath.length() + 1;
        final String parentDir = jasperFileXml.getAbsoluteFile().getParent();
        final String relativePathToFile;
        if (configurationAbsolutePath.equals(parentDir)) {
            relativePathToFile = getNameWithoutExtension(jasperFileXml.getName());
        } else {
            final String relativePathToContainingDirectory = parentDir.substring(prefixToConfiguration);
            relativePathToFile = relativePathToContainingDirectory + File.separator +
                                 getNameWithoutExtension(jasperFileXml.getName());
        }

        final File buildFile = new File(getJasperCompilation(configuration), relativePathToFile + extension);

        if (!buildFile.getParentFile().exists() && !buildFile.getParentFile().mkdirs()) {
            logger.error("Unable to create directory for containing compiled jasper report templates: " + buildFile.getParentFile());
        }
        return buildFile;
    }

    /**
     * Get the working directory for the configuration.
     *
     * @param configuration the configuration for the current app.
     */
    public final File getWorking(final Configuration configuration) {
        return new File(this.working, configuration.getDirectory().getName());
    }

}
