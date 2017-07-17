package org.mapfish.print.servlet.job.impl;

import org.hibernate.annotations.Target;
import org.mapfish.print.config.access.AccessAssertion;
import org.mapfish.print.servlet.job.PrintJobEntry;
import org.mapfish.print.servlet.job.PrintJobResult;
import org.mapfish.print.servlet.job.PrintJobStatus;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Represent a print job that has completed.  Contains the information about the print job.
 */
@Entity
@Table(name = "print_job_statuses")
public class PrintJobStatusImpl implements PrintJobStatus {

    private static final int LENGTH_ERROR = 1024;

    @Id
    private String referenceId;

    @Embedded
    @Target(PrintJobEntryImpl.class)
    private final PrintJobEntry entry;

    @Column
    @Enumerated(EnumType.STRING)
    private PrintJobStatus.Status status = PrintJobStatus.Status.WAITING;

    @Column
    private Long completionTime;

    @Column
    private long requestCount;

    @Column(length = LENGTH_ERROR)
    private String error;

    @OneToOne(targetEntity = PrintJobResultImpl.class, cascade = CascadeType.ALL, mappedBy = "status")
    @JoinColumn(name = "reference_id")
    private PrintJobResult result;

    private transient long waitingTime;

    private transient Long statusTime;

    /**
     * Constructor.
     *
     */
    public PrintJobStatusImpl() {
        this.entry = null;
    }

    /**
     * Constructor.
     *
     * @param entry the PrintJobEntry.
     * @param requestCount request count
     */
    public PrintJobStatusImpl(final PrintJobEntry entry, final long requestCount) {
        this.referenceId = entry.getReferenceId();
        this.entry = entry;
        this.requestCount = requestCount;
    }

    @Override
    public final PrintJobEntry getEntry() {
        return this.entry;
    }

    public final void setCompletionTime(final Long completionTime) {
        this.completionTime = completionTime;
    }

    @Override
    public final Long getCompletionTime() {
        return this.completionTime;
    }

    @Override
    public final long getRequestCount() {
        return this.requestCount;
    }

    @Override
    public final String getError() {
        return this.error;
    }

    @Override
    public final PrintJobStatus.Status getStatus() {
        return this.status;
    }

    public final void setStatus(final PrintJobStatus.Status status) {
        this.status = status;
    }

    public final void setRequestCount(final long requestCount) {
        this.requestCount = requestCount;
    }

    public final void setError(final String error) {
        this.error = error;
    }

    @Override
    public final PrintJobResult getResult() {
        return this.result;
    }

    /**
     * Set the result.
     * @param result The result
     */
    public final void setResult(final PrintJobResult result) {
        this.result = result;
    }

    @Override
    public final String getReferenceId() {
        return this.referenceId;
    }

    @Override
    public final long getStartTime() {
        return getEntry().getStartTime();
    }

    @Override
    public final AccessAssertion getAccess() {
        return getEntry().getAccess();
    }

    @Override
    public final String getAppId() {
         return getEntry().getAppId();
    }

    @Override
    public final Date getStartDate() {
        return getEntry().getStartDate();
    }

    @Override
    public final Date getCompletionDate() {
        return getCompletionTime() == null ? null : new Date(getCompletionTime());
    }

    @Override
    public final long getElapsedTime() {
        if (this.completionTime != null) {
            return this.completionTime - getEntry().getStartTime();
        } else if (this.statusTime != null) {
            return this.statusTime - getEntry().getStartTime();
        } else {
            return System.currentTimeMillis() - getEntry().getStartTime();
        }
    }

    @Override
    public final boolean isDone() {
        return getStatus() != PrintJobStatus.Status.RUNNING && getStatus() != PrintJobStatus.Status.WAITING;
    }

    @Override
    public final long getWaitingTime() {
        return this.waitingTime;
    }

    @Override
    public final void setWaitingTime(final long waitingTime) {
        this.waitingTime = waitingTime;
    }

    public final Long getStatusTime() {
        return this.statusTime;
    }

    public final void setStatusTime(final Long statusTime) {
        this.statusTime = statusTime;
    }

}
