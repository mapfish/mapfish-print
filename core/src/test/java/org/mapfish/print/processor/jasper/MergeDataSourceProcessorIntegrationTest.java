package org.mapfish.print.processor.jasper;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Collections2;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.processor.ProcessorDependencyGraph;
import org.mapfish.print.processor.ProcessorGraphNode;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

public class MergeDataSourceProcessorIntegrationTest extends AbstractMapfishSpringTest {


    private static final Predicate<? super ProcessorGraphNode> FIND_MERGE_PROCESSOR =
            input -> input.getProcessor() instanceof MergeDataSourceProcessor;
    @Autowired
    private ConfigurationFactory configurationFactory;

    @Test
    public void testCreateDependencies() throws Exception {
        final File configFile = getFile("merge-data-sources/config.yaml");
        final Configuration config = configurationFactory.getConfig(configFile);

        final Template template = config.getTemplate("A4 portrait");

        final ProcessorDependencyGraph processorGraph = template.getProcessorGraph();

        final List<ProcessorGraphNode<?, ?>> roots = processorGraph.getRoots();
        assertEquals(0, Collections2.filter(roots, FIND_MERGE_PROCESSOR::test).size());
        assertEquals(processorGraph.toString(), 3,
                     count(processorGraph.toString(), " -> \"MergeDataSourceProcessor"));

        MergeDataSourceProcessor mergeDataSourceProcessor = null;
        List<ProcessorGraphNode<?, ?>> allNodes = new ArrayList<>();
        for (Processor<?, ?> processor: processorGraph.getAllProcessors()) {
            if (processor instanceof MergeDataSourceProcessor) {
                mergeDataSourceProcessor = (MergeDataSourceProcessor) processor;
            } else {
                allNodes.add(new ProcessorGraphNode<>(processor, new MetricRegistry()));
            }
        }

        Collection<String> result = mergeDataSourceProcessor.getDependencies();
        assertEquals(3, result.size());
    }

    private int count(String string, String toFind) {
        final Matcher matcher = Pattern.compile(Pattern.quote(toFind)).matcher(string);
        int count = 0;
        while (matcher.find()) {
            count += 1;
        }
        return count;
    }


}
