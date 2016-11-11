package org.mapfish.print.map;

import junit.framework.TestCase;
import org.geotools.referencing.CRS;

public class CustomEPSGCodesTest extends TestCase

{
    public void testLookup900913() throws Exception {
        CRS.decode("EPSG:900913");

        // no error is a pass

    }
}
