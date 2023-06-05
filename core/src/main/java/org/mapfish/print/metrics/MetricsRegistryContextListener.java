package org.mapfish.print.metrics;

import static org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlets.MetricsServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import org.springframework.web.context.WebApplicationContext;

/**
 * Allows the AdminServlet to get access to the MetricRegistry so it can display the statistics via
 * the admin servlet.
 */
public class MetricsRegistryContextListener extends MetricsServlet.ContextListener {
  private ServletContext servletContext;

  @Override
  public final void contextInitialized(final ServletContextEvent event) {
    this.servletContext = event.getServletContext();
    super.contextInitialized(event);
  }

  @Override
  protected final MetricRegistry getMetricRegistry() {
    final WebApplicationContext webApplicationContext =
        getWebApplicationContext(this.servletContext);
    return webApplicationContext.getBean(MetricRegistry.class);
  }
}
