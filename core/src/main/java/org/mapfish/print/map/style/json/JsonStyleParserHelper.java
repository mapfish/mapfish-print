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
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.styling.AnchorPoint;
import org.geotools.styling.Displacement;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Font;
import org.geotools.styling.Graphic;
import org.geotools.styling.Halo;
import org.geotools.styling.LabelPlacement;
import org.geotools.styling.LinePlacement;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointPlacement;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.TextSymbolizer;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;

import java.awt.Color;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import static org.mapfish.print.FileUtils.testForLegalFileUrl;
import static org.mapfish.print.map.style.json.MapfishJsonStyleParserPlugin.Versions;

/**
 * Methods shared by various style versions for creating geotools SLD styles from the json format mapfish supports.
 *
 * @author Jesse on 7/25/2014.
 */
public final class JsonStyleParserHelper {

    static final String JSON_FONT_FAMILY = "fontFamily";
    static final String JSON_FONT_SIZE = "fontSize";
    static final String JSON_FONT_WEIGHT = "fontWeight";
    static final String JSON_FONT_STYLE = "fontStyle";
    static final String JSON_LABEL = "label";
    static final String JSON_LABEL_ANCHOR_POINT_X = "labelAnchorPointX";
    static final String JSON_LABEL_ANCHOR_POINT_Y = "labelAnchorPointY";
    static final String JSON_LABEL_ALIGN = "labelAlign";
    static final String JSON_LABEL_X_OFFSET = "labelXOffset";
    static final String JSON_LABEL_Y_OFFSET = "labelYOffset";
    static final String JSON_LABEL_ROTATION = "labelRotation";
    static final String JSON_LABEL_PERPENDICULAR_OFFSET = "labelPerpendicularOffset";
    static final String JSON_FILL_COLOR = "fillColor";
    static final String JSON_STROKE_COLOR = "strokeColor";
    static final String JSON_STROKE_OPACITY = "strokeOpacity";
    static final String JSON_STROKE_WIDTH = "strokeWidth";
    static final String JSON_STROKE_DASHSTYLE = "strokeDashstyle";
    static final String JSON_STROKE_LINECAP = "strokeLinecap";
    static final String JSON_FILL_OPACITY = "fillOpacity";
    static final String JSON_EXTERNAL_GRAPHIC = "externalGraphic";
    static final String JSON_GRAPHIC_NAME = "graphicName";
    static final String JSON_GRAPHIC_OPACITY = "graphicOpacity";
    static final String JSON_POINT_RADIUS = "pointRadius";
    static final String JSON_GRAPHIC_WIDTH = "graphicWidth";
    static final String JSON_ROTATION = "rotation";
    static final String JSON_HALO_RADIUS = "haloRadius";
    static final String JSON_HALO_COLOR = "haloColor";
    static final String JSON_HALO_OPACITY = "haloOpacity";
    static final String JSON_FONT_COLOR = "fontColor";
    static final String JSON_FONT_OPACITY = "fontOpacity";
    static final String JSON_GRAPHIC_FORMAT = "graphicFormat";
    static final String STROKE_DASHSTYLE_SOLID = "solid";
    static final String STROKE_DASHSTYLE_DOT = "dot";
    static final String STROKE_DASHSTYLE_DASH = "dash";
    static final String STROKE_DASHSTYLE_DASHDOT = "dashdot";
    static final String STROKE_DASHSTYLE_LONGDASH = "longdash";
    static final String STROKE_DASHSTYLE_LONGDASHDOT = "longdashdot";
    static final Set<Set<String>> COMPATIBLE_MIMETYPES = Sets.newIdentityHashSet();
    static {
        COMPATIBLE_MIMETYPES.add(Sets.newHashSet("image/jpeg", "image/jpg"));
        COMPATIBLE_MIMETYPES.add(Sets.newHashSet("image/jpeg1000", "image/jpg2000"));
        COMPATIBLE_MIMETYPES.add(Sets.newHashSet("image/tiff", "image/tif"));
    }

