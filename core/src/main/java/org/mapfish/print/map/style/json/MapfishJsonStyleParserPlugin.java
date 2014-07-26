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

package org.mapfish.print.map.style.json;

import com.google.common.base.Optional;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.json.JSONObject;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.map.style.StyleParserPlugin;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.http.client.ClientHttpRequestFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Supports a json based style format.
 * <pre><code>
 * {
 *   "version" : "1",
 *   "styleProperty":"_gx_style",
 *   "1": {
 *     "fillColor":"#FF0000",
 *     "fillOpacity":0,
 *     "rotation" : "30",
 *
 *     "externalGraphic" : "mark.png"
 *     "graphicName": "circle",
 *     "graphicOpacity": 0.4,
 *     "pointRadius": 5,
 *
 *     "strokeColor":"#FFA829",
 *     "strokeOpacity":1,
 *     "strokeWidth":5,
 *     "strokeLinecap":"round",
 *     "strokeDashstyle":"dot",
 *
 *     "fontColor":"#000000",
 *     "fontFamily": "sans-serif",
 *     "fontSize": "12px",
 *     "fontStyle": "normal",
 *     "fontWeight": "bold",
 *     "haloColor": "#123456",
 *     "haloOpacity": "0.7",
 *     "haloRadius": "3.0",
 *     "label": "${name}",
 *     "labelAlign": "cm",
 *     "labelRotation": "45",
 *     "labelXOffset": "-25.0",
 *     "labelYOffset": "-35.0"
 *    }
 * }
 * </code></pre>
 */
public final class MapfishJsonStyleParserPlugin implements StyleParserPlugin {

    enum Versions {
        ONE("1") {
            @Override
            Style parseStyle(final PJsonObject json,
                             final StyleBuilder styleBuilder,
                             final Configuration configuration) {
                return new MapfishJsonStyleVersion1(json, styleBuilder, configuration).parseStyle();
            }
        };
        private final String versionNumber;

        Versions(final String versionNumber) {
            this.versionNumber = versionNumber;
        }

        abstract Style parseStyle(PJsonObject json, StyleBuilder styleBuilder, Configuration configuration);
    }

    static final String JSON_VERSION = "version";

    private StyleBuilder sldStyleBuilder = new StyleBuilder();

    @Override
    public Optional<Style> parseStyle(@Nullable final Configuration configuration,
                                      @Nonnull final ClientHttpRequestFactory clientHttpRequestFactory,
                                      @Nonnull final String styleString,
                                      @Nonnull final MapfishMapContext mapContext) throws Throwable {
        String trimmed = styleString.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            final PJsonObject json = new PJsonObject(new JSONObject(styleString), "style");

            final String jsonVersion = json.optString(JSON_VERSION, "1");
            for (Versions versions : Versions.values()) {
                if (versions.versionNumber.equals(jsonVersion)) {
                    return Optional.of(versions.parseStyle(json, this.sldStyleBuilder, configuration));
                }
            }
        }
        return Optional.absent();
    }
}
