package org.mapfish.print.processor.map.scalebar;

import static org.junit.Assert.assertEquals;

import java.awt.Dimension;
import java.io.File;
import java.net.URI;

import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.attribute.ScalebarAttribute;
import org.mapfish.print.attribute.ScalebarAttribute.ScalebarAttributeValues;
import org.mapfish.print.attribute.map.CenterScaleMapBounds;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.map.Scale;
import org.mapfish.print.test.util.ImageSimilarity;


public class ScalebarGraphicTest {

    private final double TOLERANCE = 0.000000001;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private Template template;

    @Before
    public void setUp() throws Exception {
        Configuration configuration = new Configuration();
        this.template = new Template();
        this.template.setConfiguration(configuration);

    }

    @Test
    public void testGetNearestNiceValue() {
        ScalebarGraphic scalebar = new ScalebarGraphic();
        assertEquals(10.0, scalebar.getNearestNiceValue(10.0, DistanceUnit.M, false), TOLERANCE);
        assertEquals(10.0, scalebar.getNearestNiceValue(13.0, DistanceUnit.M, false), TOLERANCE);
        assertEquals(50.0, scalebar.getNearestNiceValue(67.66871, DistanceUnit.M, false), TOLERANCE);
        assertEquals(0.02, scalebar.getNearestNiceValue(0.0315, DistanceUnit.M, false), TOLERANCE);
        assertEquals(1000000000.0, scalebar.getNearestNiceValue(1240005466, DistanceUnit.M, false), TOLERANCE);
        assertEquals(50.0, scalebar.getNearestNiceValue(98.66871, DistanceUnit.M, false), TOLERANCE);
    }

    @Test
    public void testGetSize() {
        // horizontal
        ScalebarAttribute attribute = new ScalebarAttribute();
        attribute.setWidth(180);
        attribute.setHeight(40);
        ScalebarAttributeValues params = attribute.createValue(null);
        params.labelDistance = 3;
        params.barSize = 8;
        ScaleBarRenderSettings settings = new ScaleBarRenderSettings();
        settings.setParams(params);
        settings.setDpiRatio(1.0);
        settings.setIntervalLengthInPixels(40);
        settings.setLeftLabelMargin(3.0f);
        settings.setRightLabelMargin(4.0f);
        settings.setMaxSize(new Dimension(180, 40));
        settings.setBarSize(8);
        settings.setPadding(4);
        settings.setLabelDistance(3);

        assertEquals(
                new Dimension(135, 31),
                ScalebarGraphic.getSize(params, settings, new Dimension(30, 12)));


        // horizontal: barSize and labelDistance calculated from height
        params.orientation = Orientation.HORIZONTAL_LABELS_ABOVE.getLabel();
        params.labelDistance = null;
        params.barSize = null;
        settings.setBarSize(ScalebarGraphic.getBarSize(settings));
        settings.setLabelDistance(ScalebarGraphic.getLabelDistance(settings));

        assertEquals(
                new Dimension(135, 34),
                ScalebarGraphic.getSize(params, settings, new Dimension(30, 12)));


        // vertical
        attribute.setWidth(60);
        attribute.setHeight(180);
        settings.setMaxSize(new Dimension(60, 180));
        params = attribute.createValue(null);
        settings.setParams(params);
        params.orientation = Orientation.VERTICAL_LABELS_LEFT.getLabel();
        params.labelDistance = 3;
        params.barSize = 8;
        settings.setTopLabelMargin(5.0f);
        settings.setBottomLabelMargin(6.0f);
        settings.setBarSize(8);
        settings.setLabelDistance(3);

        assertEquals(
                new Dimension(49, 139),
                ScalebarGraphic.getSize(params, settings, new Dimension(30, 12)));


        // vertical: barSize and labelDistance calculated from height
        params.labelDistance = null;
        params.barSize = null;
        settings.setBarSize(ScalebarGraphic.getBarSize(settings));
        settings.setLabelDistance(ScalebarGraphic.getLabelDistance(settings));

        assertEquals(
                new Dimension(57, 139),
                ScalebarGraphic.getSize(params, settings, new Dimension(30, 12)));
    }

