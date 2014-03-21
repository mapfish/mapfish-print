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

package org.mapfish.print.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Base class for MapPrinter servlets (deals with the configuration loading).
 */
public abstract class BaseMapServlet extends HttpServlet {
    private static final long serialVersionUID = -6342262849725708850L;

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseMapServlet.class);

    /**
     * Get the base url of the webapp.
     *
     * @param httpServletRequest the http request object.
     */
    protected final String getBaseUrl(final HttpServletRequest httpServletRequest) {
        final String additionalPath = httpServletRequest.getPathInfo();
        String fullUrl = httpServletRequest.getParameter("url");
        if (fullUrl != null) {
            return fullUrl.replaceFirst(additionalPath + "$", "");
        } else {
            return httpServletRequest.getRequestURL().toString()
                    .replaceFirst(additionalPath + "$", "");
        }
    }

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
        if (variableName.toLowerCase().equals("date")) {
            return cleanUpName(DateFormat.getDateInstance().format(date));
        } else if (variableName.toLowerCase().equals("datetime")) {
            return cleanUpName(DateFormat.getDateTimeInstance().format(date));
        } else if (variableName.toLowerCase().equals("time")) {
            return cleanUpName(DateFormat.getTimeInstance().format(date));
        } else {
            try {
                return new SimpleDateFormat(variableName).format(date);
            } catch (Exception e) {
                LOGGER.error("Unable to format timestamp according to pattern: " + variableName, e);
                return "${" + variableName + "}";
            }
        }
    }

    /**
     * Send an error to the client with an exception.
     *
     * @param httpServletResponse the http response to send the rror to
     * @param e the error that occurred
     */
    protected final void error(final HttpServletResponse httpServletResponse, final Throwable e) {
        PrintWriter out = null;
        try {
            httpServletResponse.setContentType("text/plain");
            httpServletResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            out = httpServletResponse.getWriter();
            out.println("Error while generating PDF:");
            e.printStackTrace(out);

            LOGGER.error("Error while generating PDF", e);
        } catch (IOException ex) {
            throw new RuntimeException(e);
        } finally {
            if (out != null) {
                out.close();
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
    protected final void error(final HttpServletResponse httpServletResponse, final String message, final HttpStatus code) {
        PrintWriter out = null;
        try {
            httpServletResponse.setContentType("text/plain");
            httpServletResponse.setStatus(code.value());
            out = httpServletResponse.getWriter();
            out.println("Error while generating PDF:");
            out.println(message);

            LOGGER.error("Error while generating PDF: " + message);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
