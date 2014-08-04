package org.mapfish.print.processor.map.scalebar;

import static org.junit.Assert.assertEquals;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.json.JSONArray;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.attribute.ScalebarAttribute;
import org.mapfish.print.attribute.ScalebarAttribute.ScalebarAttributeValues;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.attribute.map.MapAttribute.MapAttributeValues;
import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.test.util.ImageSimilarity;
import org.mapfish.print.wrapper.json.PJsonArray;
import org.opengis.referencing.FactoryException;

public class ScalebarGraphicTest {

    private final double TOLERANCE = 0.000000001;

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
    public void testRender() throws IOException, FactoryException {
        MapAttribute mapAttribute = new MapAttribute();
        mapAttribute.setWidth(780);
        mapAttribute.setHeight(330);
        mapAttribute.setMaxDpi(600.0);
        MapAttributeValues mapParams = mapAttribute.createValue(null);
        mapParams.dpi = 72;
        mapParams.center = new double[]{-8235878.4938425, 4979784.7605681};
        mapParams.scale = 26000.0;
        mapParams.layers = new PJsonArray(null, new JSONArray(), "");
        mapParams.postConstruct();

        ScalebarAttribute scalebarAttibute = new ScalebarAttribute();
        scalebarAttibute.setWidth(300);
        scalebarAttibute.setHeight(40);
        ScalebarAttributeValues scalebarParams = scalebarAttibute.createValue(null);
        scalebarParams.verticalAlign = VerticalAlign.TOP.getLabel();

        ScalebarGraphic scalebar = new ScalebarGraphic();
        BufferedImage scalebarImage = scalebar.render(mapParams, scalebarParams);
//        ImageSimilarity.writeUncompressedImage(scalebarImage, "/tmp/expected-scalebar-graphic.tiff");
        new ImageSimilarity(scalebarImage, 4).assertSimilarity(getFile("expected-scalebar-graphic.tiff"), 15);
    }

    @Test
    public void testRenderDoubleDpi() throws IOException, FactoryException {
        MapAttribute mapAttribute = new MapAttribute();
        mapAttribute.setWidth(780);
        mapAttribute.setHeight(330);
        mapAttribute.setMaxDpi(600.0);
        MapAttributeValues mapParams = mapAttribute.createValue(null);
        // use a dpi of 144, this will create a scale bar graphic of 600x80 px
        mapParams.dpi = 144;
        mapParams.center = new double[]{-8235878.4938425, 4979784.7605681};
        mapParams.scale = 26000.0;
        mapParams.layers = new PJsonArray(null, new JSONArray(), "");
        mapParams.postConstruct();

        ScalebarAttribute scalebarAttibute = new ScalebarAttribute();
        scalebarAttibute.setWidth(300);
        scalebarAttibute.setHeight(40);
        ScalebarAttributeValues scalebarParams = scalebarAttibute.createValue(null);

        ScalebarGraphic scalebar = new ScalebarGraphic();
        BufferedImage scalebarImage = scalebar.render(mapParams, scalebarParams);
//        ImageSimilarity.writeUncompressedImage(scalebarImage, "/tmp/expected-scalebar-graphic-dpi.tiff");
        new ImageSimilarity(scalebarImage, 4).assertSimilarity(getFile("expected-scalebar-graphic-dpi.tiff"), 15);
    }

    private File getFile(String fileName) {
        return AbstractMapfishSpringTest.getFile(getClass(), fileName);
    }

}
