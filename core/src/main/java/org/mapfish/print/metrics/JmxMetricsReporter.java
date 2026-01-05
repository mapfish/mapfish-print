package org.mapfish.print.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;

/** This bean will enable JMX reporting when added to application context. */
public final class JmxMetricsReporter {

  @Autowired private MetricRegistry metricRegistry;
  private JmxReporter reporter;

  /** Add jmx reporter on startup. */
  @PostConstruct
  public void init() {
    this.reporter = JmxReporter.forRegistry(this.metricRegistry).build();
    this.reporter.start();
  }

  /** Stop JMX reporter. */
  @PreDestroy
  public void destroy() {
    this.reporter.stop();
  }
}
