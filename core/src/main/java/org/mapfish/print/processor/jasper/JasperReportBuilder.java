package org.mapfish.print.processor.jasper;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.design.JRValidationException;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.HasConfiguration;
import org.mapfish.print.config.WorkingDirectories;
import org.mapfish.print.processor.AbstractProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * <p>A processor that actually compiles a JasperReport template file.</p>
 * <p>Example</p>
 * <pre><code>
 *     processors:
 *         - !reportBuilder # compile all reports in current directory
 *               directory: '.'</code></pre>
 * [[examples=verboseExample]]
 */
public final class JasperReportBuilder extends AbstractProcessor<JasperReportBuilder.Input, Void>
        implements HasConfiguration {
    /**
     * Extension for Jasper XML Report Template files.
     */
    public static final String JASPER_REPORT_XML_FILE_EXT = ".jrxml";
    /**
     * Extension for Compiled Jasper Report Template files.
     */
    public static final String JASPER_REPORT_COMPILED_FILE_EXT = ".jasper";
    private static final Logger LOGGER = LoggerFactory.getLogger(JasperReportBuilder.class);
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
    public Void execute(final JasperReportBuilder.Input param, final ExecutionContext context)
            throws JRException {
        for (final File jasperFile: jasperXmlFiles()) {
            context.stopIfCanceled();
            compileJasperReport(this.configuration, jasperFile);
        }
        return null;
    }

    File compileJasperReport(final Configuration config, final File jasperFile) throws JRException {
        final File buildFile = this.workingDirectories.getBuildFileFor(
                config, jasperFile, JASPER_REPORT_COMPILED_FILE_EXT, LOGGER);
        return compileJasperReport(buildFile, jasperFile);
    }

    File compileJasperReport(final File buildFile, final File jasperFile) throws JRException {
        if (!buildFile.exists() || jasperFile.lastModified() > buildFile.lastModified()) {
            try {
                // May be called from multiple threads at the same time for the same report.
                // Instead of trying to protect the compiled file against modification while
                // another thread is reading it, use a temporary file as a target instead and
                // move it (atomic operation) when done. Worst case: we compile a file twice instead
                // of once.
                File tmpBuildFile = File.createTempFile(
                        "temp_", JASPER_REPORT_COMPILED_FILE_EXT, buildFile.getParentFile());

                LOGGER.info("Building Jasper report: {}", jasperFile.getAbsolutePath());
                LOGGER.debug("To: {}", buildFile.getAbsolutePath());
                final Timer.Context compileJasperReport = this.metricRegistry.timer(getClass().getName() +
                                                                                            ".compile." +
                                                                                            jasperFile)
                        .time();
                try {
                    JasperCompileManager.compileReportToFile(jasperFile.getAbsolutePath(),
                                                             tmpBuildFile.getAbsolutePath());
                } catch (JRValidationException e) {
                    LOGGER.error("The report '{}' isn't valid.", jasperFile.getAbsolutePath());
                    throw e;
                } finally {
                    final long compileTime = TimeUnit.MILLISECONDS.convert(
                            compileJasperReport.stop(), TimeUnit.NANOSECONDS);
                    LOGGER.info("Report '{}' built in {}ms.", jasperFile.getAbsolutePath(), compileTime);
                }

                java.nio.file.Files.move(
                        tmpBuildFile.toPath(), buildFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                throw new JRException(e);
            }
        } else {
            LOGGER.debug("Destination file is already up to date: {}", buildFile.getAbsolutePath());
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
            throw new IllegalArgumentException(String.format(
                    "All directories and files referenced in the configuration must be in the " +
                            "configuration directory: %s is not in %s.",
                    directoryToSearch, this.configuration.getDirectory()));
        }
        final File[] children = directoryToSearch.listFiles();
        if (children != null) {
            return StreamSupport.stream(Arrays.spliterator(children), false)
                    .filter(input -> input != null && input.getName().endsWith(JASPER_REPORT_XML_FILE_EXT))
                    .collect(Collectors.toList());
        } else {
            throw new IllegalArgumentException(String.format("%s is not a directory", directoryToSearch));
        }
    }


    @Override
    public JasperReportBuilder.Input createInputParameter() {
        return new JasperReportBuilder.Input();
    }

    /**
     * Set the directory and test that the directory exists and is contained within the Configuration
     * directory.
     *
     * @param directory the new directory
     */
    public void setDirectory(final String directory) {
        this.directory = new File(this.configuration.getDirectory(), directory);
        if (!this.directory.exists()) {
            throw new IllegalArgumentException(String.format(
                    "Directory does not exist: %s.\n" +
                            "Configuration contained value %s which is supposed to be relative to " +
                            "configuration directory.",
                    this.directory, directory));
        }

        if (!this.directory.getAbsolutePath()
                .startsWith(this.configuration.getDirectory().getAbsolutePath())) {
            throw new IllegalArgumentException(String.format(
                    "All files and directories must be contained in the configuration directory the " +
                            "directory provided in the configuration breaks that contract: %s in config " +
                            "file resolved to %s.",
                    directory, this.directory));
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

    @Override
    protected void extraValidation(final List<Throwable> validationErrors, final Configuration config) {
        // nothing to do
    }

    /**
     * The input parameter object for {@link JasperReportBuilder}.
     */
    public static final class Input {
    }
}
