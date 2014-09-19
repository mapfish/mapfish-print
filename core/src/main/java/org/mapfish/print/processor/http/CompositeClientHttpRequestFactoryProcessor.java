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

import com.google.common.collect.Lists;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.output.Values;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.ProcessorUtils;

import java.util.List;
import javax.annotation.Nullable;

/**
 * A processor that wraps several {@link AbstractClientHttpRequestFactoryProcessor}s.
 * <p>
 *   This makes it more convenient to configure multiple processors that modify
 *   {@link org.mapfish.print.http.MfClientHttpRequestFactory} objects.
 *</p>
 * <p>
 *     Consider the case where you need to:
 *     <ul>
 *     <li>Restrict allowed URIS using the !restrictUris processor</li>
 *     <li>Forward all headers from print request to all requests using !forwardHeaders</li>
 *     <li>Change the url using the !mapUri processor</li>
 *     </ul>
 *     In this case the !mapUri processor must execute before the !restrictUris processor but it is difficult to enforce this, the
 *     inputMapping and outputMapping must be carefully designed in order to do it.  The following should work but compare it with
 *     the example below:
 *     <pre><code>
 * - !mapUri
 *   mapping:
 *     (http)://localhost(.*) : "$1://127.0.0.1$2"
 *   outputMapper: {clientHttpRequestFactory: clientHttpRequestFactoryMapped}
 * - !forwardHeaders
 *   all: true
 *   inputMapper: {clientHttpRequestFactoryMapped :clientHttpRequestFactory}
 *   outputMapper: {clientHttpRequestFactory: clientHttpRequestFactoryWithHeaders}
 * - !restrictUris
 *   matchers: [!localMatch {}]
 *   inputMapper: {clientHttpRequestFactoryWithHeaders:clientHttpRequestFactory}
 *     </code></pre>
 * </p>
 * <p>
 *     The recommended way to write the above configuration is as follows:
 * </p>
 * <pre><code>
 * - !configureHttpRequests
 *   httpProcessors:
 *     - !mapUri
 *       mapping:
 *         (http)://localhost(.*) : "$1://127.0.0.1$2"
 *     - !forwardHeaders
 *       all: true
 *     - !restrictUris
 *       matchers: [!localMatch {}]
 * </code></pre>
 * @author Jesse on 6/25/2014.
 */
public final class CompositeClientHttpRequestFactoryProcessor
        extends AbstractProcessor<Values, ClientHttpFactoryProcessorParam>
        implements HttpProcessor<Values> {
    private List<HttpProcessor> httpProcessors = Lists.newArrayList();

    /**
     * Constructor.
     */
    protected CompositeClientHttpRequestFactoryProcessor() {
        super(ClientHttpFactoryProcessorParam.class);
    }

    /**
     * Sets all the http processors that will executed by this processor.
     *
     * @param httpProcessors the sub processors
     */
    public void setHttpProcessors(final List<HttpProcessor> httpProcessors) {
        this.httpProcessors = httpProcessors;
    }

    @SuppressWarnings("unchecked")
    @Override
    public MfClientHttpRequestFactory createFactoryWrapper(final Values values,
                                                         final MfClientHttpRequestFactory requestFactory) {
        MfClientHttpRequestFactory finalRequestFactory = requestFactory;
        // apply the parts in reverse so that the last part is the inner most wrapper (will be last to be called)
        for (int i = this.httpProcessors.size() - 1; i > -1; i--) {
            final HttpProcessor processor = this.httpProcessors.get(i);
            Object input = ProcessorUtils.populateInputParameter(processor, values);
            finalRequestFactory = processor.createFactoryWrapper(input, finalRequestFactory);
        }
        return finalRequestFactory;
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors, final Configuration configuration) {
        if (this.httpProcessors.isEmpty()) {
            validationErrors.add(new IllegalStateException("There are no composite elements for this processor"));
        } else {
            for (Object part : this.httpProcessors) {
                if (!(part instanceof HttpProcessor)) {
                    validationErrors.add(new IllegalStateException("One of the parts of " + getClass().getSimpleName() + " is not a " +
                                                                   HttpProcessor.class.getSimpleName()));
                }
            }
        }
    }

    @Nullable
    @Override
    public Values createInputParameter() {
        return new Values();
    }

    @Nullable
    @Override
    public ClientHttpFactoryProcessorParam execute(final Values values,
                                                   final ExecutionContext context) throws Exception {
        MfClientHttpRequestFactory requestFactory = values.getObject(Values.CLIENT_HTTP_REQUEST_FACTORY_KEY,
                MfClientHttpRequestFactory.class);

        final ClientHttpFactoryProcessorParam output = new ClientHttpFactoryProcessorParam();
        output.clientHttpRequestFactory = createFactoryWrapper(values, requestFactory);
        return output;
    }
}
