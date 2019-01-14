package org.mapfish.print.map.geotools.grid;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.attribute.map.BBoxMapBounds;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.geotools.FeatureSourceSupplier;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PointGridStrategyTest extends AbstractMapfishSpringTest {
    @Autowired
    private TestHttpClientFactory requestFactory;

    @Test
    public void testNumLines() throws Exception {
        Configuration config = new Configuration();
        Template template = new Template();
        template.setConfiguration(config);

        PointGridStrategy pointGridStrategy = new PointGridStrategy();
        GridParam layerData = new GridParam();
        layerData.numberOfLines = new int[]{3, 2};
        layerData.name = "grid";
        layerData.postConstruct();

        MapBounds bounds = new BBoxMapBounds(DefaultEngineeringCRS.GENERIC_2D, 100, 200, 500, 800);
        Dimension mapSize = new Dimension(400, 600);
        double rotation = 0;
        double dpi = 72;
        MapfishMapContext context = new MapfishMapContext(bounds, mapSize, rotation, dpi, true, true);

        FeatureSourceSupplier supplier =
                pointGridStrategy.createFeatureSource(template, layerData, new LabelPositionCollector());
        SimpleFeatureSource featureSource = (SimpleFeatureSource) supplier.load(requestFactory, context);
        assertEquals(6, featureSource.getFeatures().size());

        try (SimpleFeatureIterator features = featureSource.getFeatures().features()) {
            List<Coordinate> expectedPoints = Arrays.asList(
                    new Coordinate(200, 400), new Coordinate(300, 400), new Coordinate(400, 400),
                    new Coordinate(200, 600), new Coordinate(300, 600), new Coordinate(400, 600)
            );
            while (features.hasNext()) {
                SimpleFeature next = features.next();
                assertTrue(next.getDefaultGeometry().getClass().getName(),
                           next.getDefaultGeometry() instanceof Point);
                Coordinate coord = ((Point) next.getDefaultGeometry()).getCoordinate();
                assertTrue(coord + " is not one of the expected points", expectedPoints.contains(coord));
            }
        }
    }

    @Test
    public void testParseSpacingAndOrigin() throws Exception {
        Configuration config = new Configuration();
        Template template = new Template();
        template.setConfiguration(config);

        PointGridStrategy pointGridStrategy = new PointGridStrategy();
        GridParam layerData = new GridParam();
        layerData.spacing = new double[]{10, 15};
        layerData.origin = new double[]{5, 5};
        layerData.postConstruct();


        MapBounds bounds = new BBoxMapBounds(DefaultEngineeringCRS.GENERIC_2D, 10, 20, 55, 80);
        Dimension mapSize = new Dimension(400, 600);
        double rotation = 0;
        double dpi = 72;
        MapfishMapContext context = new MapfishMapContext(bounds, mapSize, rotation, dpi, true, true);

        FeatureSourceSupplier supplier =
                pointGridStrategy.createFeatureSource(template, layerData, new LabelPositionCollector());
        SimpleFeatureSource featureSource = (SimpleFeatureSource) supplier.load(requestFactory, context);

        try (SimpleFeatureIterator features = featureSource.getFeatures().features()) {
            List<Coordinate> expectedPoints = Arrays.asList(
                    new Coordinate(15, 35), new Coordinate(25, 35), new Coordinate(35, 35),
                    new Coordinate(45, 35),
                    new Coordinate(15, 50), new Coordinate(25, 50), new Coordinate(35, 50),
                    new Coordinate(45, 50),
                    new Coordinate(15, 65), new Coordinate(25, 65), new Coordinate(35, 65),
                    new Coordinate(45, 65)
            );

            assertEquals(expectedPoints.size(), featureSource.getFeatures().size());
            while (features.hasNext()) {
                SimpleFeature next = features.next();
                assertTrue(next.getDefaultGeometry().getClass().getName(),
                           next.getDefaultGeometry() instanceof Point);
                Coordinate coord = ((Point) next.getDefaultGeometry()).getCoordinate();
                assertTrue(coord + " is not one of the expected points", expectedPoints.contains(coord));
            }
            assertEquals(expectedPoints.size(), featureSource.getFeatures().size());
        }

    }


}
