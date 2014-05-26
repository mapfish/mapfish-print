package org.mapfish.print.config.layout;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.PDFCustomBlocks;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.ThreadResources;
import org.mapfish.print.Transformer;
import org.mapfish.print.config.Config;
import org.mapfish.print.map.readers.MapReaderFactoryFinder;
import org.mapfish.print.map.readers.VectorMapReader;
import org.mapfish.print.utils.DistanceUnit;
import org.mapfish.print.utils.PJsonObject;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.pvalsecc.misc.FileUtilities;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    
    private static String tempDir = System.getProperty("java.io.tmpdir");
    private static String fileSeparator = System.getProperty("file.separator");

    private ThreadResources threadResources;
    @Before
    public void init(){

        this.threadResources = new ThreadResources();
        this.threadResources.init();
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
        this.config.setThreadResources(this.threadResources);
        config.setDpis(dpis);
        config.setScales(scales);
        config.setDisableScaleLocking(true);

        centerY = miny + (maxy - miny) / 2;
        centerX = maxx - (maxx - minx) / 2;

    }

    @After
    public void tearDown() throws Exception {
        this.threadResources.destroy();
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
    
    @Test
    public void testMultipleMaps() throws IOException, DocumentException,
            JSONException {
    
        PJsonObject globalParams = MapPrinter.parseSpec(FileUtilities
                .readWholeTextFile(new File(MapBlockTest.class.getClassLoader()
                        .getResource("config/multiple-maps.json").getFile())));
        
        PJsonObject params = globalParams.getJSONArray("pages").getJSONObject(0);
        
        // mocked PdfWriter
        final PdfWriter writer = Mockito.mock(PdfWriter.class);
        final PdfContentByte dc = Mockito.mock(PdfContentByte.class);
        Mockito.when(writer.getDirectContent()).thenReturn(dc);
        
        // mocked RenderingContext
        final RenderingContext context = Mockito.mock(RenderingContext.class);
        Mockito.when(context.getGlobalParams()).thenReturn(globalParams);
        Mockito.when(context.getConfig()).thenReturn(config);
        Mockito.when(context.getPdfLock()).thenReturn(new Object());
        PDFCustomBlocks blocks = new PDFCustomBlocks(writer, context);
        Mockito.when(context.getCustomBlocks()).thenReturn(blocks);
        Mockito.when(context.getWriter()).thenReturn(writer);
        
        
        
        // mock the MapReader creation chain to return a VectorMapReader for test layers
        MapReaderFactoryFinder finder = Mockito.mock(MapReaderFactoryFinder.class);
        config.setMapReaderFactoryFinder(finder);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                VectorMapReader reader = new VectorMapReader(context, (PJsonObject)invocation.getArguments()[3]);
                ((List)invocation.getArguments()[0]).add(reader);
                return reader;
            }
            
        }).when(finder).create(Mockito.anyList(),Mockito.anyString(),Mockito.any(RenderingContext.class), Mockito.any(PJsonObject.class));
        
        // mock PdfElement
        Block.PdfElement target = Mockito.mock(Block.PdfElement.class);
        
        // render the mapblock named 'main'
        renderMap(params, target, context, "main");
        
        // check that a circle is drawn on map
        Mockito.verify(dc, Mockito.atLeastOnce()).circle(Mockito.anyFloat(),
                Mockito.anyFloat(), Mockito.anyFloat());
    
        // render the mapblock named 'other'
        renderMap(params, target, context, "other");
        
        // check that a linestring is drawn on map
        Mockito.verify(dc, Mockito.atLeastOnce()).moveTo(Mockito.anyFloat(),
                Mockito.anyFloat());
        Mockito.verify(dc, Mockito.atLeastOnce()).lineTo(Mockito.anyFloat(),
                Mockito.anyFloat());
        
        // render the mapblock named 'error' (not existing in spec)
        boolean error = false;
        try {
            
            renderMap(params, target, context, "error");
        } catch(RuntimeException e) {
            error = true;
        }
        assertTrue(error);
    }

    private void renderMap(PJsonObject params, Block.PdfElement target,
            final RenderingContext context, String mapName) throws DocumentException {
        MapBlock mapBlock = new MapBlock();
        mapBlock.setAbsoluteX("10");
        mapBlock.setAbsoluteY("10");
        mapBlock.setWidth("100");
        mapBlock.setHeight("100");
        mapBlock.setName(mapName);
        mapBlock.render(params, target, context);
    }
}
