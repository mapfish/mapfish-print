package org.mapfish.print.metrics;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.logback.InstrumentedAppender;
import jakarta.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/** Utility class for adding metrics instrumentation to logging framework. */
public class LoggingMetricsConfigurator {
  @Autowired private MetricRegistry metricRegistry;

  /**
   * Add an appender to Logback logging framework that will track the types of log messages made.
   */
  @PostConstruct
  public final void addMetricsAppenderToLogback() {
    final LoggerContext factory = (LoggerContext) LoggerFactory.getILoggerFactory();
    final Logger root = factory.getLogger(Logger.ROOT_LOGGER_NAME);

    final InstrumentedAppender metrics = new InstrumentedAppender(this.metricRegistry);
    metrics.setContext(root.getLoggerContext());
    metrics.start();
    root.addAppender(metrics);
  }
}
