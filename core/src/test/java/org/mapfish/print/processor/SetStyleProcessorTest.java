package org.mapfish.print.processor;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.geotools.AbstractFeatureSourceLayer;
import org.mapfish.print.output.Values;
import org.mapfish.print.processor.map.SetStyleProcessor;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("unchecked")
public class SetStyleProcessorTest extends AbstractMapfishSpringTest {
    public static final String BASE_DIR = "setstyle/";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private ForkJoinPool forkJoinPool;
    @Autowired
    private TestHttpClientFactory httpClientFactory;

    @Test
    public void testAssignStyleBasic() throws Exception {
        this.configurationFactory.setDoValidation(false);
        final Configuration config = this.configurationFactory.getConfig(getFile(BASE_DIR + "basic/config.yaml"));
        final Template template = config.getTemplate("main");
        PJsonObject requestData = parseJSONObjectFromFile(SetStyleProcessorTest.class, BASE_DIR + "basic/request.json");
        Values values = new Values("test", requestData, template, this.folder.getRoot(), this.httpClientFactory, new File("."));
        forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

        final MapAttribute.MapAttributeValues map = values.getObject("map", MapAttribute.MapAttributeValues.class);
        final AbstractFeatureSourceLayer layer = (AbstractFeatureSourceLayer) map.getLayers().get(0);
        final MapfishMapContext mapContext = AbstractMapfishSpringTest.createTestMapContext();
        assertEquals("Default Line",
                layer.getLayers(httpClientFactory, mapContext, "test").get(0).getStyle().getDescription().getTitle().toString());
    }

    @Test
    public void testAssignStyleMap() throws Exception {
        this.configurationFactory.setDoValidation(false);
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "map/config.yaml"));
        final Template template = config.getTemplate("main");

        ProcessorDependencyGraph graph = template.getProcessorGraph();
        List<ProcessorGraphNode> roots = graph.getRoots();

        assertEquals(1, roots.size());
        ProcessorGraphNode<Object, Object> rootNode = roots.get(0);
        assertEquals(SetStyleProcessor.class, rootNode.getProcessor().getClass());
        assertEquals(2, rootNode.getAllProcessors().size());
    }

    @Test
    public void testAssignStyleTwoMaps() throws Exception {
        this.configurationFactory.setDoValidation(false);
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "two_maps/config.yaml"));
        final Template template = config.getTemplate("main");

        ProcessorDependencyGraph graph = template.getProcessorGraph();
        List<ProcessorGraphNode> roots = graph.getRoots();

        assertEquals(2, roots.size());
        ProcessorGraphNode<Object, Object> rootNode1 = roots.get(0);
        assertEquals(SetStyleProcessor.class, rootNode1.getProcessor().getClass());
        assertEquals(2, rootNode1.getAllProcessors().size());

        ProcessorGraphNode<Object, Object> rootNode2 = roots.get(1);
        assertEquals(SetStyleProcessor.class, rootNode2.getProcessor().getClass());
        assertEquals(2, rootNode2.getAllProcessors().size());
    }
}
