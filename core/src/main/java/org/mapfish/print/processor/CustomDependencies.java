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

import java.util.List;
import javax.annotation.Nonnull;

/**
 * Classes that implement this interface indicate that they take responsibility for generating their dependencies as the dependency
 * graph is being created.
 * <p/>
 * Some of their dependencies depend on which nodes are in the current template and therefore can't be declared in the spring
 * configuration file in a static way.  For example the MergeDataSourceProcessor must run after all of its source, since its sources
 * differ from one configuration to another they must be determined at runtime.
 *
 * The test for this class will be part of {@link org.mapfish.print.processor.jasper.MergeDataSourceProcessor} tests.
 *
 * @author Jesse on 9/9/2014.
 */
public interface CustomDependencies {
    /**
     * Create all the dependencies for this processor.
     *
     * @param nodes all the nodes in the template.
     */
    @Nonnull
    List<ProcessorDependency> createDependencies(@Nonnull List<ProcessorGraphNode<Object, Object>> nodes);
}
