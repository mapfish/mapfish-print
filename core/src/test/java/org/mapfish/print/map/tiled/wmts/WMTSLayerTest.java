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
}
