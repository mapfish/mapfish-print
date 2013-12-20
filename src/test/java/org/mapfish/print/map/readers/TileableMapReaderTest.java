package org.mapfish.print.map.readers;

import org.junit.Test;
import org.mapfish.print.Transformer;
import org.mapfish.print.utils.DistanceUnit;

/**
 * Created by Jesse on 12/20/13.
 */
public class TileableMapReaderTest {
    @Test
    public void testFixTiledTransformer() throws Exception {

    }


    @Test
    public void testFixScale() throws Exception {
        float centerX = 430552.3f;
        float centerY = 265431.9f;
        float paperWidth = 440.0f;
        float paperHeight = 483.0f;
        int scale = 75000;
        int dpi = 300;
        DistanceUnit unitEnum = DistanceUnit.fromString("m");
        float rotation = 0.0f;
        String geodeticSRS = null;
        boolean isIntegerSvg = true;

        final Transformer transformer = new Transformer(centerX, centerY, paperWidth, paperHeight, scale, dpi, unitEnum, rotation,
                geodeticSRS, isIntegerSvg);

//        transformer.`
    }
}
