package org.mapfish.print.map.tiled.wmts;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.mapfish.print.attribute.map.CenterScaleMapBounds;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.config.Configuration;

import java.awt.Rectangle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WMTSLayerTest {
    @Test
    public void testTileBoundsCalculation() throws Exception {
        WMTSLayerParam params = new WMTSLayerParam();
        Matrix matrix = new Matrix();
        matrix.matrixSize = new long[]{67108864, 67108864};
        matrix.tileSize = new int[]{256, 256};
        matrix.topLeftCorner = new double[]{420000, 350000};
        matrix.scaleDenominator = 7500;
        params.matrices = new Matrix[]{matrix};

        WMTSLayer wmtsLayer = new WMTSLayer(null, null, params, null,
                                            new Configuration());

        Rectangle paintArea = new Rectangle(0, 0, 256, 256);
        MapBounds bounds = new CenterScaleMapBounds(CRS.decode("EPSG:21781"),
                                                    595217.02, 236708.54, 7500);
        WMTSLayer.WMTSTileCacheInfo tileInformation =
                (WMTSLayer.WMTSTileCacheInfo) wmtsLayer.createTileInformation(bounds, paintArea, 256);

        ReferencedEnvelope tileCacheBounds = tileInformation.getTileCacheBounds();

        assertEquals(tileCacheBounds.getMinX(), 420000, 0.00001);
        assertFalse("" + tileCacheBounds.getMinX(), Double.isInfinite(tileCacheBounds.getMinX()));
        assertFalse("" + tileCacheBounds.getMinX(), Double.isNaN(tileCacheBounds.getMinX()));
        assertTrue("" + tileCacheBounds.getMinY(), tileCacheBounds.getMinY() < 350000);
        assertFalse("" + tileCacheBounds.getMinY(), Double.isInfinite(tileCacheBounds.getMinY()));
        assertFalse("" + tileCacheBounds.getMinY(), Double.isNaN(tileCacheBounds.getMinY()));
        assertTrue("" + tileCacheBounds.getMaxX(), tileCacheBounds.getMaxX() > 420000);
        assertFalse("" + tileCacheBounds.getMaxX(), Double.isInfinite(tileCacheBounds.getMaxX()));
        assertFalse("" + tileCacheBounds.getMaxX(), Double.isNaN(tileCacheBounds.getMaxX()));
        assertEquals(tileCacheBounds.getMaxY(), 350000, 0.00001);
        assertFalse("" + tileCacheBounds.getMaxY(), Double.isInfinite(tileCacheBounds.getMaxY()));
        assertFalse("" + tileCacheBounds.getMaxY(), Double.isNaN(tileCacheBounds.getMaxY()));

    }

    @Test
    public void testCreateRestURI() throws Exception {
        WMTSLayerParam param = new WMTSLayerParam();
        param.layer = "wmts_layer";
        param.matrixSet = "matrix_set";
        param.baseURL =
                "http://test_server/mapproxy_4_v3/wmts/{Layer}/{TileMatrixSet}/{TileMatrix}/{TileCol" +
                        "}/{TileRow}.png";
        String restURI = WMTSLayer.createRestURI("the_matrix_id", 4, 5, param).toString();

        assertEquals("http://test_server/mapproxy_4_v3/wmts/wmts_layer/matrix_set/the_matrix_id/5/4.png",
                     restURI);
    }
}
