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

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.mapfish.print.attribute.HttpHeadersAttribute;
import org.mapfish.print.processor.AbstractProcessor;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * This processor forwarding allows adding static headers to an http request.
 *
 * @author Jesse on 6/26/2014.
 */
public final class ForwardHeadersProcessor
        extends AbstractProcessor<ForwardHeadersProcessor.Param, ClientHttpFactoryProcessorParam>
        implements HttpProcessor<ForwardHeadersProcessor.Param> {

    Set<String> headerNames = Sets.newHashSet();
    /**
     * Constructor.
     */
    public ForwardHeadersProcessor() {
        super(ClientHttpFactoryProcessorParam.class);
    }

    /**
     * Set the header names to forward from the request.
     *
     * @param names the header names.
     */
    public void setHeaders(final Set<String> names) {
        this.headerNames = names;
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors) {
        if (this.headerNames.isEmpty()) {
            validationErrors.add(new IllegalStateException("No header names defined"));
        }
    }

    @Override
    public ClientHttpRequestFactory createFactoryWrapper(final Param param,
                                                         final ClientHttpRequestFactory requestFactory) {

        Map<String, List<String>> headers = Maps.filterKeys(param.httpHeaders.getHeaders(), new Predicate<String>() {
            @Override
            public boolean apply(@Nullable final String input) {
                return input != null && ForwardHeadersProcessor.this.headerNames.contains(input);
            }
        });

        final AddHeadersProcessor addHeadersProcessor = new AddHeadersProcessor();
        addHeadersProcessor.setHeaders(headers);

        return addHeadersProcessor.createFactoryWrapper(param, requestFactory);
    }

    @Nullable
    @Override
    public Param createInputParameter() {
        return new Param();
    }

    @Nullable
    @Override
    public ClientHttpFactoryProcessorParam execute(final Param values, final ExecutionContext context) throws Exception {
        values.clientHttpRequestFactory = createFactoryWrapper(values, values.clientHttpRequestFactory);
        return values;
    }

    /**
     * The parameters required by this processor.
     */
    public static class Param extends ClientHttpFactoryProcessorParam {
        /**
         * The http headers from the print request.
         */
        public HttpHeadersAttribute.Value httpHeaders;
    }

}
