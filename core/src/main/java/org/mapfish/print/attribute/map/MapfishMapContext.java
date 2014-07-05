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

package org.mapfish.print.attribute.map;

import org.geotools.geometry.jts.ReferencedEnvelope;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

/**
 * Utility class that adjusts the bounds and the map size in case a rotation
 * is set. Also it provides an {@link AffineTransform} to render the layer graphics.
 */
public class MapfishMapContext {

    private final MapBounds bounds;
    private final Dimension mapSize;
    private final double rotation;
    private final double dpi;

    /**
     * @param bounds the map bounds
     * @param mapSize the map size
     * @param rotationInDegree the rotation in degree
     * @param dpi the dpi of the printed map
     */
    public MapfishMapContext(final MapBounds bounds, final Dimension mapSize, final double rotationInDegree, final double dpi) {
        this.bounds = bounds;
        this.mapSize = mapSize;
        this.rotation = Math.toRadians(rotationInDegree);
        this.dpi = dpi;
    }

    /**
     * @return The rotation in radians.
     */
    public final double getRotation() {
        return this.rotation;
    }
    
    public final MapBounds getBounds() {
        return this.bounds;
    }
    
    public final MapBounds getRotatedBounds() {
        return this.bounds.adjustBoundsToRotation(this.rotation);
    }
    
    public final Dimension getMapSize() {
        return this.mapSize;
    }
    
    /**
     * @return The new map size taking the rotation into account.
     */
    public final Dimension getRotatedMapSize() {
        if (this.rotation == 0.0) {
            return this.mapSize;
        }
        
        final int rotatedWidth = getRotatedMapWidth();
        final int rotatedHeight = getRotatedMapHeight();
        
        return new Dimension(rotatedWidth, rotatedHeight);
    }
    
    /**
     * Returns an {@link AffineTransform} taking the rotation into account.
     * 
     * @return an affine transformation
     */
    public final AffineTransform getTransform() {
        if (this.rotation == 0.0) {
            return null;
        }
        
        final Dimension rotatedMapSize = getRotatedMapSize();
        
        final AffineTransform transform = AffineTransform.getTranslateInstance(0.0, 0.0);
        // move to the center of the original map rectangle (this is the actual 
        // size of the graphic)
        transform.translate(this.mapSize.width / 2, this.mapSize.height / 2);
        
        // then rotate around this center
        transform.rotate(this.rotation);
        
        // then move to an artificial origin (0,0) which might be outside of the actual
        // painting area. this origin still keeps the center of the original map area
        // at the center of the rotated map area.
        transform.translate(-rotatedMapSize.width / 2, -rotatedMapSize.height / 2);

        return transform;
    }

    public final double getDPI() {
        return this.dpi;
    }

    private int getRotatedMapWidth() {
        double width = this.mapSize.getWidth();
        if (this.rotation != 0.0) {
            double height = this.mapSize.getHeight();
            width = Math.abs(width * Math.cos(this.rotation))
                    + Math.abs(height * Math.sin(this.rotation));
        }
        return (int) Math.round(width);
    }

    private int getRotatedMapHeight() {
        double height = this.mapSize.getHeight();
        if (this.rotation != 0.0) {
            double width = this.mapSize.getWidth();
            height = Math.abs(height * Math.cos(this.rotation))
                     + Math.abs(width * Math.sin(this.rotation));
        }
        return (int) Math.round(height);
    }

    public final Rectangle getPaintArea() {
        return new Rectangle(this.mapSize);
    }

    /**
     * Get the bounds as a referenced envelope.
     *
     * @return bounds as a referenced envelope.
     */
    public final ReferencedEnvelope toReferencedEnvelope() {
        return this.bounds.toReferencedEnvelope(getPaintArea(), this.dpi);
    }
}
