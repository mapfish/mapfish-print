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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.wrapper.json.PJsonArray;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.opengis.filter.Filter;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;

import static org.mapfish.print.map.style.json.MapfishJsonStyleParserPlugin.Versions;

/**
 * Support a more flexible json styling than that which is supported by version 1.
 *
 * @author Jesse on 7/26/2014.
 */
public final class MapfishJsonStyleVersion2 {
    static final String JSON_SYMB = "symbolizers";
    private static final String JSON_TYPE = "type";
    private static final Pattern VALUE_EXPR_PATTERN = Pattern.compile("\\$\\{([\\w\\d_-]+)\\}");
    private static final String JSON_MIN_SCALE = "minScale";
    private static final String JSON_MAX_SCALE = "maxScale";
    private static final String JSON_FILTER_INCLUDE = "*";

    enum SymbolizerType {
        POINT {
            @Override
            protected Symbolizer parseJson(final JsonStyleParserHelper parser, final PJsonObject symbolizerJson) {
                return parser.createPointSymbolizer(symbolizerJson);
            }
        }, LINE {
            @Override
            protected Symbolizer parseJson(final JsonStyleParserHelper parser, final PJsonObject symbolizerJson) {
                return parser.createLineSymbolizer(symbolizerJson);
            }
        }, POLYGON {
            @Override
            protected Symbolizer parseJson(final JsonStyleParserHelper parser, final PJsonObject symbolizerJson) {
                return parser.createPolygonSymbolizer(symbolizerJson);
            }
        }, TEXT {
            @Override
            protected Symbolizer parseJson(final JsonStyleParserHelper parser, final PJsonObject symbolizerJson) {
                return parser.createTextSymbolizer(symbolizerJson);
            }
        };

        protected abstract Symbolizer parseJson(JsonStyleParserHelper parser, PJsonObject symbolizerJson);
    }

    private final PJsonObject json;
    private final StyleBuilder styleBuilder;
    private final JsonStyleParserHelper parserHelper;

    MapfishJsonStyleVersion2(@Nonnull final PJsonObject json,
                             @Nonnull final StyleBuilder styleBuilder,
                             @Nonnull final Configuration configuration) {
        this.json = json;
        this.styleBuilder = styleBuilder;
        this.parserHelper = new JsonStyleParserHelper(configuration, styleBuilder, false, Versions.TWO);
    }

    Style parseStyle() {
        List<Rule> rules = Lists.newArrayList();

        final Iterator<String> keys = this.json.keys();
        while (keys.hasNext()) {
            String next = keys.next().trim();
            if (isRule(next)) {
                rules.add(createRule(next));
            }
        }

        if (rules.isEmpty()) {
            throw new IllegalArgumentException("No rules found in style.  Rules are json objects that have the key " +
                                               JSON_FILTER_INCLUDE + " or have the form: [ecql]");
        }

        return this.parserHelper.createStyle(rules);
    }

    private Rule createRule(final String jsonKey) {
        PJsonObject ruleJson = this.json.getJSONObject(jsonKey);
        Filter filter = Filter.INCLUDE;
        if (!jsonKey.equals(JSON_FILTER_INCLUDE)) {
            try {
                filter = ECQL.toFilter(jsonKey, this.styleBuilder.getFilterFactory());
            } catch (CQLException e) {
                throw new RuntimeException("Error compiling rule filter: " + jsonKey, e);
            }
        }

        PJsonArray symbolizerJsonArray = ruleJson.getJSONArray(JSON_SYMB);
        Symbolizer[] symbolizers = new Symbolizer[symbolizerJsonArray.size()];

        for (int i = 0; i < symbolizerJsonArray.size(); i++) {
            final PJsonObject symbolizerJson = symbolizerJsonArray.getJSONObject(i);
            updateSymbolizerProperties(ruleJson, symbolizerJson);

            SymbolizerType type = SymbolizerType.valueOf(symbolizerJson.getString(JSON_TYPE).toUpperCase());
            symbolizers[i] = type.parseJson(this.parserHelper, symbolizerJson);
            if (symbolizers[i] == null) {
                throw new RuntimeException("Error creating symbolizer " + symbolizerJson.getString(JSON_TYPE) + " in rule " + jsonKey);
            }
        }

        Map<String, String> ruleValues = buildValuesMap(ruleJson, new PJsonObject(new JSONObject(), "empty"));
        double minScale = getScaleDenominator(ruleValues, JSON_MIN_SCALE, Double.MIN_VALUE);
        double maxScale = getScaleDenominator(ruleValues, JSON_MAX_SCALE, Double.MAX_VALUE);
        final Rule rule = this.styleBuilder.createRule(symbolizers, minScale, maxScale);
        rule.setFilter(filter);
        return rule;
    }

