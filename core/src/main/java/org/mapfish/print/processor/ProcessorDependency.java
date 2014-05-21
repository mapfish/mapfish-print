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

import java.util.HashSet;
import java.util.Set;


/**
 * Models a dependency between two processors.
 */
public class ProcessorDependency {

    private final Class<? extends AbstractProcessor<?, ?>> required;
    private final Class<? extends AbstractProcessor<?, ?>> dependent;
    private final Set<String> commonInputs;
    
    /**
     * Constructor.
     * The processor <code>dependent</code> requires the processor <code>required</code>.
     * 
     * @param required The processor which is required to be executed before the other.
     * @param dependent The processor which requires the other to be executed first.
     * @param commonInputs The dependency is only enforced if the two processors have these inputs in common.
     */
    public ProcessorDependency(
            final Class<? extends AbstractProcessor<?, ?>> required,
            final Class<? extends AbstractProcessor<?, ?>> dependent,
            final Set<String> commonInputs) {
        this.required = required;
        this.dependent = dependent;
        this.commonInputs = commonInputs;
    }
    
    /**
     * Constructor.
     * The processor <code>dependent</code> requires the processor <code>required</code>.
     * 
     * @param required The processor which is required to be executed before the other.
     * @param dependent The processor which requires the other to be executed first.
     */
    public ProcessorDependency(
            final Class<? extends AbstractProcessor<?, ?>> required,
            final Class<? extends AbstractProcessor<?, ?>> dependent) {
        this(required, dependent, new HashSet<String>());
    }

    /**
     * Returns the processor which is required to be executed before the other.
     */
    public final Class<? extends AbstractProcessor<?, ?>> getRequired() {
        return this.required;
    }

    /**
     * Returns the processor which requires the other to be executed first.
     */
    public final Class<? extends AbstractProcessor<?, ?>> getDependent() {
        return this.dependent;
    }

    /**
     * The inputs that both processors must have in common.
     */
    public final Set<String> getCommonInputs() {
        return this.commonInputs;
    }

}
