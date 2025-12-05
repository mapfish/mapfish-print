package org.mapfish.print.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

/** A bean that will add several gauges and metric sets for tracking the JVM stats. */
public final class JvmMetricsConfigurator {

  @Autowired private MetricRegistry metricRegistry;

  /** Add several jvm metrics. */
  @PostConstruct
  public void init() {
    this.metricRegistry.register(name("gc"), new GarbageCollectorMetricSet());
    this.metricRegistry.register(name("memory"), new MemoryUsageGaugeSet());
    this.metricRegistry.register(name("thread-states"), new ThreadStatesGaugeSet());
    this.metricRegistry.register(name("fd-usage"), new FileDescriptorRatioGauge());
  }

  private String name(final String metricName) {
    return "jvm-" + metricName;
  }
}
