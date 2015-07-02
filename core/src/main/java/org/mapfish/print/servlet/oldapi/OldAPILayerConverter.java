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

package org.mapfish.print.servlet.oldapi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.config.OldApiConfig;
import org.mapfish.print.wrapper.json.PJsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts layer definitions in requests of the old API.
 */
public final class OldAPILayerConverter {

    private OldAPILayerConverter() {
    }

    private static Map<String, LayerConverter> converters = new HashMap<String, LayerConverter>();

    static {
        converters.put("osm", new OSMConverter());
        converters.put("wms", new WMSConverter());
        converters.put("wmts", new WMSTConverter());
        converters.put("vector", new GeoJsonConverter());
    }
    
    /**
     * Convert a layer definition of the old API.
     * 
     * @param oldLayer the old layer definition
     * @param oldApi configuration for oldApi to newAPI conversion
     * @return the converted layer definition
     */
    public static JSONObject convert(final PJsonObject oldLayer, final OldApiConfig oldApi) throws JSONException {
        final String layerType = oldLayer.optString("type", "").toLowerCase();
        
        if (!converters.containsKey(layerType)) {
            throw new UnsupportedOperationException("Layer type '" + layerType + "' is "
                    + "not supported by the legacy API.");
        }
        return converters.get(layerType).convert(oldLayer, oldApi);
    }

    private interface LayerConverter {
        JSONObject convert(final PJsonObject oldLayer, OldApiConfig oldApi) throws JSONException;
    }

    private abstract static class AbstractLayerConverter implements LayerConverter {

        @Override
        public JSONObject convert(final PJsonObject oldLayer, final OldApiConfig oldApi) throws JSONException {
            return new JSONObject();
        }

    }

    private static class OSMConverter extends AbstractLayerConverter {

        @Override
        public final JSONObject convert(final PJsonObject oldLayer, final OldApiConfig oldApi) throws JSONException {
            final JSONObject layer = super.convert(oldLayer, oldApi);
            layer.put("type", "osm");

            if (oldLayer.has("baseURL")) {
                layer.put("baseURL", oldLayer.getString("baseURL"));
            }
            if (oldLayer.has("opacity")) {
                layer.put("opacity", oldLayer.getDouble("opacity"));
            }
            if (oldLayer.has("extension")) {
                layer.put("imageFormat", oldLayer.getString("extension"));
            }
            if (oldLayer.has("maxExtent")) {
                layer.put("maxExtent", oldLayer.getInternalObj().getJSONArray("maxExtent"));
            }
            if (oldLayer.has("tileSize")) {
                layer.put("tileSize", oldLayer.getInternalObj().getJSONArray("tileSize"));
            }
            if (oldLayer.has("resolutions")) {
                layer.put("resolutions", oldLayer.getInternalObj().getJSONArray("resolutions"));
            }

            return layer;
        }
    }

    private static class WMSConverter extends AbstractLayerConverter {

        @Override
        public final JSONObject convert(final PJsonObject oldLayer, final OldApiConfig oldApi) throws JSONException {
            final JSONObject layer = super.convert(oldLayer, oldApi);
            layer.put("type", "wms");
            if (oldLayer.has("isTiled") && oldLayer.getInternalObj().getBoolean("isTiled")) {
                layer.put("type", "tiledwms");
                if (oldLayer.has("tileSize")) {
                    layer.put("tileSize", oldLayer.getInternalObj().getJSONArray("tileSize"));
                }
            }
            
            if (oldLayer.has("baseURL")) {
                layer.put("baseURL", oldLayer.getString("baseURL"));
            }
            if (oldLayer.has("opacity")) {
                layer.put("opacity", oldLayer.getDouble("opacity"));
            }
            if (oldLayer.has("layers")) {
                if (oldApi.isWmsReverseLayers()) {
                    layer.put("layers", reverse(oldLayer.getInternalObj().getJSONArray("layers")));
                } else {
                    layer.put("layers", oldLayer.getInternalObj().getJSONArray("layers"));
                }
            }

            if (oldLayer.has("format")) {
                layer.put("imageFormat", oldLayer.getString("format"));
            }
            if (oldLayer.has("styles")) {
                JSONArray stylesJson = oldLayer.getInternalObj().getJSONArray("styles");
                if (stylesJson.length() > 1 || (stylesJson.length() == 1 && !stylesJson.getString(0).trim().isEmpty())) {
                    layer.put("styles", stylesJson);
                }
            }
            if (oldLayer.has("customParams")) {
                JSONObject customParams = oldLayer.getInternalObj().getJSONObject("customParams");
                if (customParams.has("version")) {
                    layer.put("version", customParams.getString("version"));
                    customParams.remove("version");
                }
                layer.put("customParams", customParams);
            }
            if (oldLayer.has("useNativeAngle")) {
                layer.put("useNativeAngle", oldLayer.getBool("useNativeAngle"));
            }
            
            return layer;
        }

