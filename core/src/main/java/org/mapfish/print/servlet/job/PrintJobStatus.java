package org.mapfish.print.servlet.job;

import org.mapfish.print.config.access.AccessAssertion;

import java.util.Date;

/**
 * Print Job Status.
 */
public interface PrintJobStatus {

    /**
     * Get the entry.
     */
    PrintJobEntry getEntry();

    /**
     * Get the reference ID.
     */
    String getReferenceId();

    /**
     * Get the app ID.
     */
    String getAppId();

    /**
     * Get the start time (as date).
     */
    Date getStartDate();

    /**
     * Get the start time (as long).
     */
    long getStartTime();

    /**
     * Get the access information.
     */
    AccessAssertion getAccess();

    /**
     * Get the completion time (as long).
     */
    Long getCompletionTime();

    /**
     * Get the completion time (as date).
     */
    Date getCompletionDate();

    /**
     * Get the request count.
     */
    long getRequestCount();

    /**
     * Get the error message.
     */
    String getError();

    /**
     * Get the status.
     */
    Status getStatus();

    /**
     * Get the result.
     */
    PrintJobResult getResult();

    /**
     * Get elapsed time.
     */
    long getElapsedTime();

    /**
     * is the job done?
     */
    boolean isDone();

    /**
     * Get the estimated waiting time for the job to finish.
     */
    long getWaitingTime();

    /**
     * Set the estimated waiting time for the job to finish (this is a transient value).
     *
     * @param waitingTime the waiting time
     */
    void setWaitingTime(long waitingTime);

    /**
     * The status type.
     */
    enum Status {
        /**
         * The job hasn't yet started processing.
         */
        WAITING,
        /**
         * The job is currently being processed.
         */
        RUNNING,
        /**
         * The job is still running, but needs to be cancelled.
         */
        CANCELING,
        /**
         * The job has finished processing.
         */
        FINISHED,
        /**
         * The job was cancelled.
         */
        CANCELLED,
        /**
         * There was an error executing the job.
         */
        ERROR
    }

}
