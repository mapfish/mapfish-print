package org.mapfish.print.servlet;

import org.mapfish.print.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Base class for MapPrinter servlets (deals with the configuration loading).
 */
public abstract class BaseMapServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseMapServlet.class);
    private int cacheDurationInSeconds = 3600;

    /**
     * Remove commas and whitespace from a string.
     *
     * @param original the starting string.
     */
    protected static String cleanUpName(final String original) {
        return original.replace(",", "").replaceAll("\\s+", "_");
    }

    /**
     * Update a variable name with a date if the variable is detected as being a date.
     *
     * @param variableName the variable name.
     * @param date the date to replace the value with if the variable is a date variable.
     */
    public static String findReplacement(final String variableName, final Date date) {
        if (variableName.equalsIgnoreCase("date")) {
            return cleanUpName(DateFormat.getDateInstance().format(date));
        } else if (variableName.equalsIgnoreCase("datetime")) {
            return cleanUpName(DateFormat.getDateTimeInstance().format(date));
        } else if (variableName.equalsIgnoreCase("time")) {
            return cleanUpName(DateFormat.getTimeInstance().format(date));
        } else {
            try {
                return new SimpleDateFormat(variableName).format(date);
            } catch (Exception e) {
                LOGGER.error("Unable to format timestamp according to pattern: {}", variableName, e);
                return "${" + variableName + "}";
            }
        }
    }

    /**
     * Send an error to the client with a message.
     *
     * @param httpServletResponse the response to send the error to.
     * @param message the message to send
     * @param code the error code
     */
    protected static void error(
            final HttpServletResponse httpServletResponse, final String message, final HttpStatus code) {
        try {
            httpServletResponse.setContentType("text/plain");
            httpServletResponse.setStatus(code.value());
            setNoCache(httpServletResponse);
            try (PrintWriter out = httpServletResponse.getWriter()) {
                out.println("Error while processing request:");
                out.println(message);
            }

            LOGGER.warn("Error while processing request: {}", message);
        } catch (IOException ex) {
            throw ExceptionUtils.getRuntimeException(ex);
        }
    }

    /**
     * Disable caching of the response.
     *
     * @param response the response
     */
    protected static void setNoCache(final HttpServletResponse response) {
        response.setHeader("Cache-Control", "max-age=0, must-revalidate, no-cache, no-store");
    }

    /**
     * Send an error to the client with an exception.
     *
     * @param httpServletResponse the http response to send the error to
     * @param e the error that occurred
     */
    protected final void error(final HttpServletResponse httpServletResponse, final Throwable e) {
        httpServletResponse.setContentType("text/plain");
        httpServletResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        try (PrintWriter out = httpServletResponse.getWriter()) {
            out.println("Error while processing request:");
            LOGGER.warn("Error while processing request", e);
        } catch (IOException ex) {
            throw ExceptionUtils.getRuntimeException(ex);
        }
    }

    /**
     * Returns the base URL of the print servlet.
     *
     * @param httpServletRequest the request
     */
    protected final StringBuilder getBaseUrl(final HttpServletRequest httpServletRequest) {
        StringBuilder baseURL = new StringBuilder();
        if (httpServletRequest.getContextPath() != null && !httpServletRequest.getContextPath().isEmpty()) {
            baseURL.append(httpServletRequest.getContextPath());
        }
        if (httpServletRequest.getServletPath() != null && !httpServletRequest.getServletPath().isEmpty()) {
            baseURL.append(httpServletRequest.getServletPath());
        }
        return baseURL;
    }

    /**
     * Set the cache duration for the queries that can be cached.
     *
     * @param cacheDurationInSeconds the duration
     */
    public final void setCacheDuration(final int cacheDurationInSeconds) {
        this.cacheDurationInSeconds = cacheDurationInSeconds;
    }

    /**
     * Enable caching of the response.
     *
     * @param response the response
     */
    protected void setCache(final HttpServletResponse response) {
        response.setHeader("Cache-Control", "max-age=" + this.cacheDurationInSeconds);
    }
}
