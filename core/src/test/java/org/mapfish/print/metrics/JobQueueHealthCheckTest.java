package org.mapfish.print.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.codahale.metrics.health.HealthCheck;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.servlet.job.JobQueue;
import org.mapfish.print.servlet.job.impl.ThreadPoolJobManager;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

public class JobQueueHealthCheckTest extends AbstractMapfishSpringTest {

  @Mock private JobQueue jobQueue;

  @Mock private ThreadPoolJobManager jobManager;

  @Autowired @InjectMocks private JobQueueHealthCheck jobQueueHealthCheck;

  @BeforeEach
  public void setUp() {
    // Initialize mocks created above
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testCheck_Success_NoPrintJobs() {
    when(jobQueue.getWaitingJobsCount()).thenReturn(0L);

    HealthCheck.Result result = jobQueueHealthCheck.check();

    assertTrue(result.isHealthy());
    assertEquals("No print job is waiting in the queue.", result.getMessage());
  }

  @Test
  public void testCheck_Failed_NoPrintJobs() {
    when(jobQueue.getWaitingJobsCount()).thenReturn(1L);
    when(jobManager.getLastExecutedJobTimestamp()).thenReturn(new Date(0L));

    RuntimeException rte =
        assertThrowsExactly(
            RuntimeException.class,
            () ->
                // WHEN
                jobQueueHealthCheck.check());

    assertEquals(
        "None of the print job queued was processed by this server, in the last (seconds): 300",
        rte.getMessage());
  }

  @Test
  public void testCheck_Success_PrintJobs() {
    when(jobQueue.getWaitingJobsCount()).thenReturn(5L, 4L);
    when(jobManager.getLastExecutedJobTimestamp()).thenReturn(new Date());

    jobQueueHealthCheck.check();
    HealthCheck.Result result = jobQueueHealthCheck.check();

    assertTrue(result.isHealthy());
    assertTrue(result.getMessage().contains("This server instance is printing."));
  }

  @Test
  public void testCheck_Fail_TooManyJobsAreQueued() {
    when(jobQueue.getWaitingJobsCount()).thenReturn(4L, 5L);
    when(jobManager.getLastExecutedJobTimestamp()).thenReturn(new Date());

    jobQueueHealthCheck.check();
    HealthCheck.Result result = jobQueueHealthCheck.check();

    assertFalse(result.isHealthy());
    assertTrue(result.getMessage().contains("Number of print jobs queued is above threshold: "));
  }
}
