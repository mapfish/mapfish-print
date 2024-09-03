package org.mapfish.print.servlet.job;

import java.util.Date;

/** Manages and Executes Print Jobs. */
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
   * @throws NoSuchReferenceException When trying to cancel an unknown referenceId
   */
  void cancel(String referenceId) throws NoSuchReferenceException;

  /**
   * Get the status for a job.
   *
   * @param referenceId The referenceId of the job to check.
   * @throws NoSuchReferenceException When requesting status of an unknown referenceId.
   */
  PrintJobStatus getStatus(String referenceId) throws NoSuchReferenceException;

  /**
   * Instant at which a job was executed by this manager.
   *
   * @return the timestamp as a Date.
   */
  Date getLastExecutedJobTimestamp();
}
