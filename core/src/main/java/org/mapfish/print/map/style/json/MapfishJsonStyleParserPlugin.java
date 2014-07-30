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
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.json.JSONObject;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.map.style.StyleParserPlugin;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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

    private static final String JSON_STYLE_PROPERTY = "styleProperty";
    private static final String DEFAULT_STYLE_PROPERTY = "_style";
    private static final String JSON_VERSION = "version";

    private StyleBuilder styleBuilder = new StyleBuilder();

    @Override
    public Optional<Style> parseStyle(@Nullable final Configuration configuration,
                                      @Nonnull final ClientHttpRequestFactory clientHttpRequestFactory,
                                      @Nonnull final String styleString,
                                      @Nonnull final MapfishMapContext mapContext) throws Throwable {
        String trimmed = styleString.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            final PJsonObject json = new PJsonObject(new JSONObject(styleString), "style");

            if ("1".equals(json.optString(JSON_VERSION, "1"))) {
                String styleProperty = json.optString(JSON_STYLE_PROPERTY, DEFAULT_STYLE_PROPERTY);
                List<Rule> styleRules = getStyleRules(json, styleProperty, configuration);
                final Rule[] rulesArray = styleRules.toArray(new Rule[styleRules.size()]);
                final FeatureTypeStyle featureTypeStyle = this.styleBuilder.createFeatureTypeStyle(null, rulesArray);
                final Style style = this.styleBuilder.createStyle();
                style.featureTypeStyles().add(featureTypeStyle);

                return Optional.of(style);
            }
        }
        return Optional.absent();
    }

    /**
     * Creates SLD rules for each old style.
     */
    private List<Rule> getStyleRules(final PJsonObject styleJson, final String styleProperty,
                                     final Configuration configuration) {
        final List<Rule> styleRules = Lists.newArrayListWithExpectedSize(styleJson.size());
        JsonStyleParserHelper parserHelper = new JsonStyleParserHelper(configuration, this.styleBuilder);

        for (Iterator<String> iterator = styleJson.keys(); iterator.hasNext();) {
            String styleKey = iterator.next();
            if (styleKey.equals(JSON_STYLE_PROPERTY) || styleKey.equals(JSON_VERSION)) {
                continue;
            }
            PJsonObject oldStyle = styleJson.getJSONObject(styleKey);
            styleRules.add(createStyleRule(parserHelper, styleKey, oldStyle, styleProperty));
        }

        return styleRules;
    }

    private Rule createStyleRule(final JsonStyleParserHelper parserHelper,
                                 final String styleKey,
                                 final PJsonObject oldStyle,
                                 final String styleProperty) {

        Collection<Symbolizer> symbolizers = Collections2.filter(Lists.newArrayList(
                parserHelper.createPointSymbolizer(oldStyle),
                parserHelper.createLineSymbolizer(oldStyle),
                parserHelper.createPolygonSymbolizer(oldStyle),
                parserHelper.createTextSymbolizer(oldStyle)), Predicates.notNull());

        Rule rule = this.styleBuilder.createRule(symbolizers.toArray(new Symbolizer[symbolizers.size()]));
        rule.setName(styleKey);

        Filter filter = createFilter(styleKey, styleProperty);
        if (filter != null) {
            rule.setFilter(filter);
        }
        return rule;
    }

    @Nullable
    private org.opengis.filter.Filter createFilter(final String styleKey, final String styleProperty) {
        if (Strings.isNullOrEmpty(styleProperty) || Strings.isNullOrEmpty(styleKey)) {
            return null;
        }

        final Expression attributeExpression = this.styleBuilder.attributeExpression(styleProperty);
        final Expression valueExpression = this.styleBuilder.literalExpression(styleKey);
        return this.styleBuilder.getFilterFactory().equals(attributeExpression, valueExpression);
    }

}
