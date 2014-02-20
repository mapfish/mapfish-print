package org.mapfish.print.config.layout;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.Transformer;
import org.mapfish.print.config.Config;
import org.mapfish.print.utils.DistanceUnit;
import org.mapfish.print.utils.PJsonObject;
import org.mockito.Mockito;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.Collections;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

/**
 * Test MapBlock's createTransformer method.
 *
 * Created by Jesse on 1/15/14.
 */
public class MapBlockTest {
    int width = 780;
    int height = 330;
    double minx, maxx, miny, maxy;
    TreeSet<Integer> dpis = new TreeSet<Integer>();

    TreeSet<Number> scales;
    private Config config;
    private double centerY;
    private double centerX;

    @Before
    public void init(){
        minx = -139.84870868359;
        miny = 18.549281576172;
        maxx = -51.852562316406;
        maxy = 55.778420423828;

        dpis.add(60);
        dpis.add(90);
        dpis.add(300);

        double projWidth = (maxx - minx);
        double projHeight = (maxy - miny);
        double scale = Math.max(
                projWidth / (DistanceUnit.PT.convertTo(width, DistanceUnit.DEGREES)),
                projHeight / (DistanceUnit.PT.convertTo(height, DistanceUnit.DEGREES)));
        scales = new TreeSet<Number>();
        scales.add(scale);


        this.config = new Config();
        config.setDpis(dpis);
        config.setScales(scales);
        config.setDisableScaleLocking(true);

        centerY = miny + (maxy - miny) / 2;
        centerX = maxx - (maxx - minx) / 2;

    }

    @Test @Ignore
    public void testCreateTransformerBBoxDef_Geodetic() throws Exception {
        final MapBlock mapBlock = new MapBlock();

        mapBlock.setWidth("" + width);
        mapBlock.setHeight("" + height);
        RenderingContext context = Mockito.mock(RenderingContext.class);

        JSONObject globalParams = new JSONObject();
        globalParams.accumulate("dpi", "90");
        globalParams.accumulate("units", DistanceUnit.DEGREES.toString());

        Mockito.when(context.getGlobalParams()).thenReturn(new PJsonObject(globalParams, "globalParams"));

        Mockito.when(context.getConfig()).thenReturn(config);

        JSONObject internalObj = new JSONObject();
        internalObj.accumulate("type", "WMS");
        internalObj.accumulate("geodetic", "true");
        internalObj.accumulate("srs", "EPSG:4326");
        internalObj.append("bbox", minx);
        internalObj.append("bbox", miny);
        internalObj.append("bbox", maxx);
        internalObj.append("bbox", maxy);
        final Transformer transformer = mapBlock.createTransformer(context, new PJsonObject(internalObj, "page"));

        assertEquals(width, transformer.getPaperW(), 0.0000000001);
        assertEquals(height, transformer.getPaperH(), 0.0000000001);

        assertEquals(minx, transformer.getMinGeoX(), 0.0000000001);
        assertEquals(maxx, transformer.getMaxGeoX(), 0.0000000001);
        assertEquals(miny, transformer.getMinGeoY(), 0.0000000001);
        assertEquals(maxy, transformer.getMaxGeoY(), 0.0000000001);
    }

