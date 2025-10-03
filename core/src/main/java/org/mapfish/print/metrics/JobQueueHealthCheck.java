package org.mapfish.print.metrics;

import com.codahale.metrics.health.HealthCheck;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.mapfish.print.servlet.job.JobQueue;
import org.mapfish.print.servlet.job.impl.ThreadPoolJobManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class JobQueueHealthCheck extends HealthCheck {
  @Value("${healthStatus.expectedMaxTime.sinceLastPrint.InSeconds}")
  private int secondsInFloatingWindow;

  @Value("${healthStatus.unhealthyThreshold.maxNbrPrintJobQueued}")
  private int maxNbrPrintJobQueued;

  @Autowired private JobQueue jobQueue;
  @Autowired private ThreadPoolJobManager jobManager;

  /**
   * When a Result is returned it can be healthy or unhealthy. In both cases it is associated to a
   * Http 200 status. When an exception is thrown it means the server is no longer working at all,
   * it is associated to a Http 500 status.
   */
  @Override
  protected Result check() {
    long waitingJobsCount = jobQueue.getWaitingJobsCount();
    if (waitingJobsCount == 0) {
      return Result.healthy("No print job is waiting in the queue.");
    }

    String health = ". Number of print jobs waiting is " + waitingJobsCount;

    if (jobManager.getLastExecutedJobTimestamp() == null) {
      return Result.unhealthy("This server never processed a print job" + health);
    } else if (hasThisServerPrintedRecently()) {
      // WIP (See issue https://github.com/mapfish/mapfish-print/issues/3393)
      if (waitingJobsCount > maxNbrPrintJobQueued) {
        return Result.unhealthy(
            "WIP: Number of print jobs queued is above threshold: "
                + maxNbrPrintJobQueued
                + health);
      } else {
        return Result.healthy("This server instance is printing" + health);
      }
    } else {
      throw notificationForBrokenServer();
    }
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
}
