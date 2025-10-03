package org.mapfish.print.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import java.util.TreeSet;
import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

public class UnhealthyCountersHealthCheckTest extends AbstractMapfishSpringTest {
  @Mock private MetricRegistry metricRegistry;

  @Autowired @InjectMocks private UnhealthyCountersHealthCheck unhealthyCountersHealthCheck;

  @Before
  public void setUp() {
    // Initialize mocks created above
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testCheck_Success_WithEmptyCounters() throws Exception {
    MetricRegistry metricRegistryImpl = new MetricRegistry();
    TreeSet<String> stringTreeSet = new TreeSet<>();
    when(metricRegistry.getNames()).thenReturn(stringTreeSet);
    registerCounter("Some counter", metricRegistryImpl, stringTreeSet);
    registerCounter("Some other counter", metricRegistryImpl, stringTreeSet);

    HealthCheck.Result result = unhealthyCountersHealthCheck.check();

    assertTrue(result.isHealthy());
    assertEquals("No unhealthy counter found.", result.getMessage());
  }

  private Counter registerCounter(
      final String counterName,
      final MetricRegistry metricRegistryImpl,
      final TreeSet<String> stringTreeSet) {
    Counter counter = metricRegistryImpl.counter(counterName);
    stringTreeSet.add(counterName);
    when(metricRegistry.counter(counterName)).thenReturn(counter);

    unhealthyCountersHealthCheck.recordUnhealthyCounter(counterName);
    return counter;
  }

  @Test
  public void testCheck_Success_NoCounters() throws Exception {
    HealthCheck.Result result = unhealthyCountersHealthCheck.check();

    assertTrue(result.isHealthy());
    assertEquals("No unhealthy counter found.", result.getMessage());
  }

  @Test
  public void testCheck_Fail_WithCounters() throws Exception {
    MetricRegistry metricRegistryImpl = new MetricRegistry();
    TreeSet<String> stringTreeSet = new TreeSet<>();
    when(metricRegistry.getNames()).thenReturn(stringTreeSet);
    String counterName1 = "Some unhealthy counter";
    Counter c1 = registerCounter(counterName1, metricRegistryImpl, stringTreeSet);
    c1.inc();
    String counterName2 = "Some other unhealthy counter";
    Counter c2 = registerCounter(counterName2, metricRegistryImpl, stringTreeSet);
    c2.dec();

    HealthCheck.Result result = unhealthyCountersHealthCheck.check();

    assertFalse(result.isHealthy());
    assertEquals(counterName1 + " = 1\n" + counterName2 + " = -1", result.getMessage());
  }
}
