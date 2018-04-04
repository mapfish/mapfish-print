package org.mapfish.print.processor.jasper;

import net.sf.jasperreports.engine.JasperPrint;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.attribute.DataSourceAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.AbstractJasperReportOutputFormat;
import org.mapfish.print.output.OutputFormat;
import org.mapfish.print.output.Values;
import org.mapfish.print.test.util.ImageSimilarity;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DataSourceProcessorTest extends AbstractMapfishSpringTest {

    public static final String BASE_DIR = "tablelist/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private ForkJoinPool forkJoinPool;
    @Autowired
    private TestHttpClientFactory httpRequestFactory;
    @Autowired
    private Map<String, OutputFormat> outputFormat;

    @Test @DirtiesContext
    public void testValidate() throws Exception {
        final File configFile = getFile("incorrectly-configured-DataSourceProcessor/config.yaml");
        configurationFactory.setDoValidation(false);
        final Configuration config = configurationFactory.getConfig(configFile);

        final List<Throwable> validate = config.validate();

        assertFalse(validate.isEmpty());
    }

    @Test
    public void testBasicTableProperties() throws Exception {
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        final Template template = config.getTemplate("main");
        PJsonObject requestData = loadJsonRequestData();
        Values values = new Values("test", requestData, template, getTaskDirectory(), this.httpRequestFactory, new File("."));
        forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

        final DataSourceAttribute.DataSourceAttributeValue datasource = values.getObject("datasource", DataSourceAttribute.DataSourceAttributeValue.class);

        assertEquals(2, datasource.attributesValues.length);
    }

    @Test
    public void testRenderTable() throws Exception {
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));

        PJsonObject requestData = loadJsonRequestData();

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat) this.outputFormat.get("pngOutputFormat");
        JasperPrint print = format.getJasperPrint("test", requestData, config, config.getDirectory(), getTaskDirectory()).print;

        assertEquals(1, print.getPages().size());

        new ImageSimilarity(getFile(BASE_DIR + "expected-page.png"))
                .assertSimilarity(print, 0, 15);
    }

    private static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(DataSourceProcessorTest.class, BASE_DIR + "requestData.json");
    }
}
