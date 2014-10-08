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

package org.mapfish.print.processor.map.scalebar;

import com.google.common.annotations.VisibleForTesting;
import org.apache.batik.svggen.SVGGraphics2D;
import org.geotools.referencing.GeodeticCalculator;
import org.mapfish.print.attribute.ScalebarAttribute.ScalebarAttributeValues;
import org.mapfish.print.attribute.map.MapAttribute.MapAttributeValues;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.config.Template;
import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.map.Scale;
import org.mapfish.print.processor.map.CreateMapProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Creates a scalebar graphic.
 */
public class ScalebarGraphic {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScalebarGraphic.class);

    private static final int MAX_NUMBER_LAYOUTING_TRIES = 3;

    /**
     * Render the scalebar.
     *  @param mapParams        The parameters of the map for which the scalebar is created.
     * @param scalebarParams    The scalebar parameters.
     * @param tempFolder        The directory in which the graphic file is created.
     * @param template          The template that containts the scalebar processor
     */
    public final URI render(final MapAttributeValues mapParams,
                            final ScalebarAttributeValues scalebarParams,
                            final File tempFolder,
                            final Template template)
            throws IOException, ParserConfigurationException {
        final double dpi = mapParams.getDpi();
        final double dpiRatio = dpi / mapParams.getRequestorDPI();

        // get the map bounds
        final Rectangle paintArea = new Rectangle(mapParams.getMapSize());
        MapBounds bounds = mapParams.getMapBounds();
        bounds = CreateMapProcessor.adjustBoundsToScaleAndMapSize(
                mapParams, mapParams.getRequestorDPI(), paintArea, bounds);
        paintArea.setBounds(0, 0, (int) (paintArea.width * dpiRatio), (int) (paintArea.height * dpiRatio));

        final DistanceUnit mapUnit = getUnit(bounds);
        // to calculate the scale the requestor DPI is used , because the paint area is already adjusted
        final Scale scale = bounds.getScaleDenominator(paintArea, mapParams.getRequestorDPI());

        DistanceUnit scaleUnit = scalebarParams.getUnit();
        if (scaleUnit == null) {
            scaleUnit = mapUnit;
        }

        // adjust scalebar width and height to the DPI value
        final int maxWidthInPixelAdjusted = (int) (scalebarParams.getSize().width * dpiRatio);
        final int maxHeightInPixelAdjusted = (int) (scalebarParams.getSize().height * dpiRatio);
        final int maxLengthInPixelAdjusted = (scalebarParams.getOrientation().isHorizontal()) ?
                maxWidthInPixelAdjusted : maxHeightInPixelAdjusted;

        final double maxIntervalLengthInWorldUnits = DistanceUnit.PX.convertTo(maxLengthInPixelAdjusted, scaleUnit) 
                * scale.getDenominator() / scalebarParams.intervals;
        final double niceIntervalLengthInWorldUnits =
                getNearestNiceValue(maxIntervalLengthInWorldUnits, scaleUnit, scalebarParams.lockUnits);

        final ScaleBarRenderSettings settings = new ScaleBarRenderSettings();
        settings.setParams(scalebarParams);
        settings.setMaxSize(new Dimension(maxWidthInPixelAdjusted, maxHeightInPixelAdjusted));
        settings.setDpiRatio(dpiRatio);
        settings.setPadding(getPadding(settings));

        // start the rendering
        File path = null;
        if (template.getConfiguration().renderAsSvg(scalebarParams.renderAsSvg)) {
            // render scalebar as SVG
            final SVGGraphics2D graphics2D = CreateMapProcessor.getSvgGraphics(
                    new Dimension(maxWidthInPixelAdjusted, maxHeightInPixelAdjusted));

            try {
                tryLayout(graphics2D, scaleUnit, scale, niceIntervalLengthInWorldUnits, settings, 0);

                path = File.createTempFile("scalebar-graphic-", ".svg", tempFolder);
                CreateMapProcessor.saveSvgFile(graphics2D, path);
            } finally {
                graphics2D.dispose();
            }
        } else {
            // render scalebar as raster graphic
            final BufferedImage bufferedImage = new BufferedImage(maxWidthInPixelAdjusted, maxHeightInPixelAdjusted,
                    BufferedImage.TYPE_4BYTE_ABGR);
            final Graphics2D graphics2D = bufferedImage.createGraphics();

            try {
                tryLayout(graphics2D, scaleUnit, scale, niceIntervalLengthInWorldUnits, settings, 0);

                path = File.createTempFile("scalebar-graphic-", ".tiff", tempFolder);
                ImageIO.write(bufferedImage, "tiff", path);
            } finally {
                graphics2D.dispose();
            }
        }

        return path.toURI();
    }

    private DistanceUnit getUnit(final MapBounds bounds) {
        GeodeticCalculator calculator = new GeodeticCalculator(bounds.getProjection());
        return DistanceUnit.fromString(calculator.getEllipsoid().getAxisUnit().toString());
    }

    /**
     * Try recursively to find the correct layout.
     */
    private void tryLayout(final Graphics2D graphics2D, final DistanceUnit scaleUnit, final Scale scale,
            final double intervalLengthInWorldUnits, final ScaleBarRenderSettings settings, final int tryNumber) {
        if (tryNumber > MAX_NUMBER_LAYOUTING_TRIES) {
            // if no good layout can be found, stop. an empty scalebar graphic will be shown.
            LOGGER.error("layouting the scalebar failed (unit: " + scaleUnit.toString()
                    + ", scale: " + scale.getDenominator() + ")");
            return;
        }

        final ScalebarAttributeValues scalebarParams = settings.getParams();
        final DistanceUnit intervalUnit = bestUnit(scaleUnit, intervalLengthInWorldUnits, scalebarParams.lockUnits);
        final float intervalLengthInPixels = (float) scaleUnit.convertTo(
                intervalLengthInWorldUnits / scale.getDenominator(), DistanceUnit.PX);

        //compute the label positions
        final List<Label> labels = new ArrayList<Label>(scalebarParams.intervals + 1);
        final float leftLabelMargin;
        final float rightLabelMargin;
        final float topLabelMargin;
        final float bottomLabelMargin;

        final Font font = new Font(scalebarParams.font, Font.PLAIN, getFontSize(settings));
        final FontRenderContext frc = new FontRenderContext(null, true, true);

        if (scalebarParams.intervals > 1 || scalebarParams.subIntervals) {
            //the label will be centered under each tick mark
            for (int i = 0; i <= scalebarParams.intervals; i++) {
                String labelText = createLabelText(scaleUnit, intervalLengthInWorldUnits * i, intervalUnit);
                if (i == scalebarParams.intervals) {
                    // only show unit for the last label
                    labelText += intervalUnit;
                }
                TextLayout labelLayout = new TextLayout(labelText, font, frc);
                labels.add(new Label(intervalLengthInPixels * i, labelLayout, scalebarParams.getOrientation()));
            }
            leftLabelMargin = labels.get(0).getWidth() / 2.0f;
            rightLabelMargin = labels.get(labels.size() - 1).getWidth() / 2.0f;
            topLabelMargin = labels.get(0).getHeight() / 2.0f;
            bottomLabelMargin = labels.get(labels.size() - 1).getHeight() / 2.0f;
        } else {
            //if there is only one interval, place the label centered between the two tick marks
            String labelText = createLabelText(scaleUnit, intervalLengthInWorldUnits, intervalUnit) + intervalUnit;
            TextLayout labelLayout = new TextLayout(labelText, font, frc);
            final Label label = new Label(intervalLengthInPixels / 2.0f,
                    labelLayout, scalebarParams.getOrientation());
            labels.add(label);
            rightLabelMargin = Math.max(0.0f, label.getWidth() - intervalLengthInPixels) / 2.0f;
            leftLabelMargin = rightLabelMargin;
            topLabelMargin = Math.max(0.0f, label.getHeight() - intervalLengthInPixels) / 2.0f;
            bottomLabelMargin = topLabelMargin;
        }

        if (fitsAvailableSpace(scalebarParams, intervalLengthInPixels,
                leftLabelMargin, rightLabelMargin, topLabelMargin, bottomLabelMargin, settings)) {
            //the layout fits the maxSize
            settings.setLabels(labels);
            settings.setScaleUnit(scaleUnit);
            settings.setIntervalLengthInPixels(intervalLengthInPixels);
            settings.setIntervalLengthInWorldUnits(intervalLengthInWorldUnits);
            settings.setIntervalUnit(intervalUnit);
            settings.setLeftLabelMargin(leftLabelMargin);
            settings.setRightLabelMargin(rightLabelMargin);
            settings.setTopLabelMargin(topLabelMargin);
            settings.setBottomLabelMargin(bottomLabelMargin);

            doLayout(graphics2D, scalebarParams, settings);
        } else {
            //not enough room because of the labels, try a smaller bar
            //CSOFF: MagicNumber
            double nextIntervalDistance = getNearestNiceValue(intervalLengthInWorldUnits * 0.9, scaleUnit, scalebarParams.lockUnits);
            //CSOFF: MagicNumber
            tryLayout(graphics2D, scaleUnit, scale, nextIntervalDistance, settings, tryNumber + 1);
        }
    }

    private boolean fitsAvailableSpace(final ScalebarAttributeValues scalebarParams,
            final float intervalWidthInPixels, final float leftLabelMargin,
            final float rightLabelMargin, final float topLabelMargin,
            final float bottomLabelMargin, final ScaleBarRenderSettings settings) {
        if (scalebarParams.getOrientation().isHorizontal()) {
            return scalebarParams.intervals * intervalWidthInPixels + leftLabelMargin + rightLabelMargin + 2 * settings.getPadding()
                    <= settings.getMaxSize().width;
        } else {
            return scalebarParams.intervals * intervalWidthInPixels + topLabelMargin + bottomLabelMargin + 2 * settings.getPadding()
                    <= settings.getMaxSize().height;
        }
    }

    /**
     * Called when the position of the labels and their content is known.
     * <p/>
     * Creates the drawer which draws the scalebar.
     */
    private void doLayout(final Graphics2D graphics2d, final ScalebarAttributeValues scalebarParams,
            final ScaleBarRenderSettings settings) {
        final Dimension maxLabelSize = getMaxLabelSize(settings);

        int numSubIntervals = 1;
        if (scalebarParams.subIntervals) {
            numSubIntervals = getNbSubIntervals(
                    settings.getScaleUnit(), settings.getIntervalLengthInWorldUnits(), settings.getIntervalUnit());
        }

        settings.setBarSize(getBarSize(settings));
        settings.setLabelDistance(getLabelDistance(settings));
        settings.setLineWidth(getLineWidth(settings));
        settings.setSize(getSize(scalebarParams, settings, maxLabelSize));
        settings.setMaxLabelSize(maxLabelSize);
        settings.setNumSubIntervals(numSubIntervals);

        ScalebarDrawer drawer = scalebarParams.getType().createDrawer(graphics2d, settings);
        drawer.draw();
    }

    /**
     * Get the size of the painting area required to draw the scalebar with labels.
     *
     * @param scalebarParams    Parameters for the scalebar.
     * @param settings          Parameters for rendering the scalebar.
     * @param maxLabelSize      The max. size of the labels.
     */
    @VisibleForTesting
    protected static Dimension getSize(final ScalebarAttributeValues scalebarParams,
            final ScaleBarRenderSettings settings, final Dimension maxLabelSize) {
        final float width;
        final float height;
        if (scalebarParams.getOrientation().isHorizontal()) {
            width = 2 * settings.getPadding()
                + settings.getIntervalLengthInPixels() * scalebarParams.intervals
                + settings.getLeftLabelMargin() + settings.getRightLabelMargin();
            height = 2 * settings.getPadding()
                + settings.getBarSize() + settings.getLabelDistance()
                + maxLabelSize.height;
        } else {
            width = 2 * settings.getPadding()
                + maxLabelSize.width + settings.getLabelDistance()
                + settings.getBarSize();
            height = 2 * settings.getPadding()
                + settings.getTopLabelMargin()
                + settings.getIntervalLengthInPixels() * scalebarParams.intervals
                + settings.getBottomLabelMargin();
        }
        return new Dimension((int) Math.ceil(width), (int) Math.ceil(height));
    }

    /**
     * Get the maximum width and height of the labels.
     * @param settings Parameters for rendering the scalebar.
     */
    @VisibleForTesting
    protected static Dimension getMaxLabelSize(final ScaleBarRenderSettings settings) {
        float maxLabelHeight = 0.0f;
        float maxLabelWidth = 0.0f;
        for (int i = 0; i < settings.getLabels().size(); i++) {
            Label label = settings.getLabels().get(i);
            maxLabelHeight = Math.max(maxLabelHeight, label.getHeight());
            maxLabelWidth = Math.max(maxLabelWidth, label.getWidth());
        }
        return new Dimension((int) Math.ceil(maxLabelWidth), (int) Math.ceil(maxLabelHeight));
    }

    /**
     * Format the label text.
     * @param scaleUnit     The unit used for the scalebar.
     * @param value         The scale value.
     * @param intervalUnit  The scaled unit for the intervals.
     */
    @VisibleForTesting
    protected static String createLabelText(final DistanceUnit scaleUnit, final double value, final DistanceUnit intervalUnit) {
        double scaledValue = scaleUnit.convertTo(value, intervalUnit);

        //CSOFF: MagicNumber
        // assume that there is no interval smaller then 0.0001
        scaledValue = Math.round(scaledValue * 10000) / 10000;
        //CSON: MagicNumber
        String decimals = Double.toString(scaledValue).split("\\.")[1];

        if (Double.valueOf(decimals) == 0) {
            return Long.toString(Math.round(scaledValue));
        } else {
            return Double.toString(scaledValue);
        }
    }

    private DistanceUnit bestUnit(final DistanceUnit scaleUnit, final double intervalDistance, final boolean lockUnits) {
        if (lockUnits) {
            return scaleUnit;
        } else {
            return DistanceUnit.getBestUnit(intervalDistance, scaleUnit);
        }
    }

    /**
     * Reduce the given value to the nearest smaller 1 significant digit number starting
     * with 1, 2 or 5.
     * @param value     the value to find a nice number for.
     * @param scaleUnit the unit of the value.
     * @param lockUnits if set, the values are not scaled to a "nicer" unit.
     */
    @VisibleForTesting
    //CSOFF: MagicNumber
    protected final double getNearestNiceValue(final double value, final DistanceUnit scaleUnit, final boolean lockUnits) {
        DistanceUnit bestUnit = bestUnit(scaleUnit, value, lockUnits);
        double factor = scaleUnit.convertTo(1.0, bestUnit);

        // nearest power of 10 lower than value
        int digits = (int) Math.floor((Math.log(value * factor) / Math.log(10)));
        double pow10 = Math.pow(10, digits);

        // ok, find first character
        double firstChar = value * factor / pow10;

        // right, put it into the correct bracket
        int barLen;
        if (firstChar >= 10.0) {
            barLen = 10;
        } else if (firstChar >= 5.0) {
            barLen = 5;
        } else if (firstChar >= 2.0) {
            barLen = 2;
        } else {
            barLen = 1;
        }

        // scale it up the correct power of 10
        return barLen * pow10 / factor;
    }
    //CSON: MagicNumber

    /**
     * @return The "nicest" number of sub intervals in function of the interval distance.
     */
    //CSOFF: MagicNumber
    private int getNbSubIntervals(final DistanceUnit scaleUnit, final double intervalDistance, final DistanceUnit intervalUnit) {
        double value = scaleUnit.convertTo(intervalDistance, intervalUnit);
        int digits = (int) (Math.log(value) / Math.log(10));
        double pow10 = Math.pow(10, digits);

        // ok, find first character
        int firstChar = (int) (value / pow10);
        switch (firstChar) {
            case 1:
                return 2;
            case 2:
                return 2;
            case 5:
                return 5;
            case 10:
                return 2;
            default:
                throw new RuntimeException("Invalid interval: " + value + intervalUnit + " (" + firstChar + ")");
        }
    }
    //CSON: MagicNumber

    private int getFontSize(final ScaleBarRenderSettings settings) {
        return (int) Math.ceil(settings.getParams().fontSize * settings.getDpiRatio());
    }

    //CSOFF: MagicNumber
    private static int getLineWidth(final ScaleBarRenderSettings settings) {
        if (settings.getParams().lineWidth != null) {
            return (int) Math.ceil(settings.getParams().lineWidth * settings.getDpiRatio());
        } else {
            if (settings.getParams().getOrientation().isHorizontal()) {
                return settings.getMaxSize().width / 150;
            } else {
                return settings.getMaxSize().height / 150;
            }
        }
    }
    //CSON: MagicNumber

    /**
     * Get the bar size.
     * @param settings Parameters for rendering the scalebar.
     */
    //CSOFF: MagicNumber
    @VisibleForTesting
    protected static int getBarSize(final ScaleBarRenderSettings settings) {
        if (settings.getParams().barSize != null) {
            return (int) Math.ceil(settings.getParams().barSize * settings.getDpiRatio());
        } else {
            if (settings.getParams().getOrientation().isHorizontal()) {
                return settings.getMaxSize().height / 4;
            } else {
                return settings.getMaxSize().width / 4;
            }
        }
    }
    //CSON: MagicNumber

    /**
     * Get the label distance..
     * @param settings Parameters for rendering the scalebar.
     */
    //CSOFF: MagicNumber
    @VisibleForTesting
    protected static int getLabelDistance(final ScaleBarRenderSettings settings) {
        if (settings.getParams().labelDistance != null) {
            return (int) Math.ceil(settings.getParams().labelDistance * settings.getDpiRatio());
        } else {
            if (settings.getParams().getOrientation().isHorizontal()) {
                return settings.getMaxSize().width / 40;
            } else {
                return settings.getMaxSize().height / 40;
            }
        }
    }
    //CSON: MagicNumber

    //CSOFF: MagicNumber
    private static int getPadding(final ScaleBarRenderSettings settings) {
        if (settings.getParams().padding != null) {
            return (int) Math.ceil(settings.getParams().padding * settings.getDpiRatio());
        } else {
            if (settings.getParams().getOrientation().isHorizontal()) {
                return settings.getMaxSize().width / 40;
            } else {
                return settings.getMaxSize().height / 40;
            }
        }
    }
    //CSON: MagicNumber
}
