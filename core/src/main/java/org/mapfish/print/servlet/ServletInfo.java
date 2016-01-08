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

/**
 * Provides information about the current servlet.
 *
 * @author Jesse on 4/26/2014.
 */
public interface ServletInfo {
    /**
     * Return an id that identifies this server.
     * This information is incorporated into the print request id so that
     * it is possible for load balancers to redirect a request to the same server without having to
     * keep sticky sessions.  Or to find the same server after a session has expired.
     * <p></p>
     * This provides an option for simple clusters to be created without having to have a shared registry
     * for storing the look up when needing to retrieve the results of a print job.
     */
    String getServletId();
}
