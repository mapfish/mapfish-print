package org.mapfish.print.servlet.job.impl.hibernate;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.mapfish.print.metrics.UnhealthyCountersHealthCheck;
import org.mapfish.print.servlet.job.JobQueue;
import org.mapfish.print.servlet.job.NoSuchReferenceException;
import org.mapfish.print.servlet.job.PrintJobEntry;
import org.mapfish.print.servlet.job.PrintJobResult;
import org.mapfish.print.servlet.job.PrintJobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Transfers request to the Job dao. */
@Transactional
public class HibernateJobQueue implements JobQueue {
  private static final int DEFAULT_TIME_TO_KEEP_AFTER_ACCESS = 30; /* minutes */

  private static final long DEFAULT_CLEAN_UP_INTERVAL = 300; /* seconds */
  private static final TimeUnit CLEAN_UP_INTERVAL_TIME_UNIT = TimeUnit.SECONDS;

  private static final Logger LOGGER = LoggerFactory.getLogger(HibernateJobQueue.class);

  @Autowired private PrintJobDao dao;

  @Autowired private PlatformTransactionManager txManager;

  @Autowired private MetricRegistry metricRegistry;

  @Autowired private UnhealthyCountersHealthCheck unhealthyCountersHealthCheck;

  private ScheduledExecutorService cleanUpTimer;

  /** The interval at which old records are deleted (in seconds). */
  private final long cleanupInterval = DEFAULT_CLEAN_UP_INTERVAL;

  /** The minimum time to keep records after last access. */
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
    return this.dao.count(
        PrintJobStatus.Status.FINISHED,
        PrintJobStatus.Status.CANCELED,
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
    PrintJobStatusExtImpl statusRecord = this.dao.get(referenceId);
    if (statusRecord == null) {
      throw new NoSuchReferenceException(referenceId);
    }
    statusRecord.setStatusTime(now);
    if (!statusRecord.isDone() && external) {
      this.dao.updateLastCheckTime(referenceId, System.currentTimeMillis());
    }
    return statusRecord;
  }

  @Override
  public final synchronized void add(final PrintJobEntry jobEntry) {
    this.dao.save(new PrintJobStatusExtImpl(jobEntry, getNumberOfRequestsMade()));
  }

  @Override
  public final synchronized void cancel(
      final String referenceId, final String message, final boolean forceFinal)
      throws NoSuchReferenceException {
    PrintJobStatusExtImpl statusRecord = this.dao.get(referenceId, true);
    if (statusRecord == null) {
      throw new NoSuchReferenceException(referenceId);
    }
    if (!forceFinal && statusRecord.getStatus() == PrintJobStatus.Status.RUNNING) {
      statusRecord.setStatus(PrintJobStatus.Status.CANCELING);
    } else {
      statusRecord.setCompletionTime(System.currentTimeMillis());
      statusRecord.setStatus(PrintJobStatus.Status.CANCELED);
    }
    statusRecord.setError(message);
    this.dao.save(statusRecord);
  }

  @Override
  public final synchronized void fail(final String referenceId, final String message)
      throws NoSuchReferenceException {
    PrintJobStatusExtImpl statusRecord = this.dao.get(referenceId, true);
    if (statusRecord == null) {
      throw new NoSuchReferenceException(referenceId);
    }
    statusRecord.setCompletionTime(System.currentTimeMillis());
    statusRecord.setStatus(PrintJobStatus.Status.ERROR);
    statusRecord.setError(message);
    this.dao.save(statusRecord);
  }

  @Override
  public final synchronized void start(final String referenceId) throws NoSuchReferenceException {
    PrintJobStatusExtImpl statusRecord = this.dao.get(referenceId, true);
    if (statusRecord == null) {
      throw new NoSuchReferenceException(referenceId);
    }
    statusRecord.setStatus(PrintJobStatus.Status.RUNNING);
    statusRecord.setWaitingTime(0);
    this.dao.save(statusRecord);
  }

  @Override
  public final synchronized void done(final String referenceId, final PrintJobResult result)
      throws NoSuchReferenceException {
    PrintJobStatusExtImpl statusRecord = this.dao.get(referenceId, true);
    if (statusRecord == null) {
      throw new NoSuchReferenceException(referenceId);
    }
    statusRecord.setStatus(
        statusRecord.getStatus() == PrintJobStatus.Status.CANCELING
            ? PrintJobStatus.Status.CANCELED
            : PrintJobStatus.Status.FINISHED);
    statusRecord.setResult(result);
    statusRecord.setCompletionTime(System.currentTimeMillis());
    this.dao.save(statusRecord);
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
    for (PrintJobStatusExtImpl statusRecord : list) {
      statusRecord.setStatus(PrintJobStatus.Status.RUNNING);
      statusRecord.setWaitingTime(0);
      this.dao.save(statusRecord);
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

  /** Called by spring on initialization. */
  @PostConstruct
  public final void init() {
    this.cleanUpTimer =
        Executors.newScheduledThreadPool(
            1,
            timerTask -> {
              final Thread thread = new Thread(timerTask, "Clean up old job records");
              thread.setDaemon(true);
              return thread;
            });
    this.cleanUpTimer.scheduleAtFixedRate(
        this::cleanup, cleanupInterval, cleanupInterval, CLEAN_UP_INTERVAL_TIME_UNIT);
  }

  /** Called by spring when application context is being destroyed. */
  @PreDestroy
  public final void shutdown() {
    this.cleanUpTimer.shutdownNow();
  }

  private void cleanup() {
    TransactionTemplate tmpl = new TransactionTemplate(this.txManager);
    String name = MetricRegistry.name(getClass().getSimpleName(), "deletedOldPrintJobs");
    try (Timer.Context deletion = metricRegistry.timer(name).time()) {
      final Integer nbDeleted =
          tmpl.execute(
              status -> {
                try {
                  return HibernateJobQueue.this.dao.deleteOld(
                      System.currentTimeMillis() - getTimeToKeepAfterAccessInMillis());
                } catch (Exception e) {
                  unhealthyCountersHealthCheck.recordUnhealthyProblem(name, "issue");
                  return null;
                }
              });
      if (deletion.stop() > CLEAN_UP_INTERVAL_TIME_UNIT.toNanos(cleanupInterval)) {
        unhealthyCountersHealthCheck.recordUnhealthyProblem(name, "tooManyJobsToDeleteInInterval");
        LOGGER.warn("Deleting Old Print Jobs took longer than the scheduled interval.");
      }
      if (nbDeleted != null && nbDeleted > 0) {
        // This counter counts the number of deleted old job prints by this server instance.
        // But it is not the grand total of deleted jobs in the database.
        metricRegistry.counter(MetricRegistry.name(name, "totalCount")).inc(nbDeleted);
      }
    }
  }
}
