package org.mapfish.print.processor.map.scalebar;

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.attribute.ScalebarAttribute;
import org.mapfish.print.attribute.ScalebarAttribute.ScalebarAttributeValues;
import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.test.util.ImageSimilarity;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.awt.image.BufferedImage.TYPE_4BYTE_ABGR;
import static org.junit.Assert.assertNotNull;

public class ScalebarDrawerTest {

    @Test
    public void testCreate() {
        Graphics2D graphics2d = new BufferedImage(1, 1, TYPE_4BYTE_ABGR).createGraphics();
        assertNotNull(Type.LINE.createDrawer(graphics2d, new ScaleBarRenderSettings()));
        assertNotNull(Type.BAR.createDrawer(graphics2d, new ScaleBarRenderSettings()));
        assertNotNull(Type.BAR_SUB.createDrawer(graphics2d, new ScaleBarRenderSettings()));
    }

    @Test
    public void testDrawLine() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(180, 40, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 1);
        ScalebarDrawer drawer = Type.LINE.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-line.png"))
                .assertSimilarity(bufferedImage, 330);
    }

    @Test
    public void testDrawLineLabelRotated() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(180, 80, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 1);
        settings.getParams().labelRotation = 90;
        settings.getParams().getSize().width = 180;
        settings.getParams().getSize().height = 80;
        settings.setMaxSize(new Dimension(180, 80));
        settings.setSize(ScalebarGraphic.getSize(settings.getParams(), settings, settings.getMaxLabelSize()));
        ScalebarDrawer drawer = Type.LINE.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-line-labels-rotated.png"))
                .assertSimilarity(bufferedImage, 250);
    }

    @Test
    public void testDrawLineLabelRotatedNegative() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(180, 80, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 1);
        settings.getParams().labelRotation = -90;
        settings.getParams().getSize().width = 180;
        settings.getParams().getSize().height = 80;
        settings.setMaxSize(new Dimension(180, 80));
        settings.setSize(ScalebarGraphic.getSize(settings.getParams(), settings, settings.getMaxLabelSize()));
        ScalebarDrawer drawer = Type.LINE.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-line-labels-rotated-negative.png"))
                .assertSimilarity(bufferedImage, 250);
    }

    @Test
    public void testDrawLineLabelPartiallyRotated() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(180, 80, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 1);
        settings.getParams().labelRotation = 45;
        settings.getParams().getSize().width = 180;
        settings.getParams().getSize().height = 80;
        settings.setMaxSize(new Dimension(180, 80));
        settings.setSize(ScalebarGraphic.getSize(settings.getParams(), settings, settings.getMaxLabelSize()));
        ScalebarDrawer drawer = Type.LINE.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-line-labels-partially-rotated.png"))
                .assertSimilarity(bufferedImage, 270);
    }

    @Test
    public void testDrawLineLabelsAboveRotated() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(180, 80, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 1);
        settings.getParams().orientation = Orientation.HORIZONTAL_LABELS_ABOVE.getLabel();
        settings.getParams().labelRotation = 90;
        settings.getParams().getSize().width = 180;
        settings.getParams().getSize().height = 80;
        settings.setMaxSize(new Dimension(180, 80));
        settings.setSize(ScalebarGraphic.getSize(settings.getParams(), settings, settings.getMaxLabelSize()));
        ScalebarDrawer drawer = Type.LINE.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-line-labels-above-rotated.png")).
                assertSimilarity(bufferedImage, 170);
    }

    @Test
    public void testDrawLineLabelsAbovePartiallyRotated() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(180, 80, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 1);
        settings.getParams().orientation = Orientation.HORIZONTAL_LABELS_ABOVE.getLabel();
        settings.getParams().labelRotation = 45;
        settings.getParams().getSize().width = 180;
        settings.getParams().getSize().height = 80;
        settings.setMaxSize(new Dimension(180, 80));
        settings.setSize(ScalebarGraphic.getSize(settings.getParams(), settings, settings.getMaxLabelSize()));
        ScalebarDrawer drawer = Type.LINE.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-line-labels-above-partially-rotated.png")).
                assertSimilarity(bufferedImage, 210);
    }

    @Test
    public void testDrawLineLabelsVerticalLeftRotated() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(80, 180, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 1);
        settings.getParams().orientation = Orientation.VERTICAL_LABELS_LEFT.getLabel();
        settings.getParams().labelRotation = 90;
        settings.getParams().getSize().width = 80;
        settings.getParams().getSize().height = 180;
        settings.setMaxSize(new Dimension(80, 180));
        settings.setSize(ScalebarGraphic.getSize(settings.getParams(), settings, settings.getMaxLabelSize()));
        ScalebarDrawer drawer = Type.LINE.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-line-labels-left-rotated.png"))
                .assertSimilarity(bufferedImage, 210);
    }

    @Test
    public void testDrawLineLabelsVerticalLeftPartiallyRotated() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(80, 180, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 1);
        settings.getParams().orientation = Orientation.VERTICAL_LABELS_LEFT.getLabel();
        settings.getParams().labelRotation = 45;
        settings.getParams().getSize().width = 80;
        settings.getParams().getSize().height = 180;
        settings.setMaxSize(new Dimension(80, 180));
        settings.setSize(ScalebarGraphic.getSize(settings.getParams(), settings, settings.getMaxLabelSize()));
        ScalebarDrawer drawer = Type.LINE.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-line-labels-left-partially-rotated.png"))
                .assertSimilarity(bufferedImage, 210);
    }

    @Test
    public void testDrawLineLabelsVerticalRightRotated() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(80, 180, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 1);
        settings.getParams().orientation = Orientation.VERTICAL_LABELS_RIGHT.getLabel();
        settings.getParams().labelRotation = 90;
        settings.getParams().getSize().width = 80;
        settings.getParams().getSize().height = 180;
        settings.setMaxSize(new Dimension(80, 180));
        settings.setSize(ScalebarGraphic.getSize(settings.getParams(), settings, settings.getMaxLabelSize()));
        ScalebarDrawer drawer = Type.LINE.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-line-labels-right-rotated.png"))
                .assertSimilarity(bufferedImage, 130);
    }

    @Test
    public void testDrawLineLabelsVerticalRightPartiallyRotated() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(80, 180, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 1);
        settings.getParams().orientation = Orientation.VERTICAL_LABELS_RIGHT.getLabel();
        settings.getParams().labelRotation = 45;
        settings.getParams().getSize().width = 80;
        settings.getParams().getSize().height = 180;
        settings.setMaxSize(new Dimension(80, 180));
        settings.setSize(ScalebarGraphic.getSize(settings.getParams(), settings, settings.getMaxLabelSize()));
        ScalebarDrawer drawer = Type.LINE.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-line-labels-right-partially-rotated.png"))
                .assertSimilarity(bufferedImage, 100);
    }

    @Test
    public void testDrawLineWithSubIntervals() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(180, 40, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 2);
        ScalebarDrawer drawer = Type.LINE.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-line-subs.png")).
                assertSimilarity(bufferedImage, 340);
    }

    @Test
    public void testDrawBar() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(180, 40, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 1);
        ScalebarDrawer drawer = Type.BAR.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-bar.png")).
                assertSimilarity(bufferedImage, 510);
    }

    @Test
    public void testDrawBarWithSubIntervals() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(180, 40, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 2);
        ScalebarDrawer drawer = Type.BAR.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-bar-subs.png"))
                .assertSimilarity(bufferedImage, 480);
    }

    @Test
    public void testDrawBarWithBackground() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(180, 40, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 1);
        settings.getParams().backgroundColor = "rgb(214, 214, 214)";
        ScalebarDrawer drawer = Type.BAR.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-bar-bg.png"))
                .assertSimilarity(bufferedImage, 900);
    }

    @Test
    public void testDrawBarSub() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(180, 40, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 1);
        ScalebarDrawer drawer = Type.BAR_SUB.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-barsub.png")).
                assertSimilarity(bufferedImage, 520);
    }

    @Test
    public void testDrawBarSubWithSubIntervals() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(180, 40, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 2);
        ScalebarDrawer drawer = Type.BAR_SUB.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-barsub-subs.png"))
                .assertSimilarity(bufferedImage, 500);
    }

    @Test
    public void testDrawBarHorizontalTextAbove() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(180, 40, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 1);
        settings.getParams().backgroundColor = "rgb(214, 214, 214)";
        settings.getParams().orientation = Orientation.HORIZONTAL_LABELS_ABOVE.getLabel();
        ScalebarDrawer drawer = Type.BAR.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-bar-text-above.png"))
                .assertSimilarity(bufferedImage, 470);
    }

    @Test
    public void testDrawBarVerticalTextLeft() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(80, 180, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 1);
        settings.getParams().getSize().width = 80;
        settings.getParams().getSize().height = 180;
        settings.setMaxSize(new Dimension(80, 180));
        settings.getParams().backgroundColor = "rgb(214, 214, 214)";
        settings.getParams().orientation = Orientation.VERTICAL_LABELS_LEFT.getLabel();
        settings.setSize(ScalebarGraphic.getSize(settings.getParams(), settings, settings.getMaxLabelSize()));
        ScalebarDrawer drawer = Type.BAR.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-bar-vertical-text-left.png"))
                .assertSimilarity(bufferedImage, 470);
    }

    @Test
    public void testDrawBarVerticalTextRight() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(80, 180, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 1);
        settings.getParams().getSize().width = 80;
        settings.getParams().getSize().height = 180;
        settings.setMaxSize(new Dimension(80, 180));
        settings.getParams().backgroundColor = "rgb(214, 214, 214)";
        settings.getParams().orientation = Orientation.VERTICAL_LABELS_RIGHT.getLabel();
        settings.setSize(ScalebarGraphic.getSize(settings.getParams(), settings, settings.getMaxLabelSize()));
        ScalebarDrawer drawer = Type.BAR.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-bar-vertical-text-right.png"))
                .assertSimilarity(bufferedImage, 210);
    }

    @Test
    public void testDrawBarTopRight() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(180, 40, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 1);
        settings.getParams().backgroundColor = "rgb(214, 214, 214)";
        settings.getParams().align = HorizontalAlign.RIGHT.getLabel();
        settings.getParams().verticalAlign = VerticalAlign.TOP.getLabel();
        ScalebarDrawer drawer = Type.BAR.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-top-right.png"))
                .assertSimilarity(bufferedImage, 640);
    }

    @Test
    public void testDrawBarMiddleCenter() throws Exception {
        final BufferedImage bufferedImage = new BufferedImage(180, 40, TYPE_4BYTE_ABGR);
        final Graphics2D graphics2d = bufferedImage.createGraphics();

        ScaleBarRenderSettings settings = getSettings(graphics2d, 1);
        settings.getParams().backgroundColor = "rgb(214, 214, 214)";
        settings.getParams().align = HorizontalAlign.CENTER.getLabel();
        settings.getParams().verticalAlign = VerticalAlign.MIDDLE.getLabel();
        ScalebarDrawer drawer = Type.BAR.createDrawer(graphics2d, settings);
        drawer.draw();

        new ImageSimilarity(getFile("expected-scalebar-middle-center.png"))
                .assertSimilarity(bufferedImage, 530);
    }

    private ScaleBarRenderSettings getSettings(final Graphics2D graphics2d, int numSubIntervals) {
        ScalebarAttribute attribute = new ScalebarAttribute();
        attribute.setWidth(180);
        attribute.setHeight(40);
        ScalebarAttributeValues params = attribute.createValue(null);
        params.labelDistance = 4;
        params.barSize = 10;
        params.font = "Liberation Sans";
        ScaleBarRenderSettings settings = new ScaleBarRenderSettings();
        settings.setParams(params);

        setLabels(settings, graphics2d, params, 40, 50);
        settings.setScaleUnit(DistanceUnit.M);
        settings.setIntervalLengthInPixels(40);
        settings.setIntervalLengthInWorldUnits(0);
        settings.setIntervalUnit(DistanceUnit.M);
        settings.setBarSize(10);
        settings.setLabelDistance(4);
        settings.setLineWidth(2);
        settings.setNumSubIntervals(numSubIntervals);
        settings.setPadding(4);

        final Dimension maxLabelSize = ScalebarGraphic.getMaxLabelSize(settings);
        settings.setMaxSize(new Dimension(180, 40));
        settings.setSize(ScalebarGraphic.getSize(params, settings, maxLabelSize));
        settings.setMaxLabelSize(maxLabelSize);
        return settings;
    }

    private void setLabels(
            ScaleBarRenderSettings settings, Graphics2D graphics2d, ScalebarAttributeValues params,
            int intervalWidthInPixels, int intervalWidthInWorldUnits) {
        final Font font = new Font(params.font, Font.PLAIN, params.fontSize);
        final FontRenderContext frc = new FontRenderContext(null, true, true);

        final List<Label> labels = new ArrayList<>(params.intervals + 1);
        for (int i = 0; i <= params.intervals; i++) {
            String labelText = ScalebarGraphic.createLabelText(DistanceUnit.M,
                                                               intervalWidthInWorldUnits * i, DistanceUnit.M);
            if (i == params.intervals) {
                labelText += DistanceUnit.M;
            }
            TextLayout labelLayout = new TextLayout(labelText, font, frc);
            labels.add(new Label(intervalWidthInPixels * i, labelLayout, graphics2d));
        }
        settings.setLabels(labels);

        settings.setLeftLabelMargin(labels.get(0).getWidth() / 2.0f);
        settings.setRightLabelMargin(labels.get(labels.size() - 1).getWidth() / 2.0f);
        settings.setTopLabelMargin(labels.get(0).getHeight() / 2.0f);
        settings.setBottomLabelMargin(labels.get(labels.size() - 1).getHeight() / 2.0f);
    }

    private File getFile(String fileName) {
        return AbstractMapfishSpringTest.getFile(getClass(), fileName);
    }
}
