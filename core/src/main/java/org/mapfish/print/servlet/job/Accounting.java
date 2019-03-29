package org.mapfish.print.servlet.job;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.mapfish.print.StatsUtils;
import org.mapfish.print.config.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Do some accounting for jobs.
 */
public class Accounting {
    @Autowired
    private MetricRegistry metricRegistry;

    /**
     * Start accounting for a job.
     *
     * @param entry The job description
     * @param configuration The job configuration
     * @return A JobTracker
     */
    public JobTracker startJob(final PrintJobEntry entry, final Configuration configuration) {
        return new JobTracker(entry, configuration);
    }

    /**
     * Do accounting for a job.
     */
    public class JobTracker {
        /**
         * The job description.
         */
        protected final PrintJobEntry entry;

        /**
         * The job configuration.
         */
        protected final Configuration configuration;

        /**
         * The timer to use in cas of success.
         */
        protected final Timer.Context successTimer;

        /**
         * Constructor.
         *
         * @param entry the job description.
         * @param configuration the job configuration
         */
        protected JobTracker(final PrintJobEntry entry, final Configuration configuration) {
            this.entry = entry;
            this.configuration = configuration;
            this.successTimer = Accounting.this.metricRegistry.timer(getMetricName("success")).time();
        }

        /**
         * To be called when a job is a success.
         *
         * @param printResult Output file size in bytes
         * @return the job duration in nanoseconds
         */
        public long onJobSuccess(final PrintJob.PrintResult printResult) {
            return this.successTimer.stop();
        }

        /**
         * To be called when a job is cancelled.
         */
        public void onJobCancel() {
            Accounting.this.metricRegistry.counter(getMetricName("cancel")).inc();
        }

        /**
         * To be called when a job is on error.
         */
        public void onJobError() {
            Accounting.this.metricRegistry.counter(getMetricName("error")).inc();
        }

        private String getMetricName(final String kind) {
            return getClass().getName() + "." + StatsUtils.quotePart(this.entry.getAppId()) + "." + kind;
        }
    }
}
