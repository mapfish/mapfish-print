package org.mapfish.print.processor.map;

import org.junit.Before;
import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.TestHttpClientFactory;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.http.ConfigFileResolvingHttpRequestFactory;
import org.mapfish.print.map.style.json.ColorParser;
import org.mapfish.print.test.util.ImageSimilarity;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URI;

public class NorthArrowGraphicTest extends AbstractMapfishSpringTest {
    private static final String BASE_DIR = "north_arrow/";

    @Autowired
    private ConfigurationFactory configurationFactory;

    @Autowired
    private TestHttpClientFactory requestFactory;
    private ConfigFileResolvingHttpRequestFactory requestFactoryWrapper;

    private Color bgColor;

    @Before
    public void setUp() throws IOException {
        Configuration config = this.configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        this.requestFactoryWrapper =
                new ConfigFileResolvingHttpRequestFactory(this.requestFactory, config, "test");
        this.bgColor = ColorParser.toColor("rgba(255, 255, 255, 0)");
    }

    @Test
    public void testCreatePngSquareDownScaled() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200),
                "file://" + getFile(BASE_DIR + "NorthArrow.png").toString(),
                this.bgColor, 0.0, getTaskDirectory(), this.requestFactoryWrapper);

        new ImageSimilarity(getFile(BASE_DIR + "expected-north-arrow-png-square-down.png"))
                .assertSimilarity(new File(file), 0);
    }

    @Test
    public void testCreatePngSquareUpScaled() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(50, 50),
                "file://" + getFile(BASE_DIR + "NorthArrow.png").toString(),
                this.bgColor, 0.0, getTaskDirectory(), this.requestFactoryWrapper);

        new ImageSimilarity(getFile(BASE_DIR + "expected-north-arrow-png-square-up.png"))
                .assertSimilarity(new File(file), 0);
    }

    @Test
    public void testCreatePngSquareRotatedDownScaled() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200),
                "file://" + getFile(BASE_DIR + "NorthArrow.png").toString(),
                this.bgColor, 45.0, getTaskDirectory(), this.requestFactoryWrapper);

        new ImageSimilarity(getFile(BASE_DIR + "expected-north-arrow-png-square-45-down.png"))
                .assertSimilarity(new File(file), 0);
    }

    @Test
    public void testCreatePngSquareRotatedUpScaled() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(50, 50),
                "file://" + getFile(BASE_DIR + "NorthArrow.png").toString(),
                this.bgColor, 45.0, getTaskDirectory(), this.requestFactoryWrapper);

        new ImageSimilarity(getFile(BASE_DIR + "expected-north-arrow-png-square-45-up.png"))
                .assertSimilarity(new File(file), 0);
    }

    @Test
    public void testCreatePngNoSquareDownScaled() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200),
                "file://" + getFile(BASE_DIR + "NorthArrow_10.png").toString(),
                this.bgColor, 0.0, getTaskDirectory(), this.requestFactoryWrapper);

        new ImageSimilarity(getFile(BASE_DIR + "expected-north-arrow_10-png-nosquare-down.png"))
                .assertSimilarity(new File(file), 0);
    }

    @Test
    public void testCreatePngNoSquareUpScaled() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(50, 50),
                "file://" + getFile(BASE_DIR + "NorthArrow_10.png").toString(),
                this.bgColor, 0.0, getTaskDirectory(), this.requestFactoryWrapper);

        new ImageSimilarity(getFile(BASE_DIR + "expected-north-arrow_10-png-nosquare-up.png"))
                .assertSimilarity(new File(file), 0);
    }

    @Test
    public void testCreatePngNoSquareRotatedDownScaled() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200),
                "file://" + getFile(BASE_DIR + "NorthArrow_10.png").toString(),
                this.bgColor, Math.PI / 4, getTaskDirectory(), this.requestFactoryWrapper);

        new ImageSimilarity(getFile(BASE_DIR + "expected-north-arrow_10-png-nosquare-45-down.png"))
                .assertSimilarity(new File(file), 0);
    }

    @Test
    public void testCreatePngNoSquareRotatedUpScaled() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(50, 50),
                "file://" + getFile(BASE_DIR + "NorthArrow_10.png").toString(),
                this.bgColor, Math.PI / 4, getTaskDirectory(), this.requestFactoryWrapper);

        new ImageSimilarity(getFile(BASE_DIR + "expected-north-arrow_10-png-nosquare-45-up.png"))
                .assertSimilarity(new File(file), 0);
    }

    @Test
    public void testCreatePngSquareBgDownScaled() throws Exception {
        Color backgroundColor = ColorParser.toColor("rgba(214, 214, 214, 200)");
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200),
                "file://" + getFile(BASE_DIR + "NorthArrow.png").toString(),
                backgroundColor, 0.0, getTaskDirectory(), this.requestFactoryWrapper);

        new ImageSimilarity(getFile(BASE_DIR + "expected-north-arrow-png-bg-square-down.png"))
                .assertSimilarity(new File(file), 0);
    }

    @Test
    public void testCreateSvgWidthAndHeightSet() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200),
                "file://" + getFile(BASE_DIR + "NorthArrow.svg").toString(),
                this.bgColor, 90.0, getTaskDirectory(), this.requestFactoryWrapper);

        new ImageSimilarity(getFile(BASE_DIR + "expected-north-arrow.png"))
                .assertSimilarity(file, 200, 200, 4);
    }

    @Test
    public void testCreateSvgWidthAndHeightNotSet() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200),
                "file://" + getFile(BASE_DIR + "NorthArrow_10.svg").toString(),
                this.bgColor, Math.PI / 2, getTaskDirectory(), this.requestFactoryWrapper);

        new ImageSimilarity(getFile(BASE_DIR + "expected-north-arrow_10.png"))
                .assertSimilarity(file, 200, 200, 70);
    }

    @Test
    public void testCreateSvgWidthAndHeightSetBg() throws Exception {
        Color backgroundColor = ColorParser.toColor("rgba(214, 214, 214, 200)");
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200),
                "file://" + getFile(BASE_DIR + "NorthArrow.svg").toString(),
                backgroundColor, Math.PI / 2, getTaskDirectory(), this.requestFactoryWrapper);

        new ImageSimilarity(getFile(BASE_DIR + "expected-north-arrow-bg.png"))
                .assertSimilarity(file, 200, 200, 5);
    }

    @Test
    public void testCreateSvgWidthAndHeightNotSetBg() throws Exception {
        Color backgroundColor = ColorParser.toColor("rgba(214, 214, 214, 200)");
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200),
                "file://" + getFile(BASE_DIR + "NorthArrow_10.svg").toString(),
                backgroundColor, Math.PI / 2, getTaskDirectory(), this.requestFactoryWrapper);

        new ImageSimilarity(getFile(BASE_DIR + "expected-north-arrow_10-bg.png")).assertSimilarity(
                file, 200, 200, 70);
    }

    @Test
    public void testCreateDefaultGraphic() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200), null,
                this.bgColor, Math.PI / 2, getTaskDirectory(), this.requestFactoryWrapper);

        new ImageSimilarity(getFile(BASE_DIR + "expected-north-arrow_10-default.png"))
                .assertSimilarity(file, 200, 200, 70);
    }
}
