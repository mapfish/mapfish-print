package org.mapfish.print.config;

/**
 * Represents an error made in the config.yaml file.
 */
public class ConfigurationException extends RuntimeException {
    private Configuration configuration;

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


    public final Configuration getConfiguration() {
        return this.configuration;
    }

    public final void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }
}