    private static final String[] SUPPORTED_MIME_TYPES = ImageIO.getReaderMIMETypes();
    private static final String DEFAULT_POINT_MARK = "circle";


    private final Configuration configuration;
    private boolean allowNullSymbolizer;
    private StyleBuilder styleBuilder;
    private Versions version;

    /**
     * Constructor.
     *
     * @param configuration       the configuration to use for resolving relative files or other settings.
     * @param styleBuilder        a style builder to use for creating the style objects.
     * @param allowNullSymbolizer If true then create*Symbolizer() methods can return null if expected params are missing.
     * @param version             the version being parsed.
     */
    public JsonStyleParserHelper(@Nonnull final Configuration configuration,
                                 @Nonnull final StyleBuilder styleBuilder,
                                 final boolean allowNullSymbolizer,
                                 final Versions version) {
        this.configuration = configuration;
        this.styleBuilder = styleBuilder;
        this.allowNullSymbolizer = allowNullSymbolizer;
        this.version = version;
    }

    void setAllowNullSymbolizer(final boolean allowNullSymbolizer) {
        this.allowNullSymbolizer = allowNullSymbolizer;
    }

    /**
     * Create a style from a list of rules.
     *
     * @param styleRules the rules
     */
    public Style createStyle(final List<Rule> styleRules) {
        final Rule[] rulesArray = styleRules.toArray(new Rule[styleRules.size()]);
        final FeatureTypeStyle featureTypeStyle = this.styleBuilder.createFeatureTypeStyle(null, rulesArray);
        final Style style = this.styleBuilder.createStyle();
        style.featureTypeStyles().add(featureTypeStyle);
        return style;
    }

    /**
     * Add a point symbolizer definition to the rule.
     *
     * @param styleJson The old style.
     */
    @Nullable
    public PointSymbolizer createPointSymbolizer(final PJsonObject styleJson) {

        if (this.allowNullSymbolizer && !(styleJson.has(JSON_EXTERNAL_GRAPHIC) || styleJson.has(JSON_GRAPHIC_NAME) ||
                                          styleJson.has(JSON_POINT_RADIUS))) {
            return null;
        }

        Graphic graphic = this.styleBuilder.createGraphic();
        graphic.graphicalSymbols().clear();
        if (styleJson.has(JSON_EXTERNAL_GRAPHIC)) {
            String externalGraphicUrl = validateURL(styleJson.getString(JSON_EXTERNAL_GRAPHIC));
            final String graphicFormat = getGraphicFormat(externalGraphicUrl, styleJson);
            final ExternalGraphic externalGraphic = this.styleBuilder.createExternalGraphic(externalGraphicUrl, graphicFormat);

            graphic.graphicalSymbols().add(externalGraphic);
        }

        if (styleJson.has(JSON_GRAPHIC_NAME)) {
            Expression graphicName = parseProperty(styleJson.getString(JSON_GRAPHIC_NAME), new Function<String, Object>() {
                public Object apply(final String input) {
                    return input;
                }
            });
            Fill fill = createFill(styleJson);
            Stroke stroke = createStroke(styleJson, false);

            final Mark mark = this.styleBuilder.createMark(graphicName, fill, stroke);
            graphic.graphicalSymbols().add(mark);
        }

        if (graphic.graphicalSymbols().isEmpty()) {
            Fill fill = createFill(styleJson);
            Stroke stroke = createStroke(styleJson, false);

            final Mark mark = this.styleBuilder.createMark(DEFAULT_POINT_MARK, fill, stroke);
            graphic.graphicalSymbols().add(mark);
        }

        graphic.setOpacity(parseExpression(null, styleJson, JSON_GRAPHIC_OPACITY, new Function<String, Object>() {
            @Nullable
            @Override
            public Object apply(final String opacityString) {
                return Double.parseDouble(opacityString);
            }
        }));

        if (!Strings.isNullOrEmpty(styleJson.optString(JSON_POINT_RADIUS))) {
            Expression size = parseExpression(null, styleJson, JSON_POINT_RADIUS, new Function<String, Object>() {
                @Nullable
                @Override
                public Object apply(final String input) {
                    return Double.parseDouble(input) * 2;
                }
            });
            graphic.setSize(size);
        } else if (!Strings.isNullOrEmpty(styleJson.optString(JSON_GRAPHIC_WIDTH))) {
            Expression size = parseExpression(null, styleJson, JSON_GRAPHIC_WIDTH, new Function<String, Object>() {
                @Nullable
                @Override
                public Object apply(final String input) {
                    return Double.parseDouble(input);
                }
            });
            graphic.setSize(size);
        }

        if (!Strings.isNullOrEmpty(styleJson.optString(JSON_ROTATION))) {
            final Expression rotation = parseExpression(null, styleJson, JSON_ROTATION, new Function<String, Object>() {
                @Nullable
                @Override
                public Object apply(final String rotation) {
                    return Double.parseDouble(rotation);
                }
            });
            graphic.setRotation(rotation);
        }

        return this.styleBuilder.createPointSymbolizer(graphic);
    }

