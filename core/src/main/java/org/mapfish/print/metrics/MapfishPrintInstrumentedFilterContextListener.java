package org.mapfish.print.metrics;

import static org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlet.InstrumentedFilterContextListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import org.springframework.web.context.WebApplicationContext;

/**
 * Allows the AdminServlet to get access to the MetricRegistry so it can display the statistics via
 * the admin servlet.
 */
public class MapfishPrintInstrumentedFilterContextListener
    extends InstrumentedFilterContextListener {
  private ServletContext servletContext;

  @Override
  protected final MetricRegistry getMetricRegistry() {

    final WebApplicationContext webApplicationContext =
        getWebApplicationContext(this.servletContext);
    return webApplicationContext.getBean(MetricRegistry.class);
  }

  @Override
  public final void contextInitialized(final ServletContextEvent event) {
    this.servletContext = event.getServletContext();
    super.contextInitialized(event);
  }
}
