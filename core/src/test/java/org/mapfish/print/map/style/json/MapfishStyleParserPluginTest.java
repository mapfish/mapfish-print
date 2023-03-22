package org.mapfish.print.map.style.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mapfish.print.AbstractMapfishSpringTest.parseJSONObjectFromFile;
import static org.mapfish.print.map.style.json.JsonStyleParserHelperTest.valueOf;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.xml.styling.SLDTransformer;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.mapfish.print.Constants;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

public class MapfishStyleParserPluginTest {
  static final String REQUEST_DATA_STYLE_JSON_V1_STYLE_JSON =
      "requestData-style-json-v1-style.json";

  private static final double DELTA = 0.000001;
  final MapfishStyleParserPlugin mapfishStyleParserPlugin = new MapfishStyleParserPlugin();
  final TestHttpClientFactory httpClient = new TestHttpClientFactory();

  final SLDTransformer transformer = new SLDTransformer();
  MapfishStyleParserPlugin parser = new MapfishStyleParserPlugin();

  @Test
  public void testVersion1StyleParser() throws Throwable {
    PJsonObject layerJson =
        parseJSONObjectFromFile(
            MapfishStyleParserPluginTest.class, "bug_cant_transform_to_xml.json");

    Optional<Style> style =
        parser.parseStyle(null, new TestHttpClientFactory(), layerJson.getString("style"));
    assertTrue(style.isPresent());

    transformer.transform(style.get()); // assert it can be converted to SLD
  }

  @Test
  public void testVersion1() throws Throwable {
    PJsonObject layerJson =
        parseJSONObjectFromFile(
            MapfishStyleParserPluginTest.class, REQUEST_DATA_STYLE_JSON_V1_STYLE_JSON);
    Optional<Style> style =
        parser.parseStyle(null, new TestHttpClientFactory(), layerJson.getString("style"));
    assertTrue(style.isPresent());

    transformer.transform(style.get()); // assert it can be converted to SLD

    final List<Rule> rules = new ArrayList<>();
    style
        .get()
        .accept(
            new AbstractStyleVisitor() {
              @Override
              public void visit(Rule rule) {
                rules.add(rule);
              }
            });
    assertEquals(4, rules.size());

    PointSymbolizer point = null;
    LineSymbolizer line = null;
    PolygonSymbolizer polygon = null;
    TextSymbolizer text = null;

    for (Rule rule : rules) {
      Filter geomSelectFunction = null;
      if (!(rule.getSymbolizers()[0] instanceof TextSymbolizer)) {
        assertTrue(rule.getFilter() instanceof And);
        And andFilter = (And) rule.getFilter();
        assertEquals(2, andFilter.getChildren().size());

        PropertyIsEqualTo filter = (PropertyIsEqualTo) andFilter.getChildren().get(0);
        PropertyName propertyName = (PropertyName) filter.getExpression1();
        assertEquals("_gx_style", propertyName.getPropertyName());
        Literal valueExpression = (Literal) filter.getExpression2();
        assertEquals("1", valueExpression.getValue());

        geomSelectFunction = andFilter.getChildren().get(1);
      }

      final List<Symbolizer> symbolizers = rule.symbolizers();

      assertEquals(1, symbolizers.size());

      for (Symbolizer symbolizer : symbolizers) {
        if (symbolizer instanceof PointSymbolizer) {
          assertEquals("1_Point", rule.getName());

          assertNull(point);
          point = (PointSymbolizer) symbolizer;
          assertFilter(geomSelectFunction, Point.class, MultiPoint.class, GeometryCollection.class);
        } else if (symbolizer instanceof LineSymbolizer) {
          assertEquals("1_LineString", rule.getName());
          assertNull(line);
          line = (LineSymbolizer) symbolizer;
          assertFilter(
              geomSelectFunction,
              LineString.class,
              LinearRing.class,
              MultiLineString.class,
              GeometryCollection.class);
        } else if (symbolizer instanceof PolygonSymbolizer) {
          assertEquals("1_Polygon", rule.getName());
          assertNull(polygon);
          polygon = (PolygonSymbolizer) symbolizer;
          assertFilter(
              geomSelectFunction, Polygon.class, MultiPolygon.class, GeometryCollection.class);
        } else if (symbolizer instanceof TextSymbolizer) {
          assertEquals("1_Text", rule.getName());
          assertNull(text);
          text = (TextSymbolizer) symbolizer;
        } else {
          fail(symbolizer + " was unexpected");
        }
      }
    }

    assertNotNull(point);
    assertNotNull(line);
    assertNotNull(polygon);
    assertNotNull(text);
  }

  @SafeVarargs
  private final void assertFilter(
      Filter geomSelectFunction, Class<? extends Geometry>... geomClasses) {

    List<Class<? extends Geometry>> allowed = Arrays.asList(geomClasses);

    final List<Class<? extends Geometry>> allGeomTypes =
        Arrays.asList(
            Point.class,
            MultiPoint.class,
            LineString.class,
            LinearRing.class,
            MultiLineString.class,
            Polygon.class,
            MultiPolygon.class,
            GeometryCollection.class);

    for (Class<? extends Geometry> geomType : allGeomTypes) {
      final SimpleFeature feature =
          createFeature(geomType, MapfishJsonStyleVersion1.DEFAULT_GEOM_ATT_NAME);
      assertEquals(allowed.contains(geomType), geomSelectFunction.evaluate(feature));
    }
  }

