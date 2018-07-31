package org.mapfish.print.processor.map;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.mapfish.print.attribute.map.BBoxMapBounds;
import org.mapfish.print.attribute.map.CenterScaleMapBounds;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import static java.lang.Math.PI;
import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mapfish.print.Constants.PDF_DPI;

public class MapfishMapContextTest {

    @Test
    public void testGetRoundedScale() {
        assertRoundedScale(0.111, 0.111);
        assertRoundedScale(0.11, 0.11);
        assertRoundedScale(9, 9);
        assertRoundedScale(9.1, 9);
        assertRoundedScale(239, 240);
        assertRoundedScale(993, 990);
        assertRoundedScale(1113, 1100);
        assertRoundedScale(11130, 11000);
        assertRoundedScale(225229, 230000);
        assertRoundedScale(1234229, 1200000);
        assertRoundedScale(12342290, 12000000);
    }

    private void assertRoundedScale(double actualScaleDenominator, double roundedScale) {
        MapBounds bounds = new CenterScaleMapBounds(WGS84, 0, 0, actualScaleDenominator);
        Dimension paint = new Dimension(200, 200);
        MapfishMapContext transformer = new MapfishMapContext(bounds, paint, 90,
                                                              PDF_DPI, true, true);
        assertEquals(roundedScale, transformer.getRoundedScaleDenominator(), 0.00001);
    }

    @Test
    public void testGetRotation() {
        MapfishMapContext transformer = new MapfishMapContext(null, null, PI / 2,
                                                              PDF_DPI, null, true);
        assertEquals("converted to radians", PI / 2, transformer.getRotation(), 1e-9);
    }

    @Test
    public void testGetRotatedBounds_BBoxMapBounds() {
        MapBounds bounds = new BBoxMapBounds(WGS84, 5, 45, 25, 55);

        // rotate 90 degree
        MapfishMapContext transformer = new MapfishMapContext(bounds, null, PI / 2,
                                                              PDF_DPI, null, true);
        MapBounds rotatedBounds = new BBoxMapBounds(WGS84, 10, 40, 20, 60);
        assertEquals(rotatedBounds, transformer.getRotatedBounds());

        // rotate 180 degree
        transformer = new MapfishMapContext(bounds, null, PI, PDF_DPI,
                                            null, true);
        rotatedBounds = new BBoxMapBounds(WGS84, 5, 45, 25, 55);
        assertEquals(rotatedBounds, transformer.getRotatedBounds());

        // rotate 45 degree
        transformer = new MapfishMapContext(bounds, null, PI / 4, PDF_DPI,
                                            null, true);
        ReferencedEnvelope rotatedEnvelope =
                transformer.getRotatedBounds().toReferencedEnvelope(new Rectangle(1, 1));
        assertEquals(4.393398, rotatedEnvelope.getMinX(), 1e-6);
        assertEquals(25.606601, rotatedEnvelope.getMaxX(), 1e-6);
        assertEquals(39.393398, rotatedEnvelope.getMinY(), 1e-6);
        assertEquals(60.606601, rotatedEnvelope.getMaxY(), 1e-6);

        // rotate 45 degree
        bounds = new BBoxMapBounds(WGS84, -0.5, -0.5, 0.5, 0.5);
        transformer = new MapfishMapContext(bounds, null, PI / 4, PDF_DPI,
                                            null, true);
        rotatedEnvelope =
                transformer.getRotatedBounds().toReferencedEnvelope(new Rectangle(1, 1));
        assertEquals(-0.707106, rotatedEnvelope.getMinX(), 1e-6);
        assertEquals(0.707106, rotatedEnvelope.getMaxX(), 1e-6);
        assertEquals(-0.707106, rotatedEnvelope.getMinY(), 1e-6);
        assertEquals(0.707106, rotatedEnvelope.getMaxY(), 1e-6);
    }

    @Test
    public void testGetRotatedBounds_CenterScaleMapBounds() {
        MapBounds bounds = new CenterScaleMapBounds(WGS84, 0, 0, 1000);
        MapfishMapContext transformer = new MapfishMapContext(bounds, null, 90,
                                                              PDF_DPI, true, true);
        // nothing changes
        assertEquals(bounds, transformer.getRotatedBounds());
    }

