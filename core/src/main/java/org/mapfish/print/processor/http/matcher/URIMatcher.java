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

package org.mapfish.print.processor.http.matcher;

import org.mapfish.print.config.ConfigurationObject;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Checks if a uri is a permitted uri.
 *
 * @author Jesse on 8/6/2014.
 */
public interface URIMatcher extends ConfigurationObject {
    /**
     * Check if the uri is permitted, return true if the uri is accepted or false otherwise.
     *
     * @param matchInfo the matching information to check
     */
    boolean accepts(final MatchInfo matchInfo) throws UnknownHostException, SocketException, MalformedURLException;
}
