package org.mapfish.print.map.readers;

import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.MapTestBasic;
import org.mapfish.print.utils.PJsonObject;
import org.pvalsecc.misc.FileUtilities;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Basic testing of reader.
 * <p/>
 * Created by Jesse on 12/19/13.
 */
public class WMTSMapReaderTest extends MapTestBasic {

    private PJsonObject wmtsSpec;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        wmtsSpec = MapPrinter.parseSpec(FileUtilities.readWholeTextFile(new File(TmsMapReaderTest.class.getClassLoader()
                .getResource("layers/wmts_layer_spec.json").getFile())));

    }

    @Test
    public void testRequireMatrixIdsOrTileOrigin() throws Exception {
        try {
            wmtsSpec.getInternalObj().accumulate("zoomOffset", 1);
            new WMTSMapReader.Factory().create("layer", context, wmtsSpec);
            fail("An exception should be raised");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("matrixIds") && e.getMessage().contains("tileOrigin"));
        }
    }

    @Test
    public void testRequireMatrixIdsOrZoomOffset() throws Exception {
        try {
            wmtsSpec.getInternalObj().accumulate("tileOrigin", new JSONArray(new Integer[]{0, 0}));
            new WMTSMapReader.Factory().create("layer", context, wmtsSpec);
            fail("An exception should be raised");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("matrixIds") && e.getMessage().contains("zoomOffset"));
        }
    }

    @Test
    public void testRequireMatrixIdsOrFormatSuffix() throws Exception {
        try {
            wmtsSpec.getInternalObj().accumulate("tileOrigin", new JSONArray(new Integer[]{0, 0}));
            wmtsSpec.getInternalObj().accumulate("zoomOffset", 1);
            new WMTSMapReader.Factory().create("layer", context, wmtsSpec);
            fail("An exception should be raised");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("matrixIds") && e.getMessage().contains("formatSuffix") && e.getMessage().contains("extension"));
        }
    }

    @Test
    public void testValidConfig() throws Exception {
        wmtsSpec.getInternalObj().accumulate("tileOrigin", new JSONArray(new Integer[]{0, 0}));
        wmtsSpec.getInternalObj().accumulate("zoomOffset", 1);
        wmtsSpec.getInternalObj().accumulate("extension", "png");
        WMTSMapReader reader = (WMTSMapReader) new WMTSMapReader.Factory().create("layer", context, wmtsSpec).get(0);
        assertEquals("png", reader.tileCacheLayerInfo.getExtension());
    }
}
