package org.mapfish.print.map.tiled.wmts;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.util.Assert;
import org.mapfish.print.URIUtils;
import org.mapfish.print.map.tiled.AbstractWMXLayerParams;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.wrapper.PObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The parameters for configuration a WMTS layer. */
public final class WMTSLayerParam extends AbstractWMXLayerParams {
  private static final Logger LOGGER = LoggerFactory.getLogger(WMTSLayerParam.class);

  /**
   * The ‘ResourceURL’ available in the WMTS capabilities.
   *
   * <p>Example (for <code>requestEncoding: "KVP"</code>):
   *
   * <pre><code>
   * baseUrl: "http://domain.com/wmts"
   * </code></pre>
   *
   * Example (for <code>requestEncoding: "REST"</code>):
   *
   * <pre><code>
   * baseUrl: "http://domain.com/wmts/roads/{TileMatrixSet}/{TileMatrix}/{TileCol}/{TileRow}.png"
   * </code></pre>
   *
   * The following URL template variables are replaced:
   *
   * <ul>
   *   <li>{Layer}
   *   <li>{style}
   *   <li>{TileMatrixSet}
   *   <li>{TileMatrix}
   *   <li>{TileRow}
   *   <li>{TileCol}
   *   <li>{[DIMENSION.IDENTIFIER]}
   * </ul>
   */
  public String baseURL;

  /** The layer name. */
  public String layer;

  /** The WMTS protocol version to use. */
  @HasDefaultValue public String version = "1.0.0";

  /** The way to make the requests. Either <code>KVP</code> or <code>REST</code> (default). */
  @HasDefaultValue public RequestEncoding requestEncoding = RequestEncoding.REST;

  /** The style name (for styles on the WMTS server). */
  @HasDefaultValue public String style = "";

  /**
   * The "sample" dimensions or image color bands to retrieve.
   *
   * <p>This can be null, if so then the default dimensions will be returned. If specified they must
   * be dimensions supported by the server.
   *
   * <p>These are keys to the {@link #dimensionParams}.
   */
  @HasDefaultValue public String[] dimensions;

  /** Dictionary of dimensions name (Must be uppercase) =&gt; value. */
  @HasDefaultValue public PObject dimensionParams;

  /** The format of the image. for example image/png, image/jpeg, etc... */
  @HasDefaultValue public String imageFormat = "image/png";

  /** Reference/Identifier to a tileMatrixSet and limits. */
  public String matrixSet;

  /**
   * Array of matrix ids.
   *
   * <p>Example:
   *
   * <pre><code>
   * [{
   *   "identifier": "0",
   *   "matrixSize": [1, 1],
   *   "scaleDenominator": 4000,
   *   "tileSize": [256, 256],
   *   "topLeftCorner": [420000, 350000]
   *   }, ...]
   * </code></pre>
   */
  public Matrix[] matrices;

  @Override
  public String getBaseUrl() {
    return this.baseURL;
  }

  /** Validate some of the properties of this layer. */
  public void postConstruct() {
    Assert.isTrue(validateBaseUrl(), "invalid baseURL");
  }

  @Override
  public String createCommonUrl() throws URISyntaxException {
    if (RequestEncoding.REST == this.requestEncoding) {
      return getBaseUrl();
    } else {
      Multimap<String, String> queryParams = HashMultimap.create();

      queryParams.putAll(getCustomParams());
      queryParams.putAll(getMergeableParams());

      final URI baseUri = new URI(getBaseUrl());
      return URIUtils.addParams(
          getBaseUrl(), queryParams, URIUtils.getParameters(baseUri).keySet());
    }
  }

  @Override
  public boolean validateBaseUrl() {
    String url = getBaseUrl();
    if (StringUtils.isEmpty(url)) {
      return false;
    }

    if (RequestEncoding.REST == this.requestEncoding) {
      if (!containsVariables(url)) {
        LOGGER.warn("URL {} is missing some variables", url);
        return false;
      }
      try {
        createRestURI("matrix", 0, 0);
        return true;
      } catch (URISyntaxException exc) {
        LOGGER.warn("URL {} is invalid: {}", url, exc.getMessage());
        return false;
      }
    } else {
      return super.validateBaseUrl();
    }
  }

  private boolean containsVariables(final String url) {
    return url.contains("{TileMatrix}") && url.contains("{TileRow}") && url.contains("{TileCol}");
  }

  /**
   * Prepare the baseURL to make a request.
   *
   * @param matrixId matrixId
   * @param row row
   * @param col cold
   */
  URI createRestURI(final String matrixId, final int row, final int col) throws URISyntaxException {
    String path = baseURL;
    if (dimensions != null) {
      for (String dimension : dimensions) {
        String value = dimensionParams.optString(dimension);
        if (value == null) {
          value = dimensionParams.getString(dimension.toUpperCase());
        }
        path = path.replace("{" + dimension + "}", value);
      }
    }
    path = path.replaceAll("(?i)" + Pattern.quote("{TileMatrixSet}"), matrixSet);
    path = path.replaceAll("(?i)" + Pattern.quote("{TileMatrix}"), matrixId);
    path = path.replaceAll("(?i)" + Pattern.quote("{TileRow}"), String.valueOf(row));
    path = path.replaceAll("(?i)" + Pattern.quote("{TileCol}"), String.valueOf(col));
    path = path.replaceAll("(?i)" + Pattern.quote("{style}"), style);
    path = path.replaceAll("(?i)" + Pattern.quote("{Layer}"), layer);

    return new URI(path);
  }

  URI createKVPUri(
      final URI commonURI, final int row, final int col, final String matrixIdentifier) {
    final Multimap<String, String> queryParams = URIUtils.getParameters(commonURI);
    queryParams.put("SERVICE", "WMTS");
    queryParams.put("REQUEST", "GetTile");
    queryParams.put("VERSION", version);
    queryParams.put("LAYER", layer);
    queryParams.put("STYLE", style);
    queryParams.put("TILEMATRIXSET", matrixSet);
    queryParams.put("TILEMATRIX", matrixIdentifier);
    queryParams.put("TILEROW", String.valueOf(row));
    queryParams.put("TILECOL", String.valueOf(col));
    queryParams.put("FORMAT", imageFormat);
    if (dimensions != null) {
      for (String d : dimensions) {
        String dimensionValue = dimensionParams.optString(d);
        if (dimensionValue == null) {
          dimensionValue = dimensionParams.getString(d.toUpperCase());
        }
        queryParams.put(d, dimensionValue);
      }
    }
    return URIUtils.setQueryParams(commonURI, queryParams);
  }
}