    private String validateURL(final String externalGraphicUrl) {
        try {
            new URL(externalGraphicUrl);
        } catch (MalformedURLException e) {
            // not a url so assume a file url and verify that it is valid
            try {
                final URL fileURL = new URL("file://" + externalGraphicUrl);
                return testForLegalFileUrl(this.configuration, fileURL).toExternalForm();
            } catch (MalformedURLException e1) {
                // unable to convert to file url so give up and throw exception;
                throw ExceptionUtils.getRuntimeException(e);
            }
        }
        return externalGraphicUrl;
    }

    /**
     * Add a line symbolizer definition to the rule.
     *
     * @param styleJson The old style.
     */
    @VisibleForTesting
    @Nullable
    protected LineSymbolizer createLineSymbolizer(final PJsonObject styleJson) {
        final Stroke stroke = createStroke(styleJson, true);
        if (stroke == null) {
            return null;
        } else {
            return this.styleBuilder.createLineSymbolizer(stroke);
        }
    }

    /**
     * Add a polygon symbolizer definition to the rule.
     *
     * @param styleJson The old style.
     */
    @Nullable
    @VisibleForTesting
    protected PolygonSymbolizer createPolygonSymbolizer(final PJsonObject styleJson) {
        if (this.allowNullSymbolizer && !styleJson.has(JSON_FILL_COLOR)) {
            return null;
        }

        final PolygonSymbolizer symbolizer = this.styleBuilder.createPolygonSymbolizer();
        symbolizer.setFill(createFill(styleJson));

        symbolizer.setStroke(createStroke(styleJson, false));

        return symbolizer;
    }

