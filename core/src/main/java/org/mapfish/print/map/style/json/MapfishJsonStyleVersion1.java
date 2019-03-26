package org.mapfish.print.map.style.json;

import org.apache.commons.lang3.StringUtils;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The strategy for parsing the Mapfish json style version 1.
 */
public final class MapfishJsonStyleVersion1 {

    /**
     * The default name to use for selecting the geometry attribute of a feature.
     */
    public static final String DEFAULT_GEOM_ATT_NAME = "geometry";
    private static final String JSON_STYLE_PROPERTY = "styleProperty";
    private static final String DEFAULT_STYLE_PROPERTY = "_style";
    private final String geometryProperty;
    private final PJsonObject json;
    private final StyleBuilder sldStyleBuilder;
    private final JsonStyleParserHelper parserHelper;

    MapfishJsonStyleVersion1(
            @Nonnull final PJsonObject json,
            @Nonnull final StyleBuilder styleBuilder,
            @Nullable final Configuration configuration,
            @Nonnull final ClientHttpRequestFactory requestFactory,
            @Nonnull final String defaultGeomAttName) {
        this.json = json;
        this.sldStyleBuilder = styleBuilder;
        this.parserHelper = new JsonStyleParserHelper(configuration, requestFactory, this.sldStyleBuilder,
                                                      true);
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
        final List<Rule> styleRules = new ArrayList<>(this.json.size());

        for (Iterator<String> iterator = this.json.keys(); iterator.hasNext(); ) {
            String styleKey = iterator.next();
            if (styleKey.equals(JSON_STYLE_PROPERTY) ||
                    styleKey.equals(MapfishStyleParserPlugin.JSON_VERSION)) {
                continue;
            }
            PJsonObject styleJson = this.json.getJSONObject(styleKey);
            final List<Rule> currentRules = createStyleRule(styleKey, styleJson, styleProperty);
            for (Rule currentRule: currentRules) {
                if (currentRule != null) {
                    styleRules.add(currentRule);
                }
            }
        }

        return styleRules;
    }

    private List<Rule> createStyleRule(
            final String styleKey,
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

        return Arrays.asList(
                createGeometryFilteredRule(pointSymbolizer, styleKey, styleProperty, Point.class,
                                           MultiPoint.class,
                                           GeometryCollection.class),
                createGeometryFilteredRule(lineSymbolizer, styleKey, styleProperty, LineString.class,
                                           MultiLineString.class,
                                           LinearRing.class, GeometryCollection.class),
                createGeometryFilteredRule(polygonSymbolizer, styleKey, styleProperty, Polygon.class,
                                           MultiPolygon.class,
                                           GeometryCollection.class),
                textRule);
    }

    @SafeVarargs
    //CHECKSTYLE:OFF
    private final Rule createGeometryFilteredRule(
            final Symbolizer symb,
            final String styleKey,
            final String styleProperty,
            final Class<? extends Geometry>... geomClass) {
        //CHECKSTYLE:ON
        if (symb != null) {
            Expression geomProperty = this.sldStyleBuilder.attributeExpression(this.geometryProperty);
            final Function geometryTypeFunction =
                    this.sldStyleBuilder.getFilterFactory().function("geometryType", geomProperty);
            final ArrayList<Filter> geomOptions = new ArrayList<>(geomClass.length);
            for (Class<? extends Geometry> requiredType: geomClass) {
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
    private Filter createFilter(final String styleKey, final String styleProperty) {
        if (StringUtils.isEmpty(styleProperty) || StringUtils.isEmpty(styleKey)) {
            return null;
        }

        final Expression attributeExpression = this.sldStyleBuilder.attributeExpression(styleProperty);
        final Expression valueExpression = this.sldStyleBuilder.literalExpression(styleKey);
        return this.sldStyleBuilder.getFilterFactory().equals(attributeExpression, valueExpression);
    }
}
