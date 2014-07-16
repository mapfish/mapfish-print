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

import org.mapfish.print.config.Template;
import org.mapfish.print.map.Scale;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.parser.OneOf;
import org.mapfish.print.parser.Requires;
import org.mapfish.print.wrapper.PArray;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * The attributes for {@link org.mapfish.print.processor.map.CreateMapProcessor}.
 */
public final class MapAttribute extends GenericMapAttribute<MapAttribute.MapAttributeValues> {

    private static final double DEFAULT_SNAP_TOLERANCE = 0.05;
    private static final ZoomLevelSnapStrategy DEFAULT_SNAP_STRATEGY = ZoomLevelSnapStrategy.CLOSEST_LOWER_SCALE_ON_TIE;


    @Override
    public MapAttributeValues createValue(final Template template) {
        MapAttributeValues result = new MapAttributeValues(template, new Dimension(this.getWidth(), this.getHeight()));
        return result;
    }

    /**
     * The value of {@link MapAttribute}.
     */
    public class MapAttributeValues extends GenericMapAttribute<?>.GenericMapAttributeValues {

        private static final boolean DEFAULT_USEADJUSTBOUNDS = false;
        private static final double DEFAULT_ROTATION = 0.0;
        private static final String DEFAULT_PROJECTION = "EPSG:3857";

        private MapBounds mapBounds;
        
        /**
         * An array of 4 doubles, minX, minY, maxX, maxY.  The bounding box of the map.
         * <p/>
         * Either the bbox or the center + scale must be defined
         * <p/>
         */
        @OneOf("MapBounds")
        public double[] bbox;
        /**
         * An array of 2 doubles, (x, y).  The center of the map.
         */
        @Requires("scale")
        @OneOf("MapBounds")
        public double[] center;
        /**
         * If center is defined then this is the scale of the map centered at center.
         */
        @HasDefaultValue
        public double scale;
        
        /**
         * The json with all the layer information.  This will be parsed in postConstruct into a list of layers and
         * therefore this field should not normally be accessed.
         */
        public PArray layers;

        /**
         * The output dpi of the printed map.
         */
        public double dpi;

        /**
         * Constructor.
         *
         * @param template the template this map is part of.
         * @param mapSize  the size of the map.
         */
        public MapAttributeValues(final Template template, final Dimension mapSize) {
            super(template, mapSize);
        }

        //CSOFF: DesignForExtension
        @Override
        public Double getDpi() {
        //CSON: DesignForExtension
            return this.dpi;
        }

        @Override
        protected final PArray getRawLayers() {
            return this.layers;
        }

        @Override
        public final void postConstruct() throws FactoryException {
            super.postConstruct();
            
            if (this.getDpi() > MapAttribute.this.getMaxDpi()) {
                throw new IllegalArgumentException(
                        "dpi parameter was " + this.getDpi() + " must be limited to " + MapAttribute.this.getMaxDpi()
                        + ".  The path to the parameter is: " + this.getDpi());
            }

            this.mapBounds = parseBounds();
        }

        private MapBounds parseBounds() throws FactoryException {
            final CoordinateReferenceSystem crs = parseProjection();
            if (this.center != null && this.bbox != null) {
                throw new IllegalArgumentException("Cannot have both center and bbox defined");
            }
            MapBounds bounds;
            if (this.center != null) {
                double centerX = this.center[0];
                double centerY = this.center[1];
                Scale scaleObject = new Scale(this.scale);

                bounds = new CenterScaleMapBounds(crs, centerX, centerY, scaleObject);
            } else if (this.bbox != null) {
                final int maxYIndex = 3;
                double minX = this.bbox[0];
                double minY = this.bbox[1];
                double maxX = this.bbox[2];
                double maxY = this.bbox[maxYIndex];
                bounds = new BBoxMapBounds(crs, minX, minY, maxX, maxY);
            } else {
                throw new IllegalArgumentException("Expected either center and scale or bbox for the map bounds");
            }
            return bounds;
        }

        //CSOFF: DesignForExtension
        public MapBounds getMapBounds() {
        //CSON: DesignForExtension
            return this.mapBounds;
        }

        //CSOFF: DesignForExtension
        @Override
        public String getProjection() {
        //CSON: DesignForExtension
            return MapAttribute.getValueOr(super.getProjection(), DEFAULT_PROJECTION);
        }

        //CSOFF: DesignForExtension
        @Override
        public Double getZoomSnapTolerance() {
        //CSON: DesignForExtension
            return MapAttribute.getValueOr(super.getZoomSnapTolerance(), DEFAULT_SNAP_TOLERANCE);
        }

        //CSOFF: DesignForExtension
        @Override
        public ZoomLevelSnapStrategy getZoomLevelSnapStrategy() {
        //CSON: DesignForExtension
            return MapAttribute.getValueOr(super.getZoomLevelSnapStrategy(), DEFAULT_SNAP_STRATEGY);
        }

        //CSOFF: DesignForExtension
        @Override
        public Double getRotation() {
        //CSON: DesignForExtension
            return MapAttribute.getValueOr(super.getRotation(), DEFAULT_ROTATION);
        }

