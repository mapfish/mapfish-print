package org.mapfish.print.servlet;

/**
 * Provides information about the current servlet.
 */
public interface ServletInfo {
    /**
     * Return an id that identifies this server. This information is incorporated into the print request id so
     * that it is possible for load balancers to redirect a request to the same server without having to keep
     * sticky sessions.  Or to find the same server after a session has expired.
     *
     * This provides an option for simple clusters to be created without having to have a shared registry for
     * storing the look up when needing to retrieve the results of a print job.
     */
    String getServletId();
}
