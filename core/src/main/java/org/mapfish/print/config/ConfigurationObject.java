package org.mapfish.print.config;

import java.util.List;

/**
 * A flag interface for a configuration object.  This to allow spring to find them as plugins.
 *
 */
public interface ConfigurationObject {
    /**
     * validate that the configuration was correct.
     *
     * @param validationErrors a list to add any detected errors to.
     * @param configuration the containing configuration
     */
    void validate(List<Throwable> validationErrors, Configuration configuration);
}