  private SimpleFeature createFeature(
      final Class<? extends Geometry> geomClass, String geomAttName) {
    final SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
    builder.add(geomAttName, geomClass);
    builder.setName(geomClass.getName() + "Feature");
    GeometryFactory factory = new GeometryFactory();
    Geometry geom = createGeometry(geomClass, factory);
    return SimpleFeatureBuilder.build(
        builder.buildFeatureType(), new Object[] {geom}, "testFeature");
  }

  private <T extends Geometry> T createGeometry(Class<T> geomClass, GeometryFactory factory) {
    Geometry geom;
    if (geomClass.equals(Point.class)) {
      geom = factory.createPoint(new Coordinate(2, 3));
    } else if (geomClass.equals(MultiPoint.class)) {
      geom = factory.createMultiPoint(new Point[] {createGeometry(Point.class, factory)});
    } else if (geomClass.equals(LineString.class)) {
      geom =
          factory.createLineString(new Coordinate[] {new Coordinate(2, 3), new Coordinate(1, 3)});
    } else if (geomClass.equals(LinearRing.class)) {
      geom =
          factory.createLinearRing(
              new Coordinate[] {
                new Coordinate(2, 3),
                new Coordinate(2, 2),
                new Coordinate(1, 2),
                new Coordinate(2, 3)
              });
    } else if (geomClass.equals(MultiLineString.class)) {
      geom =
          factory.createMultiLineString(
              new LineString[] {createGeometry(LineString.class, factory)});
    } else if (geomClass.equals(Polygon.class)) {
      geom = factory.createPolygon(createGeometry(LinearRing.class, factory));
    } else if (geomClass.equals(MultiPolygon.class)) {
      geom = factory.createMultiPolygon(new Polygon[] {createGeometry(Polygon.class, factory)});
    } else if (geomClass.equals(GeometryCollection.class)) {
      geom =
          factory.createGeometryCollection(
              new Geometry[] {
                createGeometry(Point.class, factory),
                createGeometry(LineString.class, factory),
                createGeometry(MultiPolygon.class, factory)
              });
    } else {
      throw new IllegalArgumentException(geomClass + " not known");
    }
    return geomClass.cast(geom);
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testV2ParseSymbolizersWithDefaultsAndValues() throws Throwable {
    final Style style = parseStyle("v2-style-symbolizers-default-values.json");

    final List<FeatureTypeStyle> featureTypeStyles = style.featureTypeStyles();
    assertEquals(1, featureTypeStyles.size());
    final List<Rule> rules = featureTypeStyles.get(0).rules();
    assertEquals(1, rules.size());
    final Rule rule = rules.get(0);

    assertEquals(1000000, rule.getMaxScaleDenominator(), DELTA);
    assertEquals(100, rule.getMinScaleDenominator(), DELTA);
    final Filter filter = rule.getFilter();

    assertTrue(filter instanceof PropertyIsLessThan);
    assertEquals("att < 3", ECQL.toCQL(filter));

    assertEquals(2, rule.symbolizers().size());

    PointSymbolizer symbolizer = (PointSymbolizer) rule.symbolizers().get(0);

    assertEquals(1, symbolizer.getGraphic().graphicalSymbols().size());

    Mark mark = (Mark) symbolizer.getGraphic().graphicalSymbols().get(0);

    assertEquals("circle", valueOf(mark.getWellKnownName()));
    assertEquals(30, (Double) valueOf(symbolizer.getGraphic().getRotation()), DELTA);
    assertEquals(0.4, (Double) valueOf(symbolizer.getGraphic().getOpacity()), DELTA);
    assertEquals("#00FF00", valueOf(mark.getStroke().getColor()));

    LineSymbolizer lineSymbolizer = (LineSymbolizer) rule.symbolizers().get(1);
    assertNull(lineSymbolizer.getStroke().getDashArray());
  }

  @Test
  public void testV2ParseDefaultSymbolizers() throws Throwable {
    final Style style = parseStyle("v2-style-default-symbolizers.json");

    final List<FeatureTypeStyle> featureTypeStyles = style.featureTypeStyles();
    assertEquals(1, featureTypeStyles.size());
    final List<Rule> rules = featureTypeStyles.get(0).rules();
    assertEquals(1, rules.size());
    final Rule rule = rules.get(0);

    assertEquals(Filter.INCLUDE, rule.getFilter());

    assertEquals(4, rule.symbolizers().size());

    PointSymbolizer pointSymbolizer = (PointSymbolizer) rule.symbolizers().get(0);
    LineSymbolizer lineSymbolizer = (LineSymbolizer) rule.symbolizers().get(1);
    PolygonSymbolizer polygonSymbolizer = (PolygonSymbolizer) rule.symbolizers().get(2);
    TextSymbolizer textSymbolizer = (TextSymbolizer) rule.symbolizers().get(3);

    assertNotNull(pointSymbolizer);
    assertNotNull(lineSymbolizer);
    assertNotNull(polygonSymbolizer);
    assertNotNull(textSymbolizer);
  }

  private Style parseStyle(String styleJsonFileName) throws Throwable {
    Configuration config = new Configuration();
    config.setConfigurationFile(getFile(styleJsonFileName));
    final String styleJson = getSpec(styleJsonFileName);

    final Optional<Style> styleOptional =
        mapfishStyleParserPlugin.parseStyle(config, httpClient, styleJson);

    assertTrue(styleOptional.isPresent());

    return styleOptional.get();
  }

  private String getSpec(String name) throws IOException, URISyntaxException {
    return new String(
        java.nio.file.Files.readAllBytes(getFile(name).toPath()), Constants.DEFAULT_CHARSET);
  }

  private File getFile(String name) throws URISyntaxException {
    return new File(MapfishJsonStyleVersion2Test.class.getResource(name).toURI());
  }
}
