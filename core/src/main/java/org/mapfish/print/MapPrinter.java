package org.mapfish.print;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import jakarta.annotation.Nonnull;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.WorkingDirectories;
import org.mapfish.print.output.OutputFormat;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.servlet.MapPrinterServlet;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The main class for printing maps. Will parse the spec, create the PDF document and generate it.
 *
 * <p>This class should not be directly created but rather obtained from an application context
 * object so that all plugins and dependencies are correctly injected into it
 */
public class MapPrinter {

  private static final String OUTPUT_FORMAT_BEAN_NAME_ENDING = "OutputFormat";
  private static final String MAP_OUTPUT_FORMAT_BEAN_NAME_ENDING = "MapOutputFormat";

  private Configuration configuration;
  @Autowired private Map<String, OutputFormat> outputFormat;
  @Autowired private ConfigurationFactory configurationFactory;

  private File configFile;
  @Autowired private WorkingDirectories workingDirectories;

  /**
   * Parse the JSON string and return the object. The string is expected to be the JSON print data
   * from the client.
   *
   * @param spec the JSON formatted string.
   * @return The encapsulated JSON object
   */
  public static PJsonObject parseSpec(final String spec) {
    final JSONObject jsonSpec;
    try {
      jsonSpec = new JSONObject(spec);
    } catch (JSONException e) {
      throw new RuntimeException("Cannot parse the spec file: " + spec, e);
    }
    return new PJsonObject(jsonSpec, "spec");
  }

  /**
   * Set the configuration file and update the configuration for this printer.
   *
   * @param newConfigFile the file containing the new configuration.
   * @param configFileData the config file data.
   */
  public final void setConfiguration(final URI newConfigFile, final byte[] configFileData)
      throws IOException {
    this.configFile = new File(newConfigFile);
    this.configuration =
        this.configurationFactory.getConfig(
            this.configFile, new ByteArrayInputStream(configFileData));
  }

  public final Configuration getConfiguration() {
    return this.configuration;
  }

  /**
   * Set the configuration file and update the configuration for this printer.
   *
   * @param newConfigFile the file containing the new configuration.
   */
  public final void setConfiguration(final File newConfigFile) throws IOException {
    setConfiguration(newConfigFile.toURI(), Files.readAllBytes(newConfigFile.toPath()));
  }

  /**
   * Use by /info.json to generate its returned content.
   *
   * @param json the writer for outputting the config specification
   */
  public final void printClientConfig(final JSONWriter json) throws JSONException {
    this.configuration.printClientConfig(json);
  }

  /**
   * Get the object responsible for printing to the correct output format.
   *
   * @param specJson the request json from the client
   */
  public final OutputFormat getOutputFormat(final PJsonObject specJson) {
    final String format = specJson.getString(MapPrinterServlet.JSON_OUTPUT_FORMAT);
    final boolean mapExport =
        this.configuration.getTemplate(specJson.getString(Constants.JSON_LAYOUT_KEY)).isMapExport();
    final String beanName =
        format + (mapExport ? MAP_OUTPUT_FORMAT_BEAN_NAME_ENDING : OUTPUT_FORMAT_BEAN_NAME_ENDING);

    if (!this.outputFormat.containsKey(beanName)) {
      throw new RuntimeException(
          "Format '" + format + "' with mapExport '" + mapExport + "' is not supported.");
    }

    return this.outputFormat.get(beanName);
  }

  /**
   * Start a print.
   *
   * @param mdcContext the MDC context for the current print job.
   * @param specJson the client json request.
   * @param out the stream to write to.
   */
  public final Processor.ExecutionContext print(
      @Nonnull final Map<String, String> mdcContext,
      final PJsonObject specJson,
      final OutputStream out)
      throws Exception {
    final OutputFormat format = getOutputFormat(specJson);
    final File taskDirectory = this.workingDirectories.getTaskDirectory();

    try {
      return format.print(
          mdcContext,
          specJson,
          getConfiguration(),
          this.configFile.getParentFile(),
          taskDirectory,
          out);
    } finally {
      this.workingDirectories.removeDirectory(taskDirectory);
    }
  }

  /** Return the available format ids. */
  public final Set<String> getOutputFormatsNames() {
    SortedSet<String> formats = new TreeSet<>();
    for (String formatBeanName : this.outputFormat.keySet()) {
      int endingIndex = formatBeanName.indexOf(MAP_OUTPUT_FORMAT_BEAN_NAME_ENDING);
      if (endingIndex < 0) {
        endingIndex = formatBeanName.indexOf(OUTPUT_FORMAT_BEAN_NAME_ENDING);
      }
      formats.add(formatBeanName.substring(0, endingIndex));
    }
    return formats;
  }
}