    private void updateSymbolizerProperties(final PJsonObject ruleJson, final PJsonObject symbolizerJson) {
        Map<String, String> values = buildValuesMap(ruleJson, symbolizerJson);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            try {
                symbolizerJson.getInternalObj().put(entry.getKey(), entry.getValue());
            } catch (JSONException e) {
                throw ExceptionUtils.getRuntimeException(e);
            }
        }
    }

    private double getScaleDenominator(final Map<String, String> ruleValues, final String keyName, final double defaultValue) {
        String scaleString = ruleValues.get(keyName);
        if (scaleString != null) {
            return Double.parseDouble(scaleString);
        }
        return defaultValue;
    }

    private Map<String, String> buildValuesMap(final PJsonObject ruleJson,
                                               final PJsonObject symbolizerJson) {
        Map<String, String> values = Maps.newHashMap();

        Iterator<String> keys = this.json.keys();
        while (keys.hasNext()) {
            String key = keys.next().trim();
            if (!isRule(key)) {
                values.put(key, this.json.getString(key));
            }
        }
        keys = ruleJson.keys();
        while (keys.hasNext()) {
            String key = keys.next().trim();
            if (!key.equals(JSON_SYMB)) {
                values.put(key, ruleJson.getString(key));
            }
        }

        keys = symbolizerJson.keys();
        while (keys.hasNext()) {
            String key = keys.next().trim();
            if (!key.equals(JSON_SYMB)) {
                values.put(key, symbolizerJson.getString(key));
            }
        }

        return resolveAllValues(values);
    }

    @VisibleForTesting
    static Map<String, String> resolveAllValues(final Map<String, String> values) {
        Map<String, String> toResolve = Maps.newHashMap(values);
        Map<String, String> resolved = Maps.newHashMapWithExpectedSize(values.size());
        while (!toResolve.isEmpty()) {
            Iterator<Map.Entry<String, String>> entries = toResolve.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String, String> next = entries.next();
                String value = next.getValue();
                final String resolve = resolve(values, value);
                if (resolve == null) {
                    resolved.put(next.getKey(), value);
                    entries.remove();
                } else {
                    next.setValue(resolve);
                }

            }
        }

        return resolved;
    }

    private static String resolve(final Map<String, String> values, final String value) {
        int lastEnd = 0;
        final Matcher matcher = VALUE_EXPR_PATTERN.matcher(value);
        boolean changed = false;
        StringBuilder updatedValue = new StringBuilder();
        while (matcher.find()) {
            final String valName = matcher.group(1);
            String replacement;
            if (values.containsKey(valName)) {
                changed = true;
                replacement = values.get(valName);
            } else {
                replacement = matcher.group(0);
            }
            updatedValue.append(value.substring(lastEnd, matcher.start()));
            updatedValue.append(replacement);
            lastEnd = matcher.end();
        }

        if (changed) {
            updatedValue.append(value.substring(lastEnd));
            return updatedValue.toString();
        } else {
            return null;
        }
    }

    private boolean isRule(final String jsonKey) {
        return this.json.optJSONObject(jsonKey) != null;
    }
}
