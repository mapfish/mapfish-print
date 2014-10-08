/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.servlet.job;

import com.google.common.base.Optional;
import org.json.JSONException;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.config.access.AccessAssertionPersister;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
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

    /**
     * The maximum number of threads that will be used for print jobs, this is not the number of threads
     * used by the system because there can be more used by the {@link org.mapfish.print.processor.ProcessorDependencyGraph}
     * when actually doing the printing.
     */
    private int maxNumberOfRunningPrintJobs = Runtime.getRuntime().availableProcessors();
    /**
     * The maximum number of print job requests that are waiting to be executed.
     * <p/>
     * This prevents spikes in requests from completely destroying the server.
     */
    private int maxNumberOfWaitingJobs = DEFAULT_MAX_WAITING_JOBS;
    /**
     * The amount of time to let a thread wait before being shutdown.
     */
    private long maxIdleTime = DEFAULT_THREAD_IDLE_TIME;
    /**
     * A print job is canceled, if it is not completed after this
     * amount of time (in seconds).
     */
    private long timeout = DEFAULT_TIMEOUT_IN_SECONDS;
    /**
     * A print job is canceled, if this amount of time (in seconds) has
     * passed, without that the user checked the status of the job.
     */
    private long abandonedTimeout = DEFAULT_ABANDONED_TIMEOUT_IN_SECONDS;
    /**
     * A comparator for comparing {@link org.mapfish.print.servlet.job.SubmittedPrintJob}s and
     * prioritizing them.
     * <p/>
     * For example it could be that requests from certain users (like executive officers) are prioritized over requests from
     * other users.
     */
    private Comparator<PrintJob> jobPriorityComparator = new Comparator<PrintJob>() {
        @Override
        public int compare(final PrintJob o1, final PrintJob o2) {
            return 0;
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
    private Timer timer;
    @Qualifier("accessAssertionPersister")
    @Autowired
    private AccessAssertionPersister assertionPersister;

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
                if (o1 instanceof PrintJob) {
                    if (o2 instanceof PrintJob) {
                        return ThreadPoolJobManager.this.jobPriorityComparator.compare((PrintJob) o1, (PrintJob) o2);
                    }
                    return 1;
                } else if (o2 instanceof PrintJob) {
                    return -1;
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
                this.maxIdleTime, TimeUnit.SECONDS, this.queue, threadFactory);

        this.timer = new Timer("Post result to registry", true);
        this.timer.schedule(new PostResultToRegistryTask(this.assertionPersister), PostResultToRegistryTask.CHECK_INTERVAL,
                PostResultToRegistryTask.CHECK_INTERVAL);
    }

    /**
     * Called by spring when application context is being destroyed.
     */
    @PreDestroy
    public final void shutdown() {
        this.timer.cancel();
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
        final Future<PrintJobStatus> future = this.executor.submit(job);
        try {
            final PendingPrintJob pendingPrintJob = new PendingPrintJob(job.getReferenceId(), job.getAppId(), job.getAccess());
            pendingPrintJob.store(this.registry, this.assertionPersister);
            this.registry.put(LAST_POLL + job.getReferenceId(), new Date().getTime());
        } catch (JSONException e) {
            throw ExceptionUtils.getRuntimeException(e);
        } finally {
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

        // even if the job is already finished, we store it as "canceled" in the registry,
        // so that all subsequent status requests return "canceled"
        final FailedPrintJob failedJob = new FailedPrintJob(
                referenceId, jobStatus.get().getAppId(), new Date(), "", "task canceled", jobStatus.get().getAccess());
        try {
            failedJob.store(this.registry, this.assertionPersister);
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
                return jobStatus;
            }
        } catch (JSONException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }

    /**
     * This timer task changes the status of finished jobs in the registry.
     * Also it stops jobs that have been running for too long (timeout).
     */
    private class PostResultToRegistryTask extends TimerTask {

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
                    // remove all canceled tasks from the work queue (otherwise the queue comparator
                    // might stumble on non-PrintJob entries)
                    ThreadPoolJobManager.this.executor.purge();
                }

                if (printJob.getReportFuture().isDone()) {
                    submittedJobs.remove();
                    final Registry registryRef = ThreadPoolJobManager.this.registry;
                    try {
                        printJob.getReportFuture().get().store(registryRef, this.assertionPersister);
                        registryRef.incrementInt(NB_PRINT_DONE, 1);
                        registryRef.incrementLong(TOTAL_PRINT_TIME, printJob.getTimeSinceStart());
                    } catch (InterruptedException e) {
                        // if this happens, the timer task was interrupted. restore the interrupted
                        // status to not lose the information.
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        // TODO check if in this case the job remains in the registry with status pending!
                        registryRef.incrementInt(LAST_PRINT_COUNT, 1);
                    } catch (JSONException e) {
                        registryRef.incrementInt(LAST_PRINT_COUNT, 1);
                    } catch (CancellationException e) {
                        try {
                            final FailedPrintJob failedJob = new FailedPrintJob(
                                    printJob.getReportRef(), printJob.getAppId(), new Date(), "", "task canceled (timeout)",
                                    printJob.getAccessAssertion());
                            failedJob.store(registryRef, this.assertionPersister);
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
}
