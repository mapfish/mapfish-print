package org.mapfish.print.output;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import org.junit.Test;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

public class JasperReportSvgOutputFormatTest extends AbstractJasperReportOutputFormatTest {
  @Autowired private ConfigurationFactory configurationFactory;
  @Autowired private Map<String, OutputFormat> outputFormat;

  @Test
  public void testPrint() throws Exception {
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));

    PJsonObject requestData = loadJsonRequestData();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    OutputFormat format = this.outputFormat.get("svgOutputFormat");
    format.print(
        "test",
        requestData,
        config,
        getFile(JasperReportSvgOutputFormatTest.class, BASE_DIR),
        getTaskDirectory(),
        outputStream);

    File actual =
        new File(
            getFile(BASE_DIR + "expectedReport.svg")
                .toPath()
                .toString()
                .replaceFirst("test", "actual")
                .replace("expectedReport.svg", "actualReport.svg"));
    actual.getParentFile().mkdirs();
    BufferedWriter writer = new BufferedWriter(new FileWriter(actual));
    writer.write(outputStream.toString());
    writer.close();

    String expected = getFileContent(BASE_DIR + "expectedReport.svg");
    assertEquals(actual.toString(), expected, outputStream.toString());
  }
}
