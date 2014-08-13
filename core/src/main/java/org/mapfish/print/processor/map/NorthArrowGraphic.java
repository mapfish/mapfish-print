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

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.io.Closer;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.dom.util.DOMUtilities;
import org.apache.batik.util.SVGConstants;
import org.apache.batik.util.XMLResourceDescriptor;
import org.mapfish.print.map.style.json.ColorParser;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGElement;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.imageio.ImageIO;

/**
 * Takes care of scaling and rotating a graphic for the north-arrow.
 */
public final class NorthArrowGraphic {

    private static final String DEFAULT_GRAPHIC = "NorthArrow_10.svg";
    private static final String SVG_NS = SVGDOMImplementation.SVG_NAMESPACE_URI;

    private NorthArrowGraphic() { }

    /**
     * Creates the north-arrow graphic.
     *
     * Scales the given graphic to the given size and applies the given
     * rotation.
     *
     * @param targetSize                The size of the graphic to create.
     * @param graphicFile               The graphic to use as north-arrow.
     * @param backgroundColor           The background color.
     * @param rotation                  The rotation to apply.
     * @param workingDir                The directory in which the graphic is created.
     * @param graphicLoader             The graphic loader.
     * @param clientHttpRequestFactory  The request factory.
     * @return                          The path to the created graphic.
     */
    public static URI create(
            final Dimension targetSize,
            final String graphicFile,
            final Color backgroundColor, 
            final Double rotation,
            final File workingDir,
            final GraphicLoader graphicLoader,
            final ClientHttpRequestFactory clientHttpRequestFactory) throws Exception {
        final Closer closer = Closer.create();
        try {
            final InputStream input = loadGraphic(graphicFile, graphicLoader, clientHttpRequestFactory, closer);
            if (graphicFile == null || graphicFile.toLowerCase().trim().endsWith("svg")) {
                return createSvg(targetSize, input, rotation, backgroundColor, workingDir, clientHttpRequestFactory);
            } else {
                return createRaster(targetSize, input, rotation, backgroundColor, workingDir, clientHttpRequestFactory);
            }
        } finally {
            closer.close();
        }
    }

    private static InputStream loadGraphic(final String graphicFile,
            final GraphicLoader graphicLoader,
            final ClientHttpRequestFactory clientHttpRequestFactory,
            final Closer closer) throws IOException, URISyntaxException {
        if (Strings.isNullOrEmpty(graphicFile)) {
            // if no graphic is set, take a default graphic
            URL file = NorthArrowGraphic.class.getResource(DEFAULT_GRAPHIC);
            InputStream inputStream = new BufferedInputStream(new FileInputStream(new File(file.toURI())));
            return closer.register(inputStream);
        }

        // try to load graphic from configuration directory
        Optional<InputStream> input = graphicLoader.load(graphicFile, clientHttpRequestFactory);
        if (input.isPresent()) {
            return closer.register(input.get());
        } else {
            // load graphic from URL
            final URI uri = new URI(graphicFile);
            final ClientHttpRequest request = clientHttpRequestFactory.createRequest(uri, HttpMethod.GET);
            final ClientHttpResponse response = closer.register(request.execute());
            return new BufferedInputStream(response.getBody());
        }
    }

