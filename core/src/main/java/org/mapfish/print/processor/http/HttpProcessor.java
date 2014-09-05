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
import org.mapfish.print.processor.Processor;

/**
 * A flag interface indicating that this type of processor affects the {@link org.mapfish.print.http.MfClientHttpRequestFactory}
 * object.
 *
 * @author Jesse on 6/26/2014.
 * @param <Param> the type of parameter object required when creating the wrapper object.
 */
public interface HttpProcessor<Param> extends Processor<Param, ClientHttpFactoryProcessorParam> {

    /**
     * Create the {@link org.mapfish.print.http.MfClientHttpRequestFactory} to use.
     *
     * @param param extra parameters required to create the updated request factory
     * @param requestFactory the basic request factory.  It should be unmodified and just wrapped with a proxy class.
     */
    MfClientHttpRequestFactory createFactoryWrapper(Param param, MfClientHttpRequestFactory requestFactory);
}
