package org.mapfish.print.map.geotools.grid;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.Constants;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.attribute.map.BBoxMapBounds;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.geotools.FeatureSourceSupplier;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.Dimension;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mapfish.print.map.geotools.grid.PointGridStrategy.TEXT_DISPLACEMENT;

/**
 * @author Jesse on 6/29/2015.
 */
public class PointGridStrategyTest extends AbstractMapfishSpringTest {
    @Autowired
    private ConfigurationFactory configurationFactory;
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
        MapfishMapContext context = new MapfishMapContext(bounds, mapSize, rotation, dpi, Constants.PDF_DPI, null, true);

        FeatureSourceSupplier supplier = pointGridStrategy.createFeatureSource(template, layerData);
        SimpleFeatureSource featureSource = (SimpleFeatureSource) supplier.load(requestFactory, context);
        assertEquals(16, featureSource.getFeatures().size());

        SimpleFeatureIterator features = featureSource.getFeatures().features();
        List<Coordinate> expectedPoints = Lists.newArrayList(
                new Coordinate(200,200 + TEXT_DISPLACEMENT),new Coordinate(300,200 + TEXT_DISPLACEMENT),new Coordinate(400,200 + TEXT_DISPLACEMENT),
                new Coordinate(100 + TEXT_DISPLACEMENT,400),new Coordinate(200,400),new Coordinate(300,400),new Coordinate(400,400),new Coordinate(500 - TEXT_DISPLACEMENT, 400),
                new Coordinate(100 + TEXT_DISPLACEMENT,600),new Coordinate(200,600),new Coordinate(300,600),new Coordinate(400,600),new Coordinate(500 - TEXT_DISPLACEMENT, 600),
                new Coordinate(200,800 - TEXT_DISPLACEMENT),new Coordinate(300,800 - TEXT_DISPLACEMENT),new Coordinate(400,800 - TEXT_DISPLACEMENT)
                );
        while (features.hasNext()) {
            SimpleFeature next = features.next();
            assertTrue(next.getDefaultGeometry().getClass().getName(), next.getDefaultGeometry() instanceof Point);
            Coordinate coord = ((Point) next.getDefaultGeometry()).getCoordinate();
            assertTrue(coord + " is not one of the expected points", expectedPoints.contains(coord));

            String label = (String) next.getAttribute(Constants.Style.Grid.ATT_LABEL);
            if (coord.x == 100 + TEXT_DISPLACEMENT || coord.x == 500 - TEXT_DISPLACEMENT) {
                assertEquals(GridType.createLabel(coord.y, "m"), label);
            } else if (coord.y == 200 + TEXT_DISPLACEMENT || coord.y == 800 - TEXT_DISPLACEMENT) {
                assertEquals(GridType.createLabel(coord.x, "m"), label);
            } else {
                assertEquals("", label);
            }
        }
        features.close();
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
        MapfishMapContext context = new MapfishMapContext(bounds, mapSize, rotation, dpi, Constants.PDF_DPI, null, true);

        FeatureSourceSupplier supplier = pointGridStrategy.createFeatureSource(template, layerData);
        SimpleFeatureSource featureSource = (SimpleFeatureSource) supplier.load(requestFactory, context);

        SimpleFeatureIterator features = featureSource.getFeatures().features();

        List<Coordinate> expectedPoints = Lists.newArrayList(
                new Coordinate(25, 30), new Coordinate(35, 30),
                new Coordinate(20, 35), new Coordinate(25, 35), new Coordinate(35, 35), new Coordinate(45, 35),
                new Coordinate(20, 50), new Coordinate(25, 50), new Coordinate(35, 50), new Coordinate(45, 50),
                new Coordinate(20, 65), new Coordinate(25, 65), new Coordinate(35, 65), new Coordinate(45, 65),
                new Coordinate(25, 70), new Coordinate(35, 70)
                );
        while (features.hasNext()) {
            SimpleFeature next = features.next();
            assertTrue(next.getDefaultGeometry().getClass().getName(), next.getDefaultGeometry() instanceof Point);
            Coordinate coord = ((Point) next.getDefaultGeometry()).getCoordinate();
            assertTrue(coord + " is not one of the expected points", expectedPoints.contains(coord));

            String label = (String) next.getAttribute(Constants.Style.Grid.ATT_LABEL);
            if (coord.x == 10 + TEXT_DISPLACEMENT || coord.x == 55 - TEXT_DISPLACEMENT) {
                assertEquals(GridType.createLabel(coord.y, "m"), label);
            } else if (coord.y == 20 + TEXT_DISPLACEMENT || coord.y == 80 - TEXT_DISPLACEMENT) {
                assertEquals(GridType.createLabel(coord.x, "m"), label);
            } else {
                assertEquals("" + coord.x + "," + coord.y, "", label);
            }

        }
        features.close();
        assertEquals(expectedPoints.size(), featureSource.getFeatures().size());
    }


}