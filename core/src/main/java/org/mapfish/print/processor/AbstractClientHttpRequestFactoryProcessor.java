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

package org.mapfish.print.processor;

import org.springframework.http.client.ClientHttpRequestFactory;

import javax.annotation.Nullable;

/**
 * @author Jesse on 6/25/2014.
 */
public abstract class AbstractClientHttpRequestFactoryProcessor
        extends AbstractProcessor<ClientHttpFactoryProcessorParam, ClientHttpFactoryProcessorParam> {

    /**
     * Constructor.
     */
    protected AbstractClientHttpRequestFactoryProcessor() {
        super(ClientHttpFactoryProcessorParam.class);
    }

    @Nullable
    @Override
    public final ClientHttpFactoryProcessorParam createInputParameter() {
        return new ClientHttpFactoryProcessorParam();
    }

    @Nullable
    @Override
    public final ClientHttpFactoryProcessorParam execute(final ClientHttpFactoryProcessorParam values,
                               final ExecutionContext context) throws Exception {
        values.clientHttpRequestFactory = createFactoryWrapper(values.clientHttpRequestFactory);
        return values;
    }

    /**
     * Create the {@link org.springframework.http.client.ClientHttpRequestFactory} to use.
     *
     * @param requestFactory the basic request factory.  It should be unmodified and just wrapped with a proxy class.
     */
    protected abstract ClientHttpRequestFactory createFactoryWrapper(ClientHttpRequestFactory requestFactory);

}
