package org.mapfish.print.map.style.json;

import static org.mapfish.print.FileUtils.testForLegalFileUrl;
import static org.springframework.http.HttpMethod.HEAD;

import com.google.common.annotations.VisibleForTesting;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.StringUtils;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.style.AnchorPoint;
import org.geotools.api.style.Displacement;
import org.geotools.api.style.ExternalGraphic;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.Fill;
import org.geotools.api.style.Font;
import org.geotools.api.style.Graphic;
import org.geotools.api.style.Halo;
import org.geotools.api.style.LabelPlacement;
import org.geotools.api.style.LinePlacement;
import org.geotools.api.style.LineSymbolizer;
import org.geotools.api.style.Mark;
import org.geotools.api.style.PointPlacement;
import org.geotools.api.style.PointSymbolizer;
import org.geotools.api.style.PolygonSymbolizer;
import org.geotools.api.style.Rule;
import org.geotools.api.style.Stroke;
import org.geotools.api.style.Style;
import org.geotools.api.style.Symbolizer;
import org.geotools.api.style.TextSymbolizer;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.styling.StyleBuilder;
import org.mapfish.print.FontTools;
import org.mapfish.print.PrintException;
import org.mapfish.print.SetsUtils;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.map.DistanceUnit;
import org.mapfish.print.url.data.Handler;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Methods shared by various style versions for creating geotools SLD styles from the json format
 * mapfish supports.
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
  static final String JSON_LABEL_OUTLINE_COLOR = "labelOutlineColor";
  static final String JSON_LABEL_OUTLINE_WIDTH = "labelOutlineWidth";
  static final String JSON_LABEL_ALLOW_OVERRUNS = "allowOverruns";
  static final String JSON_LABEL_AUTO_WRAP = "autoWrap";
  static final String JSON_LABEL_CONFLICT_RESOLUTION = "conflictResolution";
  static final String JSON_LABEL_FOLLOW_LINE = "followLine";
  static final String JSON_LABEL_GOODNESS_OF_FIT = "goodnessOfFit";
  static final String JSON_LABEL_GROUP = "group";
  static final String JSON_LABEL_MAX_DISPLACEMENT = "maxDisplacement";
  static final String JSON_LABEL_SPACE_AROUND = "spaceAround";
  static final String JSON_FONT_COLOR = "fontColor";
  static final String JSON_FONT_OPACITY = "fontOpacity";
  static final String JSON_FILL_COLOR = "fillColor";
  static final String JSON_STROKE_COLOR = "strokeColor";
  static final String JSON_STROKE_OPACITY = "strokeOpacity";
  static final String JSON_STROKE_WIDTH = "strokeWidth";
  static final String JSON_STROKE_DASHSTYLE = "strokeDashstyle";
  static final String JSON_STROKE_DASHOFFSET = "strokeDashoffset";
  static final String JSON_STROKE_LINECAP = "strokeLinecap";
  static final String JSON_STROKE_LINEJOIN = "strokeLinejoin";
  static final String JSON_FILL_OPACITY = "fillOpacity";
  static final String JSON_EXTERNAL_GRAPHIC = "externalGraphic";
  static final String JSON_GRAPHIC_NAME = "graphicName";
  static final String JSON_GRAPHIC_OPACITY = "graphicOpacity";
  static final String JSON_GRAPHIC_Y_OFFSET = "graphicYOffset";
  static final String JSON_GRAPHIC_X_OFFSET = "graphicXOffset";
  static final String JSON_POINT_RADIUS = "pointRadius";
  static final String JSON_GRAPHIC_WIDTH = "graphicWidth";
  static final String JSON_ROTATION = "rotation";
  static final String JSON_HALO_RADIUS = "haloRadius";
  static final String JSON_HALO_COLOR = "haloColor";
  static final String JSON_HALO_OPACITY = "haloOpacity";
  static final String JSON_GRAPHIC_FORMAT = "graphicFormat";
  static final String STROKE_DASHSTYLE_SOLID = "solid";
  static final String STROKE_DASHSTYLE_DOT = "dot";
  static final String STROKE_DASHSTYLE_DASH = "dash";
  static final String STROKE_DASHSTYLE_DASHDOT = "dashdot";
  static final String STROKE_DASHSTYLE_LONGDASH = "longdash";
  static final String STROKE_DASHSTYLE_LONGDASHDOT = "longdashdot";
  static final List<Set<String>> COMPATIBLE_MIMETYPES = new ArrayList<>();
  private static final Logger LOGGER = LoggerFactory.getLogger(JsonStyleParserHelper.class);
  private static final String[] SUPPORTED_MIME_TYPES = ImageIO.getReaderMIMETypes();
  private static final String DEFAULT_POINT_MARK = "circle";
  private static final Pattern VALUE_UNIT_PATTERN = Pattern.compile("^([0-9.]+)([a-z]*)");
  private static final Pattern DATA_FORMAT_PATTERN = Pattern.compile("^data:([^;,]+)[;,]");

  static {
    COMPATIBLE_MIMETYPES.add(SetsUtils.create("image/jpeg", "image/jpg"));
    COMPATIBLE_MIMETYPES.add(SetsUtils.create("image/jpeg1000", "image/jpg2000"));
    COMPATIBLE_MIMETYPES.add(SetsUtils.create("image/tiff", "image/tif"));
  }

  private final Configuration configuration;
  private final ClientHttpRequestFactory requestFactory;
  private boolean allowNullSymbolizer;
  private StyleBuilder styleBuilder;

  /**
   * Constructor.
   *
   * @param configuration the configuration to use for resolving relative files or other settings.
   * @param requestFactory Request factory for making the request.
   * @param styleBuilder a style builder to use for creating the style objects.
   * @param allowNullSymbolizer If true then create*Symbolizer() methods can return null if expected
   *     params are missing.
   */
  public JsonStyleParserHelper(
      @Nullable final Configuration configuration,
      @Nonnull final ClientHttpRequestFactory requestFactory,
      @Nonnull final StyleBuilder styleBuilder,
      final boolean allowNullSymbolizer) {
    this.configuration = configuration;
    this.requestFactory = requestFactory;
    this.styleBuilder = styleBuilder;
    this.allowNullSymbolizer = allowNullSymbolizer;
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
    final Rule[] rulesArray = styleRules.toArray(new Rule[0]);
    final FeatureTypeStyle featureTypeStyle =
        this.styleBuilder.createFeatureTypeStyle(null, rulesArray);
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

    if (this.allowNullSymbolizer
        && !(styleJson.has(JSON_EXTERNAL_GRAPHIC)
            || styleJson.has(JSON_GRAPHIC_NAME)
            || styleJson.has(JSON_POINT_RADIUS))) {
      return null;
    }

    Graphic graphic = this.styleBuilder.createGraphic();
    graphic.graphicalSymbols().clear();
    if (styleJson.has(JSON_EXTERNAL_GRAPHIC)) {
      String externalGraphicUrl = validateURL(styleJson.getString(JSON_EXTERNAL_GRAPHIC));

      try {
        final URI uri = URI.create(externalGraphicUrl);
        if (uri.getScheme().startsWith("http")) {
          final ClientHttpRequest request = this.requestFactory.createRequest(uri, HttpMethod.GET);
          externalGraphicUrl = request.getURI().toString();
        }
      } catch (IOException ignored) {
        // ignored
      }

      final String graphicFormat = getGraphicFormat(externalGraphicUrl, styleJson);
      ExternalGraphic externalGraphic = null;
      if (externalGraphicUrl.startsWith("data:")) {
        try {
          URL url = URL.of(new URI(externalGraphicUrl), new Handler());
          externalGraphic = this.styleBuilder.createExternalGraphic(url, graphicFormat);
        } catch (MalformedURLException | URISyntaxException e) {
          // ignored
        }
      } else {
        externalGraphic =
            this.styleBuilder.createExternalGraphic(externalGraphicUrl, graphicFormat);
      }
      if (externalGraphic != null) {
        graphic.graphicalSymbols().add(externalGraphic);
      }
    }

    if (styleJson.has(JSON_GRAPHIC_NAME)) {
      Expression graphicName =
          parseProperty(styleJson.getString(JSON_GRAPHIC_NAME), input -> input);
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

    graphic.setOpacity(parseExpression(null, styleJson, JSON_GRAPHIC_OPACITY, Double::parseDouble));

    if (!StringUtils.isEmpty(styleJson.optString(JSON_POINT_RADIUS))) {
      Expression size =
          parseExpression(
              null,
              styleJson,
              JSON_POINT_RADIUS,
              (final String input) -> Double.parseDouble(input) * 2);
      graphic.setSize(size);
    } else if (!StringUtils.isEmpty(styleJson.optString(JSON_GRAPHIC_WIDTH))) {
      Expression size = parseExpression(null, styleJson, JSON_GRAPHIC_WIDTH, Double::parseDouble);
      graphic.setSize(size);
    }

    if (!StringUtils.isEmpty(styleJson.optString(JSON_GRAPHIC_Y_OFFSET))
        && !StringUtils.isEmpty(styleJson.optString(JSON_GRAPHIC_X_OFFSET))) {
      Expression dy = parseExpression(null, styleJson, JSON_GRAPHIC_Y_OFFSET, Double::parseDouble);
      Expression dx = parseExpression(null, styleJson, JSON_GRAPHIC_X_OFFSET, Double::parseDouble);
      Displacement offset = this.styleBuilder.createDisplacement(dx, dy);
      graphic.setDisplacement(offset);
    }

    if (!StringUtils.isEmpty(styleJson.optString(JSON_ROTATION))) {
      final Expression rotation =
          parseExpression(null, styleJson, JSON_ROTATION, Double::parseDouble);
      graphic.setRotation(rotation);
    }

    return this.styleBuilder.createPointSymbolizer(graphic);
  }

  private String validateURL(final String externalGraphicUrl) {
    try {
      URL.of(new URI(externalGraphicUrl), null);
    } catch (MalformedURLException | URISyntaxException | IllegalArgumentException e) {
      // not a url so assume a file or data url and verify that it is valid
      try {
        if (externalGraphicUrl.startsWith("data:")) {
          URL.of(new URI(externalGraphicUrl), new Handler());
          return externalGraphicUrl;
        }
        final URL fileURL = URL.of(new URI("file://" + externalGraphicUrl), null);
        return testForLegalFileUrl(this.configuration, fileURL).toExternalForm();
      } catch (MalformedURLException | URISyntaxException e1) {
        throw new PrintException("Unable to convert to file URL " + externalGraphicUrl, e);
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

    // make sure that labels are also rendered if a part of the text would be outside
    // the view context, see http://docs.geoserver.org/stable/en/user/styling/sld-reference/labeling
    // .html#partials
    textSymbolizer.getOptions().put("partials", "true");

    if (styleJson.has(JSON_LABEL)) {
      final Expression label =
          parseExpression(
              null,
              styleJson,
              JSON_LABEL,
              (final String labelValue) -> labelValue.replace("${", "").replace("}", ""));

      textSymbolizer.setLabel(label);
    } else {
      return null;
    }

    textSymbolizer.setFont(createFont(textSymbolizer.getFont(), styleJson));

    if (styleJson.has(JSON_LABEL_ANCHOR_POINT_X)
        || styleJson.has(JSON_LABEL_ANCHOR_POINT_Y)
        || styleJson.has(JSON_LABEL_ALIGN)
        || styleJson.has(JSON_LABEL_X_OFFSET)
        || styleJson.has(JSON_LABEL_Y_OFFSET)
        || styleJson.has(JSON_LABEL_ROTATION)
        || styleJson.has(JSON_LABEL_PERPENDICULAR_OFFSET)) {
      textSymbolizer.setLabelPlacement(createLabelPlacement(styleJson));
    }

    if (!StringUtils.isEmpty(styleJson.optString(JSON_HALO_RADIUS))
        || !StringUtils.isEmpty(styleJson.optString(JSON_HALO_COLOR))
        || !StringUtils.isEmpty(styleJson.optString(JSON_HALO_OPACITY))
        || !StringUtils.isEmpty(styleJson.optString(JSON_LABEL_OUTLINE_WIDTH))
        || !StringUtils.isEmpty(styleJson.optString(JSON_LABEL_OUTLINE_COLOR))) {
      textSymbolizer.setHalo(createHalo(styleJson));
    }

    if (!StringUtils.isEmpty(styleJson.optString(JSON_FONT_COLOR))
        || !StringUtils.isEmpty(styleJson.optString(JSON_FONT_OPACITY))) {
      textSymbolizer.setFill(
          addFill(
              styleJson.optString(JSON_FONT_COLOR, "black"),
              styleJson.optString(JSON_FONT_OPACITY, "1.0")));
    }

    this.addVendorOptions(JSON_LABEL_ALLOW_OVERRUNS, styleJson, textSymbolizer);
    this.addVendorOptions(JSON_LABEL_AUTO_WRAP, styleJson, textSymbolizer);
    this.addVendorOptions(JSON_LABEL_CONFLICT_RESOLUTION, styleJson, textSymbolizer);
    this.addVendorOptions(JSON_LABEL_FOLLOW_LINE, styleJson, textSymbolizer);
    this.addVendorOptions(JSON_LABEL_GOODNESS_OF_FIT, styleJson, textSymbolizer);
    this.addVendorOptions(JSON_LABEL_GROUP, styleJson, textSymbolizer);
    this.addVendorOptions(JSON_LABEL_MAX_DISPLACEMENT, styleJson, textSymbolizer);
    this.addVendorOptions(JSON_LABEL_SPACE_AROUND, styleJson, textSymbolizer);

    return textSymbolizer;
  }

  private void addVendorOptions(
      final String key, final PJsonObject styleJson, final Symbolizer symbolizer) {
    final String value = styleJson.optString(key);
    if (!StringUtils.isEmpty(value)) {
      symbolizer.getOptions().put(key, value);
    }
  }

  private double getPxSize(final String size) {
    Matcher matcher = VALUE_UNIT_PATTERN.matcher(size);
    matcher.find();

    double value = Double.parseDouble(matcher.group(1));
    String unit = matcher.group(2);

    if (unit.isEmpty()) {
      return value;
    } else {
      return DistanceUnit.fromString(unit).convertTo(value, DistanceUnit.PX);
    }
  }

  private Font createFont(final Font defaultFont, final PJsonObject styleJson) {

    Expression fontWeight = parseExpression(null, styleJson, JSON_FONT_WEIGHT, Function.identity());
    if (fontWeight == null) {
      fontWeight = defaultFont.getWeight();
    }

    Expression fontFamily =
        parseExpression(null, styleJson, JSON_FONT_FAMILY, fontFamily1 -> fontFamily1);
    if (fontFamily == null) {
      fontFamily = defaultFont.getFamily().getFirst();
    }
    List<Expression> fontFamilies = getFontExpressions(fontFamily, fontWeight);

    Expression fontSize = parseExpression(null, styleJson, JSON_FONT_SIZE, this::getPxSize);
    if (fontSize == null) {
      fontSize = defaultFont.getSize();
    }

    Expression fontStyle = parseExpression(null, styleJson, JSON_FONT_STYLE, Function.identity());
    if (fontStyle == null) {
      fontStyle = defaultFont.getStyle();
    }

    Font font =
        this.styleBuilder.createFont(fontFamilies.getFirst(), fontStyle, fontWeight, fontSize);
    if (fontFamilies.size() > 1) {
      // add remaining "fallback" fonts
      for (int i = 1; i < fontFamilies.size(); i++) {
        font.getFamily().add(fontFamilies.get(i));
      }
    }

    return font;
  }

  private List<Expression> getFontExpressions(
      final Expression fontFamily, final Expression fontWeight) {
    List<Expression> fontFamilies = new LinkedList<>();
    if (fontFamily instanceof Literal) {
      String fonts = (String) ((Literal) fontFamily).getValue();
      for (String font : fonts.split(",")) {
        font = font.trim();
        // translate SVG/CSS font expressions to Java logical fonts
        if (font.equalsIgnoreCase("serif")) {
          font = "Serif";
        } else if (font.equalsIgnoreCase("sans-serif")) {
          font = "SansSerif";
        } else if (font.equalsIgnoreCase("monospace")) {
          font = "Monospaced";
        }

        if (!FontTools.FONT_FAMILIES.contains(font)) {
          List<FontTools.FontConfigDescription> listFontConfigFonts =
              FontTools.listFontConfigFonts(font);
          int weight = -1;
          if (fontWeight instanceof Literal) {
            String stringWeight = (String) ((Literal) fontWeight).getValue();
            // See also: https://developer.mozilla.org/fr/docs/Web/CSS/font-weight#Valeurs
            if (stringWeight.equals("normal")) {
              weight = 400;
            } else if (stringWeight.equals("bold")) {
              weight = 700;
            } else {
              try {
                weight = Integer.parseInt(stringWeight);
              } catch (NumberFormatException e) {
                LOGGER.warn("Wrong weight {}", stringWeight);
              }
            }
            if (weight != -1) {
              LOGGER.debug("Search equivalent font for '{}' with weight {}", font, weight);
            }
          }
          boolean match = false;
          int currentDiff = 10000;
          Set<String> families = new HashSet<>();
          for (FontTools.FontConfigDescription fcd : listFontConfigFonts) {
            for (String family : fcd.family) {
              if (FontTools.FONT_FAMILIES.contains(family)) {
                match = true;
                if (weight == -1) {
                  families.add(family);
                } else {
                  int newDiff = Math.abs(fcd.weight - weight);
                  if (newDiff == currentDiff) {
                    LOGGER.debug("Match font '{}' with weight {}", family, fcd.weight);
                    families.add(family);
                  } else if (newDiff < currentDiff) {
                    LOGGER.debug("Reset match");
                    LOGGER.debug("Match font '{}' with weight {}", family, fcd.weight);
                    families.clear();
                    families.add(family);
                    currentDiff = newDiff;
                  }
                }
              }
            }
          }
          for (String family : families) {
            if (weight == -1) {
              LOGGER.info("Use font '{}' in place of font '{}'", family, font);
            } else {
              LOGGER.info(
                  "Use font '{}' in place of font '{}', with weight {}", family, font, weight);
            }
            fontFamilies.add(this.styleBuilder.literalExpression(family));
          }
          if (!match) {
            fontFamilies.add(this.styleBuilder.literalExpression(font));
          }
        } else {
          fontFamilies.add(this.styleBuilder.literalExpression(font));
        }
      }
    } else {
      fontFamilies.add(fontFamily);
    }
    return fontFamilies;
  }

  private LabelPlacement createLabelPlacement(final PJsonObject styleJson) {
    if ((styleJson.has(JSON_LABEL_ANCHOR_POINT_X)
            || styleJson.has(JSON_LABEL_ANCHOR_POINT_Y)
            || styleJson.has(JSON_LABEL_ALIGN)
            || styleJson.has(JSON_LABEL_X_OFFSET)
            || styleJson.has(JSON_LABEL_Y_OFFSET)
            || styleJson.has(JSON_LABEL_ROTATION))
        && StringUtils.isEmpty(styleJson.optString(JSON_LABEL_PERPENDICULAR_OFFSET))) {
      return createPointPlacement(styleJson);
    }

    return createLinePlacement(styleJson);
  }

  private LinePlacement createLinePlacement(final PJsonObject styleJson) {
    Expression linePlacement =
        parseExpression(null, styleJson, JSON_LABEL_PERPENDICULAR_OFFSET, Double::parseDouble);

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

      Expression xOffset =
          parseExpression(0.0, styleJson, JSON_LABEL_X_OFFSET, Double::parseDouble);
      Expression yOffset =
          parseExpression(0.0, styleJson, JSON_LABEL_Y_OFFSET, Double::parseDouble);

      displacement = this.styleBuilder.createDisplacement(xOffset, yOffset);
    }

    Expression rotation = parseExpression(0.0, styleJson, JSON_LABEL_ROTATION, Double::parseDouble);

    if (anchorPoint == null) {
      anchorPoint = this.styleBuilder.createAnchorPoint(0, 0);
    }

    if (displacement == null) {
      displacement = this.styleBuilder.createDisplacement(0, 0);
    }

    return this.styleBuilder.createPointPlacement(anchorPoint, displacement, rotation);
  }

  private AnchorPoint createAnchorPoint(final PJsonObject styleJson) {
    Expression anchorX =
        parseExpression(null, styleJson, JSON_LABEL_ANCHOR_POINT_X, Double::parseDouble);
    Expression anchorY =
        parseExpression(null, styleJson, JSON_LABEL_ANCHOR_POINT_Y, Double::parseDouble);

    if (anchorX == null && anchorY == null && styleJson.has(JSON_LABEL_ALIGN)) {
      String labelAlign = styleJson.getString(JSON_LABEL_ALIGN);
      String xAlign = labelAlign.substring(0, 1);
      String yAlign = labelAlign.substring(1, 2);

      final double anchorInMiddle = 0.5;
      anchorX =
          switch (xAlign) {
            case "l" -> this.styleBuilder.literalExpression(0.0);
            case "c" -> this.styleBuilder.literalExpression(anchorInMiddle);
            case "r" -> this.styleBuilder.literalExpression(1.0);
            default -> anchorX;
          };
      anchorY =
          switch (yAlign) {
            case "b" -> this.styleBuilder.literalExpression(0.0);
            case "m" -> this.styleBuilder.literalExpression(anchorInMiddle);
            case "t" -> this.styleBuilder.literalExpression(1.0);
            default -> anchorY;
          };
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
      Expression radius = parseExpression(1.0, styleJson, JSON_HALO_RADIUS, Double::parseDouble);

      final Fill fill;
      if (styleJson.has(JSON_HALO_COLOR) || styleJson.has(JSON_HALO_OPACITY)) {
        fill =
            addFill(
                styleJson.optString(JSON_HALO_COLOR, "white"),
                styleJson.optString(JSON_HALO_OPACITY, "1.0"));
        return this.styleBuilder.createHalo(fill, radius);
      }
    }

    // labelOutlineColor and labelOutlineWidth are aliases for halo that is used by some V2 Clients
    if (styleJson.has(JSON_LABEL_OUTLINE_COLOR) || styleJson.has(JSON_LABEL_OUTLINE_WIDTH)) {
      Expression radius =
          parseExpression(1.0, styleJson, JSON_LABEL_OUTLINE_WIDTH, Double::parseDouble);
      Fill fill = addFill(styleJson.optString(JSON_LABEL_OUTLINE_COLOR, "white"), "1.0");
      return this.styleBuilder.createHalo(fill, radius);
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
    final Expression finalFillColor = parseProperty(fillColor, this::toColorExpression);
    final Expression opacity = parseProperty(fillOpacity, Double::parseDouble);
    return this.styleBuilder.createFill(finalFillColor, opacity);
  }

  private Object toColorExpression(final String color) {
    return ((Literal)
            JsonStyleParserHelper.this.styleBuilder.colorExpression(ColorParser.toColor(color)))
        .getValue();
  }

  @Nullable
  @VisibleForTesting
  Stroke createStroke(final PJsonObject styleJson, final boolean allowNull) {

    if (this.allowNullSymbolizer && allowNull && !styleJson.has(JSON_STROKE_COLOR)) {
      return null;
    }
    Expression strokeColor =
        parseExpression(Color.black, styleJson, JSON_STROKE_COLOR, this::toColorExpression);
    Expression strokeOpacity =
        parseExpression(
            1.0,
            styleJson,
            JSON_STROKE_OPACITY,
            (final String input) -> Double.parseDouble(styleJson.getString(JSON_STROKE_OPACITY)));
    Expression widthExpression =
        parseExpression(
            1.0,
            styleJson,
            JSON_STROKE_WIDTH,
            (final String input) -> Double.parseDouble(styleJson.getString(JSON_STROKE_WIDTH)));

    List<Expression> dashArray = new ArrayList<>();
    if (styleJson.has(JSON_STROKE_DASHSTYLE)
        && !STROKE_DASHSTYLE_SOLID.equals(styleJson.getString(JSON_STROKE_DASHSTYLE))) {
      double width = 1.0;
      if (widthExpression instanceof Literal expression) {
        width = ((Number) expression.getValue()).doubleValue();
      }
      final Expression defaultDashSpacingE = this.styleBuilder.literalExpression(0.1f);
      final Expression doubleWidthE = this.styleBuilder.literalExpression((float) (2 * width));
      final Expression tripleWidthE = this.styleBuilder.literalExpression((float) (3 * width));
      final Expression quadrupleWidthE = this.styleBuilder.literalExpression((float) (4 * width));
      final Expression quintupleWidthE = this.styleBuilder.literalExpression((float) (5 * width));
      String dashStyle = styleJson.getString(JSON_STROKE_DASHSTYLE);
      if (dashStyle.equalsIgnoreCase(STROKE_DASHSTYLE_DOT)) {
        dashArray.add(defaultDashSpacingE);
        dashArray.add(doubleWidthE);
      } else if (dashStyle.equalsIgnoreCase(STROKE_DASHSTYLE_DASH)) {
        dashArray.add(doubleWidthE);
        dashArray.add(doubleWidthE);
      } else if (dashStyle.equalsIgnoreCase(STROKE_DASHSTYLE_DASHDOT)) {
        dashArray.add(tripleWidthE);
        dashArray.add(doubleWidthE);
        dashArray.add(defaultDashSpacingE);
        dashArray.add(doubleWidthE);
      } else if (dashStyle.equalsIgnoreCase(STROKE_DASHSTYLE_LONGDASH)) {
        dashArray.add(quadrupleWidthE);
        dashArray.add(doubleWidthE);
      } else if (dashStyle.equalsIgnoreCase(STROKE_DASHSTYLE_LONGDASHDOT)) {
        dashArray.add(quintupleWidthE);
        dashArray.add(doubleWidthE);
        dashArray.add(defaultDashSpacingE);
        dashArray.add(doubleWidthE);
      } else if (dashStyle.contains(" ")) {
        // check for pattern if empty array, throw.
        try {
          String[] x = dashStyle.split(" ");
          for (final String aX : x) {
            dashArray.add(this.styleBuilder.literalExpression(Float.parseFloat(aX)));
          }
        } catch (NumberFormatException e) {
          // assume solid!
        }
      } else {
        throw new IllegalArgumentException(
            "strokeDashstyle does not have a legal value: " + dashStyle);
      }
    }

    Expression lineCap = parseExpression(null, styleJson, JSON_STROKE_LINECAP, Function.identity());
    Expression lineJoin =
        parseExpression(null, styleJson, JSON_STROKE_LINEJOIN, Function.identity());

    final Stroke stroke = this.styleBuilder.createStroke(strokeColor, widthExpression);
    stroke.setLineCap(lineCap);
    stroke.setOpacity(strokeOpacity);
    stroke.setLineJoin(lineJoin);
    if (!dashArray.isEmpty()) {
      stroke.setDashArray(dashArray);
      Expression dashOffset =
          parseExpression(null, styleJson, JSON_STROKE_DASHOFFSET, Function.identity());
      stroke.setDashOffset(dashOffset);
    }
    return stroke;
  }

  @VisibleForTesting
  String getGraphicFormat(final String externalGraphicFile, final PJsonObject styleJson) {
    String mimeType = getMimeTypeFromStyleJson(styleJson);

    if (mimeType == null) {
      mimeType = getMimeTypeFromExternalFile(externalGraphicFile);
    }

    if (mimeType == null) {
      mimeType = getMimeTypeFromFileExtension(externalGraphicFile);
    }

    if (mimeType == null) {
      mimeType = getMimeTypeFromHttpResponse(externalGraphicFile);
    }

    mimeType = toSupportedMimeType(mimeType);
    return mimeType;
  }

  private String getMimeTypeFromStyleJson(final PJsonObject styleJson) {
    return !StringUtils.isEmpty(styleJson.optString(JSON_GRAPHIC_FORMAT))
        ? styleJson.getString(JSON_GRAPHIC_FORMAT)
        : null;
  }

  private String getMimeTypeFromExternalFile(final String externalGraphicFile) {
    Matcher matcher = DATA_FORMAT_PATTERN.matcher(externalGraphicFile);
    return matcher.find() ? matcher.group(1) : null;
  }

  private String getMimeTypeFromFileExtension(final String externalGraphicFile) {
    int separatorPos = externalGraphicFile.lastIndexOf(".");
    return separatorPos >= 0
        ? "image/" + externalGraphicFile.substring(separatorPos + 1).toLowerCase()
        : null;
  }

  private String getMimeTypeFromHttpResponse(final String externalGraphicFile) {
    try {
      URI uri = getUri(externalGraphicFile);
      final ClientHttpRequest request = this.requestFactory.createRequest(uri, HEAD);
      List<String> contentTypes;
      try (ClientHttpResponse httpResponse = request.execute()) {
        contentTypes = httpResponse.getHeaders().get("Content-Type");
      }

      if (contentTypes != null && contentTypes.size() == 1) {
        String contentType = contentTypes.getFirst();
        int index = contentType.lastIndexOf(";");
        return index >= 0 ? contentType.substring(0, index) : contentType;
      }

      LOGGER.info("No content type found for: {}", externalGraphicFile);
    } catch (IOException e) {
      throw new RuntimeException(
          "Unable to get a mime type for the external graphic " + externalGraphicFile, e);
    }

    return null;
  }

  private URI getUri(final String externalGraphicFile) {
    try {
      return new URI(externalGraphicFile);
    } catch (URISyntaxException e) {
      return new File(externalGraphicFile).toURI();
    }
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

  private <T> Expression parseExpression(
      final T defaultValue,
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

  private <T> Expression parseProperty(
      final String property, final Function<String, T> literalSupplier) {
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
      throw new PrintException(
          "Failed to convert from CQL to expression, the property " + property, e);
    }
  }

  private boolean isExpression(final String property) {
    return property != null && property.startsWith("[") && property.endsWith("]");
  }
}