    /**
     * Add a text symbolizer definition to the rule.
     *
     * @param styleJson The old style.
     */
    @VisibleForTesting
    protected TextSymbolizer createTextSymbolizer(final PJsonObject styleJson) {
        final TextSymbolizer textSymbolizer = this.styleBuilder.createTextSymbolizer();

        if (styleJson.has(JSON_LABEL)) {
            final Expression label = parseExpression(null, styleJson, JSON_LABEL, new Function<String, Object>() {
                @Nullable
                @Override
                public Object apply(final String labelValue) {
                    return labelValue.replace("${", "").replace("}", "");
                }
            });

            textSymbolizer.setLabel(label);
        } else {
            return null;
        }

        textSymbolizer.setFont(createFont(textSymbolizer.getFont(), styleJson));

        if (styleJson.has(JSON_LABEL_ANCHOR_POINT_X) ||
            styleJson.has(JSON_LABEL_ANCHOR_POINT_Y) ||
            styleJson.has(JSON_LABEL_ALIGN) ||
            styleJson.has(JSON_LABEL_X_OFFSET) ||
            styleJson.has(JSON_LABEL_Y_OFFSET) ||
            styleJson.has(JSON_LABEL_ROTATION) ||
            styleJson.has(JSON_LABEL_PERPENDICULAR_OFFSET)) {
            textSymbolizer.setLabelPlacement(createLabelPlacement(styleJson));
        }

        if (!Strings.isNullOrEmpty(styleJson.optString(JSON_HALO_RADIUS)) ||
            !Strings.isNullOrEmpty(styleJson.optString(JSON_HALO_COLOR)) ||
            !Strings.isNullOrEmpty(styleJson.optString(JSON_HALO_OPACITY))) {
            textSymbolizer.setHalo(createHalo(styleJson));
        }

        if (!Strings.isNullOrEmpty(styleJson.optString(JSON_FONT_COLOR)) ||
            !Strings.isNullOrEmpty(styleJson.optString(JSON_FONT_OPACITY))) {
            textSymbolizer.setFill(addFill(styleJson.optString(JSON_FONT_COLOR, "black"), styleJson.optString(JSON_FONT_OPACITY, "1.0")));
        }

        return textSymbolizer;
    }

    private Font createFont(final Font defaultFont, final PJsonObject styleJson) {

        Expression fontFamily = parseExpression(null, styleJson, JSON_FONT_FAMILY, new Function<String, String>() {
            @Override
            public String apply(final String fontFamily) {
                return fontFamily;
            }
        });
        if (fontFamily == null) {
            fontFamily = defaultFont.getFamily().get(0);
        }

        Expression fontSize = parseExpression(null, styleJson, JSON_FONT_SIZE, new Function<String, Object>() {
            @Nullable
            @Override
            public Object apply(final String input) {
                String fontSizeString = input;
                if (fontSizeString.endsWith("px")) {
                    fontSizeString = fontSizeString.substring(0, fontSizeString.length() - 2);
                }
                return Integer.parseInt(fontSizeString);
            }
        });
        if (fontSize == null) {
            fontSize = defaultFont.getSize();
        }

        Expression fontWeight = parseExpression(null, styleJson, JSON_FONT_WEIGHT, Functions.<String>identity());
        if (fontWeight == null) {
            fontWeight = defaultFont.getWeight();
        }

        Expression fontStyle = parseExpression(null, styleJson, JSON_FONT_STYLE, Functions.<String>identity());
        if (fontStyle == null) {
            fontStyle = defaultFont.getStyle();
        }

        return this.styleBuilder.createFont(fontFamily, fontStyle, fontWeight, fontSize);
    }

    private LabelPlacement createLabelPlacement(final PJsonObject styleJson) {
        if ((styleJson.has(JSON_LABEL_ANCHOR_POINT_X) ||
             styleJson.has(JSON_LABEL_ANCHOR_POINT_Y) ||
             styleJson.has(JSON_LABEL_ALIGN) ||
             styleJson.has(JSON_LABEL_X_OFFSET) ||
             styleJson.has(JSON_LABEL_Y_OFFSET) ||
             styleJson.has(JSON_LABEL_ROTATION))
            && Strings.isNullOrEmpty(styleJson.optString(JSON_LABEL_PERPENDICULAR_OFFSET))) {
            return createPointPlacement(styleJson);
        }

        return createLinePlacement(styleJson);
    }

    private LinePlacement createLinePlacement(final PJsonObject styleJson) {
        Expression linePlacement = parseExpression(null, styleJson, JSON_LABEL_PERPENDICULAR_OFFSET, new Function<String, Object>() {
            @Nullable
            @Override
            public Object apply(final String input) {
                return Double.parseDouble(input);
            }
        });

        if (linePlacement != null) {
            return this.styleBuilder.createLinePlacement(linePlacement);
        }
        return null;
    }

