package org.mapfish.print.output;

import static java.util.Map.entry;
import static org.mapfish.print.Constants.PDF_DPI;

import org.geotools.api.coverage.grid.GridCoverageWriter;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValueGroup;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.geometry.jts.ReferencedEnvelope;

import org.geotools.referencing.CRS;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;
import net.sf.jasperreports.engine.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;

public final class JasperReportGeoTiffOutputFormat extends AbstractJasperReportOutputFormat
    implements OutputFormat {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(JasperReportGeoTiffOutputFormat.class);

  public static final Map<String, Integer> IMAGE_TYPES =
      Map.ofEntries(entry("cog", BufferedImage.TYPE_4BYTE_ABGR));

  private String fileSuffix;

  @Override
  public String getContentType() {
    return "image/" + this.fileSuffix;
  }

  @Override
  public String getFileSuffix() {
    return this.fileSuffix;
  }

  public void setFileSuffix(final String fileSuffix) {
    this.fileSuffix = fileSuffix;
  }

  @Override
  protected void doExport(final OutputStream outputStream, final Print print)
      throws JRException, IOException {

    JasperPrint jasperPrint = print.print();
    final int numPages = jasperPrint.getPages().size();

    final float dpiRatio = (float) (print.dpi() / PDF_DPI);
    final int pageHeightOnImage = (int) (jasperPrint.getPageHeight() * dpiRatio);
    final int pageWidthOnImage = (int) (jasperPrint.getPageWidth() * dpiRatio);
    final int separatorHeight = 1;
    final int separatorHeightOnImage = (int) (separatorHeight * dpiRatio);

    final int imageType = IMAGE_TYPES.get(getFileSuffix().toLowerCase());
    BufferedImage reportImage =
        new BufferedImage(
            pageWidthOnImage,
            numPages * pageHeightOnImage + (numPages - 1) * separatorHeightOnImage,
            imageType);
    Graphics2D graphics2D = reportImage.createGraphics();
    if (imageType != BufferedImage.TYPE_4BYTE_ABGR) {
      graphics2D.setColor(Color.WHITE);
      graphics2D.fillRect(0, 0, pageWidthOnImage, pageHeightOnImage);
    }

    try {
      JasperPrintManager printManager = JasperPrintManager.getInstance(print.context());

      for (int pageIndex = 0; pageIndex < numPages; pageIndex++) {
        Image pageImage = printManager.printToImage(jasperPrint, pageIndex, dpiRatio);

        graphics2D.drawImage(
            pageImage,
            0,
            (pageHeightOnImage + separatorHeight) * pageIndex,
            pageWidthOnImage,
            (pageHeightOnImage + separatorHeight) * pageIndex + pageHeightOnImage,
            0,
            0,
            pageWidthOnImage,
            pageHeightOnImage,
            null);
      }

      // draw separator line between the pages
      final Stroke stroke = new BasicStroke(separatorHeightOnImage);
      for (int pageIndex = 0; pageIndex < numPages - 1; pageIndex++) {
        graphics2D.setColor(Color.black);
        graphics2D.setStroke(stroke);
        int y = (pageHeightOnImage + separatorHeight) * pageIndex + pageHeightOnImage;
        graphics2D.drawLine(0, y, pageWidthOnImage, y);
      }
    } finally {
      graphics2D.dispose();
    }

    writeCOG(reportImage, outputStream, print);
  }


  private void writeCOG(BufferedImage image, OutputStream outputStream, Print print)
      throws IOException {

    Map<String, org.mapfish.print.attribute.map.MapAttribute.MapAttributeValues> maps =
        print.values().find(org.mapfish.print.attribute.map.MapAttribute.MapAttributeValues.class);

    if (maps.isEmpty()) {
      throw new IllegalStateException("No map found in print values");
    }

    // Get the first map's bounds
    org.mapfish.print.attribute.map.MapAttribute.MapAttributeValues mapValues =
        maps.values().iterator().next();
    ReferencedEnvelope bbox = mapValues.getMapBounds().toReferencedEnvelope(
        new java.awt.Rectangle(mapValues.getWidth(), mapValues.getHeight()));

    String srs = mapValues.getProjection();
    LOGGER.info("Map SRS: {}", srs);

    try {

      bbox.setCoordinateReferenceSystem(CRS.decode(srs));

      GridCoverageFactory factory = new GridCoverageFactory();
      GridCoverage2D coverage = factory.create(
              "coverage",
              image,
              bbox
      );

      final int tileWidth = 512;  // fixeded value
      final int tileHeight = 512; // fixeded value

      File tmp = File.createTempFile("cog-", ".tif");

      //getting a format
      final GeoTiffFormat format = new GeoTiffFormat();

      //getting the write parameters
      final GeoTiffWriteParams wp = new GeoTiffWriteParams();

      //setting compression to LZW
      wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
      wp.setCompressionType("LZW");
      wp.setCompressionQuality(0.75F);

      //setting the tile size
      wp.setTilingMode(GeoToolsWriteParams.MODE_EXPLICIT);
      wp.setTiling(tileWidth, tileHeight);

      //setting the write parameters for this geotiff
      final ParameterValueGroup params = format.getWriteParameters();
      params.parameter(
                      AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString())
                      .setValue(wp);

      GridCoverageWriter writer = null;

      writer = format.getWriter(tmp);
      writer.write(coverage, (GeneralParameterValue[]) params.values()
                .toArray(new GeneralParameterValue[1]));

      Files.copy(tmp.toPath(), outputStream);
      writer.dispose();
      tmp.delete();
    }
    catch (Exception e) {
      LOGGER.error("Error writing GeoTIFF: ", e);
    }
    LOGGER.info("GeoTIFF written successfully");
  }
}
