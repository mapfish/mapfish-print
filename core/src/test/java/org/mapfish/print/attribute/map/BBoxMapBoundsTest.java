package org.mapfish.print.attribute.map;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Rectangle;

import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
import static org.junit.Assert.assertEquals;
import static org.mapfish.print.attribute.map.CenterScaleMapBoundsTest.CH1903;

public class BBoxMapBoundsTest {
    public static final CoordinateReferenceSystem SPHERICAL_MERCATOR;

    static {
        try {
            SPHERICAL_MERCATOR = CRS.decode("EPSG:3857");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testToReferencedEnvelope() {
        final BBoxMapBounds bboxMapBounds = new BBoxMapBounds(WGS84, -180, -90, 180, 90);

        final ReferencedEnvelope envelope = bboxMapBounds.toReferencedEnvelope(new Rectangle(10, 5));


        assertEquals(-180, envelope.getMinX(), 0.001);
        assertEquals(180, envelope.getMaxX(), 0.001);
        assertEquals(-90, envelope.getMinY(), 0.001);
        assertEquals(90, envelope.getMaxY(), 0.001);
        assertEquals(WGS84, envelope.getCoordinateReferenceSystem());
    }

    @Test
    public void testAdjustedEnvelope() {
        final BBoxMapBounds bboxMapBounds = new BBoxMapBounds(WGS84, -10, -90, 10, 90);
        final MapBounds mapBounds = bboxMapBounds.adjustedEnvelope(new Rectangle(5, 5));
        final ReferencedEnvelope envelope = mapBounds.toReferencedEnvelope(new Rectangle(5, 5));

        assertEquals(-90, envelope.getMinX(), 0.001);
        assertEquals(90, envelope.getMaxX(), 0.001);
        assertEquals(-90, envelope.getMinY(), 0.001);
        assertEquals(90, envelope.getMaxY(), 0.001);
        assertEquals(WGS84, envelope.getCoordinateReferenceSystem());
    }

    @Test
    public void testAdjustToScale() {
        int scaleDenominator = 24000;
        double dpi = 100;
        Rectangle screen = new Rectangle(100, 100);
        ZoomLevels zoomLevels = new ZoomLevels(15000, 20000, 25000, 30000, 350000);

        final CenterScaleMapBounds mapBounds = new CenterScaleMapBounds(CH1903, 50000, 50000,
                                                                        scaleDenominator);
        final ReferencedEnvelope originalBBox = mapBounds.toReferencedEnvelope(screen);

        BBoxMapBounds linear = new BBoxMapBounds(CH1903,
                                                 originalBBox.getMinX(), originalBBox.getMinY(),
                                                 originalBBox.getMaxX(), originalBBox.getMaxY());

        final MapBounds newMapBounds = linear.adjustBoundsToNearestScale(zoomLevels, 0.05,
                                                                         ZoomLevelSnapStrategy.CLOSEST_LOWER_SCALE_ON_TIE,
                                                                         false, screen, dpi);
        ReferencedEnvelope newBBox = newMapBounds.toReferencedEnvelope(screen);

        final double delta = 0.00001;
        assertEquals(originalBBox.getMedian(0), newBBox.getMedian(0), delta);
        assertEquals(originalBBox.getMedian(1), newBBox.getMedian(1), delta);

        double expectedScale = 25000;
        CenterScaleMapBounds expectedMapBounds = new CenterScaleMapBounds(CH1903,
                                                                          originalBBox.centre().x,
                                                                          originalBBox.centre().y,
                                                                          expectedScale);
        assertEquals(expectedMapBounds.toReferencedEnvelope(screen), newBBox);
    }

    @Test
    public void testAdjustToScaleLatLong() {
        int scaleDenominator = 24000;
        double dpi = 100;
        Rectangle screen = new Rectangle(100, 100);
        ZoomLevels zoomLevels = new ZoomLevels(15000, 20000, 25000, 30000, 350000);


        final CenterScaleMapBounds mapBounds = new CenterScaleMapBounds(SPHERICAL_MERCATOR, 5, 5,
                                                                        scaleDenominator);
        final ReferencedEnvelope originalBBox = mapBounds.toReferencedEnvelope(screen);

        BBoxMapBounds linear = new BBoxMapBounds(SPHERICAL_MERCATOR,
                                                 originalBBox.getMinX(), originalBBox.getMinY(),
                                                 originalBBox.getMaxX(), originalBBox.getMaxY());

        final MapBounds newMapBounds = linear.adjustBoundsToNearestScale(zoomLevels, 0.05,
                                                                         ZoomLevelSnapStrategy.CLOSEST_LOWER_SCALE_ON_TIE,
                                                                         false,
                                                                         screen, dpi);
        ReferencedEnvelope newBBox = newMapBounds.toReferencedEnvelope(screen);

        final double delta = 0.00001;
        assertEquals(originalBBox.getMedian(0), newBBox.getMedian(0), delta);
        assertEquals(originalBBox.getMedian(1), newBBox.getMedian(1), delta);

        double expectedScale = 25000;
        CenterScaleMapBounds expectedMapBounds = new CenterScaleMapBounds(SPHERICAL_MERCATOR,
                                                                          originalBBox.centre().x,
                                                                          originalBBox.centre().y,
                                                                          expectedScale);
        assertEquals(expectedMapBounds.getScale(screen, dpi), newMapBounds.getScale(screen, dpi));
        assertEquals(expectedMapBounds.getCenter(), newMapBounds.getCenter());
        assertEquals(expectedMapBounds.toReferencedEnvelope(screen), newBBox);
    }

    @Test
    public void testZoomOut() {
        BBoxMapBounds bboxMapBounds = new BBoxMapBounds(WGS84, -10, -10, 10, 10);
        MapBounds bounds = bboxMapBounds.zoomOut(1);
        assertEquals(bboxMapBounds, bounds);

        bounds = bboxMapBounds.zoomOut(2);
        ReferencedEnvelope envelope = bounds.toReferencedEnvelope(new Rectangle(5, 5));

        assertEquals(-20, envelope.getMinX(), 0.001);
        assertEquals(20, envelope.getMaxX(), 0.001);
        assertEquals(-20, envelope.getMinY(), 0.001);
        assertEquals(20, envelope.getMaxY(), 0.001);
        assertEquals(WGS84, envelope.getCoordinateReferenceSystem());

        bboxMapBounds = new BBoxMapBounds(WGS84, -10, -5, 10, 5);
        bounds = bboxMapBounds.zoomOut(1);
        assertEquals(bboxMapBounds, bounds);

        bounds = bboxMapBounds.zoomOut(4);
        envelope = bounds.toReferencedEnvelope(new Rectangle(40, 20));

        assertEquals(-40, envelope.getMinX(), 0.001);
        assertEquals(40, envelope.getMaxX(), 0.001);
        assertEquals(-20, envelope.getMinY(), 0.001);
        assertEquals(20, envelope.getMaxY(), 0.001);
        assertEquals(WGS84, envelope.getCoordinateReferenceSystem());
    }

    @Test
    public void testAdjustToGeodeticScale() {
        int scaleDenominator = 24000;
        double dpi = 100;
        Rectangle screen = new Rectangle(100, 100);
        ZoomLevels zoomLevels = new ZoomLevels(15000, 20000, 25000, 30000, 350000);

        final CenterScaleMapBounds mapBounds = new CenterScaleMapBounds(SPHERICAL_MERCATOR,
                                                                        400000, 5000000, scaleDenominator);
        final ReferencedEnvelope originalBBox = mapBounds.toReferencedEnvelope(screen);

        BBoxMapBounds linear = new BBoxMapBounds(SPHERICAL_MERCATOR,
                                                 originalBBox.getMinX(), originalBBox.getMinY(),
                                                 originalBBox.getMaxX(), originalBBox.getMaxY());

        final MapBounds newMapBounds = linear.adjustBoundsToNearestScale(zoomLevels, 0.005,
                                                                         ZoomLevelSnapStrategy.CLOSEST_LOWER_SCALE_ON_TIE,
                                                                         true, screen, dpi);
        ReferencedEnvelope newBBox = newMapBounds.toReferencedEnvelope(screen);

        final double delta = 0.00001;
        assertEquals(originalBBox.getMedian(0), newBBox.getMedian(0), delta);
        assertEquals(originalBBox.getMedian(1), newBBox.getMedian(1), delta);

        assertEquals(20000,
                     newMapBounds.getScale(screen, dpi).getGeodeticDenominator(
                             SPHERICAL_MERCATOR, dpi, newBBox.centre()),
                     1);
        assertEquals(26428, newMapBounds.getScale(screen, dpi).getDenominator(dpi), 1);
        assertEquals(399664, newBBox.getMinX(), 1);
        assertEquals(4999664, newBBox.getMinY(), 1);
        assertEquals(400335, newBBox.getMaxX(), 1);
        assertEquals(5000335, newBBox.getMaxY(), 1);
    }

    @Test
    public void testExpandHorizontalFeatureInVerticalMap() {
        final BBoxMapBounds bounds = new BBoxMapBounds(CH1903, 300000, 500000, 300200, 500100);

        // a margin of 50px on an area of 200*300px should divide by two the scale
        final Rectangle paintArea = new Rectangle(200, 300);
        final BBoxMapBounds expandedBounds = (BBoxMapBounds) bounds.expand(50, paintArea);

        assertEquals(2.0, expandedBounds.toReferencedEnvelope(null).getWidth() /
                expandedBounds.toReferencedEnvelope(null).getHeight(), 0.01);


        assertEquals(bounds.toReferencedEnvelope(null).getWidth() * 2.0,
                     expandedBounds.toReferencedEnvelope(null).getWidth(), 0.01);

        assertEquals(bounds.toReferencedEnvelope(null).getHeight() * 2.0,
                     expandedBounds.toReferencedEnvelope(null).getHeight(), 0.01);

        final BBoxMapBounds adjusted = (BBoxMapBounds) expandedBounds.adjustedEnvelope(paintArea);

        assertEquals(expandedBounds.toReferencedEnvelope(paintArea).getWidth(),
                     adjusted.toReferencedEnvelope(paintArea).getWidth(), 0.01);

        assertEquals(adjusted.toReferencedEnvelope(paintArea).getWidth() /
                             adjusted.toReferencedEnvelope(paintArea).getHeight(),
                     paintArea.getWidth() / paintArea.getHeight(), 0.01);
    }

    @Test
    public void testExpandVerticalFeatureInHorizontalMap() {
        final BBoxMapBounds bounds = new BBoxMapBounds(CH1903, 300000, 500000, 300100, 500200);

        // a margin of 50px on an area of 200*200px should divide by 1.5 the scale
        final Rectangle paintArea = new Rectangle(300, 200);
        final BBoxMapBounds expandedBounds = (BBoxMapBounds) bounds.expand(50, paintArea);

        assertEquals(0.5, expandedBounds.toReferencedEnvelope(null).getWidth() /
                expandedBounds.toReferencedEnvelope(null).getHeight(), 0.01);

        assertEquals(bounds.toReferencedEnvelope(null).getWidth() * 2.0,
                     expandedBounds.toReferencedEnvelope(null).getWidth(), 0.01);

        assertEquals(bounds.toReferencedEnvelope(null).getHeight() * 2.0,
                     expandedBounds.toReferencedEnvelope(null).getHeight(), 0.01);

        final BBoxMapBounds adjusted = (BBoxMapBounds) expandedBounds.adjustedEnvelope(paintArea);

        assertEquals(expandedBounds.toReferencedEnvelope(paintArea).getHeight(),
                     adjusted.toReferencedEnvelope(paintArea).getHeight(), 0.01);

        assertEquals(adjusted.toReferencedEnvelope(paintArea).getWidth() /
                             adjusted.toReferencedEnvelope(paintArea).getHeight(),
                     paintArea.getWidth() / paintArea.getHeight(), 0.01);
    }

    @Test
    public void testExpandPoint() {
        final BBoxMapBounds bounds = new BBoxMapBounds(CH1903, 300000, 500000, 300000, 500000);

        // a margin of 50px on an area of 200*200px should divide by 1.5 the scale
        final BBoxMapBounds expandedBounds = (BBoxMapBounds) bounds.expand(50, new Rectangle(200, 200));

        assertEquals(0.0, expandedBounds.toReferencedEnvelope(null).getWidth(), 0.01);
        assertEquals(0.0, expandedBounds.toReferencedEnvelope(null).getHeight(), 0.01);
    }
}
