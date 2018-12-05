package org.mapfish.print.servlet.job.impl.hibernate;

import com.codahale.metrics.MetricRegistry;
import org.mapfish.print.servlet.job.JobQueue;
import org.mapfish.print.servlet.job.NoSuchReferenceException;
import org.mapfish.print.servlet.job.PrintJobEntry;
import org.mapfish.print.servlet.job.PrintJobResult;
import org.mapfish.print.servlet.job.PrintJobStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Db Job Manager.
 */
@Transactional
public class HibernateJobQueue implements JobQueue {
    private static final int DEFAULT_TIME_TO_KEEP_AFTER_ACCESS = 30; /* minutes */

    private static final long DEFAULT_CLEAN_UP_INTERVAL = 300; /* seconds */

    @Autowired
    private PrintJobDao dao;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private MetricRegistry metricRegistry;


    private ScheduledExecutorService cleanUpTimer;

    /**
     * The interval at which old records are deleted (in seconds).
     */
    private long cleanupInterval = DEFAULT_CLEAN_UP_INTERVAL;

    /**
     * The minimum time to keep records after last access.
     */
    private int timeToKeepAfterAccessInMinutes = DEFAULT_TIME_TO_KEEP_AFTER_ACCESS;

    public final void setTimeToKeepAfterAccessInMinutes(final int timeToKeepAfterAccessInMinutes) {
        this.timeToKeepAfterAccessInMinutes = timeToKeepAfterAccessInMinutes;
    }

    @Override
    public final long getTimeToKeepAfterAccessInMillis() {
        return TimeUnit.MINUTES.toMillis(this.timeToKeepAfterAccessInMinutes);
    }

    @Override
    public final long getLastPrintCount() {
        return this.dao.count(PrintJobStatus.Status.FINISHED, PrintJobStatus.Status.CANCELLED,
                              PrintJobStatus.Status.ERROR);
    }

    @Override
    public final long getWaitingJobsCount() {
        return this.dao.count(PrintJobStatus.Status.WAITING, PrintJobStatus.Status.RUNNING);
    }

    @Override
    public final long getNumberOfRequestsMade() {
        return this.dao.count();
    }

    @Override
    public final long getAverageTimeSpentPrinting() {
        return this.dao.getTotalTimeSpentPrinting() / Math.max(1, getLastPrintCount());
    }

    @Override
    @Transactional(readOnly = true)
    public long timeSinceLastStatusCheck(final String referenceId) {
        final Number lastCheckTime = (Number) this.dao.getValue(referenceId, "lastCheckTime");
        if (lastCheckTime == null) {
            return 0;
        }
        return System.currentTimeMillis() - lastCheckTime.longValue();
    }

    @Override
    public final PrintJobStatus get(final String referenceId, final boolean external)
            throws NoSuchReferenceException {
        long now = System.currentTimeMillis();
        PrintJobStatusExtImpl record = this.dao.get(referenceId);
        if (record == null) {
            throw new NoSuchReferenceException(referenceId);
        }
        record.setStatusTime(now);
        if (!record.isDone() && external) {
            this.dao.updateLastCheckTime(referenceId, System.currentTimeMillis());
        }
        return record;
    }

    @Override
    public final synchronized void add(final PrintJobEntry jobEntry) {
        this.dao.save(new PrintJobStatusExtImpl(jobEntry, getNumberOfRequestsMade()));
    }

    @Override
    public final synchronized void cancel(
            final String referenceId, final String message, final boolean forceFinal)
            throws NoSuchReferenceException {
        PrintJobStatusExtImpl record = this.dao.get(referenceId, true);
        if (record == null) {
            throw new NoSuchReferenceException(referenceId);
        }
        if (!forceFinal && record.getStatus() == PrintJobStatus.Status.RUNNING) {
            record.setStatus(PrintJobStatus.Status.CANCELING);
        } else {
            record.setCompletionTime(System.currentTimeMillis());
            record.setStatus(PrintJobStatus.Status.CANCELLED);
        }
        record.setError(message);
        this.dao.save(record);
    }

    @Override
    public final synchronized void fail(final String referenceId, final String message)
            throws NoSuchReferenceException {
        PrintJobStatusExtImpl record = this.dao.get(referenceId, true);
        if (record == null) {
            throw new NoSuchReferenceException(referenceId);
        }
        record.setCompletionTime(System.currentTimeMillis());
        record.setStatus(PrintJobStatus.Status.ERROR);
        record.setError(message);
        this.dao.save(record);
    }

    @Override
    public final synchronized void start(final String referenceId) throws NoSuchReferenceException {
        PrintJobStatusExtImpl record = this.dao.get(referenceId, true);
        if (record == null) {
            throw new NoSuchReferenceException(referenceId);
        }
        record.setStatus(PrintJobStatus.Status.RUNNING);
        record.setWaitingTime(0);
        this.dao.save(record);
    }

    @Override
    public final synchronized void done(final String referenceId, final PrintJobResult result)
            throws NoSuchReferenceException {
        PrintJobStatusExtImpl record = this.dao.get(referenceId, true);
        if (record == null) {
            throw new NoSuchReferenceException(referenceId);
        }
        record.setStatus(
                record.getStatus() == PrintJobStatus.Status.CANCELING ? PrintJobStatus.Status.CANCELLED
                        : PrintJobStatus.Status.FINISHED);
        record.setResult(result);
        record.setCompletionTime(System.currentTimeMillis());
        this.dao.save(record);
    }

    @Override
    public final synchronized void cancelOld(
            final long startTimeOut, final long abandonTimeout, final String message) {
        long now = System.currentTimeMillis();
        this.dao.cancelOld(now - startTimeOut, now - abandonTimeout, message);
    }

    @Override
    public final synchronized List<? extends PrintJobStatus> start(final int number) {
        List<PrintJobStatusExtImpl> list = this.dao.poll(number);
        for (PrintJobStatusExtImpl record: list) {
            record.setStatus(PrintJobStatus.Status.RUNNING);
            record.setWaitingTime(0);
            this.dao.save(record);
        }
        return list;
    }

    @Override
    public final List<? extends PrintJobStatus> toCancel() {
        return this.dao.get(PrintJobStatus.Status.CANCELING);
    }

    @Override
    public void delete(final String referenceId) {
        this.dao.delete(referenceId);
    }

    /**
     * Called by spring on initialization.
     */
    @PostConstruct
    public final void init() {
        this.cleanUpTimer = Executors.newScheduledThreadPool(1, timerTask -> {
            final Thread thread = new Thread(timerTask, "Clean up old job records");
            thread.setDaemon(true);
            return thread;
        });
        this.cleanUpTimer.scheduleAtFixedRate(this::cleanup, this.cleanupInterval, this.cleanupInterval,
                                              TimeUnit.SECONDS);
    }

    /**
     * Called by spring when application context is being destroyed.
     */
    @PreDestroy
    public final void shutdown() {
        this.cleanUpTimer.shutdownNow();
    }

    private void cleanup() {
        TransactionTemplate tmpl = new TransactionTemplate(this.txManager);
        final int nbDeleted = tmpl.execute(
                status -> HibernateJobQueue.this.dao
                        .deleteOld(System.currentTimeMillis() - getTimeToKeepAfterAccessInMillis()));
        metricRegistry.counter(getClass().getName() + ".deleted_old").inc(nbDeleted);
    }
}
