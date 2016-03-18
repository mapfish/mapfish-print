package org.mapfish.print.processor;

import java.util.List;
import javax.annotation.Nonnull;

/**
 * Classes that implement this interface indicate that they take responsibility for generating their dependencies as the dependency
 * graph is being created.
 * <p></p>
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
