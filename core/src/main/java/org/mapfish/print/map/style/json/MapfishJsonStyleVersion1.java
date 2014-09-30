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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.mapfish.print.map.style.json.MapfishJsonStyleParserPlugin.Versions;

/**
 * The strategy for parsing the Mapfish json style version 1.
 *
 * @author Jesse on 7/26/2014.
 */
public final class MapfishJsonStyleVersion1 {

    private static final String JSON_STYLE_PROPERTY = "styleProperty";
    private static final String DEFAULT_STYLE_PROPERTY = "_style";
    /**
     * The default name to use for selecting the geometry attribute of a feature.
     */
    public static final String DEFAULT_GEOM_ATT_NAME = "geometry";

    private final String geometryProperty;
    private final PJsonObject json;
    private final StyleBuilder sldStyleBuilder;
    private final JsonStyleParserHelper parserHelper;

    MapfishJsonStyleVersion1(@Nonnull final PJsonObject json,
                             @Nonnull final StyleBuilder styleBuilder,
                             @Nonnull final Configuration configuration,
                             @Nonnull final String defaultGeomAttName) {
        this.json = json;
        this.sldStyleBuilder = styleBuilder;
        this.parserHelper = new JsonStyleParserHelper(configuration, this.sldStyleBuilder, true, Versions.ONE);
        this.geometryProperty = defaultGeomAttName;
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
            final List<Rule> currentRules = createStyleRule(styleKey, styleJson, styleProperty);
            for (Rule currentRule : currentRules) {
                if (currentRule != null) {
                    styleRules.add(currentRule);
                }
            }
        }

        return styleRules;
    }

    @SuppressWarnings("unchecked")
    private List<Rule> createStyleRule(final String styleKey,
                                       final PJsonObject styleJson,
                                       final String styleProperty) {

        final PointSymbolizer pointSymbolizer = this.parserHelper.createPointSymbolizer(styleJson);
        final LineSymbolizer lineSymbolizer = this.parserHelper.createLineSymbolizer(styleJson);
        final PolygonSymbolizer polygonSymbolizer = this.parserHelper.createPolygonSymbolizer(styleJson);
        final TextSymbolizer textSymbolizer = this.parserHelper.createTextSymbolizer(styleJson);

        Filter propertyMatches = createFilter(styleKey, styleProperty);
        Rule textRule = null;
        if (textSymbolizer != null) {
            textRule = this.sldStyleBuilder.createRule(textSymbolizer);
            if (propertyMatches != null) {
                textRule.setFilter(propertyMatches);
            }
            textRule.setName(styleKey + "_Text");
        }

        return Lists.newArrayList(
                createGeometryFilteredRule(pointSymbolizer, styleKey, styleProperty, Point.class, MultiPoint.class,
                        GeometryCollection.class),
                createGeometryFilteredRule(lineSymbolizer, styleKey, styleProperty, LineString.class, MultiLineString.class,
                        LinearRing.class, GeometryCollection.class),
                createGeometryFilteredRule(polygonSymbolizer, styleKey, styleProperty, Polygon.class, MultiPolygon.class,
                        GeometryCollection.class),
                textRule);
    }

    private Rule createGeometryFilteredRule(final Symbolizer symb,
                                            final String styleKey,
                                            final String styleProperty,
                                            final Class<? extends Geometry>... geomClass) {
        if (symb != null) {
            Expression geomProperty = this.sldStyleBuilder.attributeExpression(this.geometryProperty);
            final Function geometryTypeFunction = this.sldStyleBuilder.getFilterFactory().function("geometryType", geomProperty);
            final ArrayList<Filter> geomOptions = Lists.newArrayListWithExpectedSize(geomClass.length);
            for (Class<? extends Geometry> requiredType : geomClass) {
                Expression expr = this.sldStyleBuilder.literalExpression(requiredType.getSimpleName());
                geomOptions.add(this.sldStyleBuilder.getFilterFactory().equals(geometryTypeFunction, expr));
            }

            Filter ruleFilter = this.sldStyleBuilder.getFilterFactory().or(geomOptions);
            Filter propertyMatches = createFilter(styleKey, styleProperty);
            Rule rule = this.sldStyleBuilder.createRule(symb);
            if (propertyMatches != null) {
                ruleFilter = this.sldStyleBuilder.getFilterFactory().and(propertyMatches, ruleFilter);
            }
            rule.setFilter(ruleFilter);
            rule.setName(styleKey + "_" + geomClass[0].getSimpleName());
            return rule;
        } else {
            return null;
        }
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
