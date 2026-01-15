package org.mapfish.print.servlet.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.Constants;
import org.mapfish.print.config.access.AlwaysAllowAssertion;
import org.mapfish.print.servlet.ClusteredMapPrinterServletTest;
import org.mapfish.print.servlet.MapPrinterServlet;
import org.mapfish.print.servlet.job.impl.ThreadPoolJobManager;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = {ClusteredMapPrinterServletTest.CLUSTERED_CONTEXT})
public class ClusteringTaskTest extends AbstractMapfishSpringTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusteringTaskTest.class);

  TestJobManager jobMan1;
  TestJobManager jobMan2;
  @Autowired private ApplicationContext context;

  @BeforeEach
  public void setup() {
    context.getBean(ThreadPoolJobManager.class).shutdown();
    jobMan1 = new TestJobManager("uno");
    jobMan2 = new TestJobManager("duo");
  }

  @Test
  @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
  public void testRun() throws Exception {
    LOGGER.error("Starting jobs");

    PJsonObject requestData =
        new PJsonObject(
            new JSONObject(
                "{"
                    // App
                    + "\""
                    + MapPrinterServlet.JSON_APP
                    + "\": \"default\", "
                    // Output format
                    + "\""
                    + MapPrinterServlet.JSON_OUTPUT_FORMAT
                    + "\": \"pdf\", "
                    // Layout
                    + "\""
                    + Constants.JSON_LAYOUT_KEY
                    + "\": \"A4 Landscape\""
                    + "}"),
            "job");
    jobMan1.submit(
        new PrintJobEntry(
            "first job", requestData, System.currentTimeMillis(), new AlwaysAllowAssertion()));
    jobMan1.submit(
        new PrintJobEntry(
            "second job", requestData, System.currentTimeMillis(), new AlwaysAllowAssertion()));
    jobMan1.submit(
        new PrintJobEntry(
            "third job", requestData, System.currentTimeMillis(), new AlwaysAllowAssertion()));
    jobMan1.submit(
        new PrintJobEntry(
            "fourth job", requestData, System.currentTimeMillis(), new AlwaysAllowAssertion()));

    int ready = 0;
    while (ready < 4) {
      if (jobMan2.getStatus("first job").isDone()) {
        ready++;
      }
      if (jobMan2.getStatus("second job").isDone()) {
        ready++;
      }
      if (jobMan2.getStatus("third job").isDone()) {
        ready++;
      }
      if (jobMan2.getStatus("fourth job").isDone()) {
        ready++;
      }
    }

    LOGGER.error("All jobs are done");

    // verify each job was run only once
    assertEquals(4, jobMan1.getJobsRun() + jobMan2.getJobsRun());

    // verify each job manager ran some jobs
    assertTrue(jobMan1.getJobsRun() > 0);
    assertTrue(jobMan2.getJobsRun() > 0);
  }

  private class TestJobManager extends ThreadPoolJobManager {
    private final String name;
    private int jobsRun;

    public TestJobManager(String name) {
      initForTesting(ClusteringTaskTest.this.context);
      setClustered(true);
      setMaxNumberOfRunningPrintJobs(1);
      this.name = name;
    }

    protected PrintJob createJob(final PrintJobEntry entry) {
      LOGGER.error("createJob on " + name);
      PrintJob job =
          new PrintJob() {
            @Override
            protected PrintJobResult createResult(
                String fileName, String fileExtension, String mimeType) {
              return null;
            }

            @Override
            public PrintJobResult call() throws Exception {
              LOGGER.error(getEntry().getReferenceId() + " is being run by jobman " + name);
              jobsRun++;
              return super.call();
            }
          };
      job.initForTesting(ClusteringTaskTest.this.context);
      job.setEntry(entry);
      job.setSecurityContext(SecurityContextHolder.getContext());
      return job;
    }

    public final int getJobsRun() {
      return jobsRun;
    }
  }
}
