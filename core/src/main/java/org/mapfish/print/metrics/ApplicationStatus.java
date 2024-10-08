package org.mapfish.print.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.mapfish.print.servlet.job.JobQueue;
import org.mapfish.print.servlet.job.impl.ThreadPoolJobManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class ApplicationStatus extends HealthCheck {
  @Value("${healthStatus.expectedMaxTime.sinceLastPrint.InSeconds}")
  private int secondsInFloatingWindow;

  @Value("${healthStatus.unhealthyThreshold.maxNbrPrintJobQueued}")
  private int maxNbrPrintJobQueued;

  @Autowired private JobQueue jobQueue;
  @Autowired private ThreadPoolJobManager jobManager;
  @Autowired private MetricRegistry metricRegistry;

  private final Set<String> unhealthyCounters = new HashSet<>();

  /**
   * When a Result is returned it can be healthy or unhealthy. In both cases it is associated to a
   * Http 200 status. When an exception is thrown it means the server is no longer working at all,
   * it is associated to a Http 500 status.
   */
  @Override
  protected Result check() throws Exception {
    long waitingJobsCount = jobQueue.getWaitingJobsCount();
    if (waitingJobsCount == 0) {
      return createResult(Result.healthy("No print job is waiting in the queue."));
    }

    String health = ". Number of print jobs waiting is " + waitingJobsCount;

    if (jobManager.getLastExecutedJobTimestamp() == null) {
      return createResult(Result.unhealthy("This server never processed a print job" + health));
    } else if (hasThisServerPrintedRecently()) {
      // WIP (See issue https://github.com/mapfish/mapfish-print/issues/3393)
      if (waitingJobsCount > maxNbrPrintJobQueued) {
        return createResult(
            Result.unhealthy(
                "WIP: Number of print jobs queued is above threshold: "
                    + maxNbrPrintJobQueued
                    + health));
      } else {
        return createResult(Result.healthy("This server instance is printing" + health));
      }
    } else {
      throw notificationForBrokenServer();
    }
  }

  private Result createResult(final Result current) {
    StringBuilder messageBuilder = new StringBuilder(current.getMessage());
    boolean first = true;
    for (String unhealthyCounter : this.unhealthyCounters) {
      if (metricRegistry.getNames().contains(unhealthyCounter)) {
        Counter counter = metricRegistry.counter(unhealthyCounter);
        if (counter.getCount() != 0) {
          if (first) {
            first = false;
            if (current.isHealthy()) {
              messageBuilder.append(" But ");
            } else {
              messageBuilder.append(" And ");
            }
          }
          messageBuilder.append("\n");
          messageBuilder.append(unhealthyCounter);
          messageBuilder.append(" = ");
          messageBuilder.append(counter.getCount());
        }
      }
    }
    if (first) {
      return current;
    }
    return Result.unhealthy(messageBuilder.toString());
  }

  private RuntimeException notificationForBrokenServer() {
    return new RuntimeException(
        "None of the print job queued was processed by this server, in the last (seconds): "
            + secondsInFloatingWindow);
  }

  private boolean hasThisServerPrintedRecently() {
    final Instant lastExecutedJobTime = jobManager.getLastExecutedJobTimestamp().toInstant();
    final Instant beginningOfTimeWindow = getBeginningOfTimeWindow();
    return lastExecutedJobTime.isAfter(beginningOfTimeWindow);
  }

  private Instant getBeginningOfTimeWindow() {
    return new Date().toInstant().minus(Duration.ofSeconds(secondsInFloatingWindow));
  }

  /** To record a counter which might the server unhealthy if its count is not zero. */
  public void recordUnhealthyCounter(final String counterName) {
    unhealthyCounters.add(counterName);
  }
}
