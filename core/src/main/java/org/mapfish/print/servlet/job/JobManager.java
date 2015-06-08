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

/**
 * Manages and Executes Print Jobs.
 *
 * @author jesseeichar on 3/17/14.
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

    /**
     * The job status.
     */
    public class JobStatus {
        private boolean isDone;
        private String error;
        private long elapsedTime;
        private Status status;

        /**
         * Constructor.
         * @param isDone Is done?
         * @param error Possible error message.
         * @param elapsedTime Elapsed time between job creation and end.
         * @param status The status.
         */
        public JobStatus(final boolean isDone, final String error, final long elapsedTime,
                final Status status) {
            this.isDone = isDone;
            this.error = error;
            this.elapsedTime = elapsedTime;
            this.status = status;
        }

        public final boolean isDone() {
            return this.isDone;
        }

        public final String getError() {
            return this.error;
        }

        public final long getElapsedTime() {
            return this.elapsedTime;
        }

        public final Status getStatus() {
            return this.status;
        }

        /**
         * The job status.
         */
        public enum Status {
            /**
             * The job hasn't yet started processing.
             */
            WAITING,
            /**
             * The job is currently being processed.
             */
            RUNNING,
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
        };
    }
}
