/*
 * Copyright (C) 2014-2015  Camptocamp
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

import org.mapfish.print.config.Configuration;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.http.matcher.URIMatcher;
import org.mapfish.print.processor.http.matcher.UriMatchers;

import java.util.List;
import javax.annotation.Nullable;

/**
 * @author Jesse on 6/25/2014.
 */
public abstract class AbstractClientHttpRequestFactoryProcessor
        extends AbstractProcessor<ClientHttpFactoryProcessorParam, ClientHttpFactoryProcessorParam>
        implements HttpProcessor<ClientHttpFactoryProcessorParam> {

    /**
     * The matchers that choose if the processor is applied or not.
     */
    protected final UriMatchers matchers = new UriMatchers();

    /**
     * Constructor.
     */
    protected AbstractClientHttpRequestFactoryProcessor() {
        super(ClientHttpFactoryProcessorParam.class);
    }

    /**
     * The matchers used to select the urls that are going to be modified by the processor.
     * For example:
     * <pre><code>
     * - !restrictUris
     *   matchers:
     *     - !localMatch
     *       dummy: true
     *     - !ipMatch
     *     ip: www.camptocamp.org
     *     - !dnsMatch
     *       host: mapfish-geoportal.demo-camptocamp.com
     *       port: 80
     *     - !dnsMatch
     *       host: labs.metacarta.com
     *       port: 80
     *     - !dnsMatch
     *       host: terraservice.net
     *       port: 80
     *     - !dnsMatch
     *       host: tile.openstreetmap.org
     *       port: 80
     *     - !dnsMatch
     *       host: www.geocat.ch
     *       port: 80
     * </code></pre>
     *
     * @param matchers the list of matcher to use to check if a url is permitted
     */
    public final void setMatchers(final List<? extends URIMatcher> matchers) {
        this.matchers.setMatchers(matchers);
    }

    //CSOFF: DesignForExtension
    @Override
    protected void extraValidation(final List<Throwable> validationErrors, final Configuration configuration) {
        this.matchers.validate(validationErrors);
    }
    //CSON: DesignForExtension

    @Nullable
    @Override
    public final ClientHttpFactoryProcessorParam createInputParameter() {
        return new ClientHttpFactoryProcessorParam();
    }

    @Nullable
    @Override
    public final ClientHttpFactoryProcessorParam execute(final ClientHttpFactoryProcessorParam values,
                               final ExecutionContext context) throws Exception {
        values.clientHttpRequestFactory = createFactoryWrapper(values, values.clientHttpRequestFactory);
        return values;
    }

}
