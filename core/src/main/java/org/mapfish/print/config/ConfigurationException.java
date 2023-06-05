package org.mapfish.print.config;

/** Represents an error made in the config.yaml file. */
public class ConfigurationException extends RuntimeException {

  /** */
  private static final long serialVersionUID = -5693438899003802581L;

  /**
   * Constructor.
   *
   * @param message the error message.
   */
  public ConfigurationException(final String message) {
    super(message);
  }

  /**
   * Constructor.
   *
   * @param message the error message.
   * @param cause an exception that is the true cause of the error.
   */
  public ConfigurationException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
