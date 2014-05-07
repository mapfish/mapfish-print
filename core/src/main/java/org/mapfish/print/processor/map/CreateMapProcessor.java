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

import com.google.common.collect.Lists;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.attribute.map.MapBounds;
import org.mapfish.print.attribute.map.MapLayer;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.processor.AbstractProcessor;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * @author jesseeichar on 3/17/14.
 * @author sbrunner
 */
public final class CreateMapProcessor extends AbstractProcessor<CreateMapProcessor.Input, CreateMapProcessor.Output> {
    enum BufferedImageType {
        TYPE_4BYTE_ABGR(BufferedImage.TYPE_4BYTE_ABGR),
        TYPE_4BYTE_ABGR_PRE(BufferedImage.TYPE_4BYTE_ABGR_PRE),
        TYPE_3BYTE_BGR(BufferedImage.TYPE_3BYTE_BGR),
        TYPE_BYTE_BINARY(BufferedImage.TYPE_BYTE_BINARY),
        TYPE_BYTE_GRAY(BufferedImage.TYPE_BYTE_GRAY),
        TYPE_BYTE_INDEXED(BufferedImage.TYPE_BYTE_INDEXED),
        TYPE_INT_BGR(BufferedImage.TYPE_INT_BGR),
        TYPE_INT_RGB(BufferedImage.TYPE_INT_RGB),
        TYPE_INT_ARGB(BufferedImage.TYPE_INT_ARGB),
        TYPE_INT_ARGB_PRE(BufferedImage.TYPE_INT_ARGB_PRE),
        TYPE_USHORT_555_RGB(BufferedImage.TYPE_USHORT_555_RGB),
        TYPE_USHORT_565_RGB(BufferedImage.TYPE_USHORT_565_RGB),
        TYPE_USHORT_GRAY(BufferedImage.TYPE_USHORT_GRAY);
        private final int value;

        private BufferedImageType(final int value) {
            this.value = value;
        }

        static BufferedImageType lookupValue(final String name) {
            for (BufferedImageType bufferedImageType : values()) {
                if (bufferedImageType.name().equalsIgnoreCase(name)) {
                    return bufferedImageType;
                }
            }

            throw new IllegalArgumentException("'" + name + "is not a recognized " + BufferedImageType.class.getName() + " enum value");

        }
    }

    private BufferedImageType imageType = BufferedImageType.TYPE_4BYTE_ABGR;

    /**
     * Constructor.
     */
    protected CreateMapProcessor() {
        super(Output.class);
    }


    @Override
    public Input createInputParameter() {
        return new Input();
    }

    @Override
    public Output execute(final Input param) throws Exception {
        MapAttribute.MapAttributeValues mapValues = param.map;
        final Dimension mapSize = mapValues.getMapSize();
        final double dpi = mapValues.getDpi();
        final Rectangle paintArea = new Rectangle(mapSize);

        MapBounds bounds = mapValues.getMapBounds();

        if (mapValues.isUseNearestScale()) {
                bounds = bounds.adjustBoundsToNearestScale(
                        mapValues.getZoomLevels(),
                        mapValues.getZoomSnapTolerance(),
                        mapValues.getZoomLevelSnapStrategy(), paintArea, dpi);
        }

        final BufferedImage bufferedImage = new BufferedImage(mapSize.width, mapSize.height, this.imageType.value);

        Graphics2D graphics2D = bufferedImage.createGraphics();
        try {
            // reverse layer list to draw from bottom to top.  normally position 0 is top-most layer.
            final List<MapLayer> layers = Lists.reverse(mapValues.getLayers());

            int i = 0;
            for (MapLayer layer : layers) {
                boolean isFirstLayer = i == 0;
                layer.render(graphics2D, bounds, paintArea, dpi, isFirstLayer);
                i++;
            }
        } finally {
            graphics2D.dispose();
        }

        return new Output(bufferedImage);
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors) {
        if (this.imageType == null) {
            validationErrors.add(new ConfigurationException("No imageType defined in " + getClass().getName()));
        }
    }

    /**
     * Set the type of buffered image rendered to.  See {@link org.mapfish.print.processor.map.CreateMapProcessor.BufferedImageType}.
     * <p/>
     * Default is {@link org.mapfish.print.processor.map.CreateMapProcessor.BufferedImageType#TYPE_4BYTE_ABGR}.
     *
     * @param imageType one of the {@link org.mapfish.print.processor.map.CreateMapProcessor.BufferedImageType} values.
     */
    public void setImageType(final String imageType) {
        this.imageType = BufferedImageType.lookupValue(imageType);
    }

    /**
     * The Input object for processor.
     */
    public static final class Input {
        /**
         * The required parameters for the map.
         */
        public MapAttribute.MapAttributeValues map;
    }

    /**
     * Output for the processor.
     */
    public static final class Output {
        /**
         * The rendered map.
         */
        public final BufferedImage image;

        private Output(final BufferedImage bufferedImage) {
            this.image = bufferedImage;
        }

    }

}
