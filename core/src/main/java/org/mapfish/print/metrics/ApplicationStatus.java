package org.mapfish.print.metrics;

import com.codahale.metrics.health.HealthCheck;
import org.mapfish.print.servlet.job.JobQueue;
import org.springframework.beans.factory.annotation.Autowired;

class ApplicationStatus extends HealthCheck {
  public static final int MAX_WAITING_JOBS_TILL_UNHEALTHY = 5;
  @Autowired private JobQueue jobQueue;

  @Override
  protected Result check() throws Exception {
    long waitingJobsCount = jobQueue.getWaitingJobsCount();
    String health = "Number of jobs waiting is " + waitingJobsCount;

    if (waitingJobsCount >= MAX_WAITING_JOBS_TILL_UNHEALTHY) {
      return Result.unhealthy(health + ". It is too high.");
    } else {
      return Result.healthy(health);
    }
  }
}
