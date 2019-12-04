package org.mapfish.print.processor.map.scalebar;

import com.google.common.annotations.VisibleForTesting;
import org.apache.batik.svggen.SVGGraphics2D;
import org.geotools.referencing.GeodeticCalculator;
import org.mapfish.print.ImageUtils;
import org.mapfish.print.attribute.ScalebarAttribute.ScalebarAttributeValues;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapfishMapContext;
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
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;

import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;
import static java.awt.image.BufferedImage.TYPE_4BYTE_ABGR;
import static org.mapfish.print.Constants.PDF_DPI;

/**
 * Creates a scalebar graphic.
 */
public class ScalebarGraphic {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScalebarGraphic.class);

    private static final int MAX_NUMBER_LAYOUTING_TRIES = 3;

    /**
     * Try recursively to find the correct layout.
     */
    private static void tryLayout(
            final Graphics2D graphics2D, final DistanceUnit scaleUnit, final double scaleDenominator,
            final double intervalLengthInWorldUnits, final ScaleBarRenderSettings settings,
            final int tryNumber) {
        if (tryNumber > MAX_NUMBER_LAYOUTING_TRIES) {
            // if no good layout can be found, stop. an empty scalebar graphic will be shown.
            LOGGER.error("layouting the scalebar failed (unit: {}, scale: {})", scaleUnit, scaleDenominator);
            return;
        }

        final ScalebarAttributeValues scalebarParams = settings.getParams();
        final DistanceUnit intervalUnit =
                bestUnit(scaleUnit, intervalLengthInWorldUnits, scalebarParams.lockUnits);
        final float intervalLengthInPixels = (float) scaleUnit.convertTo(
                intervalLengthInWorldUnits / scaleDenominator, DistanceUnit.PX);

        //compute the label positions
        final List<Label> labels = new ArrayList<>(scalebarParams.intervals + 1);
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
                labels.add(new Label(intervalLengthInPixels * i, labelLayout, graphics2D));
            }
            leftLabelMargin = labels.get(0).getRotatedWidth(scalebarParams.getLabelRotation()) / 2.0f;
            rightLabelMargin =
                    labels.get(labels.size() - 1).getRotatedWidth(scalebarParams.getLabelRotation()) / 2.0f;
            topLabelMargin = labels.get(0).getRotatedHeight(scalebarParams.getLabelRotation()) / 2.0f;
            bottomLabelMargin =
                    labels.get(labels.size() - 1).getRotatedHeight(scalebarParams.getLabelRotation()) / 2.0f;
        } else {
            //if there is only one interval, place the label centered between the two tick marks
            String labelText =
                    createLabelText(scaleUnit, intervalLengthInWorldUnits, intervalUnit) + intervalUnit;
            TextLayout labelLayout = new TextLayout(labelText, font, frc);
            final Label label = new Label(intervalLengthInPixels / 2.0f, labelLayout, graphics2D);
            labels.add(label);
            rightLabelMargin = 0;
            leftLabelMargin = 0;
            topLabelMargin = 0;
            bottomLabelMargin = 0;
        }

        if (fitsAvailableSpace(scalebarParams, intervalLengthInPixels,
                               leftLabelMargin, rightLabelMargin, topLabelMargin, bottomLabelMargin,
                               settings)) {
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
            double nextIntervalDistance = getNearestNiceValue(
                    intervalLengthInWorldUnits * 0.9, scaleUnit, scalebarParams.lockUnits);
            tryLayout(graphics2D, scaleUnit, scaleDenominator, nextIntervalDistance, settings,
                      tryNumber + 1);
        }
    }

    private static boolean fitsAvailableSpace(
            final ScalebarAttributeValues scalebarParams,
            final float intervalWidthInPixels, final float leftLabelMargin,
            final float rightLabelMargin, final float topLabelMargin,
            final float bottomLabelMargin, final ScaleBarRenderSettings settings) {
        if (scalebarParams.getOrientation().isHorizontal()) {
            return scalebarParams.intervals * intervalWidthInPixels + leftLabelMargin +
                    rightLabelMargin + 2 * settings.getPadding()
                    <= settings.getMaxSize().width;
        } else {
            return scalebarParams.intervals * intervalWidthInPixels + topLabelMargin +
                    bottomLabelMargin + 2 * settings.getPadding()
                    <= settings.getMaxSize().height;
        }
    }

    /**
     * Called when the position of the labels and their content is known.
     *
     * Creates the drawer which draws the scalebar.
     */
    private static void doLayout(
            final Graphics2D graphics2d, final ScalebarAttributeValues scalebarParams,
            final ScaleBarRenderSettings settings) {
        final Dimension maxLabelSize = getMaxLabelSize(settings);

        int numSubIntervals = 1;
        if (scalebarParams.subIntervals) {
            numSubIntervals = getNbSubIntervals(
                    settings.getScaleUnit(), settings.getIntervalLengthInWorldUnits(),
                    settings.getIntervalUnit());
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
     * @param scalebarParams Parameters for the scalebar.
     * @param settings Parameters for rendering the scalebar.
     * @param maxLabelSize The max. size of the labels.
     */
    @VisibleForTesting
    protected static Dimension getSize(
            final ScalebarAttributeValues scalebarParams,
            final ScaleBarRenderSettings settings, final Dimension maxLabelSize) {
        final float width;
        final float height;
        if (scalebarParams.getOrientation().isHorizontal()) {
            width = 2 * settings.getPadding()
                    + settings.getIntervalLengthInPixels() * scalebarParams.intervals
                    + settings.getLeftLabelMargin() + settings.getRightLabelMargin();
            height = 2 * settings.getPadding()
                    + settings.getBarSize() + settings.getLabelDistance()
                    + Label.getRotatedHeight(maxLabelSize, scalebarParams.getLabelRotation());
        } else {
            width = 2 * settings.getPadding()
                    + settings.getLabelDistance() + settings.getBarSize()
                    + Label.getRotatedWidth(maxLabelSize, scalebarParams.getLabelRotation());
            height = 2 * settings.getPadding()
                    + settings.getTopLabelMargin()
                    + settings.getIntervalLengthInPixels() * scalebarParams.intervals
                    + settings.getBottomLabelMargin();
        }
        return new Dimension((int) Math.ceil(width), (int) Math.ceil(height));
    }

    /**
     * Get the maximum width and height of the labels.
     *
     * @param settings Parameters for rendering the scalebar.
     */
    @VisibleForTesting
    protected static Dimension getMaxLabelSize(final ScaleBarRenderSettings settings) {
        float maxLabelHeight = 0.0f;
        float maxLabelWidth = 0.0f;
        for (final Label label: settings.getLabels()) {
            maxLabelHeight = Math.max(maxLabelHeight, label.getHeight());
            maxLabelWidth = Math.max(maxLabelWidth, label.getWidth());
        }
        return new Dimension((int) Math.ceil(maxLabelWidth), (int) Math.ceil(maxLabelHeight));
    }

    /**
     * Format the label text.
     *
     * @param scaleUnit The unit used for the scalebar.
     * @param value The scale value.
     * @param intervalUnit The scaled unit for the intervals.
     */
    @VisibleForTesting
    protected static String createLabelText(
            final DistanceUnit scaleUnit, final double value, final DistanceUnit intervalUnit) {
        double scaledValue = scaleUnit.convertTo(value, intervalUnit);

        // assume that there is no interval smaller then 0.0001
        scaledValue = Math.round(scaledValue * 10000) / 10000;
        String decimals = Double.toString(scaledValue).split("\\.")[1];

        if (Double.valueOf(decimals) == 0) {
            return Long.toString(Math.round(scaledValue));
        } else {
            return Double.toString(scaledValue);
        }
    }

    private static DistanceUnit bestUnit(
            final DistanceUnit scaleUnit, final double intervalDistance, final boolean lockUnits) {
        if (lockUnits) {
            return scaleUnit;
        } else {
            return DistanceUnit.getBestUnit(intervalDistance, scaleUnit);
        }
    }

    /**
     * Reduce the given value to the nearest smaller 1 significant digit number starting with 1, 2 or 5.
     *
     * @param value the value to find a nice number for.
     * @param scaleUnit the unit of the value.
     * @param lockUnits if set, the values are not scaled to a "nicer" unit.
     */
    @VisibleForTesting
    protected static double getNearestNiceValue(
            final double value, final DistanceUnit scaleUnit, final boolean lockUnits) {
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

    /**
     * @return The "nicest" number of sub intervals in function of the interval distance.
     */
    private static int getNbSubIntervals(
            final DistanceUnit scaleUnit, final double intervalDistance, final DistanceUnit intervalUnit) {
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
                throw new RuntimeException(
                        "Invalid interval: " + value + intervalUnit + " (" + firstChar + ")");
        }
    }

    private static int getFontSize(final ScaleBarRenderSettings settings) {
        return settings.getParams().fontSize;
    }

    private static int getLineWidth(final ScaleBarRenderSettings settings) {
        if (settings.getParams().lineWidth != null) {
            return settings.getParams().lineWidth;
        } else {
            if (settings.getParams().getOrientation().isHorizontal()) {
                return settings.getMaxSize().width / 150;
            } else {
                return settings.getMaxSize().height / 150;
            }
        }
    }

    /**
     * Get the bar size.
     *
     * @param settings Parameters for rendering the scalebar.
     */
    @VisibleForTesting
    protected static int getBarSize(final ScaleBarRenderSettings settings) {
        if (settings.getParams().barSize != null) {
            return settings.getParams().barSize;
        } else {
            if (settings.getParams().getOrientation().isHorizontal()) {
                return settings.getMaxSize().height / 4;
            } else {
                return settings.getMaxSize().width / 4;
            }
        }
    }

    /**
     * Get the label distance..
     *
     * @param settings Parameters for rendering the scalebar.
     */
    @VisibleForTesting
    protected static int getLabelDistance(final ScaleBarRenderSettings settings) {
        if (settings.getParams().labelDistance != null) {
            return settings.getParams().labelDistance;
        } else {
            if (settings.getParams().getOrientation().isHorizontal()) {
                return settings.getMaxSize().width / 40;
            } else {
                return settings.getMaxSize().height / 40;
            }
        }
    }

    private static int getPadding(final ScaleBarRenderSettings settings) {
        if (settings.getParams().padding != null) {
            return settings.getParams().padding;
        } else {
            if (settings.getParams().getOrientation().isHorizontal()) {
                return settings.getMaxSize().width / 40;
            } else {
                return settings.getMaxSize().height / 40;
            }
        }
    }

    /**
     * Render the scalebar.
     *
     * @param mapContext The context of the map for which the scalebar is created.
     * @param scalebarParams The scalebar parameters.
     * @param tempFolder The directory in which the graphic file is created.
     * @param template The template that containts the scalebar processor
     */
    public final URI render(
            final MapfishMapContext mapContext,
            final ScalebarAttributeValues scalebarParams,
            final File tempFolder,
            final Template template)
            throws IOException, ParserConfigurationException {
        final double dpi = mapContext.getDPI();

        // get the map bounds
        final Rectangle paintArea = new Rectangle(mapContext.getMapSize());
        MapBounds bounds = mapContext.getBounds();

        final DistanceUnit mapUnit = getUnit(bounds);
        final Scale scale = bounds.getScale(paintArea, PDF_DPI);
        final double scaleDenominator = scale.getDenominator(scalebarParams.geodetic,
                                                             bounds.getProjection(), dpi, bounds.getCenter());

        DistanceUnit scaleUnit = scalebarParams.getUnit();
        if (scaleUnit == null) {
            scaleUnit = mapUnit;
        }

        // adjust scalebar width and height to the DPI value
        final double maxLengthInPixel = (scalebarParams.getOrientation().isHorizontal()) ?
                scalebarParams.getSize().width : scalebarParams.getSize().height;

        final double maxIntervalLengthInWorldUnits = DistanceUnit.PX.convertTo(maxLengthInPixel, scaleUnit)
                * scaleDenominator / scalebarParams.intervals;
        final double niceIntervalLengthInWorldUnits =
                getNearestNiceValue(maxIntervalLengthInWorldUnits, scaleUnit, scalebarParams.lockUnits);

        final ScaleBarRenderSettings settings = new ScaleBarRenderSettings();
        settings.setParams(scalebarParams);
        settings.setMaxSize(scalebarParams.getSize());
        settings.setPadding(getPadding(settings));

        // start the rendering
        File path = null;
        if (template.getConfiguration().renderAsSvg(scalebarParams.renderAsSvg)) {
            // render scalebar as SVG
            final SVGGraphics2D graphics2D = CreateMapProcessor.createSvgGraphics(scalebarParams.getSize());

            try {
                tryLayout(
                        graphics2D, scaleUnit, scaleDenominator,
                        niceIntervalLengthInWorldUnits, settings, 0);

                path = File.createTempFile("scalebar-graphic-", ".svg", tempFolder);
                CreateMapProcessor.saveSvgFile(graphics2D, path);
            } finally {
                graphics2D.dispose();
            }
        } else {
            // render scalebar as raster graphic
            double dpiRatio = mapContext.getDPI() / PDF_DPI;
            final BufferedImage bufferedImage = new BufferedImage(
                    (int) Math.round(scalebarParams.getSize().width * dpiRatio),
                    (int) Math.round(scalebarParams.getSize().height * dpiRatio),
                    template.isAllowTransparency() ? TYPE_4BYTE_ABGR : TYPE_3BYTE_BGR);
            final Graphics2D graphics2D = bufferedImage.createGraphics();

            try {
                AffineTransform saveAF = new AffineTransform(graphics2D.getTransform());
                graphics2D.scale(dpiRatio, dpiRatio);
                tryLayout(
                        graphics2D, scaleUnit, scaleDenominator,
                        niceIntervalLengthInWorldUnits, settings, 0);
                graphics2D.setTransform(saveAF);

                path = File.createTempFile("scalebar-graphic-", ".png", tempFolder);
                ImageUtils.writeImage(bufferedImage, "png", path);
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
}
