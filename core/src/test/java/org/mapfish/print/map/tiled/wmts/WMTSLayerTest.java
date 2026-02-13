package org.mapfish.print.map.tiled.wmts;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Rectangle;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.jupiter.api.Test;
import org.mapfish.print.attribute.map.CenterScaleMapBounds;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.config.Configuration;

public class WMTSLayerTest {
  @Test
  public void testTileBoundsCalculation() throws Exception {
    WMTSLayerParam params = new WMTSLayerParam();
    Matrix matrix = new Matrix();
    matrix.matrixSize = new long[] {67108864, 67108864};
    matrix.tileSize = new int[] {256, 256};
    matrix.topLeftCorner = new double[] {420000, 350000};
    matrix.scaleDenominator = 7500;
    params.matrices = new Matrix[] {matrix};

    WMTSLayer wmtsLayer = new WMTSLayer(null, null, params, null, new Configuration());

    Rectangle paintArea = new Rectangle(0, 0, 256, 256);
    MapBounds bounds =
        new CenterScaleMapBounds(CRS.decode("EPSG:21781"), 595217.02, 236708.54, 7500);
    WMTSLayer.WMTSTileInfo tileInformation =
        (WMTSLayer.WMTSTileInfo) wmtsLayer.createTileInformation(bounds, paintArea, 256);

    ReferencedEnvelope tileBounds = tileInformation.getTileBounds();

    assertEquals(420000, tileBounds.getMinX(), 0.00001);
    assertFalse(Double.isInfinite(tileBounds.getMinX()), "" + tileBounds.getMinX());
    assertFalse(Double.isNaN(tileBounds.getMinX()), "" + tileBounds.getMinX());
    assertTrue(tileBounds.getMinY() < 350000, "" + tileBounds.getMinY());
    assertFalse(Double.isInfinite(tileBounds.getMinY()), "" + tileBounds.getMinY());
    assertFalse(Double.isNaN(tileBounds.getMinY()), "" + tileBounds.getMinY());
    assertTrue(tileBounds.getMaxX() > 420000, "" + tileBounds.getMaxX());
    assertFalse(Double.isInfinite(tileBounds.getMaxX()), "" + tileBounds.getMaxX());
    assertFalse(Double.isNaN(tileBounds.getMaxX()), "" + tileBounds.getMaxX());
    assertEquals(350000, tileBounds.getMaxY(), 0.00001);
    assertFalse(Double.isInfinite(tileBounds.getMaxY()), "" + tileBounds.getMaxY());
    assertFalse(Double.isNaN(tileBounds.getMaxY()), "" + tileBounds.getMaxY());
  }

  @Test
  public void testCreateRestURI() throws Exception {
    WMTSLayerParam param = new WMTSLayerParam();
    param.layer = "wmts_layer";
    param.matrixSet = "matrix_set";
    param.baseURL =
        "http://test_server/mapproxy_4_v3/wmts/{Layer}/{TileMatrixSet}/{TileMatrix}/{TileCol"
            + "}/{TileRow}.png";
    String restURI = param.createRestURI("the_matrix_id", 4, 5).toString();

    assertEquals(
        "http://test_server/mapproxy_4_v3/wmts/wmts_layer/matrix_set/the_matrix_id/5/4.png",
        restURI);
  }

  @Test
  public void testCreateRestURIMixedCase() throws Exception {
    WMTSLayerParam param = new WMTSLayerParam();
    param.layer = "wmts_layer";
    param.matrixSet = "matrix_set";
    param.style = "default";
    param.baseURL =
        "http://test_server/literal/style/tilematrixset/mapproxy_4_v3/wmts/"
            + "{LaYer}/{style}/{tilematrixset}/{TILEMATRIX}/{TileCol}/"
            + "{TileRow}.png";
    String restURI = param.createRestURI("the_matrix_id", 4, 5).toString();

    assertEquals(
        "http://test_server/literal/style/tilematrixset/mapproxy_4_v3/wmts/wmts_layer/default/matrix_set/the_matrix_id/5/4.png",
        restURI);
  }

  @Test
  public void testComputeExtraScalingFactorPseudoMercatorGeodetic() throws Exception {
    // Setup WMTSLayer with EPSG:3857 (Pseudo-Mercator)
    WMTSLayerParam params = new WMTSLayerParam();
    Matrix matrix1 = new Matrix();
    matrix1.matrixSize = new long[] {2, 2};
    matrix1.tileSize = new int[] {256, 256};
    matrix1.topLeftCorner = new double[] {-20037508.3427892, 20037508.3427892};
    matrix1.scaleDenominator = 559082264.028;
    Matrix matrix2 = new Matrix();
    matrix2.matrixSize = new long[] {4, 4};
    matrix2.tileSize = new int[] {256, 256};
    matrix2.topLeftCorner = new double[] {-20037508.3427892, 20037508.3427892};
    matrix2.scaleDenominator = 279541132.014;
    params.matrices = new Matrix[] {matrix1, matrix2};

    WMTSLayer wmtsLayer = new WMTSLayer(null, null, params, null, new Configuration());

    // Center at non-equatorial latitude (e.g., Lausanne, Switzerland)
    double centerX = 732000.0;
    double centerY = 5860000.0;
    MapBounds bounds =
        new CenterScaleMapBounds(
            CRS.decode("EPSG:3857"), centerX, centerY, 10000.0, true // useGeodeticCalculations
            );

    Rectangle paintArea = new Rectangle(0, 0, 256, 256);
    WMTSLayer.WMTSTileInfo tileInformation =
        (WMTSLayer.WMTSTileInfo) wmtsLayer.createTileInformation(bounds, paintArea, 256);

    // Access computeExtraScalingFactor via reflection (since it's private)
    java.lang.reflect.Method method =
        WMTSLayer.class.getDeclaredClasses()[0].getDeclaredMethod(
            "computeExtraScalingFactor", MapBounds.class);
    method.setAccessible(true);
    double scalingFactor = (double) method.invoke(tileInformation, bounds);

    // The scaling factor should not be 1 at non-equatorial latitude
    assertTrue(
        scalingFactor != 1.0, "Scaling factor should differ from 1 for non-equatorial latitude");
    // Should be > 1 at higher latitudes (distance per degree decreases)
    assertTrue(scalingFactor > 1.0, "Scaling factor should be > 1 at higher latitudes");
    // Should be finite and positive
    assertTrue(Double.isFinite(scalingFactor), "Scaling factor should be finite");
    assertTrue(scalingFactor > 0, "Scaling factor should be positive");
  }
}
