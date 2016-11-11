package org.mapfish.print.servlet.job;

import com.google.common.base.Optional;

/**
 * Manages and Executes Print Jobs.
 */
public interface JobManager {
    /**
     * Submit a new job for execution.
     *
     * @param job the job to run.
     */
    void submit(PrintJob job);

    /**
     * Get the number of prints that finished (either by error or success).
     */
    int getLastPrintCount();

    /**
     * Get the average time print jobs take to complete.
     */
    long getAverageTimeSpentPrinting();

    /**
     * Get the total number of print requests made.
     */
    int getNumberOfRequestsMade();

    /**
     * Get the time since a client has last requested the print job.
     *
     * @param referenceId the id of the printjob
     */
    long timeSinceLastStatusCheck(String referenceId);

    /**
     * Check if the job is done.
     *
     * @param referenceId the job to check.
     * @throws NoSuchReferenceException 
     */
    boolean isDone(String referenceId) throws NoSuchReferenceException;

    /**
     * Return the completed job object if the job has completed or absent otherwise.
     *
     * @param referenceId the referenceId of the report to lookup
     * @throws NoSuchReferenceException 
     */
    Optional<? extends PrintJobStatus> getCompletedPrintJob(String referenceId) throws NoSuchReferenceException;

    /**
     * Cancel a job.
     *
     * @param referenceId The referenceId of the job to cancel.
     * @throws NoSuchReferenceException 
     */
    void cancel(String referenceId) throws NoSuchReferenceException;

    /**
     * Get the status for a job.
     *
     * @param referenceId The referenceId of the job to check.
     * @throws NoSuchReferenceException
     */
    JobStatus getStatus(String referenceId) throws NoSuchReferenceException;
}
