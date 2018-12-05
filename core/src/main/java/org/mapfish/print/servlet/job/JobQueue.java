package org.mapfish.print.servlet.job;

import java.util.List;

/**
 * Manages the Statuses of the Print Jobs. Should not be used directly unless by Job Manager.
 */
public interface JobQueue {

    /**
     * Return the amount of time the queue will keep an entry before purging the record.
     *
     * @return the number of milliseconds between the last access of a record and the time when a record can
     *         be purged from the registry. -1 if there it is unlimited.
     */
    long getTimeToKeepAfterAccessInMillis();

    /**
     * Get the number of prints that finished (either by error or success).
     */
    long getLastPrintCount();

    /**
     * Get the average time print jobs take to complete.
     */
    long getAverageTimeSpentPrinting();

    /**
     * Get the total number of print requests made.
     */
    long getNumberOfRequestsMade();

    /**
     * Get the time since a client has last requested the print job.
     *
     * @param referenceId the id of the print job
     */
    long timeSinceLastStatusCheck(String referenceId);

    /**
     * Get the total number of waiting/running jobs.
     */
    long getWaitingJobsCount();

    /**
     * Return the completed job object if the job has completed or absent otherwise.
     *
     * @param referenceId the referenceId of the report to lookup
     * @param external true if external status request
     * @throws NoSuchReferenceException
     */
    PrintJobStatus get(String referenceId, boolean external) throws NoSuchReferenceException;

    /**
     * Add new job entry to the queue.
     *
     * @param jobEntry the job to run.
     */
    void add(PrintJobEntry jobEntry);

    /**
     * Mark job as canceling (if running) or cancelled (if waiting / finished).
     *
     * @param referenceId reference id to the job that has failed.
     * @param message the error message
     * @param forceFinal finalize, even if status is running
     * @throws NoSuchReferenceException
     */
    void cancel(String referenceId, String message, boolean forceFinal) throws NoSuchReferenceException;

    /**
     * Mark job as failed.
     *
     * @param referenceId reference id to the job that has failed.
     * @param message the error message
     * @throws NoSuchReferenceException
     */
    void fail(String referenceId, String message) throws NoSuchReferenceException;

    /**
     * Mark job as running.
     *
     * @param referenceId reference id to the job to start.
     * @throws NoSuchReferenceException
     */
    void start(String referenceId) throws NoSuchReferenceException;

    /**
     * Mark job as done.
     *
     * @param referenceId reference id to the job that is done.
     * @param result the result of the print job
     * @throws NoSuchReferenceException
     */
    void done(String referenceId, PrintJobResult result) throws NoSuchReferenceException;

    /**
     * Cancel old WAITING tasks.
     *
     * @param startTimeOut time-out value from when job started
     * @param abandonTimeout time-out value form last status request
     * @param message error message
     */
    void cancelOld(long startTimeOut, long abandonTimeout, String message);

    /**
     * Start the next [N] number of jobs at once.
     *
     * @param number the number of jobs to start
     * @return the jobs that were just started
     */
    List<? extends PrintJobStatus> start(int number);

    /**
     * Get the jobs that are marked as "CANCELING" and must be cancelled.
     */
    List<? extends PrintJobStatus> toCancel();

    /**
     * Delete the job.
     *
     * @param referenceId reference id to the job.
     */
    void delete(String referenceId);
}
