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

import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * Models a dependency between two processors.
 */
public class ProcessorDependency {

    private final Class<? extends Processor<?, ?>> required;
    private final Class<? extends Processor<?, ?>> dependent;
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
            final Class<? extends Processor<?, ?>> required,
            final Class<? extends Processor<?, ?>> dependent,
            final Set<String> commonInputs) {
        this.required = required;
        this.dependent = dependent;
        this.commonInputs = Sets.newHashSet(commonInputs);
    }
    
    /**
     * Constructor.
     * The processor <code>dependent</code> requires the processor <code>required</code>.
     * 
     * @param required The processor which is required to be executed before the other.
     * @param dependent The processor which requires the other to be executed first.
     */
    public ProcessorDependency(
            final Class<? extends Processor<?, ?>> required,
            final Class<? extends Processor<?, ?>> dependent) {
        this(required, dependent, new HashSet<String>());
    }

    /**
     * Returns the processor which is required to be executed before the other.
     */
    public final Class<? extends Processor<?, ?>> getRequired() {
        return this.required;
    }

    /**
     * Returns the processor which requires the other to be executed first.
     */
    public final Class<? extends Processor<?, ?>> getDependent() {
        return this.dependent;
    }

    /**
     * The inputs that both processors must have in common.
     */
    public final Set<String> getCommonInputs() {
        return Collections.unmodifiableSet(this.commonInputs);
    }

    /**
     * Add a common input to this dependency.
     *
     * @param inputName the name of the input to add
     */
    public final void addCommonInput(final String inputName) {
        this.commonInputs.add(inputName);
    }

    @Override
    public final String toString() {
        return "ProcessorDependency{" +
               "required=" + this.required.getSimpleName() +
               ", dependent=" + this.dependent.getSimpleName() +
               ", commonInputs=" + this.commonInputs +
               '}';
    }
}
