package org.mapfish.print.servlet.job;

import org.mapfish.print.config.access.AccessAssertion;
import org.mapfish.print.wrapper.json.PJsonObject;

import java.util.Date;

/**
 * Print Job Entry.
 */
public interface PrintJobEntry {

    /**
     * Get reference ID.
     */
    String getReferenceId();

    /**
     * Get request data.
     */
    PJsonObject getRequestData();

    /**
     * Get start time (as long).
     */
    long getStartTime();

    /**
     * Get start time (as date).
     */
    Date getStartDate();

    /**
     * Get access assertion.
     */
    AccessAssertion getAccess();

    /**
     * Get app ID.
     */
    String getAppId();

    /**
     * Get time since start.
     */
    long getTimeSinceStart();

    /**
     * Assert that the current is authorized to access this job.
     */
    void assertAccess();

}
