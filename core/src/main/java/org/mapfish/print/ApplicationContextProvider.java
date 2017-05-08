package org.mapfish.print;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 *
 * Provides application context in static context.
 *
 */
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext context;

    public static ApplicationContext getApplicationContext() {
        return context;
    }

    @Override
    public final void setApplicationContext(final ApplicationContext ctx) {
        context = ctx;
    }
}