    @Nullable
    private PointPlacement createPointPlacement(final PJsonObject styleJson) {
        AnchorPoint anchorPoint = createAnchorPoint(styleJson);

        Displacement displacement = null;
        if (styleJson.has(JSON_LABEL_X_OFFSET) || styleJson.has(JSON_LABEL_Y_OFFSET)) {

            Expression xOffset = parseExpression(0.0, styleJson, JSON_LABEL_X_OFFSET, new Function<String, Double>() {
                @Nullable
                @Override
                public Double apply(final String input) {
                    return Double.parseDouble(input);
                }
            });
            Expression yOffset = parseExpression(0.0, styleJson, JSON_LABEL_Y_OFFSET, new Function<String, Double>() {
                @Nullable
                @Override
                public Double apply(final String input) {
                    return Double.parseDouble(input);
                }
            });

            displacement = this.styleBuilder.createDisplacement(xOffset, yOffset);
        }

        Expression rotation = parseExpression(0.0, styleJson, JSON_LABEL_ROTATION, new Function<String, Double>() {
            @Nullable
            @Override
            public Double apply(final String input) {
                return Double.parseDouble(input);
            }
        });

        if (anchorPoint == null) {
            anchorPoint = this.styleBuilder.createAnchorPoint(0, 0);
        }

        if (displacement == null) {
            displacement = this.styleBuilder.createDisplacement(0, 0);
        }

        return this.styleBuilder.createPointPlacement(anchorPoint, displacement, rotation);
    }

    private AnchorPoint createAnchorPoint(final PJsonObject styleJson) {
        Expression anchorX = parseExpression(null, styleJson, JSON_LABEL_ANCHOR_POINT_X, new Function<String, Double>() {
            @Nullable
            @Override
            public Double apply(final String input) {
                return Double.parseDouble(input);
            }
        });
        Expression anchorY = parseExpression(null, styleJson, JSON_LABEL_ANCHOR_POINT_Y, new Function<String, Double>() {
            @Nullable
            @Override
            public Double apply(final String input) {
                return Double.parseDouble(input);
            }
        });

        if (anchorX == null && anchorY == null) {
            String labelAlign = styleJson.getString(JSON_LABEL_ALIGN);
            String xAlign = labelAlign.substring(0, 1);
            String yAlign = labelAlign.substring(1, 2);

            final double anchorInMiddle = 0.5;
            if ("l".equals(xAlign)) {
                anchorX = this.styleBuilder.literalExpression(0.0);
            } else if ("c".equals(xAlign)) {
                anchorX = this.styleBuilder.literalExpression(anchorInMiddle);
            } else if ("r".equals(xAlign)) {
                anchorX = this.styleBuilder.literalExpression(1.0);
            }
            if ("b".equals(yAlign)) {
                anchorY = this.styleBuilder.literalExpression(0.0);
            } else if ("m".equals(yAlign)) {
                anchorY = this.styleBuilder.literalExpression(anchorInMiddle);
            } else if ("t".equals(yAlign)) {
                anchorY = this.styleBuilder.literalExpression(1.0);
            }
        }
        boolean returnNull = true;
        if (anchorX == null) {
            anchorX = this.styleBuilder.literalExpression(0.0);
        } else {
            returnNull = false;
        }
        if (anchorY == null) {
            anchorY = this.styleBuilder.literalExpression(0.0);
        } else {
            returnNull = false;
        }

        if (returnNull) {
            return null;
        }

        return this.styleBuilder.createAnchorPoint(anchorX, anchorY);
    }

    private Halo createHalo(final PJsonObject styleJson) {
        if (styleJson.has(JSON_HALO_RADIUS)) {
            Expression radius = parseExpression(1.0, styleJson, JSON_HALO_RADIUS, new Function<String, Double>() {
                @Nullable
                @Override
                public Double apply(final String input) {
                    return Double.parseDouble(input);
                }
            });

            final Fill fill;
            if (styleJson.has(JSON_HALO_COLOR) || styleJson.has(JSON_HALO_OPACITY)) {
                fill = addFill(styleJson.optString(JSON_HALO_COLOR, "white"), styleJson.optString(JSON_HALO_OPACITY, "1.0"));
                return this.styleBuilder.createHalo(fill, radius);
            }
        }

        return null;
    }

