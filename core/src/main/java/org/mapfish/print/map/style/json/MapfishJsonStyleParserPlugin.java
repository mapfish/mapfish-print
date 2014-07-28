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
 * Supports a JSON based style format.
 * <p>
 *     This style parser support two versions of JSON formatting.  Both versions use the same parameter names for configuring
 *     the values of the various properties of the style but the layout differs between the two and version 2 is more flexible
 *     and powerful than version 1.
 * </p>
 * <h2>Mapfish JSON Style Version 1</h2>
 * <p>
 *     Version 1 is compatible with mapfish print <= v2 and is based on the open layers v2 styling.  The layout is as follows:
 * </p>
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
 *
 * <h2>Mapfish JSON Style Version 2</h2>
 * <p>
 *     Version 2 uses the same property names as version 1 but has a different structure.  The layout is as follows:
 * </p>
 * <pre><code>
 * {
 *   "version" : "2",
 *   // shared values can be declared here (at top level)
 *   // and used in form ${constName} later in json
 *   "val1" : "FFA829",
 *   // default values for properties can be defined here
 *   " strokeDashstyle" : "dot"
 *   "[ECQL filter statement]" : {
 *     // default values for current rule can be defined here
 *     // they will override default values defined at
 *     // higher level
 *     "rotation" : "30",
 *
 *     //min and max scale denominator are optional
 *     "maxScale" : 1000000,
 *     "minScale" : 100000,
 *     "symb" : [{
 *       // values defined in symbolizer will override defaults
 *       "type" : "point",
 *       "fillColor":"#FF0000",
 *       "fillOpacity":0,
 *       "rotation" : "30",
 *
 *       "externalGraphic" : "mark.png"
 *       "graphicName": "circle",
 *       "graphicOpacity": 0.4,
 *       "pointRadius": 5,
 *
 *       "strokeColor":"${val1}",
 *       "strokeOpacity":1,
 *       "strokeWidth":5,
 *       "strokeLinecap":"round",
 *       "strokeDashstyle":"dot"
 *     },{
 *       "type" : "line",
 *       "strokeColor":"${val1}",
 *       "strokeOpacity":1,
 *       "strokeWidth":5,
 *       "strokeLinecap":"round",
 *       "strokeDashstyle":"dot"
 *     },{
 *       "type" : "polygon",
 *       "fillColor":"#FF0000",
 *       "fillOpacity":0,
 *
 *       "strokeColor":"${val1}",
 *       "strokeOpacity":1,
 *       "strokeWidth":5,
 *       "strokeLinecap":"round",
 *       "strokeDashstyle":"dot"
 *     },{
 *       "type" : "text",
 *       "fontColor":"#000000",
 *       "fontFamily": "sans-serif",
 *       "fontSize": "12px",
 *       "fontStyle": "normal",
 *       "fontWeight": "bold",
 *       "haloColor": "#123456",
 *       "haloOpacity": "0.7",
 *       "haloRadius": "3.0",
 *       "label": "${name}",
 *       "labelAlign": "cm",
 *       "labelRotation": "45",
 *       "labelXOffset": "-25.0",
 *       "labelYOffset": "-35.0"
 *     }
 *   ]}
 * }
 * </code></pre>
 * <p>
 *     As illustrated above the style consists of:
 *     <ul>
 *         <li>The version number (2) (required)</li>
 *         <li>
 *             Common values which can be referenced in symbolizer property values.(optional)
 *             <p>Values can be referenced in the value of a property with the pattern: ${valName}</p>
 *             <p>Value names can only contain numbers, characters, _ or -</p>
 *             <p>
 *                 Values do not have to be the full property they will be interpolated.  For example:
 *                 <code>The value is ${val}</code>
 *             </p>
*          </li>
 *          <li>
 *              Defaults property definitions(optional):
 *              <p>
 *                  In order to reduce duplication and keep the style definitions small, default values can be specified.  The
 *                  default values in the root (style level) will be used in all symbolizers if the value is not defined.  The
 *                  style level default will apply to all symbolizers defined in the system.
 *              </p>
 *              <p>
 *                  The only difference between a value and a default is that the default has a well known name, therefore defaults
 *                  can also be used as values.
 *              </p>
 *          </li>
 *          <li>
 *              All the styling rules (At least one is required)
 *              <p>
 *                  A styling rule has a key which is the filter which selects the features that the rule will be used to draw and the
 *                  rule definition object.
 *                  <p>The filter is either <code>*</code> or an
 *                  <a href="http://docs.geoserver.org/stable/en/user/filter/ecql_reference.html#filter-ecql-reference">
 *                      ECQL Expression</a>) surrounded by square brackets.  For example: [att < 23].</p>
 *                      The rule definition is as follows:
 *                      <ul>
 *                          <li>
 *                              Default property values (optional):
 *                              <p>
 *                                  Each rule can also have defaults.  If the style and the rule have a default for the same property
 *                                  the rule will override the style default.  All defaults can be (of course) overridden by a value
 *                                  in a symbolizer.
 *                              </p>
 *                          </li>
 *                          <li>
 *                              minScale (optional)
 *                              <p>
 *                                  The minimum scale that the rule should evaluate to true
 *                              </p>
 *                          </li>
 *                          <li>
 *                              maxScale (optional)
 *                              <p>
 *                                  The maximum scale that the rule should evaluate to true
 *                              </p>
 *                          </li>
 *                          <li>
 *                              An array of sybmolizers. (at least one required).
 *                              <p>
 *                                  A symbolizer must have a type property (point, line, polygon, text) which indicates the type of
 *                                  symbolizer and it has the attributes for that type of symbolizer.  All values have defaults
 *                                  so it is possible to define a symbolizer as with only the type property. The only exception is
 *                                  that the "text" symbolizer needs a label property.
 *                              </p>
 *                          </li>
 *                      </ul>
 *              </p>
 *          </li>
 *     </ul>
 * </p>
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
        }, TWO("2") {
            @Override
            Style parseStyle(final PJsonObject json,
                             final StyleBuilder styleBuilder,
                             final Configuration configuration) {
                return new MapfishJsonStyleVersion2(json, styleBuilder, configuration).parseStyle();
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
