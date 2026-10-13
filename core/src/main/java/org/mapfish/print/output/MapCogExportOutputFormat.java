package org.mapfish.print.output;

import jakarta.annotation.Nonnull;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import org.geotools.api.coverage.grid.GridCoverageWriter;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.processor.ProcessorDependencyGraph;
import org.mapfish.print.wrapper.json.PJsonArray;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The MapCogExportOutputFormat class.
 *
 * @author Frank and Manuel
 */
public class MapCogExportOutputFormat extends MapExportOutputFormat {

  private static final Logger LOGGER = LoggerFactory.getLogger(MapCogExportOutputFormat.class);

  /**
   * Returns the raster midpoint in pixel-center coordinates.
   *
   * <p>GeoTools uses CELL_CENTER coordinates for the supplied gridToCRS transform. Pixel centers
   * are indexed from 0 to pixelCount - 1. Therefore, the raster midpoint is (pixelCount - 1) / 2.0.
   *
   * <p>For odd dimensions, the midpoint lies on a pixel center. For even dimensions, it lies
   * halfway between two pixel centers. This is a coordinate convention, not a rounding correction.
   *
   * @see org.geotools.api.referencing.datum.PixelInCell#CELL_CENTER
   */
  private static double pixelCenterCoordinate(final int pixelCount) {
    return (pixelCount - 1) / 2.0;
  }

  private Template prepareTemplate(final PJsonObject spec, final Configuration config) {

    final String templateName = spec.getString(Constants.JSON_LAYOUT_KEY);
    final Template template = config.getTemplate(templateName);

    if (template == null) {
      throw new IllegalArgumentException(
          "Template with name '" + templateName + "' does not exist");
    }
    return template;
  }

  private Values getValues(
      @Nonnull final Template template,
      final PJsonObject spec,
      @Nonnull final Map<String, String> mdcContext,
      final File taskDirectory) {
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
    return values;
  }

  private ProcessorDependencyGraph.ProcessorGraphForkJoinTask executeProcessors(
      @Nonnull final Template template, @Nonnull final Values values) throws ExecutionException {

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
    return task;
  }

  private GridCoverage2D getGridCoverage(
      @Nonnull final Template template, @Nonnull final Values values, final PJsonObject spec)
      throws URISyntaxException, IOException, FactoryException {

    final String mapSubReport = values.getString(getMapSubReportVariable(template));
    final MapfishMapContext mapContext =
        Objects.requireNonNull(
            values.getObject(getMapOutputVariable(template, "mapContext"), MapfishMapContext.class),
            "Map context is required for COG export");

    final PJsonObject mapJson = spec.getJSONObject("attributes").getJSONObject("map");
    final Path path =
        mapSubReport.startsWith("file:") ? Paths.get(new URI(mapSubReport)) : Path.of(mapSubReport);

    final BufferedImage image = ImageIO.read(path.toFile());

    final String srs = mapJson.getString("projection");
    final CoordinateReferenceSystem crs = CRS.decode(srs);
    final GridCoverageFactory factory = new GridCoverageFactory();
    final GridCoverage2D coverage;

    if (mapJson.has("center")) {
      final ReferencedEnvelope mapEnvelope = mapContext.toReferencedEnvelope();

      // Use the actual geographic extent and raster dimensions.
      // This accounts for pixel rounding during DPI scaling.
      final double pixelWidth = mapEnvelope.getWidth() / image.getWidth();
      final double pixelHeight = mapEnvelope.getHeight() / image.getHeight();

      // GeoTools uses pixel-center coordinates (CELL_CENTER).
      final double centerPixelX = pixelCenterCoordinate(image.getWidth());
      final double centerPixelY = pixelCenterCoordinate(image.getHeight());

      final AffineTransform gridToCRS = new AffineTransform();
      gridToCRS.translate(mapEnvelope.getMedian(0), mapEnvelope.getMedian(1));
      gridToCRS.rotate(mapContext.getRotation());
      gridToCRS.scale(pixelWidth, -pixelHeight);
      gridToCRS.translate(-centerPixelX, -centerPixelY);

      final MathTransform mathTransform = new AffineTransform2D(gridToCRS);

      coverage = factory.create("coverage", image, crs, mathTransform, null, null, null);

    } else if (mapJson.has("bbox")) {

      final PJsonArray bbox = mapJson.getJSONArray("bbox");

      final double minX = bbox.getDouble(0);
      final double minY = bbox.getDouble(1);
      final double maxX = bbox.getDouble(2);
      final double maxY = bbox.getDouble(3);

      final ReferencedEnvelope envelope = new ReferencedEnvelope(minX, maxX, minY, maxY, crs);

      coverage = factory.create("coverage", image, envelope);

    } else {
      throw new IllegalArgumentException("COG export requires either center + scale or bbox");
    }
    return coverage;
  }

  private ParameterValueGroup createGeoTiffParams(@Nonnull final GeoTiffFormat format) {
    final int tileWidth = 512;
    final int tileHeight = 512;

    // write the GridCoverage2D to a GeoTIFF file with LZW compression and tiling
    // using GeoTools
    final GeoTiffWriteParams wp = new GeoTiffWriteParams();

    wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
    wp.setCompressionType("LZW");
    wp.setCompressionQuality(0.75F);

    wp.setTilingMode(GeoToolsWriteParams.MODE_EXPLICIT);
    wp.setTiling(tileWidth, tileHeight);

    final ParameterValueGroup params = format.getWriteParameters();
    params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
    params.parameter(GeoTiffFormat.RETAIN_AXES_ORDER.getName().toString()).setValue(true);

    return params;
  }

  private void writeGeoTiff(
      @Nonnull final GeoTiffFormat format,
      @Nonnull final GridCoverage2D coverage,
      @Nonnull final ParameterValueGroup params,
      @Nonnull final OutputStream outputStream)
      throws IOException {
    GridCoverageWriter writer = null;
    try {
      // MapFish Print owns the output stream; GeoTools must not close it.
      writer = format.getWriter(outputStream);

      if (writer == null) {
        throw new IllegalStateException("Could not create GeoTIFF writer");
      }

      // write the coverage to the GeoTIFF file using the specified parameters
      writer.write(
          coverage,
          (GeneralParameterValue[]) params.values().toArray(new GeneralParameterValue[0]));

    } catch (Exception e) {
      throw new IOException("Error writing cog file", e);
    } finally {
      if (writer != null) {
        try {
          writer.dispose();
        } catch (Exception e) {
          LOGGER.warn("Could not dispose GeoTIFF writer", e);
        }
      }
    }
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

    final Template template = prepareTemplate(spec, config);
    final Values values = getValues(template, spec, mdcContext, taskDirectory);
    final ProcessorDependencyGraph.ProcessorGraphForkJoinTask task =
        executeProcessors(template, values);

    final GeoTiffFormat format = new GeoTiffFormat();
    final GridCoverage2D coverage = getGridCoverage(template, values, spec);
    final ParameterValueGroup params = createGeoTiffParams(format);

    writeGeoTiff(format, coverage, params, outputStream);

    return task.getExecutionContext();
  }
}