    @Test
    public void testCreateTransformerBBoxDef_Default() throws Exception {
        final MapBlock mapBlock = new MapBlock();

        mapBlock.setWidth("" + width);
        mapBlock.setHeight("" + height);
        RenderingContext context = Mockito.mock(RenderingContext.class);

        JSONObject globalParams = new JSONObject();
        globalParams.accumulate("dpi", "90");
        globalParams.accumulate("units", DistanceUnit.DEGREES.toString());

        Mockito.when(context.getGlobalParams()).thenReturn(new PJsonObject(globalParams, "globalParams"));

        Mockito.when(context.getConfig()).thenReturn(config);

        JSONObject internalObj = new JSONObject();
        internalObj.accumulate("type", "WMS");
        internalObj.append("bbox", minx);
        internalObj.append("bbox", miny);
        internalObj.append("bbox", maxx);
        internalObj.append("bbox", maxy);
        final Transformer transformer = mapBlock.createTransformer(context, new PJsonObject(internalObj, "page"));

        assertEquals(width, transformer.getPaperW(), 0.0000000001);
        assertEquals(height, transformer.getPaperH(), 0.0000000001);

        assertEquals(minx, transformer.getMinGeoX(), 0.0000000001);
        assertEquals(maxx, transformer.getMaxGeoX(), 0.0000000001);
        assertEquals(miny, transformer.getMinGeoY(), 0.0000000001);
        assertEquals(maxy, transformer.getMaxGeoY(), 0.0000000001);
    }
    @Test
    public void testCreateTransformerCenterDef_Default() throws Exception {
        final MapBlock mapBlock = new MapBlock();

        mapBlock.setWidth("" + width);
        mapBlock.setHeight("" + height);
        RenderingContext context = Mockito.mock(RenderingContext.class);

        JSONObject globalParams = new JSONObject();
        globalParams.accumulate("dpi", "90");
        globalParams.accumulate("units", DistanceUnit.DEGREES.toString());

        Mockito.when(context.getGlobalParams()).thenReturn(new PJsonObject(globalParams, "globalParams"));

        Mockito.when(context.getConfig()).thenReturn(config);

        JSONObject internalObj = new JSONObject();
        internalObj.accumulate("type", "WMS");
        internalObj.append("center", maxx - (maxx - minx) / 2);
        internalObj.append("center", maxy - (maxy-miny)/2);
        internalObj.accumulate("scale", scales.iterator().next());
        final Transformer transformer = mapBlock.createTransformer(context, new PJsonObject(internalObj, "page"));

        final double delta = 0.1;
        assertEquals(width, transformer.getPaperW(), delta);
        assertEquals(height, transformer.getPaperH(), delta);

        assertEquals(minx, transformer.getMinGeoX(), delta);
        assertEquals(maxx, transformer.getMaxGeoX(), delta);
        assertEquals(miny, transformer.getMinGeoY(), delta);
        assertEquals(maxy, transformer.getMaxGeoY(), delta);
    }
    @Test  @Ignore
    public void testCreateTransformerCenterDef_Geodetic() throws Exception {
        final CoordinateReferenceSystem crs = CRS.decode("EPSG:4326", false);
        final double scale = RendererUtilities.calculateOGCScale(new ReferencedEnvelope(minx, maxx, miny, maxy, crs), width,
                Collections.emptyMap());
//        final GeodeticCalculator calculator = new GeodeticCalculator(crs);
//
//        calculator.setStartingGeographicPoint(minx, centerY);
//        calculator.setDestinationGeographicPoint(maxx, centerY);
//        double scale = calculator.getOrthodromicDistance() / (DistanceUnit.PT.convertTo(width, DistanceUnit.M));
        scales.clear();
        scales.add(scale);

        final MapBlock mapBlock = new MapBlock();

        mapBlock.setWidth("" + width);
        mapBlock.setHeight("" + height);
        RenderingContext context = Mockito.mock(RenderingContext.class);

        JSONObject globalParams = new JSONObject();
        globalParams.accumulate("dpi", "90");
        globalParams.accumulate("units", DistanceUnit.DEGREES.toString());

        Mockito.when(context.getGlobalParams()).thenReturn(new PJsonObject(globalParams, "globalParams"));

        Mockito.when(context.getConfig()).thenReturn(config);

        JSONObject internalObj = new JSONObject();
        internalObj.accumulate("type", "WMS");
        internalObj.append("center", centerX);
        internalObj.append("center", centerY);
        internalObj.accumulate("geodetic", "true");
        internalObj.accumulate("srs", "EPSG:4326");
        internalObj.accumulate("scale", scales.iterator().next());

        final Transformer transformer = mapBlock.createTransformer(context, new PJsonObject(internalObj, "page"));

        final double delta = 0.001;
        assertEquals(width, transformer.getPaperW(), delta);
        assertEquals(height, transformer.getPaperH(), delta);

        assertEquals(minx, transformer.getMinGeoX(), delta);
        assertEquals(maxx, transformer.getMaxGeoX(), delta);
        assertEquals(miny, transformer.getMinGeoY(), delta);
        assertEquals(maxy, transformer.getMaxGeoY(), delta);
    }
}
