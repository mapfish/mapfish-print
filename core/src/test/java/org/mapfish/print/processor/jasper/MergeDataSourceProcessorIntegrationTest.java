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

package org.mapfish.print.processor.jasper;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.processor.ProcessorDependency;
import org.mapfish.print.processor.ProcessorDependencyGraph;
import org.mapfish.print.processor.ProcessorGraphNode;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

import static org.junit.Assert.assertEquals;

public class MergeDataSourceProcessorIntegrationTest extends AbstractMapfishSpringTest {


    private static final Predicate<? super ProcessorGraphNode> FIND_MERGE_PROCESSOR = new Predicate<ProcessorGraphNode>() {
        @Override
        public boolean apply(@Nonnull ProcessorGraphNode input) {
            return input.getProcessor() instanceof MergeDataSourceProcessor;
        }
    };
    @Autowired
    private ConfigurationFactory configurationFactory;

    @Test
    public void testCreateDependencies() throws Exception {
        final File configFile = getFile("merge-data-sources/config.yaml");
        final Configuration config = configurationFactory.getConfig(configFile);

        final Template template = config.getTemplate("A4 portrait");

        final ProcessorDependencyGraph processorGraph = template.getProcessorGraph();

        final List<ProcessorGraphNode> roots = processorGraph.getRoots();
        assertEquals(0, Collections2.filter(roots, FIND_MERGE_PROCESSOR).size());
        assertEquals(3, count(processorGraph.toString(), "  +-- MergeDataSourceProcessor"));

        MergeDataSourceProcessor mergeDataSourceProcessor = null;
        List<ProcessorGraphNode<Object, Object>> allNodes = Lists.newArrayList();
        for (Processor<?, ?> processor : processorGraph.getAllProcessors()) {
            if (processor instanceof MergeDataSourceProcessor) {
                mergeDataSourceProcessor = (MergeDataSourceProcessor) processor;
            } else {
                allNodes.add(new ProcessorGraphNode(processor, null));
            }
        }

        List<ProcessorDependency> result = mergeDataSourceProcessor.createDependencies(allNodes);
        assertEquals(3, result.size());

    }

    private int count(String string, String toFind) {
        final Matcher matcher = Pattern.compile(Pattern.quote(toFind)).matcher(string);
        int count = 0;
        while(matcher.find()) {
            count += 1;
        }
        return count;
    }


}