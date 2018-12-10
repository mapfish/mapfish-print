package org.mapfish.print.servlet.job;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.access.AlwaysAllowAssertion;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.servlet.ClusteredMapPrinterServletTest;
import org.mapfish.print.servlet.MapPrinterServlet;
import org.mapfish.print.servlet.job.impl.PrintJobEntryImpl;
import org.mapfish.print.servlet.job.impl.ThreadPoolJobManager;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@ContextConfiguration(locations = {
        ClusteredMapPrinterServletTest.CLUSTERED_CONTEXT
})
@Ignore //db must be set up to run this test
public class ClusteringTaskTest extends AbstractMapfishSpringTest {

    TestJobManager jobMan1;
    TestJobManager jobMan2;
    @Autowired
    private ApplicationContext context;

    @Before
    public void setup() {
        context.getBean(ThreadPoolJobManager.class).shutdown();
        jobMan1 = new TestJobManager("uno");
        jobMan2 = new TestJobManager("duo");
    }

    @Test(timeout = 60000)
    public void testRun() throws Exception {
        PJsonObject requestData =
                new PJsonObject(new JSONObject("{\"" + MapPrinterServlet.JSON_APP + "\":\"default\"}"),
                                "job");
        jobMan1.submit(new PrintJobEntryImpl("first job", requestData, System.currentTimeMillis(),
                                             new AlwaysAllowAssertion()));
        jobMan1.submit(new PrintJobEntryImpl("second job", requestData, System.currentTimeMillis(),
                                             new AlwaysAllowAssertion()));
        jobMan1.submit(new PrintJobEntryImpl("third job", requestData, System.currentTimeMillis(),
                                             new AlwaysAllowAssertion()));
        jobMan1.submit(new PrintJobEntryImpl("fourth job", requestData, System.currentTimeMillis(),
                                             new AlwaysAllowAssertion()));

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

        //verify each job was run only once
        assertEquals(4, jobMan1.getJobsRun() + jobMan2.getJobsRun());

        //verify each job manager ran some jobs
        assertTrue(jobMan1.getJobsRun() > 0);
        assertTrue(jobMan2.getJobsRun() > 0);

    }

    private class TestJobManager extends ThreadPoolJobManager {
        private String name;

        private int jobsRun;

        public TestJobManager(String name) {
            initForTesting(ClusteringTaskTest.this.context);
            setClustered(true);
            setMaxNumberOfRunningPrintJobs(1);
            this.name = name;
        }

        protected PrintJob createJob(final PrintJobEntry entry) {
            PrintJob job = new PrintJob() {
                @Override
                protected PrintResult withOpenOutputStream(PrintAction function) throws Exception {
                    System.out.println(getEntry().getReferenceId() + " is being run by jobman " + name);
                    TestJobManager.this.jobsRun++;
                    Thread.sleep(1000);
                    return new PrintResult(42, new AbstractProcessor.Context("test"));
                }

                @Override
                protected PrintJobResult createResult(
                        final String fileName, final String fileExtension, final String mimeType) {
                    return null;
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
