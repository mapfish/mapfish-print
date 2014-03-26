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

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.geotools.referencing.CRS;
import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.attribute.AbstractAttribute;
import org.mapfish.print.config.Template;
import org.mapfish.print.json.PJsonArray;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.map.MapLayerFactoryPlugin;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

/**
 * The attributes for {@link org.mapfish.print.processor.map.CreateMapProcessor}.
 */
public class MapAttribute extends AbstractAttribute<MapAttribute.MapAttributeValues> {

    /**
     * The key in the config.yaml file of the set of DPIs allowed for this map.
     */
    static final String CONFIG_DPI = "dpi";
    /**
     * The key in the config.yaml file of the width of the map.  This is used by the client to determine the area that will be printed.
     */
    static final String WIDTH = "width";
    /**
     * The key in the config.yaml file of the height of the map.  This is used by the client to determine the area that will be printed.
     */
    static final String HEIGHT = "height";

    @Autowired
    private ApplicationContext applicationContext;
    private float[] dpi;
    private int width;
    private int height;

    @Override
    public final MapAttributeValues getValue(final Template template, final PJsonObject values, final String name) {
        return new MapAttributeValues(template, values.getJSONObject(name));
    }

    @Override
    protected final String getType() {
        return "map";
    }

    @Override
    protected final void additionalPrintClientConfig(final JSONWriter json) throws JSONException {
        final JSONWriter array = json.key(CONFIG_DPI).array();
        for (float currentDpi : this.dpi) {
            array.value(currentDpi);
        }
        array.endArray();
        json.key(WIDTH).value(this.width);
        json.key(HEIGHT).value(this.height);
    }

    public final void setDpi(final float[] dpi) {
        this.dpi = dpi;
    }

    public final void setWidth(final int width) {
        this.width = width;
    }

    public final void setHeight(final int height) {
        this.height = height;
    }

    /**
     * The value of {@link MapAttribute}.
     */
    public final class MapAttributeValues {
        static final String CENTER = "center";
        static final String SCALE = "scale";
        /**
         * The property name of the bbox attribute.
         */
        public static final String BBOX = "bbox";
        static final String ROTATION = "rotation";
        static final String LAYERS = "layers";
        /**
         * Name of the projection property.
         */
        public static final String PROJECTION = "projection";
        private static final String DEFAULT_PROJECTION = "EPSG:3857";
        private static final String LONGITUDE_FIRST = "longitudeFirst ";

        private final MapBounds mapBounds;
        private final double rotation;
        private final List<MapLayer> layers;

        MapAttributeValues(final Template template, final PJsonObject requestData) {

            this.mapBounds = parseBounds(requestData);
            this.rotation = requestData.optDouble(ROTATION, 0.0);
            this.layers = parseLayers(template, requestData);
        }

        private List<MapLayer> parseLayers(final Template template, final PJsonObject requestData) {
            List<MapLayer> layerList = Lists.newArrayList();
            final PJsonArray jsonLayers = requestData.getJSONArray(LAYERS);

            for (int i = 0; i < jsonLayers.size(); i++) {
                try {
                    parseSingleLayer(template, layerList, jsonLayers, i);
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }

            return layerList;
        }

        private void parseSingleLayer(final Template template, final List<MapLayer> layerList,
                                      final PJsonArray jsonLayers, final int i) throws Throwable {
            PJsonObject layerJson = jsonLayers.getJSONObject(i);

            final Map<String, MapLayerFactoryPlugin> layerParsers = MapAttribute.this.applicationContext.getBeansOfType(MapLayerFactoryPlugin.class);
            for (MapLayerFactoryPlugin layerParser : layerParsers.values()) {
                final Optional<? extends MapLayer> layerOption = layerParser.parse(template, layerJson);
                if (layerOption.isPresent()) {
                    final MapLayer newLayer = layerOption.get();
                    if (layerList.isEmpty()) {
                        layerList.add(newLayer);
                    } else {
                        final int lastLayerIndex = layerList.size() - 1;
                        final MapLayer lastLayer = layerList.get(lastLayerIndex);
                        Optional<MapLayer> combinedLayer = lastLayer.tryAddLayer(newLayer);
                        if (combinedLayer.isPresent()) {
                            layerList.remove(lastLayerIndex);
                            layerList.add(lastLayerIndex, combinedLayer.get());
                        } else {
                            layerList.add(newLayer);
                        }
                    }
                    break;
                }
            }
        }

        private CoordinateReferenceSystem parseProjection(final PJsonObject requestData) {
            final String projectionString = requestData.optString(PROJECTION, DEFAULT_PROJECTION);
            final Boolean longitudeFirst = requestData.optBool(LONGITUDE_FIRST);
            try {
                if (longitudeFirst == null) {
                    return CRS.decode(projectionString);
                } else {
                    return CRS.decode(projectionString, longitudeFirst);
                }
            } catch (NoSuchAuthorityCodeException e) {
                throw new RuntimeException(projectionString + "was not recognized as a crs code", e);
            } catch (FactoryException e) {
                throw new RuntimeException("Error occured while parsing: " + projectionString, e);
            }
        }

        private MapBounds parseBounds(final PJsonObject requestData) {
            final PJsonArray center = requestData.optJSONArray(CENTER);
            final PJsonArray bbox = requestData.optJSONArray(BBOX);
            final CoordinateReferenceSystem projection = parseProjection(requestData);
            if (center != null && bbox != null) {
                throw new IllegalArgumentException("Cannot have both center and bbox defined");
            }
            MapBounds bounds;
            if (center != null) {
                double centerX = center.getDouble(0);
                double centerY = center.getDouble(1);
                double scale = requestData.getDouble(SCALE);
                bounds = new CenterScaleMapBounds(projection, centerX, centerY, scale);
            } else if (bbox != null) {
                final int maxYIndex = 3;
                double minX = bbox.getDouble(0);
                double minY = bbox.getDouble(1);
                double maxX = bbox.getDouble(2);
                double maxY = bbox.getDouble(maxYIndex);
                bounds = new BBoxMapBounds(projection, minX, minY, maxX, maxY);
            } else {
                throw new IllegalArgumentException("Expected either center and scale or bbox for the map bounds");
            }
            return bounds;
        }

        public MapBounds getMapBounds() {
            return this.mapBounds;
        }

        public double getRotation() {
            return this.rotation;
        }

        public List<MapLayer> getLayers() {
            return Lists.newArrayList(this.layers);
        }

        public int getWidth() {
            return MapAttribute.this.width;
        }

        public int getHeight() {
            return MapAttribute.this.height;
        }
    }
}
