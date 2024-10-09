package org.mapfish.print.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;

public class UnhealthyCountersHealthCheck extends HealthCheck {
  @Autowired private MetricRegistry metricRegistry;

  private final Set<String> unhealthyCounters = new HashSet<>();

  /**
   * When a Result is returned it can be healthy or unhealthy. In both cases it is associated to a
   * Http 200 status. When an exception is thrown it means the server is no longer working at all,
   * it is associated to a Http 500 status.
   */
  @Override
  protected HealthCheck.Result check() throws Exception {
    StringBuilder messageBuilder = new StringBuilder();
    boolean first = true;
    for (String unhealthyCounter : this.unhealthyCounters) {
      if (metricRegistry.getNames().contains(unhealthyCounter)) {
        Counter counter = metricRegistry.counter(unhealthyCounter);
        if (counter.getCount() != 0) {
          if (first) {
            first = false;
          } else {
            messageBuilder.append("\n");
          }
          messageBuilder.append(unhealthyCounter);
          messageBuilder.append(" = ");
          messageBuilder.append(counter.getCount());
        }
      }
    }
    if (first) {
      return Result.healthy("No unhealthy counter found.");
    }
    return Result.unhealthy(messageBuilder.toString());
  }

  /** To record a counter which might make the server unhealthy if its count is not zero. */
  public void recordUnhealthyCounter(final String counterName) {
    unhealthyCounters.add(counterName);
  }
}