        private JSONArray reverse(final JSONArray oldApiLayers) throws JSONException {
            JSONArray newApiLayers = new JSONArray();
            for (int i = oldApiLayers.length(); i > 0; i--) {
                newApiLayers.put(oldApiLayers.get(i - 1));
            }
            return newApiLayers;
        }
    }

    private static class GeoJsonConverter extends AbstractLayerConverter {

        @Override
        public final JSONObject convert(final PJsonObject oldLayer, final OldApiConfig oldApi) throws JSONException {
            final JSONObject layer = super.convert(oldLayer, oldApi);
            layer.put("type", "geojson");

            if (oldLayer.has("geoJson")) {
                // the GeoJSON can either be given directly inline, or as URL to a file 
                try {
                    // first try to get an inline GeoJSON definition
                    layer.put("geoJson", oldLayer.getInternalObj().getJSONObject("geoJson"));
                } catch (JSONException e) {
                    // if that doesn't work, assume that it is linking to a file
                    layer.put("geoJson", oldLayer.getString("geoJson"));
                }
            }

            if (oldLayer.has("styles")) {
                final JSONObject styles = oldLayer.getInternalObj().getJSONObject("styles");
                if (oldLayer.has("styleProperty")) {
                    styles.put("styleProperty", oldLayer.getString("styleProperty"));
                }
                styles.put("version", "1");
                layer.put("style", styles);
                oldLayer.getInternalObj().remove("styles");
            }

            return layer;
        }
    }

    private static class WMSTConverter extends AbstractLayerConverter {

        @Override
        public final JSONObject convert(final PJsonObject oldLayer, final OldApiConfig oldApi) throws JSONException {
            final JSONObject layer = super.convert(oldLayer, oldApi);
            layer.put("type", "wmts");

            if (oldLayer.has("baseURL")) {
                layer.put("baseURL", oldLayer.getString("baseURL"));
            }
            if (oldLayer.has("opacity")) {
                layer.put("opacity", oldLayer.getDouble("opacity"));
            }
            if (oldLayer.has("layer")) {
                layer.put("layer", oldLayer.getString("layer"));
            }

            if (oldLayer.has("format")) {
                String format = oldLayer.getString("format");
                if (format.toLowerCase().startsWith("image/")) {
                    layer.put("imageFormat", format);
                } else {
                    layer.put("imageFormat", "image/" + format);
                }

            }
            if (oldLayer.has("style")) {
                layer.put("style", oldLayer.getString("style"));
            }
            if (oldLayer.has("requestEncoding")) {
                layer.put("requestEncoding", oldLayer.getString("requestEncoding"));
            }

            if (oldLayer.has("customParams")) {
                JSONObject customParams = oldLayer.getInternalObj().getJSONObject("customParams");
                if (customParams.has("version")) {
                    layer.put("version", customParams.getString("version"));
                    customParams.remove("version");
                }
                layer.put("customParams", customParams);
            }
            layer.put("dimensions", JSONObject.NULL);
            layer.put("dimensionParams", new JSONObject());
            if (oldLayer.has("matrixSet")) {
                layer.put("matrixSet", oldLayer.getString("matrixSet"));
            }
            if (oldLayer.has("matrixSet")) {
                layer.put("matrices", convertMatrices(oldLayer.getInternalObj().getJSONArray("matrixIds")));
            }
            return layer;
        }

        private JSONArray convertMatrices(final JSONArray oldMatrices) {
            JSONArray matrices = new JSONArray();
            if (oldMatrices != null && oldMatrices.length() > 0) {
                for (int i = 0; i < oldMatrices.length(); i++) {
                    JSONObject matrix = convertMatrix(oldMatrices.optJSONObject(i));
                    if (matrix != null) {
                        matrices.put(matrix);
                    }
                }
            }
            return matrices;
        }

        private JSONObject convertMatrix(final JSONObject old) {
            JSONObject matrix = null;
            try {
                if (old != null) {

                    matrix = new JSONObject();
                    if (old.has("identifier")) {
                        matrix.put("identifier", old.optString("identifier"));
                    }
                    if (old.has("matrixSize")) {
                        matrix.put("matrixSize", old.optJSONArray("matrixSize"));
                    }
                    if (old.has("scaleDenominator")) {
                        matrix.put("scaleDenominator", old.optDouble("scaleDenominator", 0.0D));
                    }
                    if (old.has("resolution") && !old.has("scaleDenominator")) {

                        Double scaleDenominator = 0.0D;
                        Double resolution = old.optDouble("resolution", 0.0D);
                        if (resolution != 0.0D) {
                            //works with meter based srs
                            final double conversionRatio = 0.00028D;
                            scaleDenominator = resolution / conversionRatio;
                        }
                        matrix.put("scaleDenominator", scaleDenominator);
                    }
                    if (old.has("tileSize")) {
                        matrix.put("tileSize", old.optJSONArray("tileSize"));
                    }
                    if (old.has("topLeftCorner")) {
                        matrix.put("topLeftCorner", old.optJSONArray("topLeftCorner"));
                    }

                }
            } catch (JSONException e) {
                //RAS
            }
            return matrix;
        }
    }
}
