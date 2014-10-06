/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import java.awt.image.BufferedImage;
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

    private Configuration config;
    private Color bgColor;

    @Before
    public void setup() throws IOException {
        this.config = this.configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        this.requestFactoryWrapper =
                new ConfigFileResolvingHttpRequestFactory(this.requestFactory, config);
        this.bgColor = ColorParser.toColor("rgba(255, 255, 255, 0)");
    }

    @Test
    public void testCreatePngSquareDownScaled() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200), "file://" + getFile(BASE_DIR + "NorthArrow.png").toString(),
                this.bgColor, 0.0, getTaskDirectory(), this.requestFactoryWrapper);

//        FileUtils.copyFile(new File(file), new File("/tmp/north-arrow.tiff"));
//        ImageSimilarity.writeUncompressedImage(ImageIO.read(new File(file)), "/tmp/expected-north-arrow-png-square-down.tiff");
        new ImageSimilarity(new File(file), 2).assertSimilarity(getFile(BASE_DIR + "expected-north-arrow-png-square-down.tiff"), 0);
    }

    @Test
    public void testCreatePngSquareUpScaled() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(50, 50), "file://" + getFile(BASE_DIR + "NorthArrow.png").toString(),
                this.bgColor, 0.0, getTaskDirectory(), this.requestFactoryWrapper);

//        FileUtils.copyFile(new File(file), new File("/tmp/north-arrow.tiff"));
//        ImageSimilarity.writeUncompressedImage(ImageIO.read(new File(file)), "/tmp/expected-north-arrow-png-square-up.tiff");
        new ImageSimilarity(new File(file), 2).assertSimilarity(getFile(BASE_DIR + "expected-north-arrow-png-square-up.tiff"), 0);
    }

    @Test
    public void testCreatePngSquareRotatedDownScaled() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200), "file://" + getFile(BASE_DIR + "NorthArrow.png").toString(),
                this.bgColor, 45.0, getTaskDirectory(), this.requestFactoryWrapper);

//        FileUtils.copyFile(new File(file), new File("/tmp/north-arrow.tiff"));
//        ImageSimilarity.writeUncompressedImage(ImageIO.read(new File(file)), "/tmp/expected-north-arrow-png-square-45-down.tiff");
        new ImageSimilarity(new File(file), 2).assertSimilarity(getFile(BASE_DIR + "expected-north-arrow-png-square-45-down.tiff"), 0);
    }

    @Test
    public void testCreatePngSquareRotatedUpScaled() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(50, 50), "file://" + getFile(BASE_DIR + "NorthArrow.png").toString(),
                this.bgColor, 45.0, getTaskDirectory(), this.requestFactoryWrapper);

//        FileUtils.copyFile(new File(file), new File("/tmp/north-arrow.tiff"));
//        ImageSimilarity.writeUncompressedImage(ImageIO.read(new File(file)), "/tmp/expected-north-arrow-png-square-45-up.tiff");
        new ImageSimilarity(new File(file), 2).assertSimilarity(getFile(BASE_DIR + "expected-north-arrow-png-square-45-up.tiff"), 0);
    }

    @Test
    public void testCreatePngNoSquareDownScaled() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200), "file://" + getFile(BASE_DIR + "NorthArrow_10.png").toString(),
                this.bgColor, 0.0, getTaskDirectory(), this.requestFactoryWrapper);

//        FileUtils.copyFile(new File(file), new File("/tmp/north-arrow.tiff"));
//        ImageSimilarity.writeUncompressedImage(ImageIO.read(new File(file)), "/tmp/expected-north-arrow_10-png-nosquare-down.tiff");
        new ImageSimilarity(new File(file), 2).assertSimilarity(getFile(BASE_DIR + "expected-north-arrow_10-png-nosquare-down.tiff"), 0);
    }

    @Test
    public void testCreatePngNoSquareUpScaled() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(50, 50), "file://" + getFile(BASE_DIR + "NorthArrow_10.png").toString(),
                this.bgColor, 0.0, getTaskDirectory(), this.requestFactoryWrapper);

//        FileUtils.copyFile(new File(file), new File("/tmp/north-arrow.tiff"));
//        ImageSimilarity.writeUncompressedImage(ImageIO.read(new File(file)), "/tmp/expected-north-arrow_10-png-nosquare-up.tiff");
        new ImageSimilarity(new File(file), 2).assertSimilarity(getFile(BASE_DIR + "expected-north-arrow_10-png-nosquare-up.tiff"), 0);
    }

    @Test
    public void testCreatePngNoSquareRotatedDownScaled() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200), "file://" + getFile(BASE_DIR + "NorthArrow_10.png").toString(),
                this.bgColor, 45.0, getTaskDirectory(), this.requestFactoryWrapper);

//        FileUtils.copyFile(new File(file), new File("/tmp/north-arrow.tiff"));
//        ImageSimilarity.writeUncompressedImage(ImageIO.read(new File(file)), "/tmp/expected-north-arrow_10-png-nosquare-45-down.tiff");
        new ImageSimilarity(new File(file), 2).assertSimilarity(getFile(BASE_DIR + "expected-north-arrow_10-png-nosquare-45-down.tiff"), 0);
    }

    @Test
    public void testCreatePngNoSquareRotatedUpScaled() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(50, 50), "file://" + getFile(BASE_DIR + "NorthArrow_10.png").toString(),
                this.bgColor, 45.0, getTaskDirectory(), this.requestFactoryWrapper);

