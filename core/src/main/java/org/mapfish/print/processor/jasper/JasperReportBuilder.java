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

package org.mapfish.print.processor.jasper;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.HasConfiguration;
import org.mapfish.print.config.WorkingDirectories;
import org.mapfish.print.processor.AbstractProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/**
 * A processor that actually compiles a jasper report.
 *
 * @author Jesse
 * @author sbrunner
 */
public final class JasperReportBuilder extends AbstractProcessor<JasperReportBuilder.Input, Void> implements HasConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(JasperReportBuilder.class);
    /**
     * Extension for Jasper XML Report Template files.
     */
    public static final String JASPER_REPORT_XML_FILE_EXT = ".jrxml";
    /**
     * Extension for Compiled Jasper Report Template files.
     */
    public static final String JASPER_REPORT_COMPILED_FILE_EXT = ".jasper";

    private File directory = null;
    private Configuration configuration;
    @Autowired
    private MetricRegistry metricRegistry;
    @Autowired
    private WorkingDirectories workingDirectories;

    /**
     * Constructor.
     */
    protected JasperReportBuilder() {
        super(Void.class);
    }

    @Override
    public Void execute(final JasperReportBuilder.Input param, final ExecutionContext context) throws JRException {
        Timer.Context buildReports = this.metricRegistry.timer(getClass() + "_execute()").time();
        try {
            for (final File jasperFile : jasperXmlFiles()) {
                checkCancelState(context);
                compileJasperReport(this.configuration, jasperFile);
            }
            return null;
        } finally {
            buildReports.stop();
        }
    }

    File compileJasperReport(final Configuration config, final File jasperFile) throws JRException {
        final File buildFile = this.workingDirectories.getBuildFileFor(config, jasperFile, JASPER_REPORT_COMPILED_FILE_EXT, LOGGER);
        return compileJasperReport(buildFile, jasperFile);
    }

    File compileJasperReport(final File buildFile, final File jasperFile) throws JRException {

        if (!buildFile.exists() || jasperFile.lastModified() > buildFile.lastModified()) {
            LOGGER.info("Building Jasper report: " + jasperFile.getAbsolutePath());
            LOGGER.debug("To: " + buildFile.getAbsolutePath());
            final Timer.Context compileJasperReport = this.metricRegistry.timer("compile_" + jasperFile).time();
            try {
                JasperCompileManager.compileReportToFile(jasperFile.getAbsolutePath(), buildFile.getAbsolutePath());
            } finally {
                final long compileTime = TimeUnit.MILLISECONDS.convert(compileJasperReport.stop(), TimeUnit.NANOSECONDS);
                LOGGER.info("Report built in " + compileTime + "ms.");
            }
        } else {
            LOGGER.debug("Destination file is already up to date: " + buildFile.getAbsolutePath());
        }
        return buildFile;
    }

    private Iterable<File> jasperXmlFiles() {
        File directoryToSearch = this.directory;
        if (directoryToSearch == null) {
            directoryToSearch = this.configuration.getDirectory();
        }
        final String configurationAbsolutePath = this.configuration.getDirectory().getAbsolutePath();
        if (!directoryToSearch.getAbsolutePath().startsWith(configurationAbsolutePath)) {
            throw new IllegalArgumentException("All directories and files referenced in the configuration must be in the configuration " +
                                               "directory: " + directoryToSearch + " is not in " + this.configuration.getDirectory());
        }
        final Iterable<File> children = Files.fileTreeTraverser().children(directoryToSearch);
        return Iterables.filter(children, new Predicate<File>() {
            @Override
            public boolean apply(@Nullable final File input) {
                return input != null && input.getName().endsWith(JASPER_REPORT_XML_FILE_EXT);
            }
        });
    }


    @Override
    public JasperReportBuilder.Input createInputParameter() {
        return new JasperReportBuilder.Input();
    }

    /**
     * Set the directory and test that the directory exists and is contained within the Configuration directory.
     *
     * @param directory the new directory
     */
    public void setDirectory(final String directory) {
        this.directory = new File(this.configuration.getDirectory(), directory);
        if (!this.directory.exists()) {
            throw new IllegalArgumentException("Directory does not exist: "
                                               + this.directory + ".\nConfiguration contained value "
                                               + directory + " which is supposed to be relative to configuration directory");
        }

        if (!this.directory.getAbsolutePath().startsWith(this.configuration.getDirectory().getAbsolutePath())) {
            throw new IllegalArgumentException("All files and directories must be contained in the configuration directory" +
                                               " the directory provided in the configuration breaks that contract: " + directory
                                               + " in config file resolved to " + this.directory);
        }
    }

    @Override
    public void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + this.directory + ")";
    }

    /**
     * The input parameter object for {@link JasperReportBuilder}.
     */
    public static final class Input {
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors, final Configuration config) {
        // nothing to do
    }
}
