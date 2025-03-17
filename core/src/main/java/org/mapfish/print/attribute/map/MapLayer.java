package org.mapfish.print.attribute.map;

import java.awt.Graphics2D;
import java.util.Optional;
import org.mapfish.print.http.HttpRequestFetcher;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.tiled.AbstractTiledLayerParams;
import org.mapfish.print.map.tiled.TileInformation;
import org.mapfish.print.map.tiled.TilePreparationInfo;
import org.mapfish.print.processor.Processor;

/** Encapsulates the data required to load map data for a layer and render it. */
public interface MapLayer {

  /**
   * Represents the default scaling factor applied to a map layer.
   *
   * <p>By default, this value is set to 1.0, signifying no scaling. This variable can be used to
   * adjust the scale ratio between map elements, ensuring that rendered layers maintain consistency
   * in appearance and resolution. It is generally used in map rendering operations and calculations
   * involving layer scaling.
   */
  double DEFAULT_SCALING = 1.0;

  /**
   * To record all the data linked to a particular context of a Layer.
   *
   * @param scale the scaling factor to apply to the layer in this context
   * @param tileInformation the information linked to a context of a Layer (if it exists)
   * @param tilePreparationInfo the information to prepare a tile (if it exists)
   */
  record LayerContext(
      double scale,
      TileInformation<? extends AbstractTiledLayerParams> tileInformation,
      TilePreparationInfo tilePreparationInfo) {}

  /**
   * Attempt to add the layer this layer so that both can be rendered as a single layer.
   *
   * <p>For example: 2 WMS layers from the same WMS server can be combined into a single WMS layer
   * and the map can be rendered with a single WMS request.
   *
   * @param newLayer the layer to combine with this layer. The new layer will be rendered
   *     <em>below</em> the current layer.
   * @return If the two layers can be combined then a map layer representing the two layers will be
   *     returned. If the two layers cannot be combined then Option.absent() will be returned.
   */
  Optional<MapLayer> tryAddLayer(MapLayer newLayer);

  /**
   * Render the layer to the graphics2D object.
   *
   * @param transformer the map transformer containing the map bounds and size.
   * @param clientHttpRequestFactory the factory to use for making http requests.
   * @return the LayerContext for this requested rendering.
   */
  LayerContext prepareRender(
      MapfishMapContext transformer, MfClientHttpRequestFactory clientHttpRequestFactory);

  /**
   * Render the layer to the graphics2D object.
   *
   * @param graphics2D the graphics object.
   * @param clientHttpRequestFactory The factory to use for making http requests.
   * @param transformer the map transformer containing the map bounds and size.
   * @param context the job ID
   * @param layerContext the context of this layer
   */
  void render(
      Graphics2D graphics2D,
      MfClientHttpRequestFactory clientHttpRequestFactory,
      MapfishMapContext transformer,
      Processor.ExecutionContext context,
      LayerContext layerContext);

  /**
   * Indicate if the layer supports native rotation (e.g. WMS layers with the "angle" parameter).
   *
   * @return True if the layer itself takes care of rotating.
   */
  boolean supportsNativeRotation();

  /** The layer name. */
  String getName();

  /**
   * Specify whether layer should be rendered as PNG, JPEG or SVG.
   *
   * @return render type
   */
  RenderType getRenderType();

  /**
   * Cache any needed resources on disk.
   *
   * @param httpRequestFetcher TODO
   * @param clientHttpRequestFactory client http request factory
   * @param transformer transformer
   * @param context the job ID
   * @param layerContext the context of this layer
   * @return the same layer context or one with updated contextual information
   */
  LayerContext prefetchResources(
      HttpRequestFetcher httpRequestFetcher,
      MfClientHttpRequestFactory clientHttpRequestFactory,
      MapfishMapContext transformer,
      Processor.ExecutionContext context,
      LayerContext layerContext);

  /**
   * Gets the opacity.
   *
   * @return the opacity
   */
  double getOpacity();

  /** Enumerated type to specify whether layer should be rendered as PNG, JPEG or SVG. */
  enum RenderType {
    /** Unknown Rendering Format (let CreateMapProcessor decide). */
    UNKNOWN,
    /** Render as PNG. */
    PNG,
    /** Render as JPEG. */
    JPEG,
    /** Render as TIFF. */
    TIFF,
    /** Render as SVG. */
    SVG;

    /**
     * Get RenderType from a string that represents a mime type.
     *
     * @param mimeType string with mime type
     * @return render type
     */
    public static RenderType fromMimeType(final String mimeType) {
      if (mimeType.equals("image/jpeg")) {
        return JPEG;
      } else if (mimeType.equals("image/png")) {
        return PNG;
      } else if (mimeType.matches("image/tiff(-fx)?")) {
        return TIFF;
      } else if (mimeType.matches("image/svg(\\+xml)?")) {
        return SVG;
      } else {
        return UNKNOWN;
      }
    }

    /**
     * Get RenderType from a string that represents a file extension.
     *
     * @param fileExtension string with file extension
     * @return render type
     */
    public static RenderType fromFileExtension(final String fileExtension) {
      final String extensionOrMimeLC = fileExtension.toLowerCase();
      if (extensionOrMimeLC.matches("jpe?g")) {
        return JPEG;
      } else if (extensionOrMimeLC.equals("png")) {
        return PNG;
      } else if (extensionOrMimeLC.matches("tiff?")) {
        return TIFF;
      } else if (extensionOrMimeLC.equals("svg")) {
        return SVG;
      } else {
        return UNKNOWN;
      }
    }
  }
}
