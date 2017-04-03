package org.mapfish.print.map.geotools.grid;

import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.map.Layer;
import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.junit.Test;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.attribute.map.BBoxMapBounds;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.style.StyleParser;
import org.opengis.feature.simple.SimpleFeature;

import java.awt.Dimension;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LineGridStrategyTest {

    @Test
    public void testParseSpacingAndOrigin() throws Throwable {
        final GridParam layerData = new GridParam();
        layerData.gridType = GridType.LINES;
        layerData.spacing = new double[]{20, 30};
        layerData.origin = new double[]{10, 20};
        layerData.pointsInLine = 10;


        Configuration config = new Configuration();
        Template template = new Template();
        template.setConfiguration(config);

        final GridLayerPlugin plugin = new GridLayerPlugin();
        StyleParser styleParser = new StyleParser();
        plugin.setParser(styleParser);
        final GridLayer layer = plugin.parse(template, layerData);

        MapBounds bounds = new BBoxMapBounds(DefaultEngineeringCRS.GENERIC_2D, 100, 100, 140, 140);
        Dimension mapSize = new Dimension(400, 400);
        double rotation = 0;
        double dpi = 72;
        MapfishMapContext context = new MapfishMapContext(bounds, mapSize, rotation, dpi, true, true);
        final List<? extends Layer> layers = layer.getLayers(new TestHttpClientFactory(), context, "test");
        assertEquals(1, layers.size());

        FeatureSource<?, ?> fs = layers.get(0).getFeatureSource();
        assertNotNull(layers.get(0).getStyle());

        final SimpleFeatureCollection features = (SimpleFeatureCollection) fs.getFeatures();
        assertEquals(3, features.size());

        Map<String, SimpleFeature> idToFeature = idToFeatureMap(features);
        checkFeature(layerData, idToFeature.get("grid.y.1"),
                new Coordinate(100, 110), new Coordinate(140, 110));
        checkFeature(layerData, idToFeature.get("grid.x.1"),
                new Coordinate(110, 100), new Coordinate(110, 140));
        checkFeature(layerData, idToFeature.get("grid.x.2"),
                new Coordinate(130, 100), new Coordinate(130, 140));
    }

    @Test
    public void testParseNumLines() throws Throwable {
        final GridParam layerData = new GridParam();
        layerData.gridType = GridType.LINES;
        layerData.numberOfLines = new int[]{3, 2};
        layerData.pointsInLine = 10;

        Configuration config = new Configuration();
        Template template = new Template();
        template.setConfiguration(config);

        final GridLayerPlugin plugin = new GridLayerPlugin();
        StyleParser styleParser = new StyleParser();
        plugin.setParser(styleParser);
        final GridLayer layer = plugin.parse(template, layerData);

        MapBounds bounds = new BBoxMapBounds(DefaultEngineeringCRS.GENERIC_2D, 110, 90, 150, 132);
        Dimension mapSize = new Dimension(400, 400);
        double rotation = 0;
        double dpi = 72;
        MapfishMapContext context = new MapfishMapContext(bounds, mapSize, rotation, dpi, true, true);
        final List<? extends Layer> layers = layer.getLayers(new TestHttpClientFactory(), context, "test");
        assertEquals(1, layers.size());

        FeatureSource<?, ?> fs = layers.get(0).getFeatureSource();
        assertNotNull(layers.get(0).getStyle());

        final SimpleFeatureCollection features = (SimpleFeatureCollection) fs.getFeatures();
        assertEquals(5, features.size());

        Map<String, SimpleFeature> idToFeature = idToFeatureMap(features);
        checkFeature(layerData, idToFeature.get("grid.x.1"), new Coordinate(120, 90), new Coordinate(120, 132));
        checkFeature(layerData, idToFeature.get("grid.x.2"), new Coordinate(130, 90), new Coordinate(130, 132));
        checkFeature(layerData, idToFeature.get("grid.x.3"), new Coordinate(140, 90), new Coordinate(140, 132));
        checkFeature(layerData, idToFeature.get("grid.y.1"), new Coordinate(110, 104), new Coordinate(150, 104));
        checkFeature(layerData, idToFeature.get("grid.y.2"), new Coordinate(110, 118), new Coordinate(150, 118));
    }

    private Map<String, SimpleFeature> idToFeatureMap(SimpleFeatureCollection features) {
        Map<String, SimpleFeature> result = Maps.newHashMap();

        final SimpleFeatureIterator features1 = features.features();
        while (features1.hasNext()) {
            final SimpleFeature feature = features1.next();
            result.put(feature.getID(), feature);
        }
        return result;
    }

    private void checkFeature(GridParam layerData, SimpleFeature f1, Coordinate minCoord, Coordinate maxCoord) {
        LineString defaultGeometry = (LineString) f1.getDefaultGeometry();
        assertEquals(layerData.pointsInLine + 1, defaultGeometry.getCoordinates().length);
        assertEquals(minCoord, defaultGeometry.getCoordinates()[0]);
        assertEquals(maxCoord, defaultGeometry.getCoordinates()[10]);
    }
}
