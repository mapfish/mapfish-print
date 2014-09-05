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

package org.mapfish.print.processor.http;

import org.mapfish.print.http.MfClientHttpRequestFactory;

/**
 * The parameter for a processors that have {@link org.mapfish.print.http.MfClientHttpRequestFactory}.
 *
* @author Jesse on 6/25/2014.
 * CSOFF: VisibilityModifier
*/
public class ClientHttpFactoryProcessorParam {
    /**
     * The object for creating requests.  There should always be an instance in the values object
     * so it does not need to be created.
     */
    public MfClientHttpRequestFactory clientHttpRequestFactory;
}