//        FileUtils.copyFile(new File(file), new File("/tmp/north-arrow.tiff"));
//        ImageSimilarity.writeUncompressedImage(ImageIO.read(new File(file)), "/tmp/expected-north-arrow_10-png-nosquare-45-up.tiff");
        new ImageSimilarity(new File(file), 2).assertSimilarity(getFile(BASE_DIR + "expected-north-arrow_10-png-nosquare-45-up.tiff"), 0);
    }

    @Test
    public void testCreatePngSquareBgDownScaled() throws Exception {
        Color backgroundColor = ColorParser.toColor("rgba(214, 214, 214, 200)");
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200), "file://" + getFile(BASE_DIR + "NorthArrow.png").toString(),
                backgroundColor, 0.0, getTaskDirectory(), this.requestFactoryWrapper);

//        FileUtils.copyFile(new File(file), new File("/tmp/north-arrow.tiff"));
//        ImageSimilarity.writeUncompressedImage(ImageIO.read(new File(file)), "/tmp/expected-north-arrow-png-bg-square-down.tiff");
        new ImageSimilarity(new File(file), 2).assertSimilarity(getFile(BASE_DIR + "expected-north-arrow-png-bg-square-down.tiff"), 0);
    }

    @Test
    public void testCreateSvgWidthAndHeightSet() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200), "file://" + getFile(BASE_DIR + "NorthArrow.svg").toString(),
                this.bgColor, 90.0, getTaskDirectory(), this.requestFactoryWrapper);

        BufferedImage referenceImage = ImageSimilarity.convertFromSvg(file, 200, 200);
//        FileUtils.copyFile(new File(file), new File("/tmp/north-arrow.svg"));
//        ImageSimilarity.writeUncompressedImage(referenceImage, "/tmp/expected-north-arrow.tiff");
        new ImageSimilarity(referenceImage, 2).assertSimilarity(getFile(BASE_DIR + "expected-north-arrow.tiff"), 0);
    }

    @Test
    public void testCreateSvgWidthAndHeightNotSet() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200), "file://" + getFile(BASE_DIR + "NorthArrow_10.svg").toString(),
                this.bgColor, 90.0, getTaskDirectory(), this.requestFactoryWrapper);

        BufferedImage referenceImage = ImageSimilarity.convertFromSvg(file, 200, 200);
//        FileUtils.copyFile(new File(file), new File("/tmp/north-arrow_10.svg"));
//        ImageSimilarity.writeUncompressedImage(referenceImage, "/tmp/expected-north-arrow_10.tiff");
        new ImageSimilarity(referenceImage, 2).assertSimilarity(getFile(BASE_DIR + "expected-north-arrow_10.tiff"), 75);
    }

    @Test
    public void testCreateSvgWidthAndHeightSetBg() throws Exception {
        Color backgroundColor = ColorParser.toColor("rgba(214, 214, 214, 200)");
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200), "file://" + getFile(BASE_DIR + "NorthArrow.svg").toString(),
                backgroundColor, 90.0, getTaskDirectory(), this.requestFactoryWrapper);

        BufferedImage referenceImage = ImageSimilarity.convertFromSvg(file, 200, 200);
//        FileUtils.copyFile(new File(file), new File("/tmp/north-arrow.svg"));
//        ImageSimilarity.writeUncompressedImage(referenceImage, "/tmp/expected-north-arrow-bg.tiff");
        new ImageSimilarity(referenceImage, 2).assertSimilarity(getFile(BASE_DIR + "expected-north-arrow-bg.tiff"), 0);
    }

    @Test
    public void testCreateSvgWidthAndHeightNotSetBg() throws Exception {
        Color backgroundColor = ColorParser.toColor("rgba(214, 214, 214, 200)");
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200), "file://" + getFile(BASE_DIR + "NorthArrow_10.svg").toString(),
                backgroundColor, 90.0, getTaskDirectory(), this.requestFactoryWrapper);

        BufferedImage referenceImage = ImageSimilarity.convertFromSvg(file, 200, 200);
//        FileUtils.copyFile(new File(file), new File("/tmp/north-arrow_10.svg"));
//        ImageSimilarity.writeUncompressedImage(referenceImage, "/tmp/expected-north-arrow_10-bg.tiff");
        new ImageSimilarity(referenceImage, 2).assertSimilarity(getFile(BASE_DIR + "expected-north-arrow_10-bg.tiff"), 75);
    }

    @Test
    public void testCreateDefaultGraphic() throws Exception {
        URI file = NorthArrowGraphic.create(
                new Dimension(200, 200), null,
                this.bgColor, 90.0, getTaskDirectory(), this.requestFactoryWrapper);

        BufferedImage referenceImage = ImageSimilarity.convertFromSvg(file, 200, 200);
//        FileUtils.copyFile(new File(file), new File("/tmp/north-arrow_10-default.svg"));
//        ImageSimilarity.writeUncompressedImage(referenceImage, "/tmp/expected-north-arrow_10-default.tiff");
        new ImageSimilarity(referenceImage, 2).assertSimilarity(getFile(BASE_DIR + "expected-north-arrow_10-default.tiff"), 75);
    }
}
