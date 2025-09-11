package org.mapfish.print.config;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

/** Strategy/plug-in for loading {@link Configuration} objects. */
public class ConfigurationFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFactory.class);
  @Autowired private ConfigurableApplicationContext context;
  private ThreadLocal<Yaml> yamlThreadLocal;
  private boolean doValidation = true;

  /** initialize this factory. Called by spring after construction. */
  @PostConstruct
  public final void init() {
    LoaderOptions loaderOptions = new LoaderOptions();
    String maxAliases = System.getenv("PRINT_YAML_MAX_ALIASES");
    if (maxAliases != null) {
      loaderOptions.setMaxAliasesForCollections(Integer.parseInt(maxAliases));
    }
    DumperOptions dumperOptions = new DumperOptions();
    Representer representer = new Representer(dumperOptions);
    this.yamlThreadLocal =
        ThreadLocal.withInitial(
            () -> {
              MapfishPrintConstructor constructor = new MapfishPrintConstructor(this.context);
              return new Yaml(constructor, representer, dumperOptions, loaderOptions);
            });
  }

  /**
   * Create a configuration object from a config file.
   *
   * @param configFile the file to read the configuration from.
   */
  @VisibleForTesting
  public final Configuration getConfig(final File configFile) throws IOException {
    try (FileInputStream in = new FileInputStream(configFile)) {
      return getConfig(configFile, in);
    }
  }

  /**
   * Create a configuration object from a config file.
   *
   * @param configFile the file that contains the configuration data.
   * @param configData the config file data
   */
  public final Configuration getConfig(final File configFile, final InputStream configData)
      throws IOException {
    final Configuration configuration = this.context.getBean(Configuration.class);
    configuration.setConfigurationFile(configFile);
    MapfishPrintConstructor.setConfigurationUnderConstruction(configuration);

    try (InputStreamReader reader = new InputStreamReader(configData, StandardCharsets.UTF_8)) {
      final Configuration config = this.yamlThreadLocal.get().load(reader);
      if (this.doValidation) {
        final List<Throwable> validate = config.validate();
        if (!validate.isEmpty()) {
          StringBuilder errors = new StringBuilder();
          for (Throwable throwable : validate) {
            errors.append("\n\t* ").append(throwable.getMessage());
            LOGGER.error("Configuration Error found", throwable);
          }
          throw new Error(errors.toString(), validate.get(0));
        }
      }
      return config;
    } finally {
      this.yamlThreadLocal.remove();
    }
  }

  /**
   * If doValidation is true then the Configuration object will be validated after loading. However
   * for some tests we don't want this so this method allows it to be set to false for tests.
   *
   * <p>By default it is true so only tests should modify this.
   *
   * @param doValidation the new validation value.
   */
  public final void setDoValidation(final boolean doValidation) {
    this.doValidation = doValidation;
  }
}
