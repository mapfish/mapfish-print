package org.mapfish.print.servlet.job;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.primitives.Longs;

import org.json.JSONException;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.config.WorkingDirectories;
import org.mapfish.print.config.access.AccessAssertionPersister;
import org.mapfish.print.servlet.job.JobStatus.Status;
import org.mapfish.print.servlet.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * A JobManager backed by a {@link java.util.concurrent.ThreadPoolExecutor}.
 *
 * @author jesseeichar on 3/18/14.
 */
public class ThreadPoolJobManager implements JobManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPoolJobManager.class);
    
    /**
     * The prefix for looking up the uri a completed report in the registry.
     */
    private static final String REPORT_URI_PREFIX = "REPORT_URI_";
    /**
     * Key for storing the number of print jobs currently running.
     */
    private static final String NEW_PRINT_COUNT = "newPrintCount";
    /**
     * The number of print requests made. ???
     */
    private static final String LAST_PRINT_COUNT = "lastPrintCount";

    /**
     * Total time spent printing.
     */
    private static final String TOTAL_PRINT_TIME = "totalPrintTime";
    /**
     * Number of print jobs done.
     */
    private static final String NB_PRINT_DONE = "nbPrintDone";
    /**
     * A registry tracking when the last time a metadata was check to see if it is done.
     */
    private static final String LAST_POLL = "lastPoll_";
    private static final int DEFAULT_MAX_WAITING_JOBS = 5000;
    private static final long DEFAULT_THREAD_IDLE_TIME = 60L;
    private static final long DEFAULT_TIMEOUT_IN_SECONDS = 600L;
    private static final long DEFAULT_ABANDONED_TIMEOUT_IN_SECONDS = 120L;
    private static final boolean DEFAULT_OLD_FILES_CLEAN_UP = true;
    private static final long DEFAULT_CLEAN_UP_INTERVAL_IN_SECONDS = 86400;

    /**
     * The maximum number of threads that will be used for print jobs, this is not the number of threads
     * used by the system because there can be more used by the {@link org.mapfish.print.processor.ProcessorDependencyGraph}
     * when actually doing the printing.
     */
    private int maxNumberOfRunningPrintJobs = Runtime.getRuntime().availableProcessors();
    /**
     * The maximum number of print job requests that are waiting to be executed.
     * <p></p>
     * This prevents spikes in requests from completely destroying the server.
     */
    private int maxNumberOfWaitingJobs = DEFAULT_MAX_WAITING_JOBS;
    /**
     * The amount of time to let a thread wait before being shutdown.
     */
    private long maxIdleTime = DEFAULT_THREAD_IDLE_TIME;
    /**
     * A print job is cancelled, if it is not completed after this
     * amount of time (in seconds).
     */
    private long timeout = DEFAULT_TIMEOUT_IN_SECONDS;
    /**
     * A print job is cancelled, if this amount of time (in seconds) has
     * passed, without that the user checked the status of the job.
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
     * A comparator for comparing {@link org.mapfish.print.servlet.job.SubmittedPrintJob}s and
     * prioritizing them.
     * <p></p>
     * For example it could be that requests from certain users (like executive officers) are prioritized over requests from
     * other users.
     */
    private Comparator<PrintJob> jobPriorityComparator = new Comparator<PrintJob>() {
        @Override
        public int compare(final PrintJob o1, final PrintJob o2) {
            return Longs.compare(o1.getCreateTime(), o2.getCreateTime());
        }
    };

    private ThreadPoolExecutor executor;

    /**
     * A collection of jobs that are currently being processed or that are awaiting
     * to be processed.
     */
    private final Map<String, SubmittedPrintJob> runningTasksFutures =
            Collections.synchronizedMap(new HashMap<String, SubmittedPrintJob>());
    @Autowired
    private Registry registry;

    private PriorityBlockingQueue<Runnable> queue;
    private ScheduledExecutorService timer;
    private ScheduledExecutorService cleanUpTimer;
    @Qualifier("accessAssertionPersister")
    @Autowired
    private AccessAssertionPersister assertionPersister;
    @Autowired
    private WorkingDirectories workingDirectories;

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

    /**
     * Called by spring after constructing the java bean.
     */
    @PostConstruct
    public final void init() {
        if (TimeUnit.SECONDS.toMillis(this.abandonedTimeout) >= this.registry.getTimeToKeepAfterAccessInMillis()) {
            final String msg = String.format("%s abandonTimeout must be smaller than %s timeToKeepAfterAccess",
                    getClass().getName(), this.registry.getClass().getName());
            throw new IllegalStateException(msg);
        }
        if (TimeUnit.SECONDS.toMillis(this.timeout) >= this.registry.getTimeToKeepAfterAccessInMillis()) {
            final String msg = String.format("%s timeout must be smaller than %s timeToKeepAfterAccess",
                    getClass().getName(), this.registry.getClass().getName());
            throw new IllegalStateException(msg);
        }
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory();
        threadFactory.setDaemon(true);
        threadFactory.setThreadNamePrefix("PrintJobManager-");

        this.queue = new PriorityBlockingQueue<Runnable>(this.maxNumberOfWaitingJobs, new Comparator<Runnable>() {
            @Override
            public int compare(final Runnable o1, final Runnable o2) {
                if (o1 instanceof JobFutureTask<?> && o2 instanceof JobFutureTask<?>) {
                    Callable<?> callable1 = ((JobFutureTask<?>) o1).getCallable();
                    Callable<?> callable2 = ((JobFutureTask<?>) o2).getCallable();

                    if (callable1 instanceof PrintJob) {
                        if (callable2 instanceof PrintJob) {
                            return ThreadPoolJobManager.this.jobPriorityComparator.compare((PrintJob) callable1, (PrintJob) callable2);
                        }
                        return 1;
                    } else if (callable2 instanceof PrintJob) {
                        return -1;
                    }
                }
                return 0;
            }
        });
        /* The ThreadPoolExecutor uses a unbounded queue (though we are enforcing a limit in `submit()`).
         * Because of that, the executor creates only `corePoolSize` threads. But to use all threads,
         * we set both `corePoolSize` and `maximumPoolSize` to `maxNumberOfRunningPrintJobs`. As a
         * consequence, the `maxIdleTime` will be ignored, idle threads will not be terminated.
         */
        this.executor = new ThreadPoolExecutor(this.maxNumberOfRunningPrintJobs, this.maxNumberOfRunningPrintJobs,
                this.maxIdleTime, TimeUnit.SECONDS, this.queue, threadFactory) {
            @Override
            protected <T> RunnableFuture<T> newTaskFor(final Callable<T> callable) {
                return new JobFutureTask<T>(callable);
            }
            @Override
            protected void beforeExecute(final Thread t, final Runnable runnable) {
                if (runnable instanceof JobFutureTask<?>) {
                    JobFutureTask<?> task = (JobFutureTask<?>) runnable;
                    if (task.getCallable() instanceof PrintJob) {
                        PrintJob printJob = (PrintJob) task.getCallable();
                        ThreadPoolJobManager.this.markAsRunning(printJob.getReferenceId());
                    }
                }
                super.beforeExecute(t, runnable);
            }
        };

        this.timer = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable timerTask) {
                final Thread thread = new Thread(timerTask, "Post result to registry");
                thread.setDaemon(true);
                return thread;
            }
        });
        this.timer.scheduleAtFixedRate(new PostResultToRegistryTask(this.assertionPersister), PostResultToRegistryTask.CHECK_INTERVAL,
                PostResultToRegistryTask.CHECK_INTERVAL, TimeUnit.MILLISECONDS);

        if (this.oldFileCleanUp) {
            this.cleanUpTimer = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                @Override
                public Thread newThread(final Runnable timerTask) {
                    final Thread thread = new Thread(timerTask, "Clean up old files");
                    thread.setDaemon(true);
                    return thread;
                }
            });
            this.cleanUpTimer.scheduleAtFixedRate(
                    this.workingDirectories.getCleanUpTask(), 0,
                    this.oldFileCleanupInterval, TimeUnit.SECONDS);
        }
    }

    private void markAsRunning(final String referenceId) {
        synchronized (this.registry) {
            try {
                Optional<? extends PrintJobStatus> jobStatus = PrintJobStatus.load(
                        referenceId, this.registry, this.assertionPersister);
                if (jobStatus.get() instanceof PendingPrintJob) {
                    PendingPrintJob job = (PendingPrintJob) jobStatus.get();
                    job.setRunning(true);
                    job.store(this.registry, this.assertionPersister);
                }
            } catch (JSONException e) {
                LOGGER.error("failed to mark job as running", e);
            } catch (NoSuchReferenceException e) {
                LOGGER.error("tried to mark non-existing job as 'running': " + referenceId, e);
            }
        }
    }

    /**
     * Called by spring when application context is being destroyed.
     */
    @PreDestroy
    public final void shutdown() {
        this.timer.shutdownNow();
        this.executor.shutdownNow();
    }

    @Override
    public final void submit(final PrintJob job) {
        final int numberOfWaitingRequests = this.queue.size();
        if (numberOfWaitingRequests >= this.maxNumberOfWaitingJobs) {
            throw new RuntimeException("Max. number of waiting print job requests exceeded.  Number of waiting requests are: " +
                                       numberOfWaitingRequests);
        }

        this.registry.incrementInt(NEW_PRINT_COUNT, 1);
        try {
            final Date startDate = job.getCreateTimeAsDate();
            final PendingPrintJob pendingPrintJob = new PendingPrintJob(
                    job.getReferenceId(), job.getAppId(), startDate, getNumberOfRequestsMade(), job.getAccess());
            pendingPrintJob.assertAccess();
            pendingPrintJob.store(this.registry, this.assertionPersister);
            this.registry.put(LAST_POLL + job.getReferenceId(), new Date().getTime());
            LOGGER.info("Submitted print job " + job.getReferenceId());
        } catch (JSONException e) {
            throw ExceptionUtils.getRuntimeException(e);
        } finally {
            final Future<PrintJobStatus> future = this.executor.submit(job);
            this.runningTasksFutures.put(job.getReferenceId(), new SubmittedPrintJob(future, job.getReferenceId(), job.getAppId(),
                    job.getAccess()));
        }
    }

    @Override
    public final int getNumberOfRequestsMade() {
        return this.registry.opt(NEW_PRINT_COUNT, 0);
    }

    @Override
    public final boolean isDone(final String referenceId) throws NoSuchReferenceException {
        boolean done = getCompletedPrintJob(referenceId).isPresent();
        if (!done) {
            this.registry.put(LAST_POLL + referenceId, new Date().getTime());
        }
        return done;
    }

    @Override
    public final void cancel(final String referenceId) throws NoSuchReferenceException {
        Optional<? extends PrintJobStatus> jobStatus = null;
        try {
            // check if the reference id is valid
            jobStatus = PrintJobStatus.load(referenceId, this.registry, this.assertionPersister);
        } catch (JSONException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
        synchronized (this.runningTasksFutures) {
            if (this.runningTasksFutures.containsKey(referenceId)) {
                // the job is not yet finished (or has not even started), cancel
                final SubmittedPrintJob printJob = this.runningTasksFutures.get(referenceId);
                if (!printJob.getReportFuture().cancel(true)) {
                    LOGGER.info("Could not cancel job " + referenceId);
                }
                this.runningTasksFutures.remove(referenceId);
                this.registry.incrementInt(NB_PRINT_DONE, 1);
                this.registry.incrementLong(TOTAL_PRINT_TIME, printJob.getTimeSinceStart());
            }
        }

        // even if the job is already finished, we store it as "cancelled" in the registry,
        // so that all subsequent status requests return "cancelled"
        final FailedPrintJob failedJob = new FailedPrintJob(
                referenceId, jobStatus.get().getAppId(), jobStatus.get().getStartDate(), new Date(),
                jobStatus.get().getRequestCount(), "", "task cancelled", true,
                jobStatus.get().getAccess());
        try {
            synchronized (this.registry) {
                failedJob.store(this.registry, this.assertionPersister);
            }
        } catch (JSONException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }

    @Override
    public final long timeSinceLastStatusCheck(final String referenceId) {
        return this.registry.opt(LAST_POLL + referenceId, System.currentTimeMillis());
    }

    @Override
    public final long getAverageTimeSpentPrinting() {
        return this.registry.opt(TOTAL_PRINT_TIME, 0L) / this.registry.opt(NB_PRINT_DONE, 1).longValue();
    }

    @Override
    public final int getLastPrintCount() {
        return this.registry.opt(LAST_PRINT_COUNT, 0);
    }

    @Override
    public final Optional<? extends PrintJobStatus> getCompletedPrintJob(final String referenceId)
            throws NoSuchReferenceException {
        try {
            Optional<? extends PrintJobStatus> jobStatus = PrintJobStatus.load(referenceId, this.registry, this.assertionPersister);
            if (jobStatus.get() instanceof PendingPrintJob) {
                // not yet completed
                return Optional.absent();
            } else {
                jobStatus.get().assertAccess();
                return jobStatus;
            }
        } catch (JSONException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }

    @Override
    public final JobStatus getStatus(final String referenceId) throws NoSuchReferenceException {
        PrintJobStatus jobStatus = null;
        try {
            // check if the reference id is valid
            jobStatus = PrintJobStatus.load(referenceId, this.registry, this.assertionPersister).get();
            jobStatus.assertAccess();
        } catch (JSONException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }

        boolean done = true;
        String error = "";
        long elapsedTime = jobStatus.getElapsedTime();
        long waitingTime = 0L;
        Status status = Status.FINISHED;

        if (jobStatus instanceof PendingPrintJob) {
            PendingPrintJob pendingJob = (PendingPrintJob) jobStatus;
            done = false;
            status = pendingJob.isRunning() ? Status.RUNNING : Status.WAITING;
        } else if (jobStatus instanceof FailedPrintJob) {
            FailedPrintJob failedJob = (FailedPrintJob) jobStatus;
            error = failedJob.getError();
            status = failedJob.getCancelled() ? Status.CANCELLED : Status.ERROR;
        }

        if (status == Status.WAITING) {
            // calculate an estimate for how long the job still has to wait
            // before it starts running
            long requestsMadeAtStart = jobStatus.getRequestCount();
            long finishedJobs = getLastPrintCount();
            long jobsRunningOrInQueue = requestsMadeAtStart - finishedJobs;
            long jobsInQueue = jobsRunningOrInQueue - this.maxNumberOfRunningPrintJobs;
            long queuePosition = jobsInQueue / this.maxNumberOfRunningPrintJobs;
            waitingTime = Math.max(0L, queuePosition * getAverageTimeSpentPrinting());
        }

        return new JobStatus(done, error, elapsedTime, status, waitingTime);
    }

    /**
     * This timer task changes the status of finished jobs in the registry.
     * Also it stops jobs that have been running for too long (timeout).
     */
    @VisibleForTesting
    class PostResultToRegistryTask implements Runnable {

        private static final int CHECK_INTERVAL = 500;
        private final AccessAssertionPersister assertionPersister;

        public PostResultToRegistryTask(final AccessAssertionPersister assertionPersister) {
            this.assertionPersister = assertionPersister;
        }

        @Override
        public void run() {
            if (ThreadPoolJobManager.this.executor.isShutdown()) {
                return;
            }

            // run in try-catch to ensure that the timer task is not stopped
            try {
                synchronized (ThreadPoolJobManager.this.runningTasksFutures) {
                    updateRegistry();
                }
            } catch (Throwable t) {
                LOGGER.error("Error while updating registry", t);
            }
        }

        private void updateRegistry() {
            final Iterator<SubmittedPrintJob> submittedJobs = ThreadPoolJobManager.this.runningTasksFutures.values().iterator();
            while (submittedJobs.hasNext()) {
                final SubmittedPrintJob printJob = submittedJobs.next(); 
                if (!printJob.getReportFuture().isDone() &&
                        (isTimeoutExceeded(printJob) || isAbandoned(printJob))) {
                    LOGGER.info("Cancelling job after timeout " + printJob.getReportRef());
                    if (!printJob.getReportFuture().cancel(true)) {
                        LOGGER.info("Could not cancel job after timeout " + printJob.getReportRef());
                    }
                    // remove all cancelled tasks from the work queue (otherwise the queue comparator
                    // might stumble on non-PrintJob entries)
                    ThreadPoolJobManager.this.executor.purge();
                }

                if (printJob.getReportFuture().isDone()) {
                    submittedJobs.remove();
                    final Registry registryRef = ThreadPoolJobManager.this.registry;
                    try {
                        // set the completion date to the moment the job was marked as completed
                        // in the registry.
                        synchronized (registryRef) {
                            printJob.getReportFuture().get().setCompletionDate(new Date());
                            printJob.getReportFuture().get().store(registryRef, this.assertionPersister);
                        }

                        registryRef.incrementInt(NB_PRINT_DONE, 1);
                        registryRef.incrementLong(TOTAL_PRINT_TIME, printJob.getTimeSinceStart());
                        registryRef.incrementInt(LAST_PRINT_COUNT, 1);
                    } catch (InterruptedException e) {
                        // if this happens, the timer task was interrupted. restore the interrupted
                        // status to not lose the information.
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        // TODO check if in this case the job remains in the registry with status pending!
                        LOGGER.debug("Error occurred while running PrintJob: " + e.getMessage(), e);
                        registryRef.incrementInt(LAST_PRINT_COUNT, 1);
                    } catch (JSONException e) {
                        registryRef.incrementInt(LAST_PRINT_COUNT, 1);
                    } catch (CancellationException e) {
                        try {
                            final FailedPrintJob failedJob = new FailedPrintJob(
                                    printJob.getReportRef(), printJob.getAppId(), printJob.getStartDate(), new Date(), 0L,
                                    "", "task cancelled (timeout)",
                                    true, printJob.getAccessAssertion());
                            synchronized (registryRef) {
                                failedJob.store(registryRef, this.assertionPersister);
                            }
                            registryRef.incrementInt(NB_PRINT_DONE, 1);
                            registryRef.incrementLong(TOTAL_PRINT_TIME, printJob.getTimeSinceStart());
                        } catch (JSONException e1) {
                            registryRef.incrementInt(LAST_PRINT_COUNT, 1);
                        }
                    }
                }
            }
        }

        private boolean isTimeoutExceeded(final SubmittedPrintJob printJob) {
            return printJob.getTimeSinceStart() > 
                TimeUnit.MILLISECONDS.convert(ThreadPoolJobManager.this.timeout, TimeUnit.SECONDS);
        }

        /**
         * If the status of a print job is not checked for a while, we assume that the user
         * is no longer interested in the report, and we cancel the job.
         * 
         * @param printJob
         * @return is the abandoned timeout exceeded?
         */
        private boolean isAbandoned(final SubmittedPrintJob printJob) {
            final long duration = new Date().getTime() - timeSinceLastStatusCheck(printJob.getReportRef());
            final boolean abandoned = duration > TimeUnit.SECONDS.toMillis(ThreadPoolJobManager.this.abandonedTimeout);
            if (abandoned) {
                LOGGER.info("Job " + printJob.getReportRef() + " is abandoned (no status check within the "
                        + "last " + ThreadPoolJobManager.this.abandonedTimeout + " seconds)");
            }
            return abandoned;
        }
    }

    /**
     * A custom FutureTask implementation which allows to retrieve the
     * wrapped Callable.
     */
    private static final class JobFutureTask<V> extends FutureTask<V> {

        private final Callable<V> callable;

        public JobFutureTask(final Callable<V> callable) {
            super(callable);
            this.callable = callable;
        }

        public Callable<V> getCallable() {
            return this.callable;
        }

    }
}