        //CSOFF: DesignForExtension
        @Override
        public Boolean isUseNearestScale() {
        //CSON: DesignForExtension
            return (this.useNearestScale == null || this.useNearestScale)
                        && getZoomLevels() != null;
        }

        //CSOFF: DesignForExtension
        @Override
        public Boolean isUseAdjustBounds() {
        //CSON: DesignForExtension
            return MapAttribute.getValueOr(super.isUseAdjustBounds(), DEFAULT_USEADJUSTBOUNDS);
        }
        
        /**
         * Creates an {@link OverridenMapAttributeValues} instance with the current object
         * and a given {@link OverviewMapAttributeValues} instance.
         * 
         * @param paramOverrides Attributes set in this instance will override attributes in
         *  the current instance.
         */
        public final MapAttribute.OverridenMapAttributeValues getWithOverrides(
                final OverviewMapAttribute.OverviewMapAttributeValues paramOverrides) {
            return new OverridenMapAttributeValues(this, paramOverrides, getTemplate(), null);
        }
    }

    /**
     * A wrapper around a {@link MapAttributeValues} instance and an {@link OverviewMapAttributeValues} instance,
     * which is used to render the overview map.
     * 
     * If attributes on the {@link OverviewMapAttributeValues} instance are set, those
     * attributes will be returned, otherwise the ones on {@link MapAttributeValues}.
     */
    public class OverridenMapAttributeValues extends MapAttribute.MapAttributeValues {
        
        private MapAttribute.MapAttributeValues params;
        private OverviewMapAttribute.OverviewMapAttributeValues paramOverrides;
        private MapBounds zoomedOutBounds = null;
        private MapLayer mapExtentLayer = null;
        
        /**
         * Constructor.
         * 
         * @param params The fallback parameters.
         * @param paramOverrides The parameters explicitly defined for the overview map.
         * @param template The template this map is part of.
         * @param mapSize  The size of the map (ignored, the size of the overview map is used).
         */
        public OverridenMapAttributeValues(
                final MapAttribute.MapAttributeValues params,
                final OverviewMapAttribute.OverviewMapAttributeValues paramOverrides,
                final Template template, final Dimension mapSize) {
            super(template, paramOverrides.getMapSize());
            this.params = params;
            this.paramOverrides = paramOverrides;
        }

        @Override
        public final MapBounds getMapBounds() {
            return this.zoomedOutBounds;
        }

        public final MapBounds getOriginalBounds() {
            return this.params.getMapBounds();
        }
        
        public final void setZoomedOutBounds(final MapBounds zoomedOutBounds) {
            this.zoomedOutBounds = zoomedOutBounds;
        }
        
        @Override
        public final Double getDpi() {
            return MapAttribute.getValueOr(this.paramOverrides.getDpi(), this.params.getDpi());
        }
        
        @Override
        public final Double getZoomSnapTolerance() {
            return MapAttribute.getValueOr(this.paramOverrides.getZoomSnapTolerance(), this.params.getZoomSnapTolerance());
        }

        @Override
        public final ZoomLevelSnapStrategy getZoomLevelSnapStrategy() {
            return MapAttribute.getValueOr(this.paramOverrides.getZoomLevelSnapStrategy(), this.params.getZoomLevelSnapStrategy());
        }
        
        @Override
        public final ZoomLevels getZoomLevels() {
            return MapAttribute.getValueOr(this.paramOverrides.getZoomLevels(), this.params.getZoomLevels());
        }
        
        @Override
        public final Double getRotation() {
            return MapAttribute.getValueOr(this.paramOverrides.getRotation(), this.params.getRotation());
        }
        
        @Override
        public final Boolean isUseAdjustBounds() {
            return MapAttribute.getValueOr(this.paramOverrides.isUseAdjustBounds(), this.params.isUseAdjustBounds());
        }
        
        @Override
        public final Boolean isUseNearestScale() {
            final Boolean useNearestScale = MapAttribute.getValueOr(this.paramOverrides.useNearestScale, this.params.useNearestScale);
            return (useNearestScale == null || useNearestScale)
                    && getZoomLevels() != null;
        }
        
        public final void setMapExtentLayer(final MapLayer mapExtentLayer) {
            this.mapExtentLayer = mapExtentLayer;
        }

        @Override
        public final List<MapLayer> getLayers() {
            // return the layers together with a layer for the bbox rectangle of the map
            List<MapLayer> layers = new ArrayList<MapLayer>();
            if (this.mapExtentLayer != null) {
                layers.add(this.mapExtentLayer);
            }

            if (!this.paramOverrides.getLayers().isEmpty()) {
                layers.addAll(this.paramOverrides.getLayers());
            } else {
                layers.addAll(this.params.getLayers());
            }
            
            return layers;
        }
    }
    
    private static <T extends Object> T getValueOr(final T value, final T defaultValue) {
        if (value != null) {
            return value;
        } else {
            return defaultValue;
        }
    }
}