    private Fill createFill(final PJsonObject styleJson) {
        if (this.allowNullSymbolizer && !styleJson.has(JSON_FILL_COLOR)) {
            return null;
        }
        final String fillColor = styleJson.optString(JSON_FILL_COLOR, "black");
        return addFill(fillColor, styleJson.optString(JSON_FILL_OPACITY, "1.0"));
    }

    private Fill addFill(final String fillColor, final String fillOpacity) {
        final Expression finalFillColor = parseProperty(fillColor, new Function<String, Object>() {
            @Nullable
            @Override
            public Object apply(final String fillColor) {
                return toColorExpression(fillColor);
            }
        });
        final Expression opacity = parseProperty(fillOpacity, new Function<String, Object>() {
            @Nullable
            @Override
            public Object apply(final String fillOpacity) {
                return Double.parseDouble(fillOpacity);
            }
        });
        return this.styleBuilder.createFill(finalFillColor, opacity);
    }

    private Object toColorExpression(final String color) {
        return ((Literal) JsonStyleParserHelper.this.styleBuilder.colorExpression(ColorParser.toColor(color))).getValue();
    }

    @Nullable
    @VisibleForTesting
    Stroke createStroke(final PJsonObject styleJson, final boolean allowNull) {
        final float defaultDashSpacing = 0.1f;
        final int doubleWidth = 2;
        final int tripleWidth = 3;
        final int quadrupleWidth = 4;
        final int quintupleWidth = 5;

        if (this.allowNullSymbolizer && allowNull && !styleJson.has(JSON_STROKE_COLOR)) {
            return null;
        }
        Expression strokeColor = parseExpression(Color.black, styleJson, JSON_STROKE_COLOR, new Function<String, Object>() {
            @Nullable
            @Override
            public Object apply(final String input) {
                return toColorExpression(input);
            }
        });
        Expression strokeOpacity = parseExpression(1.0, styleJson, JSON_STROKE_OPACITY, new Function<String, Object>() {
            @Nullable
            @Override
            public Object apply(final String input) {
                return Double.parseDouble(styleJson.getString(JSON_STROKE_OPACITY));
            }
        });
        Expression widthExpression = parseExpression(1, styleJson, JSON_STROKE_WIDTH, new Function<String, Object>() {
            @Nullable
            @Override
            public Object apply(final String input) {
                return Double.parseDouble(styleJson.getString(JSON_STROKE_WIDTH));
            }
        });

        float[] dashArray = null;
        if (styleJson.has(JSON_STROKE_DASHSTYLE) && !STROKE_DASHSTYLE_SOLID.equals(styleJson.getString(JSON_STROKE_DASHSTYLE))) {
            Double width = 1.0;
            if (widthExpression instanceof Literal) {
                Literal expression = (Literal) widthExpression;
                width = ((Number) expression.getValue()).doubleValue();
            }
            String dashStyle = styleJson.getString(JSON_STROKE_DASHSTYLE);
            if (dashStyle.equalsIgnoreCase(STROKE_DASHSTYLE_DOT)) {
                dashArray = new float[]{defaultDashSpacing, (float) (doubleWidth * width)};
            } else if (dashStyle.equalsIgnoreCase(STROKE_DASHSTYLE_DASH)) {
                dashArray = new float[]{(float) (doubleWidth * width), (float) (doubleWidth * width)};
            } else if (dashStyle.equalsIgnoreCase(STROKE_DASHSTYLE_DASHDOT)) {
                dashArray = new float[]{
                        (float) (tripleWidth * width),
                        (float) (doubleWidth * width),
                        defaultDashSpacing,
                        (float) (doubleWidth * width)};
            } else if (dashStyle.equalsIgnoreCase(STROKE_DASHSTYLE_LONGDASH)) {
                dashArray = new float[]{
                        (float) (quadrupleWidth * width),
                        (float) (doubleWidth * width)};
            } else if (dashStyle.equalsIgnoreCase(STROKE_DASHSTYLE_LONGDASHDOT)) {
                dashArray = new float[]{
                        (float) (quintupleWidth * width),
                        (float) (doubleWidth * width),
                        defaultDashSpacing,
                        (float) (doubleWidth * width)};
            } else if (dashStyle.contains(" ")) {
                //check for pattern if empty array, throw.
                try {
                    String[] x = dashStyle.split(" ");
                    if (x.length > 1) {
                        dashArray = new float[x.length];
                        for (int i = 0; i < x.length; i++) {
                            dashArray[i] = Float.parseFloat(x[i]);
                        }
                    }
                } catch (NumberFormatException e) {
                    //assume solid!
                }
            } else {
                throw new IllegalArgumentException("strokeDashstyle does not have a legal value: " + dashStyle);
            }
        }

        Expression lineCap = parseExpression(null, styleJson, JSON_STROKE_LINECAP, Functions.<String>identity());

        final Stroke stroke = this.styleBuilder.createStroke(strokeColor, widthExpression);
        stroke.setLineCap(lineCap);
        stroke.setOpacity(strokeOpacity);
        stroke.setDashArray(dashArray);
        return stroke;
    }

