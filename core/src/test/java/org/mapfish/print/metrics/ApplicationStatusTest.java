package org.mapfish.print.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import java.util.Date;
import java.util.TreeSet;
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

  @Mock private MetricRegistry metricRegistry;

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

    RuntimeException rte =
        assertThrowsExactly(
            RuntimeException.class,
            () -> {
              // WHEN
              applicationStatus.check();
            });

    assertEquals(
        "None of the print job queued was processed by this server, in the last (seconds): 300",
        rte.getMessage());
  }

  @Test
  public void testCheck_Success_PrintJobs() throws Exception {
    when(jobQueue.getWaitingJobsCount()).thenReturn(5L, 4L);
    when(jobManager.getLastExecutedJobTimestamp()).thenReturn(new Date());

    applicationStatus.check();
    HealthCheck.Result result = applicationStatus.check();

    assertTrue(result.isHealthy());
    assertTrue(result.getMessage().contains("This server instance is printing."));
  }

  @Test
  public void testCheck_Fail_TooManyJobsAreQueued() throws Exception {
    when(jobQueue.getWaitingJobsCount()).thenReturn(4L, 5L);
    when(jobManager.getLastExecutedJobTimestamp()).thenReturn(new Date());

    applicationStatus.check();
    HealthCheck.Result result = applicationStatus.check();

    assertFalse(result.isHealthy());
    assertTrue(result.getMessage().contains("Number of print jobs queued is above threshold: "));
  }

  @Test
  public void testCheck_Success_WithEmptyCounters() throws Exception {
    when(jobQueue.getWaitingJobsCount()).thenReturn(0L);
    MetricRegistry metricRegistryImpl = new MetricRegistry();
    TreeSet<String> stringTreeSet = new TreeSet<>();
    when(metricRegistry.getNames()).thenReturn(stringTreeSet);
    registerCounter("Some counter", metricRegistryImpl, stringTreeSet);
    registerCounter("Some other counter", metricRegistryImpl, stringTreeSet);

    HealthCheck.Result result = applicationStatus.check();

    assertTrue(result.isHealthy());
    assertEquals("No print job is waiting in the queue.", result.getMessage());
  }

  private Counter registerCounter(
      final String counterName,
      final MetricRegistry metricRegistryImpl,
      final TreeSet<String> stringTreeSet) {
    Counter counter = metricRegistryImpl.counter(counterName);
    stringTreeSet.add(counterName);
    when(metricRegistry.counter(counterName)).thenReturn(counter);

    applicationStatus.recordUnhealthyCounter(counterName);
    return counter;
  }

  @Test
  public void testCheck_Success_WithCounters() throws Exception {
    when(jobQueue.getWaitingJobsCount()).thenReturn(0L);
    MetricRegistry metricRegistryImpl = new MetricRegistry();
    TreeSet<String> stringTreeSet = new TreeSet<>();
    when(metricRegistry.getNames()).thenReturn(stringTreeSet);
    Counter c1 = registerCounter("Some counter", metricRegistryImpl, stringTreeSet);
    c1.inc();
    Counter c2 = registerCounter("Some other counter", metricRegistryImpl, stringTreeSet);
    c2.dec();

    HealthCheck.Result result = applicationStatus.check();

    assertFalse(result.isHealthy());
    assertEquals(
        "No print job is waiting in the queue. But \n"
            + "Some other counter = -1\n"
            + "Some counter = 1",
        result.getMessage());
  }
}