    @Test
    public void testRender() throws Exception {
        MapBounds bounds = new CenterScaleMapBounds(
                CRS.decode("EPSG:3857"), -8235878.4938425, 4979784.7605681, new Scale(26000));
        MapfishMapContext mapParams = new MapfishMapContext(
                bounds, new Dimension(780, 330), 0, 72, 72, true, false);

        ScalebarAttribute scalebarAttibute = new ScalebarAttribute();
        scalebarAttibute.setWidth(300);
        scalebarAttibute.setHeight(40);
        ScalebarAttributeValues scalebarParams = scalebarAttibute.createValue(null);
        scalebarParams.verticalAlign = VerticalAlign.TOP.getLabel();
        scalebarParams.font = "Liberation Sans";

        ScalebarGraphic scalebar = new ScalebarGraphic();
        URI file = scalebar.render(mapParams, scalebarParams, folder.getRoot(), this.template);
//      Files.copy(new File(file), new File("/tmp/expected-scalebar-graphic.tiff"));
        new ImageSimilarity(new File(file), 4).assertSimilarity(getFile("expected-scalebar-graphic.tiff"), 15);
    }

    @Test
    public void testRenderDoubleDpi() throws Exception {
        MapBounds bounds = new CenterScaleMapBounds(
                CRS.decode("EPSG:3857"), -8235878.4938425, 4979784.7605681, new Scale(26000));
        MapfishMapContext mapParams = new MapfishMapContext(
                bounds, new Dimension(780, 330), 0, 144, 72, true, false);

        ScalebarAttribute scalebarAttibute = new ScalebarAttribute();
        scalebarAttibute.setWidth(300);
        scalebarAttibute.setHeight(40);
        ScalebarAttributeValues scalebarParams = scalebarAttibute.createValue(null);
        scalebarParams.font = "Liberation Sans";

        ScalebarGraphic scalebar = new ScalebarGraphic();
        URI file = scalebar.render(mapParams, scalebarParams, folder.getRoot(), this.template);
//      Files.copy(new File(file), new File("/tmp/expected-scalebar-graphic-dpi.tiff"));
        new ImageSimilarity(new File(file), 4).assertSimilarity(getFile("expected-scalebar-graphic-dpi.tiff"), 15);
    }

    @Test
    public void testRenderSvg() throws Exception {
        MapBounds bounds = new CenterScaleMapBounds(
                CRS.decode("EPSG:3857"), -8235878.4938425, 4979784.7605681, new Scale(26000));
        MapfishMapContext mapParams = new MapfishMapContext(
                bounds, new Dimension(780, 330), 0, 72, 72, true, false);

        ScalebarAttribute scalebarAttibute = new ScalebarAttribute();
        scalebarAttibute.setWidth(300);
        scalebarAttibute.setHeight(40);
        ScalebarAttributeValues scalebarParams = scalebarAttibute.createValue(null);
        scalebarParams.verticalAlign = VerticalAlign.TOP.getLabel();
        scalebarParams.renderAsSvg = true;
        scalebarParams.font = "Liberation Sans";

        ScalebarGraphic scalebar = new ScalebarGraphic();
        URI file = scalebar.render(mapParams, scalebarParams, folder.getRoot(), this.template);
//        Files.copy(new File(file), new File(TPM, getClass().getSimpleName() + "expected-scalebar-graphic.svg"));
//        ImageSimilarity.writeUncompressedImage(
//                ImageSimilarity.convertFromSvg(file, 300, 40), TMP + "/" + getClass().getSimpleName() + "expected-scalebar-graphic-svg.tiff");
        new ImageSimilarity(ImageSimilarity.convertFromSvg(file, 300, 40), 4).assertSimilarity(getFile("expected-scalebar-graphic-svg.tiff"), 15);
    }

    private File getFile(String fileName) {
        return AbstractMapfishSpringTest.getFile(getClass(), fileName);
    }

}
