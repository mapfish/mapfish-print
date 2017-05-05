package org.mapfish.print.servlet.job;

/**
 * Manages and Executes Print Jobs.
 */
public interface JobManager {

    /**
     * Submit a new job for execution.
     *
     * @param entry the job to run.
     */
    void submit(PrintJobEntry entry);

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
    PrintJobStatus getStatus(String referenceId) throws NoSuchReferenceException;

}
