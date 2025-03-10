package org.mapfish.print.config;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.mapfish.print.metrics.UnhealthyCountersHealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/** Class for configuring the working directories and ensuring they exist correctly. */
public class WorkingDirectories {
  private static final String TASK_DIR_PREFIX = "task-";

  private static final Logger LOGGER = LoggerFactory.getLogger(WorkingDirectories.class);

  private File working;
  private File reports;
  private int maxAgeReport;
  private int maxAgeTaskDir;

  @Autowired private ServletContext servletContext;

  @Autowired private UnhealthyCountersHealthCheck unhealthyCountersHealthCheck;

  public final File getWorking() {
    return this.working;
  }

  /**
   * Defines what is the root directory used to store every temporary files.
   *
   * <p>The given path can contain the pattern "{WEBAPP}" and it will be replaced by the webapp
   * name.
   *
   * @param working The path
   */
  public final void setWorking(final String working) {
    final String webappPath = this.servletContext.getRealPath("/");
    final String webappName;
    if (webappPath != null) {
      webappName = new File(webappPath).getName();
    } else {
      webappName = "default"; // left like that only in UTs
    }
    this.working = new File(working.replace("{WEBAPP}", webappName));
    createIfMissing(this.working, "working");
    LOGGER.debug("Working directory: {}", this.working);
  }

  public final void setMaxAgeReport(final int maxAgeReport) {
    this.maxAgeReport = maxAgeReport;
  }

  public final void setMaxAgeTaskDir(final int maxAgeTaskDir) {
    this.maxAgeTaskDir = maxAgeTaskDir;
  }

  /** Called by spring after bean has been created and populated. */
  @PostConstruct
  public final void init() {
    this.reports = new File(this.working, "reports");
  }

  /**
   * Get the directory where the compiled jasper reports should be put.
   *
   * @param configuration the configuration for the current app.
   */
  public final File getJasperCompilation(final Configuration configuration) {
    File jasperCompilation = new File(getWorking(configuration), "jasper-bin");
    createIfMissing(jasperCompilation, "Jasper Compilation");
    return jasperCompilation;
  }

  /**
   * Get the directory where the reports are written to. This may be a temporary location before
   * sending the files to a central repository that can better handle clustering.
   */
  public final File getReports() {
    createIfMissing(this.reports, "Reports");
    return this.reports;
  }

  /** Creates and returns a temporary directory for a printing task. */
  public final File getTaskDirectory() {
    createIfMissing(this.working, "Working");
    try {
      return Files.createTempDirectory(this.working.toPath(), TASK_DIR_PREFIX).toFile();
    } catch (IOException e) {
      throw new AssertionError("Unable to create temporary directory in '" + this.working + "'");
    }
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
      LOGGER.error("Unable to delete directory '{}'", directory);
    }
  }

  private void createIfMissing(final File directory, final String name) {
    if (!directory.exists() && !directory.mkdirs()) {
      if (!directory.exists()) { // Maybe somebody else created it in the meantime
        throw new AssertionError(
            "Unable to create working directory: '"
                + directory
                + "' it is the '"
                + name
                + "' directory");
      }
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
  public final File getBuildFileFor(
      final Configuration configuration,
      final File jasperFileXml,
      final String extension,
      final Logger logger) {
    final String configurationAbsolutePath = configuration.getDirectory().getPath();
    final String relativePathToFile =
        determineRelativePath(jasperFileXml, configurationAbsolutePath);

    final File buildFile =
        new File(getJasperCompilation(configuration), relativePathToFile + extension);

    if (!buildFile.getParentFile().exists() && !buildFile.getParentFile().mkdirs()) {
      logger.error(
          "Unable to create directory for containing compiled jasper report templates: {}",
          buildFile.getParentFile());
    }
    return buildFile;
  }

  private String determineRelativePath(final File jasperXmlFile, final String configAbsolutePath) {
    final int prefixToConfiguration = configAbsolutePath.length() + 1;
    final String parentDir = jasperXmlFile.getAbsoluteFile().getParent();
    final String relativePathToFile;
    if (configAbsolutePath.equals(parentDir)) {
      relativePathToFile = FilenameUtils.getBaseName(jasperXmlFile.getName());
    } else {
      final String relativePathToContainingDirectory = parentDir.substring(prefixToConfiguration);
      relativePathToFile =
          relativePathToContainingDirectory
              + File.separator
              + FilenameUtils.getBaseName(jasperXmlFile.getName());
    }
    return relativePathToFile;
  }

  /**
   * Get the working directory for the configuration.
   *
   * @param configuration the configuration for the current app.
   */
  public final File getWorking(final Configuration configuration) {
    return new File(this.working, configuration.getDirectory().getName());
  }

  public final Runnable getCleanUpTask() {
    return new CleanUpTask(this.maxAgeReport, this.maxAgeTaskDir);
  }

  /** A task that deletes old reports and task directories. */
  @VisibleForTesting
  final class CleanUpTask implements Runnable {

    /** The maximum age for a report in seconds. Files older than that age will be deleted. */
    private final long maxAgeReport;

    /**
     * The maximum age for a task directory in seconds. Directories older than that age will be
     * deleted.
     */
    private final long maxAgeTaskDir;

    /**
     * @param maxAgeReport The maximum age for a report in seconds.
     * @param maxAgeTaskDir The maximum age for a task directory in seconds.
     */
    CleanUpTask(final long maxAgeReport, final long maxAgeTaskDir) {
      this.maxAgeReport = maxAgeReport;
      this.maxAgeTaskDir = maxAgeTaskDir;
    }

    @Override
    public void run() {
      try {
        removeOldFiles(WorkingDirectories.this.reports, null, this.maxAgeReport);
        removeOldFiles(WorkingDirectories.this.working, TASK_DIR_PREFIX, this.maxAgeTaskDir);
        // temporary "fix" for https://github.com/mapfish/mapfish-print/issues/317
        removeOldFiles(new File(System.getProperty("java.io.tmpdir")), "+~JF", this.maxAgeTaskDir);
      } catch (Exception e) {
        LOGGER.error("error running file clean-up task", e);
        unhealthyCountersHealthCheck.recordUnhealthyProblem(
            getClass().getSimpleName(), "errorRunningFileCleanUpTask");
      }
    }

    private void removeOldFiles(final File dir, final String prefix, final long maxAge) {
      final long ageThreshold = new Date().getTime() - maxAge * 1000;

      int deletedFiles = 0;
      if (dir.exists()) {
        for (File file : Objects.requireNonNull(dir.listFiles())) {
          if ((prefix == null || file.getName().startsWith(prefix))
              && file.lastModified() < ageThreshold) {
            if (!FileUtils.deleteQuietly(file)) {
              LOGGER.warn("failed to delete file {}", file.getAbsolutePath());
            } else {
              deletedFiles++;
            }
          }
        }
      }
      LOGGER.info("deleted {} old file(s) in {}", deletedFiles, dir.getPath());
    }
  }
}
