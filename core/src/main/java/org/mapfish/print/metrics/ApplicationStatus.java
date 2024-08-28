package org.mapfish.print.metrics;

import com.codahale.metrics.health.HealthCheck;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.mapfish.print.servlet.job.JobQueue;
import org.mapfish.print.servlet.job.impl.ThreadPoolJobManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

class ApplicationStatus extends HealthCheck {
  @Value("${expectedMaxTime.sinceLastPrint.InSeconds}")
  private int secondsInFloatingWindow;

  @Autowired private JobQueue jobQueue;
  @Autowired private ThreadPoolJobManager jobManager;

  private long previousNumberOfWaitingJobs = 0L;

  @Override
  protected Result check() throws Exception {
    long waitingJobsCount = jobQueue.getWaitingJobsCount();
    if (waitingJobsCount == 0) {
      previousNumberOfWaitingJobs = waitingJobsCount;
      return Result.healthy("No print job is waiting in the queue.");
    }

    String health = "Number of print jobs waiting is " + waitingJobsCount;

    if (jobManager.getLastExecutedJobTimestamp() == null) {
      return Result.unhealthy("No print job was ever processed by this server. " + health);
    } else if (hasJobExecutedRecently()) {
      if (waitingJobsCount > previousNumberOfWaitingJobs) {
        previousNumberOfWaitingJobs = waitingJobsCount;
        return Result.unhealthy(
            "Number of print jobs queued is increasing. But this server is processing them. "
                + health);
      } else {
        previousNumberOfWaitingJobs = waitingJobsCount;
        return Result.healthy(
            "Print jobs are being dequeued. Number of print jobs waiting is " + waitingJobsCount);
      }
    } else {
      previousNumberOfWaitingJobs = waitingJobsCount;
      throw new RuntimeException(
          "No print job was processed by this server, in the last (seconds): "
              + secondsInFloatingWindow);
    }
  }

  private boolean hasJobExecutedRecently() {
    final Instant lastExecutedJobTime = jobManager.getLastExecutedJobTimestamp().toInstant();
    final Instant beginningOfTimeWindow = getBeginningOfTimeWindow();
    return lastExecutedJobTime.isAfter(beginningOfTimeWindow);
  }

  private Instant getBeginningOfTimeWindow() {
    return new Date().toInstant().minus(Duration.ofSeconds(secondsInFloatingWindow));
  }
}
