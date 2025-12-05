package org.mapfish.print.output;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.PDFConfig;
import org.mapfish.print.config.Template;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

public class JRPdfExporterWeakHashMapTest extends AbstractMapfishSpringTest {
  public static final String BASE_DIR = "pdf-config-multi-images/";
  @Autowired private ConfigurationFactory configurationFactory;
  @Autowired private Map<String, OutputFormat> outputFormat;

  public static PJsonObject loadJsonRequestData() throws IOException {
    return parseJSONObjectFromFile(
        JasperReportOutputFormatSimpleMapTest.class, BASE_DIR + "requestData.json");
  }

  /**
   * Check that {@link org.mapfish.print.output.JRPdfExporterWeakHashMap} that override {@link
   * net.sf.jasperreports.engine.export.JRPdfExporter} loadedImagesMap attribute does not break the
   * functionality that when an image is present multiple time in the pdf, the same reference is
   * used in order to prevent to duplicate the image in the pdf, and keep the pdf size small.
   *
   * <p>In the test below multi-images.yaml will generate a pdf page that contains 8 times the same
   * image, and one-image.yml will generate a PDF that contains only one time this image, and
   * no-image-yml will generate a pdf with 0 image for reference.
   *
   * @throws Exception
   */
  @Test
  public void testMultiImagesVsOneImage() throws Exception {
    int noImagePdfSizeByte = generatedPdfAndGetSize("no-image.yaml");
    int oneImagePdfSizeByte = generatedPdfAndGetSize("one-image.yaml");
    int multiImagesPdfSizeByte = generatedPdfAndGetSize("multi-images.yaml");

    int imageSizeInThePdfByte = oneImagePdfSizeByte - noImagePdfSizeByte;
    // If multiImagesPdfSizeByte is smaller that oneImagePdfSizeByte + imageSizeInThePdf whereas it
    // contains the images 8 times, it means the image is stored by reference in this PDF.
    Assertions.assertTrue(
        oneImagePdfSizeByte + imageSizeInThePdfByte > multiImagesPdfSizeByte,
        "multiImagesPdfSizeByte should not be higher than pdf oneImagePdfSizeByte +"
            + " imageSizeInThePdf ");
  }

  public int generatedPdfAndGetSize(final String configName) throws Exception {
    final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + configName));
    final Template rawTemplate = config.getTemplate("main");
    final PDFConfig pdfConfigSpy = Mockito.spy(rawTemplate.getPdfConfig());
    Template templateSpy = Mockito.spy(rawTemplate);
    Mockito.when(templateSpy.getPdfConfig()).thenReturn(pdfConfigSpy);

    final Map<String, Template> templates = config.getTemplates();
    templates.put("main", templateSpy);
    config.setTemplates(templates);

    PJsonObject requestData = loadJsonRequestData();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    OutputFormat format = this.outputFormat.get("pdfOutputFormat");
    format.print(
        new HashMap<>(),
        requestData,
        config,
        getFile(JRPdfExporterWeakHashMapTest.class, BASE_DIR),
        getTaskDirectory(),
        outputStream);
    return outputStream.size();
  }
}
