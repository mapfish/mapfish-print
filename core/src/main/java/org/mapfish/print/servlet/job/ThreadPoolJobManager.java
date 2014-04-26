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
import com.vividsolutions.jts.util.Assert;
import org.json.JSONException;
import org.mapfish.print.servlet.registry.Registry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
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

    private ExecutorService executor;

    private final Collection<SubmittedPrintJob> runningTasksFutures = new ArrayList<SubmittedPrintJob>();
    @Autowired
    private Registry registry;

    public final void setMaxNumberOfRunningPrintJobs(final int maxNumberOfRunningPrintJobs) {
        this.maxNumberOfRunningPrintJobs = maxNumberOfRunningPrintJobs;
    }

    public final void setMaxNumberOfWaitingJobs(final int maxNumberOfWaitingJobs) {
        this.maxNumberOfWaitingJobs = maxNumberOfWaitingJobs;
    }

    public final void setJobPriorityComparator(final Comparator<PrintJob> jobPriorityComparator) {
        this.jobPriorityComparator = jobPriorityComparator;
    }

    /**
     * Called by spring after constructing the java bean.
     */
    @PostConstruct
    public final void init() {
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory();
        threadFactory.setDaemon(true);
        threadFactory.setThreadNamePrefix("PrintJobManager-");

        BlockingQueue<Runnable> queue = new PriorityBlockingQueue<Runnable>(this.maxNumberOfWaitingJobs, new Comparator<Runnable>() {
            @Override
            public int compare(final Runnable o1, final Runnable o2) {
                Assert.isTrue(o1 instanceof PrintJob, o1 + " should be a " + PrintJob.class.getName());
                Assert.isTrue(o2 instanceof PrintJob, o2 + " should be a " + PrintJob.class.getName());
                return ThreadPoolJobManager.this.jobPriorityComparator.compare((PrintJob) o1, (PrintJob) o2);
            }
        });
        this.executor = new ThreadPoolExecutor(0, this.maxNumberOfRunningPrintJobs, this.maxIdleTime, TimeUnit.SECONDS, queue, threadFactory);
        this.executor.submit(new PostResultToRegistryTask());
    }

    /**
     * Called by spring when application context is being destroyed.
     */
    @PreDestroy
    public final void shutdown() {
        this.executor.shutdownNow();
    }

    @Override
    public final void submit(final PrintJob job) {
        this.registry.incrementInt(NEW_PRINT_COUNT, 1);
        final Future<CompletedPrintJob> future = this.executor.submit(job);
        this.runningTasksFutures.add(new SubmittedPrintJob(future, job.getReferenceId()));
    }

    @Override
    public final int getNumberOfRequestsMade() {
        return this.registry.getNumber(NEW_PRINT_COUNT).intValue();
    }

    @Override
    public final URI getURI(final String ref) {
        return this.registry.getURI(REPORT_URI_PREFIX + ref);
    }

    @Override
    public final boolean isDone(final String referenceId) {
        boolean done = this.registry.containsKey(REPORT_URI_PREFIX + referenceId);
        if (!done) {
            this.registry.put(LAST_POLL + referenceId, new Date().getTime());
        }
        return done;
    }

    @Override
    public final long timeSinceLastStatusCheck(final String referenceId) {
        return this.registry.getNumber(LAST_POLL + referenceId).longValue();
    }

    @Override
    public final long getAverageTimeSpentPrinting() {
        return this.registry.getNumber(TOTAL_PRINT_TIME).longValue() / this.registry.getNumber(NB_PRINT_DONE).longValue();
    }

    @Override
    public final int getLastPrintCount() {
        return this.registry.getNumber(LAST_PRINT_COUNT).intValue();
    }

    @Override
    public final Optional<? extends CompletedPrintJob> getCompletedPrintJob(final String referenceId) {
        try {
            return CompletedPrintJob.load(referenceId, this.registry);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private class PostResultToRegistryTask implements Callable<Void> {

        private static final int CHECK_INTERVAL = 500;

        @Override
        public Void call() throws Exception {
            while (true) {
                if (ThreadPoolJobManager.this.executor.isShutdown()) {
                    return null;
                }
                Iterator<SubmittedPrintJob> iterator = ThreadPoolJobManager.this.runningTasksFutures.iterator();
                while (iterator.hasNext()) {
                    SubmittedPrintJob next = iterator.next();
                    if (next.getReportFuture().isDone()) {
                        iterator.remove();
                        final Registry registryRef = ThreadPoolJobManager.this.registry;
                        try {
                            next.getReportFuture().get().store(registryRef);
                            registryRef.incrementInt(NB_PRINT_DONE, 1);
                            registryRef.incrementLong(TOTAL_PRINT_TIME, next.getTimeSinceStart());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            registryRef.incrementInt(LAST_PRINT_COUNT, 1);
                        }
                    }
                }

                try {
                    Thread.sleep(CHECK_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
    }
}
