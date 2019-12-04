package org.mapfish.print.servlet.job.impl;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.config.WorkingDirectories;
import org.mapfish.print.servlet.job.JobManager;
import org.mapfish.print.servlet.job.JobQueue;
import org.mapfish.print.servlet.job.NoSuchReferenceException;
import org.mapfish.print.servlet.job.PrintJob;
import org.mapfish.print.servlet.job.PrintJobEntry;
import org.mapfish.print.servlet.job.PrintJobResult;
import org.mapfish.print.servlet.job.PrintJobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * A JobManager backed by a {@link java.util.concurrent.ThreadPoolExecutor}.
 */
public class ThreadPoolJobManager implements JobManager {
    /**
     * Default timeout for the duration of a print job.
     */
    public static final long DEFAULT_TIMEOUT_IN_SECONDS = 600L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPoolJobManager.class);
    private static final int DEFAULT_MAX_WAITING_JOBS = 5000;
    private static final long DEFAULT_THREAD_IDLE_TIME = 60L;
    private static final long DEFAULT_ABANDONED_TIMEOUT_IN_SECONDS = 120L;
    private static final boolean DEFAULT_OLD_FILES_CLEAN_UP = true;
    private static final long DEFAULT_CLEAN_UP_INTERVAL_IN_SECONDS = 86400;
    /**
     * A collection of jobs that are currently being processed or that are awaiting to be processed.
     */
    private final Map<String, SubmittedPrintJob> runningTasksFutures =
            Collections.synchronizedMap(new HashMap<>());
    /**
     * The maximum number of threads that will be used for print jobs, this is not the number of threads used
     * by the system because there can be more used by the
     * {@link org.mapfish.print.processor.ProcessorDependencyGraph}
     * when actually doing the printing.
     */
    private int maxNumberOfRunningPrintJobs = Runtime.getRuntime().availableProcessors();
    /**
     * The maximum number of print job requests that are waiting to be executed.
     *
     * This prevents spikes in requests from completely destroying the server.
     */
    private int maxNumberOfWaitingJobs = DEFAULT_MAX_WAITING_JOBS;
    /**
     * The amount of time to let a thread wait before being shutdown.
     */
    private long maxIdleTime = DEFAULT_THREAD_IDLE_TIME;
    /**
     * A print job is cancelled, if it is not completed after this amount of time (in seconds).
     */
    private long timeout = DEFAULT_TIMEOUT_IN_SECONDS;
    /**
     * A print job is cancelled, if this amount of time (in seconds) has passed, without that the user checked
     * the status of the job.
     */
    private long abandonedTimeout = DEFAULT_ABANDONED_TIMEOUT_IN_SECONDS;
    /**
     * Delete old report files?
     */
    private boolean oldFileCleanUp = DEFAULT_OLD_FILES_CLEAN_UP;
    /**
     * The interval at which old reports are deleted (in seconds).
     */
    private long oldFileCleanupInterval = DEFAULT_CLEAN_UP_INTERVAL_IN_SECONDS;
    /**
     * When this true, new jobs are not put automatically in the thread pool but only on the queue, which is
     * polled repeatedly for new jobs. This way other instances can take the jobs as well.
     */
    private boolean clustered = false;
    /**
     * A comparator for comparing {@link org.mapfish.print.servlet.job.impl.SubmittedPrintJob}s and
     * prioritizing them.
     *
     * For example it could be that requests from certain users (like executive officers) are prioritized over
     * requests from other users.
     */
    private Comparator<PrintJob> jobPriorityComparator =
            Comparator.comparingLong(o -> o.getEntry().getStartTime());
    private ThreadPoolExecutor executor;
    private ScheduledExecutorService timer;
    private ScheduledExecutorService cleanUpTimer;

    @Autowired
    private WorkingDirectories workingDirectories;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private JobQueue jobQueue;

    @Autowired
    private MetricRegistry metricRegistry;

    private boolean requestedToStop = false;

    public final void setMaxNumberOfRunningPrintJobs(final int maxNumberOfRunningPrintJobs) {
        this.maxNumberOfRunningPrintJobs = maxNumberOfRunningPrintJobs;

    }

    public final void setMaxNumberOfWaitingJobs(final int maxNumberOfWaitingJobs) {
        this.maxNumberOfWaitingJobs = maxNumberOfWaitingJobs;
    }

    public final void setTimeout(final long timeout) {
        this.timeout = timeout;
    }

    public final void setAbandonedTimeout(final long abandonedTimeout) {
        this.abandonedTimeout = abandonedTimeout;
    }

    public final void setJobPriorityComparator(final Comparator<PrintJob> jobPriorityComparator) {
        this.jobPriorityComparator = jobPriorityComparator;
    }

    public final void setOldFileCleanUp(final boolean oldFileCleanUp) {
        this.oldFileCleanUp = oldFileCleanUp;
    }

    public final void setOldFileCleanupInterval(final long oldFileCleanupInterval) {
        this.oldFileCleanupInterval = oldFileCleanupInterval;
    }

    public final void setClustered(final boolean clustered) {
        this.clustered = clustered;
    }

    /**
     * Initialize for testing.
     *
     * @param appContext application context
     */
    protected final void initForTesting(final ApplicationContext appContext) {
        this.context = appContext;
        this.workingDirectories = this.context.getBean(WorkingDirectories.class);
        this.jobQueue = this.context.getBean(JobQueue.class);
        init();
    }

    /**
     * Called by spring after constructing the java bean.
     */
    @PostConstruct
    public final void init() {
        long timeToKeepAfterAccessInMillis = this.jobQueue.getTimeToKeepAfterAccessInMillis();
        if (timeToKeepAfterAccessInMillis >= 0) {
            if (TimeUnit.SECONDS.toMillis(this.abandonedTimeout) >=
                    this.jobQueue.getTimeToKeepAfterAccessInMillis()) {
                final String msg =
                        String.format("%s abandonTimeout must be smaller than %s timeToKeepAfterAccess",
                                      getClass().getName(), this.jobQueue.getClass().getName());
                throw new IllegalStateException(msg);
            }
            if (TimeUnit.SECONDS.toMillis(this.timeout) >= this.jobQueue.getTimeToKeepAfterAccessInMillis()) {
                final String msg = String.format("%s timeout must be smaller than %s timeToKeepAfterAccess",
                                                 getClass().getName(), this.jobQueue.getClass().getName());
                throw new IllegalStateException(msg);
            }
        }
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory();
        threadFactory.setDaemon(true);
        threadFactory.setThreadNamePrefix("PrintJobManager-");

        PriorityBlockingQueue<Runnable> queue =
                new PriorityBlockingQueue<>(
                        this.maxNumberOfWaitingJobs,
                        (o1, o2) -> {
                            if (o1 instanceof JobFutureTask<?> &&
                                    o2 instanceof JobFutureTask<?>) {
                                Callable<?> callable1 = ((JobFutureTask<?>) o1).getCallable();
                                Callable<?> callable2 = ((JobFutureTask<?>) o2).getCallable();

                                if (callable1 instanceof PrintJob) {
                                    if (callable2 instanceof PrintJob) {
                                        return ThreadPoolJobManager.this.jobPriorityComparator
                                                .compare((PrintJob) callable1,
                                                         (PrintJob) callable2);
                                    }
                                    return 1;
                                } else if (callable2 instanceof PrintJob) {
                                    return -1;
                                }
                            }
                            return 0;
                        });
        /* The ThreadPoolExecutor uses a unbounded queue (though we are enforcing a limit in `submit()`).
         * Because of that, the executor creates only `corePoolSize` threads. But to use all threads,
         * we set both `corePoolSize` and `maximumPoolSize` to `maxNumberOfRunningPrintJobs`. As a
         * consequence, the `maxIdleTime` will be ignored, idle threads will not be terminated.
         */
        this.executor =
                new ThreadPoolExecutor(this.maxNumberOfRunningPrintJobs, this.maxNumberOfRunningPrintJobs,
                                       this.maxIdleTime, TimeUnit.SECONDS, queue, threadFactory) {
                    @Override
                    protected <T> RunnableFuture<T> newTaskFor(final Callable<T> callable) {
                        return new JobFutureTask<>(callable);
                    }

                    @Override
                    protected void beforeExecute(final Thread t, final Runnable runnable) {
                        if (!ThreadPoolJobManager.this.clustered && runnable instanceof JobFutureTask<?>) {
                            JobFutureTask<?> task = (JobFutureTask<?>) runnable;
                            if (task.getCallable() instanceof PrintJob) {
                                PrintJob printJob = (PrintJob) task.getCallable();
                                try {
                                    ThreadPoolJobManager.this.jobQueue
                                            .start(printJob.getEntry().getReferenceId());
                                } catch (RuntimeException e) {
                                    LOGGER.error("failed to mark job as running", e);
                                } catch (NoSuchReferenceException e) {
                                    LOGGER.error("tried to mark non-existing job as 'running': {}",
                                                 printJob.getEntry().getReferenceId(), e);
                                }
                            }
                        }
                        super.beforeExecute(t, runnable);
                    }
                };

        this.timer = Executors.newScheduledThreadPool(1, timerTask -> {
            final Thread thread = new Thread(timerTask, "Post result to registry");
            thread.setDaemon(true);
            return thread;
        });
        this.timer.scheduleAtFixedRate(new RegistryTask(), RegistryTask.CHECK_INTERVAL,
                                       RegistryTask.CHECK_INTERVAL, TimeUnit.MILLISECONDS);

        if (this.oldFileCleanUp) {
            this.cleanUpTimer = Executors.newScheduledThreadPool(1, timerTask -> {
                final Thread thread = new Thread(timerTask, "Clean up old files");
                thread.setDaemon(true);
                return thread;
            });
            this.cleanUpTimer.scheduleAtFixedRate(
                    this.workingDirectories.getCleanUpTask(), 0,
                    this.oldFileCleanupInterval, TimeUnit.SECONDS);
        }
    }

    /**
     * Called by spring when application context is being destroyed.
     */
    @PreDestroy
    public final void shutdown() {
        this.timer.shutdownNow();
        this.executor.shutdownNow();
        if (this.cleanUpTimer != null) {
            this.cleanUpTimer.shutdownNow();
        }
    }

    private void executeJob(final PrintJob job) {
        final Future<PrintJobResult> future = this.executor.submit(job);
        this.runningTasksFutures.put(job.getEntry().getReferenceId(),
                                     new SubmittedPrintJob(future, job.getEntry()));
    }

    /**
     * Create job from entry.
     *
     * @param entry the entry
     * @return the job
     */
    //CHECKSTYLE:OFF
    protected PrintJob createJob(final PrintJobEntry entry) {
        //CHECKSTYLE:ON
        PrintJob job = this.context.getBean(PrintJob.class);
        job.setEntry(entry);
        job.setSecurityContext(SecurityContextHolder.getContext());
        return job;
    }

    private void submitInternal(final PrintJobEntry jobEntry) {
        final long numberOfWaitingRequests = this.jobQueue.getWaitingJobsCount();
        if (numberOfWaitingRequests >= this.maxNumberOfWaitingJobs) {
            metricRegistry.counter(getClass().getName() + ".queue_overflow").inc();
            throw new RuntimeException(
                    "Max. number of waiting print job requests exceeded.  Number of waiting requests are: " +
                            numberOfWaitingRequests);
        }
        jobEntry.assertAccess();
        this.jobQueue.add(jobEntry);
        LOGGER.info("Submitted print job {}", jobEntry.getReferenceId());
    }

    /**
     * Submit with custom PrintJob (for testing).
     *
     * @param job the job
     */
    public final void submit(final PrintJob job) {
        try {
            submitInternal(job.getEntry());
        } finally {
            executeJob(job);
        }
    }

    @Override
    public final void submit(final PrintJobEntry entry) {
        try {
            submitInternal(entry);
        } finally {
            if (!this.clustered) {
                executeJob(createJob(entry));
            }
        }
    }

    private void cancelJobIfRunning(final String referenceId) throws NoSuchReferenceException {
        synchronized (this.runningTasksFutures) {
            if (this.runningTasksFutures.containsKey(referenceId)) {
                // the job is not yet finished (or has not even started), cancel
                final SubmittedPrintJob printJob = this.runningTasksFutures.get(referenceId);
                printJob.getEntry().assertAccess();
                if (!printJob.getReportFuture().cancel(true)) {
                    LOGGER.info("Could not cancel job {}", referenceId);
                }
                this.runningTasksFutures.remove(referenceId);
                //now from canceling to cancelled state
                this.jobQueue.cancel(referenceId, "task cancelled", true);
            }
        }
    }

    @Override
    public final void cancel(final String referenceId) throws NoSuchReferenceException {
        // check if the reference id is valid
        // and set canceling / cancelled status already
        this.jobQueue.cancel(referenceId, "task cancelled", false);
        cancelJobIfRunning(referenceId);
    }

    @Override
    public final PrintJobStatus getStatus(final String referenceId) throws NoSuchReferenceException {
        // check if the reference id is valid
        final PrintJobStatus jobStatus = this.jobQueue.get(referenceId, true);
        jobStatus.getEntry().assertAccess();

        if (jobStatus.getStatus() == PrintJobStatus.Status.WAITING) {
            // calculate an estimate for how long the job still has to wait
            // before it starts running
            long requestsMadeAtStart = jobStatus.getRequestCount();
            long finishedJobs = this.jobQueue.getLastPrintCount();
            long jobsRunningOrInQueue = requestsMadeAtStart - finishedJobs;
            long jobsInQueue = jobsRunningOrInQueue - this.maxNumberOfRunningPrintJobs;
            long queuePosition = jobsInQueue / this.maxNumberOfRunningPrintJobs;
            jobStatus.setWaitingTime(
                    Math.max(0L, queuePosition * this.jobQueue.getAverageTimeSpentPrinting()));
        }

        return jobStatus;
    }

    private void cancelOld() {
        //cancel old tasks
        this.jobQueue.cancelOld(TimeUnit.MILLISECONDS.convert(this.timeout, TimeUnit.SECONDS),
                                TimeUnit.MILLISECONDS.convert(this.abandonedTimeout, TimeUnit.SECONDS),
                                "task cancelled (timeout)");
    }

    private void pollRegistry() {
        //check if anything needs to be cancelled
        for (PrintJobStatus stat: this.jobQueue.toCancel()) {
            try {
                cancelJobIfRunning(stat.getReferenceId());
            } catch (NoSuchReferenceException e) {
                throw ExceptionUtils.getRuntimeException(e);
            }
        }
        //get new jobs to execute
        if (this.runningTasksFutures.size() < this.maxNumberOfRunningPrintJobs) {
            for (PrintJobStatus stat:
                    this.jobQueue.start(this.maxNumberOfRunningPrintJobs - this.runningTasksFutures.size())) {
                executeJob(createJob(stat.getEntry()));
            }
        }
    }

    private boolean updateRegistry() {
        boolean updated = false;
        final Iterator<SubmittedPrintJob> submittedJobs = this.runningTasksFutures.values().iterator();
        while (submittedJobs.hasNext()) {
            final SubmittedPrintJob printJob = submittedJobs.next();
            if (!printJob.getReportFuture().isDone() &&
                    (isTimeoutExceeded(printJob) || isAbandoned(printJob))) {
                LOGGER.info("Cancelling job after timeout {}", printJob.getEntry().getReferenceId());
                if (!printJob.getReportFuture().cancel(true)) {
                    LOGGER.info("Could not cancel job after timeout {}",
                                printJob.getEntry().getReferenceId());
                }
                // remove all cancelled tasks from the work queue (otherwise the queue comparator
                // might stumble on non-PrintJob entries)
                this.executor.purge();
            }

            if (printJob.getReportFuture().isDone()) {
                updated = true;
                submittedJobs.remove();
                try {
                    try {
                        // set the completion date to the moment the job was
                        // marked as completed
                        // in the registry.
                        final PrintJobResult result = printJob.getReportFuture().get();
                        if (result != null) {
                            this.jobQueue
                                    .done(printJob.getEntry().getReferenceId(), result);
                        } else {
                            // The report was sent to the user => don't need to keep it
                            this.jobQueue.delete(printJob.getEntry().getReferenceId());
                        }
                    } catch (InterruptedException e) {
                        // if this happens, the timer task was interrupted.
                        // restore the interrupted
                        // status to not lose the information.
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        //failure occurred
                        this.jobQueue.fail(printJob.getEntry().getReferenceId(),
                                           ExceptionUtils.getRootCause(e).toString());

                    } catch (CancellationException e) {
                        //cancellation occurred, set cancellation status
                        this.jobQueue.cancel(printJob.getEntry().getReferenceId(),
                                             "task cancelled (timeout)", true);
                    }
                    notifyIfStopped();
                } catch (NoSuchReferenceException e) { // shouldnt'// really happen
                    throw ExceptionUtils.getRuntimeException(e);
                }
            }
        }
        return updated;
    }

    private boolean isTimeoutExceeded(final SubmittedPrintJob printJob) {
        return printJob.getEntry().getTimeSinceStart() >
                TimeUnit.MILLISECONDS.convert(ThreadPoolJobManager.this.timeout, TimeUnit.SECONDS);
    }

    /**
     * If the status of a print job is not checked for a while, we assume that the user is no longer
     * interested in the report, and we cancel the job.
     *
     * @param printJob
     * @return is the abandoned timeout exceeded?
     */
    private boolean isAbandoned(final SubmittedPrintJob printJob) {
        final long duration = ThreadPoolJobManager.this.jobQueue.timeSinceLastStatusCheck(
                printJob.getEntry().getReferenceId());
        final boolean abandoned =
                duration > TimeUnit.SECONDS.toMillis(ThreadPoolJobManager.this.abandonedTimeout);
        if (abandoned) {
            LOGGER.info("Job {} is abandoned (no status check within the last {} seconds)",
                        printJob.getEntry().getReferenceId(), ThreadPoolJobManager.this.abandonedTimeout);
        }
        return abandoned;
    }

    /**
     * Check if the print has not been asked to stop taking new jobs.
     *
     * @return true if it's OK to take new jobs.
     */
    private boolean isAcceptingNewJobs() {
        if (this.requestedToStop) {
            return false;
        } else if (new File(this.workingDirectories.getWorking(), "stop").exists()) {
            LOGGER.info("The print has been requested to stop");
            this.requestedToStop = true;
            notifyIfStopped();
            return false;
        } else {
            return true;
        }
    }

    /**
     * Add a file to notify the script that asked to stop the print that it is now done processing the remain
     * jobs.
     */
    private void notifyIfStopped() {
        if (isAcceptingNewJobs() || !this.runningTasksFutures.isEmpty()) {
            return;
        }
        final File stoppedFile = new File(this.workingDirectories.getWorking(), "stopped");
        try {
            LOGGER.info("The print has finished processing jobs and can now stop");
            stoppedFile.createNewFile();
        } catch (IOException e) {
            LOGGER.warn("Cannot create the {} file", stoppedFile, e);
        }
    }

    /**
     * A custom FutureTask implementation which allows to retrieve the wrapped Callable.
     */
    private static final class JobFutureTask<V> extends FutureTask<V> {

        private final Callable<V> callable;

        JobFutureTask(final Callable<V> callable) {
            super(callable);
            this.callable = callable;
        }

        public Callable<V> getCallable() {
            return this.callable;
        }

    }

    /**
     * This timer task changes the status of finished jobs in the registry. Also it stops jobs that have been
     * running for too long (timeout).
     */
    @VisibleForTesting
    class RegistryTask implements Runnable {

        private static final int CHECK_INTERVAL = 500;  // ms
        private long cancelOldModulo = 120;  // once a minute
        private long pollModulo = 1;  // twice a second
        private long counter = 0;

        RegistryTask() {
            if (System.getenv("PRINT_CANCEL_OLD_POLL_INTERVAL") != null) {
                this.cancelOldModulo = Math.max(Math.round(Double.parseDouble(
                        System.getenv("PRINT_CANCEL_OLD_POLL_INTERVAL")) * 1000.0 / CHECK_INTERVAL), 1);
            }
            if (System.getenv("PRINT_POLL_INTERVAL") != null) {
                this.pollModulo = Math.max(Math.round(Double.parseDouble(
                        System.getenv("PRINT_POLL_INTERVAL")) * 1000.0 / CHECK_INTERVAL), 1);
            }
        }

        @Override
        public void run() {
            if (ThreadPoolJobManager.this.executor.isShutdown()) {
                return;
            }

            ++this.counter;

            // run in try-catch to ensure that the timer task is not stopped
            try {
                synchronized (ThreadPoolJobManager.this.runningTasksFutures) {
                    boolean updated = updateRegistry();
                    if (ThreadPoolJobManager.this.clustered) {
                        if (this.counter % this.cancelOldModulo == 0) {
                            cancelOld();
                        }
                        if ((updated || this.counter % this.pollModulo == 0) && isAcceptingNewJobs()) {
                            pollRegistry();
                        }
                    }
                }
            } catch (Throwable t) {
                LOGGER.error("Error while polling/updating registry", t);
            }
        }
    }

}
