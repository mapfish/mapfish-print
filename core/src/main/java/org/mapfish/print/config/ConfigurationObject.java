package org.mapfish.print.config;

import java.util.List;

/**
 * A flag interface for a configuration object.  This to allow spring to find them as plugins.
 * <p></p>
 * @author jesseeichar on 3/4/14.
 */
public interface ConfigurationObject {
    /**
     * validate that the configuration was correct.
     *
     * @param validationErrors a list to add any detected errors to.
     * @param configuration the containing configuration
     */
    void validate(List<Throwable> validationErrors, final Configuration configuration);
}
