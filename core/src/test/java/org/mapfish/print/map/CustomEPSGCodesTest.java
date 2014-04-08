package org.mapfish.print.map;

import junit.framework.TestCase;
import org.geotools.referencing.CRS;

/**
 * @author Jesse on 4/8/2014.
 */
public class CustomEPSGCodesTest extends TestCase

{
    public void testLookup900913() throws Exception {
        CRS.decode("EPSG:900913");

        // no error is a pass

    }
}