    /**
     * Renders a given graphic into a new image, scaled to fit the new size and rotated.
     */
    private static URI createRaster(final Dimension targetSize, final InputStream inputStream,
            final Double rotation, final Color backgroundColor,
            final File workingDir, final ClientHttpRequestFactory clientHttpRequestFactory) throws IOException {
        final File path = File.createTempFile("north-arrow-", ".tiff", workingDir);

        // TODO apply DPI value?
        final BufferedImage newImage = new BufferedImage(targetSize.width, targetSize.height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics2d = null;
        try {
            graphics2d = newImage.createGraphics();
            BufferedImage originalImage = ImageIO.read(inputStream);

            // set background color
            graphics2d.setColor(backgroundColor);
            graphics2d.fillRect(0, 0, targetSize.width, targetSize.height);

            // scale the original image to fit the new size
            int newWidth, newHeight;
            if (originalImage.getWidth() > originalImage.getHeight()) {
                newWidth = targetSize.width;
                newHeight = Math.min(
                                targetSize.height,
                                (int) Math.ceil(newWidth / (originalImage.getWidth() / (double) originalImage.getHeight())));
            } else {
                newHeight = targetSize.height;
                newWidth = Math.min(
                                targetSize.width,
                                (int) Math.ceil(newHeight / (originalImage.getHeight() / (double) originalImage.getWidth())));
            }

            // position the original image in the center of the new
            int deltaX = (int) Math.floor((targetSize.width - newWidth) / 2.0);
            int deltaY = (int) Math.floor((targetSize.height - newHeight) / 2.0);

            if (rotation != 0.0) {
                final AffineTransform rotate = AffineTransform.getRotateInstance(
                        Math.toRadians(rotation), targetSize.width / 2, targetSize.height / 2);
                graphics2d.setTransform(rotate);
            }

            graphics2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics2d.drawImage(originalImage, deltaX, deltaY, newWidth, newHeight, null);

            ImageIO.write(newImage, "tiff", path);
        } finally {
            if (graphics2d != null) {
                graphics2d.dispose();
            }
        }
        return path.toURI();
    }

    /**
     * With the Batik SVG library it is only possible to create new SVG graphics,
     * but you can not modify an existing graphic. So, we are loading the SVG file
     * as plain XML and doing the modifications by hand.
     */
    private static URI createSvg(final Dimension targetSize,
            final InputStream inputStream, final Double rotation,
            final Color backgroundColor, final File workingDir,
            final ClientHttpRequestFactory clientHttpRequestFactory)
            throws IOException {
        // load SVG graphic
        final SVGElement svgRoot = parseSvg(inputStream);

        // create a new SVG graphic in which the existing graphic is embedded (scaled and rotated)
        DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
        Document newDocument = impl.createDocument(SVG_NS, "svg", null);
        SVGElement newSvgRoot = (SVGElement) newDocument.getDocumentElement();
        newSvgRoot.setAttributeNS(null, "width", Integer.toString(targetSize.width));
        newSvgRoot.setAttributeNS(null, "height", Integer.toString(targetSize.height));

        setSvgBackground(backgroundColor, targetSize, newDocument, newSvgRoot);
        embedSvgGraphic(svgRoot, newSvgRoot, newDocument, targetSize, rotation);
        File path = writeSvgToFile(newDocument, workingDir);

        return path.toURI();
    }

    private static void setSvgBackground(final Color backgroundColor,  final Dimension targetSize,
            final Document newDocument, final SVGElement newSvgRoot) {
        final Element rect = newDocument.createElementNS(SVG_NS, "rect");
        rect.setAttributeNS(null, "x", "0");
        rect.setAttributeNS(null, "y", "0");
        rect.setAttributeNS(null, "width", Integer.toString(targetSize.width));
        rect.setAttributeNS(null, "height", Integer.toString(targetSize.height));
        String bgColor = ColorParser.toRGB(backgroundColor);
        rect.setAttributeNS(null, "fill", bgColor);
        // CSOFF: MagicNumber
        String opacity = Double.toString(backgroundColor.getAlpha() / 255.0);
        // CSON: MagicNumber
        rect.setAttributeNS(null, "fill-opacity", opacity);
        newSvgRoot.appendChild(rect);
    }

    /**
     * Embeds the given SVG element into a new SVG element scaling
     * the graphic to the given dimension and applying the given
     * rotation.
     */
    private static void embedSvgGraphic(final SVGElement svgRoot,
            final SVGElement newSvgRoot, final Document newDocument,
            final Dimension targetSize, final Double rotation) {
        final String originalWidth = svgRoot.getAttributeNS(null, "width");
        final String originalHeight = svgRoot.getAttributeNS(null, "height");
        /*
         * To scale the SVG graphic and to apply the rotation, we distinguish two
         * cases: width and height is set on the original SVG or not.
         *
         * Case 1: Width and height is set
         * If width and height is set, we wrap the original SVG into 2 new SVG elements
         * and a container element.
         *
         * Example:
         *      Original SVG:
         *          <svg width="100" height="100"></svg>
         *
         *      New SVG (scaled to 300x300 and rotated by 90 degree):
         *          <svg width="300" height="300">
         *              <g transform="rotate(90.0 150 150)">
         *                  <svg width="100%" height="100%" viewBox="0 0 100 100">
         *                      <svg width="100" height="100"></svg>
         *                  </svg>
         *              </g>
         *          </svg>
         *
         * The requested size is set on the outermost <svg>. Then, the rotation is applied to the
         * <g> container and the scaling is achieved with the viewBox parameter on the 2nd <svg>.
         *
         *
         * Case 2: Width and height is not set
         * In this case the original SVG is wrapped into just one container and one new SVG element.
         * The rotation is set on the container, and the scaling happens automatically.
         *
         * Example:
         *      Original SVG:
         *          <svg viewBox="0 0 61.06 91.83"></svg>
         *
         *      New SVG (scaled to 300x300 and rotated by 90 degree):
         *          <svg width="300" height="300">
         *              <g transform="rotate(90.0 150 150)">
         *                  <svg viewBox="0 0 61.06 91.83"></svg>
         *              </g>
         *          </svg>
         */
        if (!Strings.isNullOrEmpty(originalWidth) && !Strings.isNullOrEmpty(originalHeight)) {
            Element wrapperContainer = newDocument.createElementNS(SVG_NS, "g");
            wrapperContainer.setAttributeNS(
                    null,
                    SVGConstants.SVG_TRANSFORM_ATTRIBUTE,
                    getRotateTransformation(targetSize, rotation));
            newSvgRoot.appendChild(wrapperContainer);

            Element wrapperSvg = newDocument.createElementNS(SVG_NS, "svg");
            wrapperSvg.setAttributeNS(null, "width", "100%");
            wrapperSvg.setAttributeNS(null, "height", "100%");
            wrapperSvg.setAttributeNS(null, "viewBox", "0 0 " + originalWidth
                    + " " + originalHeight);
            wrapperContainer.appendChild(wrapperSvg);

            Node svgRootImported = newDocument.importNode(svgRoot, true);
            wrapperSvg.appendChild(svgRootImported);
        } else {
            Element wrapperContainer = newDocument.createElementNS(SVG_NS, "g");
            wrapperContainer.setAttributeNS(
                    null,
                    SVGConstants.SVG_TRANSFORM_ATTRIBUTE,
                    getRotateTransformation(targetSize, rotation));
            newSvgRoot.appendChild(wrapperContainer);

            Node svgRootImported = newDocument.importNode(svgRoot, true);
            wrapperContainer.appendChild(svgRootImported);
        }
    }

    private static String getRotateTransformation(final Dimension targetSize,
            final double rotation) {
        return "rotate(" + Double.toString(rotation) + " "
                + Integer.toString(targetSize.width / 2) + " "
                + Integer.toString(targetSize.height / 2) + ")";
    }

    private static SVGElement parseSvg(final InputStream inputStream)
            throws IOException {
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
        SVGDocument document = (SVGDocument) f.createDocument("", inputStream);
        return (SVGElement) document.getDocumentElement();
    }

    private static File writeSvgToFile(final Document document,
            final File workingDir) throws IOException {
        final File path = File.createTempFile("north-arrow-", ".svg", workingDir);
        FileWriter fw = null;
        try {
            fw = new FileWriter(path);
            DOMUtilities.writeDocument(document, fw);
            fw.flush();
        } finally {
            if (fw != null) {
                fw.close();
            }
        }
        return path;
    }

    /**
     * Graphic loader to load a graphic from the configuration directory.
     */
    public interface GraphicLoader {
        /**
         * Load the graphic.
         * @param graphicFile               The graphic file name.
         * @param clientHttpRequestFactory  The request factory.
         */
        Optional<InputStream> load(final String graphicFile, final ClientHttpRequestFactory clientHttpRequestFactory) throws IOException;
    }
}
