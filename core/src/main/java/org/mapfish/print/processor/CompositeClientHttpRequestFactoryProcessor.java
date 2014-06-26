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

import com.google.common.collect.Lists;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.util.List;

/**
 * A processor that wraps several {@link org.mapfish.print.processor.AbstractClientHttpRequestFactoryProcessor}s.
 *
 * This makes it more convenient to configure multiple processors that modify
 * {@link org.springframework.http.client.ClientHttpRequestFactory} objects.
 *
 * @author Jesse on 6/25/2014.
 */
public final class CompositeClientHttpRequestFactoryProcessor extends AbstractClientHttpRequestFactoryProcessor {
    private List<AbstractClientHttpRequestFactoryProcessor> parts = Lists.newArrayList();

    public void setParts(final List<AbstractClientHttpRequestFactoryProcessor> parts) {
        this.parts = parts;
    }

    @Override
    protected ClientHttpRequestFactory createFactoryWrapper(final ClientHttpRequestFactory requestFactory) {
        ClientHttpRequestFactory finalRequestFactory = requestFactory;
        // apply the parts in reverse so that the last part is the inner most wrapper (will be last to be called)
        for (int i = this.parts.size() - 1; i > -1; i--) {
             finalRequestFactory = this.parts.get(i).createFactoryWrapper(finalRequestFactory);
        }
        return finalRequestFactory;
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors) {
        if (this.parts.isEmpty()) {
            validationErrors.add(new IllegalStateException("There are no composite elements for this processor"));
        }
    }
}
