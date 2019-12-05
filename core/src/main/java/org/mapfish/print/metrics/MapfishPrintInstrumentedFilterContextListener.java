package org.mapfish.print.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlet.InstrumentedFilterContextListener;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import static org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext;

/**
 * Allows the AdminServlet to get access to the MetricRegistry so it can display the statistics via the admin
 * servlet.
 *
 */
public class MapfishPrintInstrumentedFilterContextListener extends InstrumentedFilterContextListener {
    private ServletContext servletContext;

    @Override
    protected final MetricRegistry getMetricRegistry() {
        final WebApplicationContext webApplicationContext = getWebApplicationContext(this.servletContext);
        return webApplicationContext.getBean(MetricRegistry.class);
    }

    @Override
    public final void contextInitialized(final ServletContextEvent event) {
        this.servletContext = event.getServletContext();
        super.contextInitialized(event);
    }

}