    @VisibleForTesting
    String getGraphicFormat(final String externalGraphicFile, final PJsonObject styleJson) {
        String mimeType = null;
        if (!Strings.isNullOrEmpty(styleJson.optString(JSON_GRAPHIC_FORMAT))) {
            mimeType = styleJson.getString(JSON_GRAPHIC_FORMAT);
        } else {
            int separatorPos = externalGraphicFile.lastIndexOf(".");

            if (separatorPos >= 0) {
                mimeType = "image/" + externalGraphicFile.substring(separatorPos + 1).toLowerCase();
            } else {
                mimeType = "";
            }
        }
        mimeType = toSupportedMimeType(mimeType);
        return mimeType;
    }

    private String toSupportedMimeType(final String mimeType) {
        for (Set<String> compatibleMimeType : COMPATIBLE_MIMETYPES) {
            if (compatibleMimeType.contains(mimeType.toLowerCase())) {
                for (String compatible : compatibleMimeType) {
                    if (isSupportedMimetype(compatible)) {
                        return compatible;
                    }
                }
            }
        }
        return mimeType;
    }

    private boolean isSupportedMimetype(final String mimeType) {
        for (String supported : SUPPORTED_MIME_TYPES) {
            if (supported.equalsIgnoreCase(mimeType)) {
                return true;
            }
        }

        return false;
    }

    public void setVersion(final Versions version) {
        this.version = version;
    }


    private <T> Expression parseExpression(final T defaultValue,
                                           final PJsonObject styleJson,
                                           final String propertyName,
                                           final Function<String, T> staticParser) {
        if (!styleJson.has(propertyName)) {
            if (defaultValue == null) {
                return null;
            }
            return this.styleBuilder.literalExpression(defaultValue);
        }

        String propertyValue = styleJson.getString(propertyName);
        if (isExpression(propertyValue)) {
            return toExpressionFromCQl(propertyValue);
        }
        return this.styleBuilder.literalExpression(staticParser.apply(propertyValue));
    }

    private <T> Expression parseProperty(final String property, final Function<String, T> literalSupplier) {
        if (isExpression(property)) {
            return toExpressionFromCQl(property);
        } else {
            return this.styleBuilder.literalExpression(literalSupplier.apply(property));
        }
    }

    private Expression toExpressionFromCQl(final String property) {
        try {
            return ECQL.toExpression(property, this.styleBuilder.getFilterFactory());
        } catch (CQLException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }

    private boolean isExpression(final String property) {
        return property != null && property.startsWith("[") && property.endsWith("]");
    }
}
