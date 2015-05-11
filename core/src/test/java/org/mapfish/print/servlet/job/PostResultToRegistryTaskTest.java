package org.mapfish.print.servlet.job;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.servlet.MapPrinterServlet;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Jesse on 5/11/2015.
 */
public class PostResultToRegistryTaskTest extends AbstractMapfishSpringTest {

    @Autowired
    ThreadPoolJobManager jobManager;
    @Autowired
    ApplicationContext context;

    @Test
    public void testRun() throws Exception {

        assertRegistryValues(null, 0, 0, true);

        TestPrintJob printJob = new TestPrintJob();
        jobManager.submit(printJob);

        assertRegistryValues(printJob, 1, 1, false);

        printJob = new FailingPrintJob();
        jobManager.submit(printJob);

        assertRegistryValues(printJob, 2, 2, false);

        final AtomicBoolean finishFlag = new AtomicBoolean(false);

        printJob = new TestPrintJob() {
            @Override
            protected URI withOpenOutputStream(PrintAction function) throws Throwable {
                while (!finishFlag.get()) {
                    Thread.sleep(100);
                }
                return super.withOpenOutputStream(function);
            }
        };
        jobManager.submit(printJob);

        assertRegistryValues(printJob, 2, 3, false);

        finishFlag.set(true);

        assertRegistryValues(printJob, 3, 3, false);
    }

    private void assertRegistryValues(TestPrintJob printJob, int expectedLastPrintCount, int expectedRequestsMade, boolean
            timeSpentIsZero) {

        long start = System.currentTimeMillis();
        AssertionError error = null;
        while ((System.currentTimeMillis() - start) > 2000) {
            try {
                int lastPrintCount = jobManager.getLastPrintCount();
                int numberOfRequestsMade = jobManager.getNumberOfRequestsMade();
                long timeSpentPrinting = jobManager.getAverageTimeSpentPrinting();

                assertEquals(expectedLastPrintCount, lastPrintCount);
                assertEquals(expectedRequestsMade, numberOfRequestsMade);

                if (timeSpentIsZero) {
                    assertTrue(0 == timeSpentPrinting);
                } else {
                    assertTrue(0 < timeSpentPrinting);
                }
            } catch (AssertionError e) {
                error = e;
            }
        }

        if (error != null) {
            throw error;
        }
    }

    private class TestPrintJob extends PrintJob {
        {
            try {
                initForTesting(context);
                setRequestData(new PJsonObject(new JSONObject("{\"" + MapPrinterServlet.JSON_APP + "\":\"default\"}"), "job"));
                setReferenceId("abc");
                Template template = new Template();
                Configuration configuration = new Configuration();
                template.setConfiguration(configuration);
                configureAccess(template);
                setSecurityContext(SecurityContextHolder.createEmptyContext());
            } catch (JSONException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        @Override
        protected URI withOpenOutputStream(PrintAction function) throws Throwable {
            return new URI("file://123.com");
        }
    }

    private class FailingPrintJob extends TestPrintJob {
        @Override
        protected URI withOpenOutputStream(PrintAction function) throws Throwable {
            throw new RuntimeException("failure");
        }
    }
}