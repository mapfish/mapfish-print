package org.mapfish.print.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import com.codahale.metrics.health.HealthCheck;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.servlet.job.JobQueue;
import org.mapfish.print.servlet.job.impl.ThreadPoolJobManager;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

public class ApplicationStatusTest extends AbstractMapfishSpringTest {

  @Mock private JobQueue jobQueue;

  @Mock private ThreadPoolJobManager jobManager;

  @Autowired @InjectMocks private ApplicationStatus applicationStatus;

  @Before
  public void setUp() throws Exception {
    // Initialize mocks created above
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testCheck_Success_NoPrintJobs() throws Exception {
    when(jobQueue.getWaitingJobsCount()).thenReturn(0L);

    HealthCheck.Result result = applicationStatus.check();

    assertTrue(result.isHealthy());
    assertEquals("No print job is waiting in the queue.", result.getMessage());
  }

  @Test
  public void testCheck_Failed_NoPrintJobs() throws Exception {
    when(jobQueue.getWaitingJobsCount()).thenReturn(1L);
    when(jobManager.getLastExecutedJobTimestamp()).thenReturn(new Date(0L));

    try {
      applicationStatus.check();
      fail("Expected exception not thrown");
    } catch (RuntimeException e) {
      assertEquals(
          "No print job was processed by this server, in the last (seconds): 300", e.getMessage());
    } catch (Exception e) {
      fail("Incorrect Exception thrown: " + e);
    }
  }

  @Test
  public void testCheck_Success_PrintJobs() throws Exception {
    when(jobQueue.getWaitingJobsCount()).thenReturn(10L, 9L);
    when(jobManager.getLastExecutedJobTimestamp()).thenReturn(new Date());

    applicationStatus.check();
    HealthCheck.Result result = applicationStatus.check();

    assertTrue(result.isHealthy());
    assertTrue(result.getMessage().contains("Print jobs are being dequeued"));
  }

  @Test
  public void testCheck_Fail_PrintJobsButIncreasing() throws Exception {
    when(jobQueue.getWaitingJobsCount()).thenReturn(0L, 1L);
    when(jobManager.getLastExecutedJobTimestamp()).thenReturn(new Date());

    applicationStatus.check();
    HealthCheck.Result result = applicationStatus.check();

    assertFalse(result.isHealthy());
    assertTrue(result.getMessage().contains("Number of print jobs queued is increasing."));
  }
}
