package org.mapfish.print.processor.jasper;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
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
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import static org.junit.Assert.assertEquals;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TableProcessorTest extends AbstractMapfishSpringTest {
    public static final String BASIC_BASE_DIR = "table/";
    public static final String DYNAMIC_BASE_DIR = "table-dynamic/";
    public static final String DEFAULT_DYNAMIC_BASE_DIR = "table-dynamic-defaults/";
    public static final String IMAGE_CONVERTER_BASE_DIR = "table-image-column-resolver/";
    public static final String TABLE_CONVERTERS = "table_converters/";
    public static final String TABLE_CONVERTERS_DYNAMIC = "table_converters_dyn/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private ForkJoinPool forkJoinPool;
    @Autowired
    private TestHttpClientFactory httpRequestFactory;
    @Autowired
    private Map<String, OutputFormat> outputFormat;

    private static PJsonObject loadJsonRequestData(String baseDir) throws IOException {
        return parseJSONObjectFromFile(TableProcessorTest.class, baseDir + "requestData.json");
    }

    @Test
    public void testDefaultDynamicTableProperties() throws Exception {
        final String baseDir = DEFAULT_DYNAMIC_BASE_DIR;

        final Configuration config = configurationFactory.getConfig(getFile(baseDir + "config.yaml"));
        PJsonObject requestData = loadJsonRequestData(baseDir);

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat)
                this.outputFormat.get("pngOutputFormat");
        final File file = getFile(TableProcessorTest.class, baseDir);
        JasperPrint print =
                format.getJasperPrint("test", requestData, config, file, getTaskDirectory()).print;

        // note that we are using a sample size of 50, because the image is quite big.
        // otherwise small differences are not detected!
        new ImageSimilarity(getFile(baseDir + "expectedImage.png"))
                .assertSimilarity(print, 0, 10);
    }

    @Test
    public void testBasicTableProperties() throws Exception {
        final String baseDir = BASIC_BASE_DIR;

        final Configuration config = configurationFactory.getConfig(getFile(baseDir + "config.yaml"));
        final Template template = config.getTemplate("main");
        PJsonObject requestData = loadJsonRequestData(baseDir);
        Values values = new Values("test", requestData, template, getTaskDirectory(),
                                   this.httpRequestFactory, new File("."));
        forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

        final JRMapCollectionDataSource tableDataSource = values.getObject(
                "tableDataSource", JRMapCollectionDataSource.class);

        int count = 0;
        while (tableDataSource.next()) {
            count++;
        }

        assertEquals(2, count);
    }

    @Test
    public void testBasicTablePrint() throws Exception {
        final String baseDir = BASIC_BASE_DIR;
        final Configuration config = configurationFactory.getConfig(getFile(baseDir + "config.yaml"));
        PJsonObject requestData = loadJsonRequestData(baseDir);

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat)
                this.outputFormat.get("pngOutputFormat");
        final File file = getFile(TableProcessorTest.class, baseDir);
        JasperPrint print =
                format.getJasperPrint("test", requestData, config, file, getTaskDirectory()).print;

        // note that we are using a sample size of 50, because the image is quite big.
        // otherwise small differences are not detected!
        new ImageSimilarity(getFile(baseDir + "expectedImage.png"))
                .assertSimilarity(print, 0, 10);
    }

    @Test
    public void testDynamicTablePrint() throws Exception {
        final String baseDir = DYNAMIC_BASE_DIR;
        final Configuration config = configurationFactory.getConfig(getFile(baseDir + "config.yaml"));
        PJsonObject requestData = loadJsonRequestData(baseDir);

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat)
                this.outputFormat.get("pngOutputFormat");
        final File file = getFile(TableProcessorTest.class, baseDir);
        JasperPrint print =
                format.getJasperPrint("test", requestData, config, file, getTaskDirectory()).print;

        // note that we are using a sample size of 50, because the image is quite big.
        // otherwise small differences are not detected!
        new ImageSimilarity(getFile(baseDir + "expectedImage.png"))
                .assertSimilarity(print, 0, 10);
    }

    @Test
    public void testColumnImageConverter() throws Exception {
        httpRequestFactory.registerHandler(input -> input.toString().contains("icons.com"),
                                           createFileHandler(uri -> "/icons" + uri.getPath()));

        final String baseDir = IMAGE_CONVERTER_BASE_DIR;
        final Configuration config = configurationFactory.getConfig(getFile(baseDir + "config.yaml"));
        PJsonObject requestData = loadJsonRequestData(baseDir);

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat)
                this.outputFormat.get("pngOutputFormat");
        final File file = getFile(TableProcessorTest.class, baseDir);
        JasperPrint print =
                format.getJasperPrint("test", requestData, config, file, getTaskDirectory()).print;

        // note that we are using a sample size of 50, because the image is quite big.
        // otherwise small differences are not detected!
        new ImageSimilarity(getFile(baseDir + "expectedImage.png"))
                .assertSimilarity(print, 0, 5);
    }

    @Test
    @DirtiesContext
    public void testTableConverters() throws Exception {
        httpRequestFactory.registerHandler(input -> input.toString().contains("icons.com"),
                                           createFileHandler(uri -> "/icons" + uri.getPath()));

        final String baseDir = TABLE_CONVERTERS;
        final Configuration config = configurationFactory.getConfig(getFile(baseDir + "config.yaml"));
        PJsonObject requestData = loadJsonRequestData(baseDir);

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat)
                this.outputFormat.get("pngOutputFormat");
        final File file = getFile(TableProcessorTest.class, baseDir);
        JasperPrint print =
                format.getJasperPrint("test", requestData, config, file, getTaskDirectory()).print;

        // note that we are using a sample size of 50, because the image is quite big.
        // otherwise small differences are not detected!
        new ImageSimilarity(getFile(baseDir + "expectedImage.png"))
                .assertSimilarity(print, 0, 10);
    }

    @Test
    public void testTableConvertersDynamic() throws Exception {
        final String baseDir = TABLE_CONVERTERS_DYNAMIC;
        final Configuration config = configurationFactory.getConfig(getFile(baseDir + "config.yaml"));
        PJsonObject requestData = loadJsonRequestData(baseDir);

        final AbstractJasperReportOutputFormat format = (AbstractJasperReportOutputFormat)
                this.outputFormat.get("pngOutputFormat");
        final File file = getFile(TableProcessorTest.class, baseDir);
        JasperPrint print =
                format.getJasperPrint("test", requestData, config, file, getTaskDirectory()).print;

        // note that we are using a sample size of 50, because the image is quite big.
        // otherwise small differences are not detected!
        new ImageSimilarity(getFile(baseDir + "expectedImage.png"))
                .assertSimilarity(print, 0, 10);
    }
}
