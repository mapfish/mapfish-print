package org.mapfish.print.config;

/**
 * Indicates that the {@link org.mapfish.print.config.ConfigurationFactory} should inject the configuration
 * object into the object.
 */
public interface HasConfiguration {
    /**
     * Set the configuration that the object belongs to.
     *
     * @param configuration the configuration object
     */
    void setConfiguration(Configuration configuration);
}
