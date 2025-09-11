package org.mapfish.print.metrics;

import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

public class HealthCheckingRegistry extends com.codahale.metrics.health.HealthCheckRegistry {
  @Autowired private JobQueueHealthCheck jobQueueHealthCheck;
  @Autowired private UnhealthyCountersHealthCheck unhealthyCountersHealthCheck;
  private final ThreadDeadlockHealthCheck threadDeadlockHealthCheck =
      new ThreadDeadlockHealthCheck();

  @PostConstruct
  public void registerHealthCheck() {
    register("jobQueueStatus", jobQueueHealthCheck);
    register("unhealthyCountersStatus", unhealthyCountersHealthCheck);
    register("threadDeadlock", threadDeadlockHealthCheck);
  }
}
