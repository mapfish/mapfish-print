package org.mapfish.print.servlet.job.impl.hibernate;

import org.mapfish.print.servlet.job.PrintJobEntry;
import org.mapfish.print.servlet.job.impl.PrintJobStatusImpl;

import javax.persistence.Column;
import javax.persistence.Entity;


/**
 *
 * Extension of PrintJob Status that holds last check time.
 *
 */
@Entity
public class PrintJobStatusExtImpl extends PrintJobStatusImpl {

    @Column
    private long lastCheckTime = System.currentTimeMillis();


    /**
     * Constructor.
     *
     */
    public PrintJobStatusExtImpl() {

    }

    /**
     * Constructor.
     *
     * @param entry the print job entry.
     * @param requestCount the request count
     */
    public PrintJobStatusExtImpl(final PrintJobEntry entry, final long requestCount) {
        super(entry, requestCount);
    }

    public final long getLastCheckTime() {
        return this.lastCheckTime;
    }

    public final void setLastCheckTime(final long lastCheckTime) {
        this.lastCheckTime = lastCheckTime;
    }

}