    /**
     * Tests that the bounds are calculated correctly for rotations.
     * <p>
     * For example when printing a map at [2742033.0, 1253823.0] at scale 25 000 with a map size of 780 x
     * 330px at 100 DPI: - The map size is adapted to the higher resolution: 1083 x 458px - A bbox is
     * calculated with the center coordinate, scale and map size: [2738594.475, 1252368.85, 2745471.525,
     * 1255277.15] - The map size is rotated: 993.4847683568174 x 1150.9542483185592px (not rounded) and 993 x
     * 1151px (rounded) - The bbox is rotated: [2738878.6857910156, 1250168.7202148438,
     * 2745187.3142089844,1257477.2797851562] - And adapted to the rounding of the map size:
     * [2738880.2249305826, 1250168.5749532534, 2745185.7750694174, 1257477.4250467466]
     */
    @Test
    public void testGetRotatedBounds_AdaptedToPaintAreaRounding() throws FactoryException {
        CRSAuthorityFactory factory = CRS.getAuthorityFactory(true);
        CoordinateReferenceSystem epsg2056 = factory.createCoordinateReferenceSystem("EPSG:2056");

        MapAttribute.MapAttributeValues mapValues =
                (new MapAttribute()).new MapAttributeValues(null, 780, 330);
        mapValues.dpi = 100;
        mapValues.rotation = 55.26239249861529;
        mapValues.setMapBounds(new CenterScaleMapBounds(epsg2056, 2742033.0, 1253823.0, 25000));

        MapBounds centerBounds = mapValues.getMapBounds();
        Rectangle paintAreaRotated = new Rectangle(
                (int) Math.round(995.0 * PDF_DPI / 100.0),
                (int) Math.round(1152.0 * PDF_DPI / 100.0));
        ReferencedEnvelope optimalBbox = centerBounds.toReferencedEnvelope(paintAreaRotated);

        paintAreaRotated = new Rectangle(995, 1152);
        MapfishMapContext transformer = CreateMapProcessor.createMapContext(mapValues);
        Rectangle2D.Double paintAreaPrecise = transformer.getRotatedMapSizePrecise();
        Rectangle paintArea = new Rectangle(MapfishMapContext.rectangleDoubleToDimension(paintAreaPrecise));

        assertEquals(paintAreaRotated, paintArea);
        MapBounds rotatedBounds = transformer.getRotatedBounds(paintAreaPrecise, paintArea);
        ReferencedEnvelope adaptedBbox = rotatedBounds.toReferencedEnvelope(null);

        assertEquals(optimalBbox.getMinX(), adaptedBbox.getMinX(), 4);
        assertEquals(optimalBbox.getMaxX(), adaptedBbox.getMaxX(), 4);
        assertEquals(optimalBbox.getMinY(), adaptedBbox.getMinY(), 4);
        assertEquals(optimalBbox.getMaxY(), adaptedBbox.getMaxY(), 4);
    }

    @Test
    public void testGetRotatedMapSize() {
        // no rotation
        MapfishMapContext transformer = new MapfishMapContext(null, new Dimension(1, 1),
                                                              0, PDF_DPI, true, true);
        assertEquals(new Dimension(1, 1), transformer.getRotatedMapSize());

        // rotate 90 degree
        transformer = new MapfishMapContext(null, new Dimension(2, 1),
                                            PI / 2, PDF_DPI, true, true);
        assertEquals(new Dimension(1, 2), transformer.getRotatedMapSize());

        // rotate 180 degree
        transformer = new MapfishMapContext(null, new Dimension(2, 1), PI,
                                            PDF_DPI, true, true);
        assertEquals(new Dimension(2, 1), transformer.getRotatedMapSize());

        // rotate 45 degree
        transformer = new MapfishMapContext(null, new Dimension(100, 100), PI / 4,
                                            PDF_DPI, true, true);
        Dimension rotatedMapSize = transformer.getRotatedMapSize();
        assertEquals(141, rotatedMapSize.getWidth(), 1e-6);
        assertEquals(141, rotatedMapSize.getHeight(), 1e-6);
    }

    @Test
    public void testGetTransform() {
        MapBounds bounds = new BBoxMapBounds(WGS84, -0.5, -0.5, 0.5, 0.5);
        Dimension mapSize = new Dimension(100, 100);

        // no rotation
        MapfishMapContext transformer = new MapfishMapContext(bounds, mapSize, 0, PDF_DPI,
                                                              true, true);
        assertNull(transformer.getTransform());

        // rotate 180 degree
        transformer = new MapfishMapContext(bounds, mapSize, PI, PDF_DPI,
                                            true, true);
        AffineTransform transform = transformer.getTransform();
        assertEquals(100, transform.getTranslateX(), 1e-6);
        assertEquals(100, transform.getTranslateY(), 1e-6);

        double[] matrix = new double[6];
        transform.getMatrix(matrix);
        assertArrayEquals(new double[]{-1.0, 0.0, 0.0, -1.0, 100.0, 100.0}, matrix, 1e-6);

        // rotate 90 degree
        transformer = new MapfishMapContext(bounds, mapSize, PI / 2, PDF_DPI,
                                            true, true);
        transform = transformer.getTransform();
        assertEquals(100, transform.getTranslateX(), 1e-6);
        assertEquals(0, transform.getTranslateY(), 1e-6);

        matrix = new double[6];
        transform.getMatrix(matrix);
        assertArrayEquals(new double[]{0.0, 1.0, -1.0, -0.0, 100.0, 0.0}, matrix, 1e-6);
    }
}
