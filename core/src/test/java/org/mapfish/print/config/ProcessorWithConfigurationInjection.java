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

package org.mapfish.print.config;

import org.mapfish.print.processor.AbstractProcessor;

import java.util.List;
import javax.annotation.Nullable;

import static org.junit.Assert.assertNotNull;

/**
 * Processor that needs the configuration object injected.
 *
 * @author jesseeichar on 3/25/14.
 */
public class ProcessorWithConfigurationInjection extends AbstractProcessor<Object, Void> implements HasConfiguration {

    private Configuration configuration;

    /**
     * Constructor.
     */
    protected ProcessorWithConfigurationInjection() {
        super(Void.class);
    }

    public void assertInjected() {
        assertNotNull(configuration);
    }


    @Override
    public void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Nullable
    @Override
    public Void execute(Object values, ExecutionContext context) throws Exception {
        return null;
    }

    @Override
    public Object createInputParameter() {
        return null;
    }

    @Override
    protected void extraValidation(List<Throwable> validationErrors, final Configuration configuration) {
        // no checks
    }
}
