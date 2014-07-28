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

import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The strategy for parsing the Mapfish json style version 1.
 *
 * @author Jesse on 7/26/2014.
 */
public final class MapfishJsonStyleVersion1 {

    private static final String JSON_STYLE_PROPERTY = "styleProperty";
    private static final String DEFAULT_STYLE_PROPERTY = "_style";
    
    private final PJsonObject json;
    private final StyleBuilder sldStyleBuilder;
    private final JsonStyleParserHelper parserHelper;

    MapfishJsonStyleVersion1(@Nonnull final PJsonObject json,
                             @Nonnull final StyleBuilder styleBuilder,
                             @Nonnull final Configuration configuration) {
        this.json = json;
        this.sldStyleBuilder = styleBuilder;
        this.parserHelper = new JsonStyleParserHelper(configuration, this.sldStyleBuilder, true);
    }

    Style parseStyle() {
        String styleProperty = this.json.optString(JSON_STYLE_PROPERTY, DEFAULT_STYLE_PROPERTY);
        List<Rule> styleRules = getStyleRules(styleProperty);
        return this.parserHelper.createStyle(styleRules);
    }


    /**
     * Creates SLD rules for each old style.
     */
    private List<Rule> getStyleRules(final String styleProperty) {
        final List<Rule> styleRules = Lists.newArrayListWithExpectedSize(this.json.size());

        for (Iterator<String> iterator = this.json.keys(); iterator.hasNext();) {
            String styleKey = iterator.next();
            if (styleKey.equals(JSON_STYLE_PROPERTY) || styleKey.equals(MapfishJsonStyleParserPlugin.JSON_VERSION)) {
                continue;
            }
            PJsonObject styleJson = this.json.getJSONObject(styleKey);
            styleRules.add(createStyleRule(styleKey, styleJson, styleProperty));
        }

        return styleRules;
    }

    private Rule createStyleRule(final String styleKey,
                                 final PJsonObject styleJson,
                                 final String styleProperty) {

        Collection<Symbolizer> symbolizers = Collections2.filter(Lists.newArrayList(
                this.parserHelper.createPointSymbolizer(styleJson),
                this.parserHelper.createLineSymbolizer(styleJson),
                this.parserHelper.createPolygonSymbolizer(styleJson),
                this.parserHelper.createTextSymbolizer(styleJson)), Predicates.notNull());

        Rule rule = this.sldStyleBuilder.createRule(symbolizers.toArray(new Symbolizer[symbolizers.size()]));
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

        final Expression attributeExpression = this.sldStyleBuilder.attributeExpression(styleProperty);
        final Expression valueExpression = this.sldStyleBuilder.literalExpression(styleKey);
        return this.sldStyleBuilder.getFilterFactory().equals(attributeExpression, valueExpression);
    }
}
