package org.mapfish.print.processor.http;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.attribute.NorthArrowAttribute;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.ConfigFileResolvingHttpRequestFactory;
import org.mapfish.print.http.MfClientHttpRequestFactoryImpl;
import org.mapfish.print.output.Values;
import org.mapfish.print.processor.ProcessorDependencyGraph;
import org.mapfish.print.processor.ProcessorGraphNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static org.junit.Assert.assertEquals;

@ContextConfiguration(locations = {
        "classpath:org/mapfish/print/processor/http/map-uri/map-uri-228-bug-fix-processor-application-context.xml"
})
public class MapUriBug228ProcessorTest extends AbstractMapfishSpringTest {
    @Autowired
    ConfigurationFactory configurationFactory;
    @Autowired
    MfClientHttpRequestFactoryImpl httpClientFactory;
    @Autowired
    ForkJoinPool forkJoinPool;


    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected String baseDir() {
        return "map-uri";
    }
    @Test
    @DirtiesContext
    public void bug228FixMapUri() throws Exception {
        this.configurationFactory.setDoValidation(false);
        final Configuration config = configurationFactory.getConfig(getFile(baseDir() + "/map-uri-228-bug-fix-config.yaml"));
        final Template template = config.getTemplate("main");

        ConfigFileResolvingHttpRequestFactory requestFactory = new ConfigFileResolvingHttpRequestFactory(this.httpClientFactory, config, "test");
        ProcessorDependencyGraph graph = template.getProcessorGraph();
        List<ProcessorGraphNode> roots = graph.getRoots();

        assertEquals(1, roots.size());

        Values values = new Values();
        values.put(Values.CLIENT_HTTP_REQUEST_FACTORY_KEY, new MfClientHttpRequestFactoryProvider(requestFactory));
        values.put(Values.TASK_DIRECTORY_KEY, temporaryFolder.getRoot());
        values.put(Values.VALUES_KEY, values);
        MapAttribute.MapAttributeValues map = getMapValue(template);
        values.put("map", map);
        NorthArrowAttribute.NorthArrowAttributeValues northArrow = getNorthArrowValue(template);
        values.put("northArrow", northArrow);
        forkJoinPool.invoke(graph.createTask(values));
    }

    private NorthArrowAttribute.NorthArrowAttributeValues getNorthArrowValue(Template template) {
        NorthArrowAttribute northArrowAttribute = new NorthArrowAttribute();
        northArrowAttribute.setSize(64);
        NorthArrowAttribute.NorthArrowAttributeValues value = northArrowAttribute.createValue(template);
        value.graphic = "NorthArrow.png";
        return value;
    }

    private MapAttribute.MapAttributeValues getMapValue(Template template) {
        MapAttribute mapAttribute = new MapAttribute();
        mapAttribute.setWidth(500);
        mapAttribute.setHeight(500);
        MapAttribute.MapAttributeValues value = mapAttribute.createValue(template);
        value.dpi = 72;
        return value;
    }
}
