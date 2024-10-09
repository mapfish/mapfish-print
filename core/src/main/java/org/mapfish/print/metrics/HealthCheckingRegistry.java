package org.mapfish.print.metrics;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

public class HealthCheckingRegistry extends com.codahale.metrics.health.HealthCheckRegistry {
  @Autowired private JobQueueHealthCheck jobQueueHealthCheck;
  @Autowired private UnhealthyCountersHealthCheck unhealthyCountersHealthCheck;

  @PostConstruct
  public void registerHealthCheck() {
    register("jobQueueStatus", jobQueueHealthCheck);
    register("unhealthyCountersStatus", unhealthyCountersHealthCheck);
  }
}
