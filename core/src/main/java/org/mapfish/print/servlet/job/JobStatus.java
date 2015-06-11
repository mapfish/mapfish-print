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


/**
 * The job status.
 */
public class JobStatus {
    private boolean isDone;
    private String error;
    private long elapsedTime;
    private JobStatus.Status status;
    private long waitingTime;

    /**
     * Constructor.
     * @param isDone Is done?
     * @param error Possible error message.
     * @param elapsedTime Elapsed time between job creation and end.
     * @param status The status.
     * @param waitingTime A rough estimate for the time in ms the job still has to wait in the queue
     *      until it starts processing.
     */
    public JobStatus(final boolean isDone, final String error, final long elapsedTime,
            final JobStatus.Status status, final long waitingTime) {
        this.isDone = isDone;
        this.error = error;
        this.elapsedTime = elapsedTime;
        this.status = status;
        this.waitingTime = waitingTime;
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

    public final JobStatus.Status getStatus() {
        return this.status;
    }

    public final long getWaitingTime() {
        return this.waitingTime;
    }

    /**
     * The status type.
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