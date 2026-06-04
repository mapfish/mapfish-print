package org.mapfish.print.output;

import jakarta.annotation.Nonnull;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import org.geotools.api.coverage.grid.GridCoverageWriter;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.mapfish.print.Constants;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactoryImpl;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.processor.ProcessorDependencyGraph;
import org.mapfish.print.processor.map.CreateMapProcessor;
import org.mapfish.print.wrapper.json.PJsonArray;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * The MapCogExportOutputFormat class.
 *
 * @author Frank and Manuel
 */
public class MapCogExportOutputFormat implements OutputFormat {

  private static final double METERS_PER_INCH = Constants.INCH_TO_MM / 1000.0;

  private static final String MAP_SUBREPORT = "mapSubReport";

  @Autowired private ForkJoinPool forkJoinPool;

  @Autowired private MfClientHttpRequestFactoryImpl httpRequestFactory;

  private String fileSuffix;

  private String contentType;

  @Value("${httpRequest.fetchRetry.maxNumber}")
  private int httpRequestMaxNumberFetchRetry;

  @Value("${httpRequest.fetchRetry.intervalMillis}")
  private int httpRequestFetchRetryIntervalMillis;

  @Override
  public final String getContentType() {
    return this.contentType;
  }

  public final void setContentType(final String contentType) {
    this.contentType = contentType;
  }

  @Override
  public final String getFileSuffix() {
    return this.fileSuffix;
  }

  public final void setFileSuffix(final String fileSuffix) {
    this.fileSuffix = fileSuffix;
  }

  private String getMapSubReportVariable(final Template template) {
    for (Processor<?, ?> processor : template.getProcessors()) {
      if (processor instanceof CreateMapProcessor) {
        String mapSubReport =
            ((CreateMapProcessor) processor).getOutputMapperBiMap().get(MAP_SUBREPORT);
        return Objects.requireNonNullElse(mapSubReport, MAP_SUBREPORT);
      }
    }
    // validation has already confirmed there is exactly one createmap processor
    // so this cannot happen
    return null;
  }

  @Override
  public final Processor.ExecutionContext print(
      @Nonnull final Map<String, String> mdcContext,
      final PJsonObject spec,
      final Configuration config,
      final File configDir,
      final File taskDirectory,
      final OutputStream outputStream)
      throws Exception {
    final String templateName = spec.getString(Constants.JSON_LAYOUT_KEY);

    final Template template = config.getTemplate(templateName);

    final Values values =
        new Values(
            mdcContext,
            spec,
            template,
            taskDirectory,
            this.httpRequestFactory,
            null,
            "tif",
            httpRequestMaxNumberFetchRetry,
            httpRequestFetchRetryIntervalMillis,
            new AtomicBoolean(false));

    final ProcessorDependencyGraph.ProcessorGraphForkJoinTask task =
        template.getProcessorGraph().createTask(values);
    final ForkJoinTask<Values> taskFuture = this.forkJoinPool.submit(task);

    try {
      taskFuture.get();
    } catch (InterruptedException exc) {
      // if cancel() is called on the current thread, this exception will be thrown.
      // in this case, also properly cancel the task future.
      taskFuture.cancel(true);
      Thread.currentThread().interrupt();
      throw new CancellationException();
    }

    String mapSubReport = values.getString(getMapSubReportVariable(template));

    GridCoverageWriter writer = null;
    Path dir = null;
    Path tmp = null;

    try {

      final PJsonObject mapJson = spec.getJSONObject("attributes").getJSONObject("map");
      Path path =
          mapSubReport.startsWith("file:")
              ? Paths.get(new URI(mapSubReport))
              : Path.of(mapSubReport);

      BufferedImage image = ImageIO.read(path.toFile());
      PJsonArray center = mapJson.getJSONArray("center");

      double centerX = center.getDouble(0);
      double centerY = center.getDouble(1);
      double cx = image.getWidth() / 2.0;
      double cy = image.getHeight() / 2.0;

      double scale = mapJson.getDouble("scale");
      double dpi = mapJson.getDouble("dpi");
      double metersPerPixel = scale * METERS_PER_INCH / dpi;
      String srs = mapJson.getString("projection");
      double rotation = mapJson.getDouble("rotation");

      AffineTransform gridToCRS = new AffineTransform();
      gridToCRS.translate(centerX, centerY);
      gridToCRS.rotate(Math.toRadians(rotation));
      gridToCRS.scale(metersPerPixel, -metersPerPixel);
      gridToCRS.translate(-cx, -cy);

      CoordinateReferenceSystem crs = CRS.decode(srs);
      MathTransform mathTransform = new AffineTransform2D(gridToCRS);
      GridCoverageFactory factory = new GridCoverageFactory();
      GridCoverage2D coverage =
          factory.create("coverage", image, crs, mathTransform, null, null, null);

      final int tileWidth = 512;
      final int tileHeight = 512;

      dir = Files.createTempDirectory("cog-print");
      tmp = Files.createTempFile(dir, "cog-", ".tif");

      // write the GridCoverage2D to a GeoTIFF file with LZW compression and tiling
      // using GeoTools
      final GeoTiffFormat format = new GeoTiffFormat();
      final GeoTiffWriteParams wp = new GeoTiffWriteParams();

      wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
      wp.setCompressionType("LZW");
      wp.setCompressionQuality(0.75F);

      wp.setTilingMode(GeoToolsWriteParams.MODE_EXPLICIT);
      wp.setTiling(tileWidth, tileHeight);

      final ParameterValueGroup params = format.getWriteParameters();
      params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
      params.parameter(GeoTiffFormat.RETAIN_AXES_ORDER.getName().toString()).setValue(true);

      // write the coverage to the GeoTIFF file using the specified parameters
      writer = format.getWriter(tmp.toFile());
      writer.write(
          coverage,
          (GeneralParameterValue[]) params.values().toArray(new GeneralParameterValue[0]));

      writer.dispose();

      // copy the temporary file to the output stream
      Files.copy(tmp, outputStream);
    } catch (Exception e) {
      throw new IOException("Error writing cog file", e);
    } finally {

      if (writer != null) {
        try {
          writer.dispose();
        } catch (Exception e) {
          /* ignore */ }
      }
      if (tmp != null) {
        Files.deleteIfExists(
            tmp); // will only succeed if the file is not locked by the writer anymore
      }
      if (dir != null) {
        Files.deleteIfExists(dir); // will only succeed if the tmp file has already been deleted
      }
    }
    return task.getExecutionContext();
  }
}
