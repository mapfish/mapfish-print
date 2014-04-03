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
import org.mapfish.print.processor.AbstractProcessor;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jesseeichar on 3/17/14.
 * @author sbrunner
 */
public class CreateMapProcessor extends AbstractProcessor {
    private static final String MAP_INPUT = "map";
    private static final String MAP_OUTPUT = "map";

    private int imageType = BufferedImage.TYPE_4BYTE_ABGR;


    @Override
    public final Map<String, Object> execute(final Map<String, Object> values) throws Exception {
        MapAttribute.MapAttributeValues mapValues = (MapAttribute.MapAttributeValues) values.get(MAP_INPUT);
        final int mapWidth = mapValues.getWidth();
        final int mapHeight = mapValues.getHeight();

        final BufferedImage bufferedImage = new BufferedImage(mapWidth, mapHeight,
                this.imageType);

        Graphics2D graphics2D = bufferedImage.createGraphics();
        try {
            // reverse layer list to draw from bottom to top.  normally position 0 is top-most layer.
            final List<MapLayer> layers = Lists.reverse(mapValues.getLayers());

            final MapBounds bounds = mapValues.getMapBounds();
            final double dpi = mapValues.getDpi();
            final Rectangle paintArea = new Rectangle(mapWidth, mapHeight);

            for (MapLayer layer : layers) {
                layer.render(graphics2D, bounds, paintArea, dpi);
            }
        } finally {
            graphics2D.dispose();
        }

        final Map<String, Object> output = new HashMap<String, Object>();
        output.put(MAP_OUTPUT, bufferedImage);
        return output;
    }

    /**
     * Set the type of buffered image rendered to.  By default the image is
     * @param imageType one of the {@link java.awt.image.BufferedImage} constants.
     */
    public final void setImageType(final int imageType) {
        this.imageType = imageType;
    }
}
