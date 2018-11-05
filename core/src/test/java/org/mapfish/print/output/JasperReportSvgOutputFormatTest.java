package org.mapfish.print.output;

import org.junit.Test;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;

public class JasperReportSvgOutputFormatTest extends AbstractJasperReportOutputFormatTest {
    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private Map<String, OutputFormat> outputFormat;

    @Test
    public void testPrint() throws Exception {
        final Configuration config =
                configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));

        PJsonObject requestData = loadJsonRequestData();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputFormat format = this.outputFormat.get("svgOutputFormat");
        format.print("test", requestData, config,
                getFile(JasperReportSvgOutputFormatTest.class, BASE_DIR), getTaskDirectory(),
                outputStream);

        byte[] expected = Files.readAllBytes(getFile(BASE_DIR + "expectedReport.svg").toPath());
        assertArrayEquals(expected, outputStream.toByteArray());
    }
}
