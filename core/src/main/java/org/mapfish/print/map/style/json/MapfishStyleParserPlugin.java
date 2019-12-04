package org.mapfish.print.map.style.json;

import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.json.JSONObject;
import org.mapfish.print.Constants;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.map.style.ParserPluginUtils;
import org.mapfish.print.map.style.SLDParserPlugin;
import org.mapfish.print.map.style.StyleParserPlugin;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.mapfish.print.map.style.json.MapfishJsonStyleVersion1.DEFAULT_GEOM_ATT_NAME;

/**
 * Supports all style format.
 * <p>
 * This style parser support two versions of JSON and SLD formatting.  Both versions use the same parameter
 * names for configuring the values of the various properties of the style but the layout differs between the
 * two and version 2 is more flexible and powerful than version 1.
 * </p>
 * <h2 id="stylev1">Mapfish JSON Style Version 1 <a class="headerlink" href="#stylev1">¶</a></h2>
 * <p>
 * Version 1 is compatible with mapfish print &lt;= v2 and is based on the OpenLayers v2 styling. The layout
 * is as follows:
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
 * <h2 id="stylev2">Mapfish JSON Style Version 2 <a class="headerlink" href="#stylev2">¶</a></h2>
 * <p>
 * Version 2 uses the same property names as version 1 but has a different structure. The layout is as
 * follows:
 * </p>
 * <pre><code>
 * {
 *   "version" : "2",
 *   // shared values can be declared here (at top level)
 *   // and used in form ${constName} later in json
 *   "val1" : "#FFA829",
 *   // default values for properties can be defined here
 *   " strokeDashstyle" : "dot"
 *   "[population &gt; 300]" : {
 *     // default values for current rule can be defined here
 *     // they will override default values defined at
 *     // higher level
 *     "rotation" : "30",
 *
 *     //min and max scale denominator are optional
 *     "maxScale" : 1000000,
 *     "minScale" : 100000,
 *     "symbolizers" : [{
 *       // values defined in symbolizer will override defaults
 *       "type" : "point",
 *       "fillColor":"#FF0000",
 *       "fillOpacity":0,
 *       "rotation" : "30",
 *       "externalGraphic" : "mark.png",
 *
 *       "graphicName": "circle",
 *       "graphicOpacity": 0.4,
 *       "pointRadius": 5,
 *
 *       "strokeColor":"${val1}",
 *       "strokeOpacity":1,
 *       "strokeWidth":5,
 *       "strokeLinecap":"round",
 *       "strokeDashstyle":"dot"
 *     }, {
 *       "type" : "line",
 *       "strokeColor":"${val1}",
 *       "strokeOpacity":1,
 *       "strokeWidth":5,
 *       "strokeLinecap":"round",
 *       "strokeDashstyle":"dot"
 *     }, {
 *       "type" : "polygon",
 *       "fillColor":"#FF0000",
 *       "fillOpacity":0,
 *
 *       "strokeColor":"${val1}",
 *       "strokeOpacity":1,
 *       "strokeWidth":5,
 *       "strokeLinecap":"round",
 *       "strokeDashstyle":"dot"
 *     }, {
 *       "type" : "text",
 *       "fontColor":"#000000",
 *       "fontFamily": "sans-serif",
 *       "fontSize": "12px",
 *       "fontStyle": "normal",
 *       "fontWeight": "bold",
 *       "haloColor": "#123456",
 *       "haloOpacity": "0.7",
 *       "haloRadius": "3.0",
 *       "label": "[name]",
 *       "fillColor":"#FF0000",
 *       "fillOpacity":0,
 *       "labelAlign": "cm",
 *       "labelRotation": "45",
 *       "labelXOffset": "-25.0",
 *       "labelYOffset": "-35.0"
 *     }
 *   ]}
 * }
 * </code></pre>
 * <p>
 * As illustrated above the style consists of:
 * </p>
 * <ul>
 * <li>The version number (2) (required)</li>
 * <li>
 * Common values which can be referenced in symbolizer property values.(optional)
 * <p>Values can be referenced in the value of a property with the pattern: ${valName}</p>
 * <p>Value names can only contain numbers, characters, _ or -</p>
 * <p>
 * Values do not have to be the full property they will be interpolated.  For example:
 * <code>The value is ${val}</code>
 * </p>
 * </li>
 * <li>
 * Defaults property definitions(optional):
 * <p>
 * In order to reduce duplication and keep the style definitions small, default values can be specified. The
 * default values in the root (style level) will be used in all symbolizers if the value is not defined. The
 * style level default will apply to all symbolizers defined in the system.
 * </p>
 * <p>
 * The only difference between a value and a default is that the default has a well known name, therefore
 * defaults can also be used as values.
 * </p>
 * </li>
 * <li>
 * All the styling rules (At least one is required)
 * <p>
 * A styling rule has a key which is the filter which selects the features that the rule will be used to draw
 * and the rule definition object.
 * </p>
 * <p>The filter is either <code>*</code> or an
 * <a href="http://docs.geoserver.org/stable/en/user/filter/ecql_reference.html#filter-ecql-reference">
 * ECQL Expression</a>) surrounded by square brackets.  For example: [att &lt; 23].
 * </p>
 * <p>
 * <em>WARNING:</em> At the moment DWITHIN and BEYOND spatial functions take a unit parameter.
 * However it is ignored by geotools and the distance is always in the crs of the geometry projection.
 * </p>
 * The rule definition is as follows:
 * <ul>
 * <li>
 * Default property values (optional):
 * <p>
 * Each rule can also have defaults.  If the style and the rule have a default for the same property the rule
 * will override the style default.  All defaults can be (of course) overridden by a value in a symbolizer.
 * </p>
 * </li>
 * <li>
 * minScale (optional)
 * <p>
 * The minimum scale that the rule should evaluate to true
 * </p>
 * </li>
 * <li>
 * maxScale (optional)
 * <p>
 * The maximum scale that the rule should evaluate to true
 * </p>
 * </li>
 * <li>
 * An array of symbolizers. (at least one required).
 * <p>
 * A symbolizer must have a type property (point, line, polygon, text) which indicates the type of symbolizer
 * and it has the attributes for that type of symbolizer.  All values have defaults so it is possible to
 * define a symbolizer as with only the type property. The only exception is that the "text" symbolizer needs
 * a label property.
 * </p>
 * </li>
 * </ul>
 * </li>
 * </ul>
 *
 * <h2 id="config">Configuration Elements <a class="headerlink" href="#config">¶</a></h2>
 * The items in the list below are the properties that can be set on the different symbolizers. In brackets
 * list the symbolizers the values can apply to.
 * <p>
 * Most properties can be static values or ECQL expressions.  If the property has <code>[ ]</code> around the
 * property value then it will be interpreted as an ECQL expression.  Otherwise it is assumed to be static
 * text.  If you need static text that start and ends with <code>[ ]</code> then you will have to enter:
 * <code>['propertyValue']</code> (where propertyValue start and ends with <code>[ ]</code>.
 * </p>
 * <p>
 * The items below with (ECQL) can have ECQL expressions.
 * </p>
 * <ul>
 * <li><strong>fillColor</strong>(ECQL) - (polygon, point, text) The color used to fill the point
 * graphic, polygon or text.</li>
 * <li><strong>fillOpacity</strong>(ECQL) - (polygon,  point, text) The opacity used when fill the
 * point graphic, polygon or text.</li>
 * <li><strong>rotation</strong>(ECQL) - (point) The rotation of the point graphic</li>
 * <li>
 * <strong>externalGraphic</strong> - (point) one of the two options for declaring the point
 * graphic to use.  This can be a URL to the icon to use or, if just a string it will be assumed to refer to a
 * file in the configuration directory (or subdirectory).  Only files in the configuration directory (or
 * subdirectory) will be allowed.
 * </li>
 * <li>
 * <strong>graphicName</strong>(ECQL) - (point) one of the two options for declaring the point
 * graphic to use.  This is the default and will be a square if not specified. The option are any of the
 * Geotools Marks.
 * <p>Geotools has by default 3 types of marks:</p>
 * <ul>
 * <li>WellKnownMarks: cross, star, triangle, arrow, X, hatch, square</li>
 * <li>ShapeMarks: shape://vertline, shape://horline, shape://slash, shape://backslash,
 * shape://dot, shape://plus, shape://times, shape://oarrow, shape://carrow, shape://coarrow,
 * shape://ccarrow</li>
 * <li>TTFMarkFactory: ttf://fontName#code (where fontName is a TrueType font and the code is
 * the code number of thecharacter to render for the point.</li>
 * </ul>
 * </li>
 * <li><strong>graphicOpacity</strong>(ECQL) - (point) the opacity to use when drawing the point
 * graphic</li>
 * <li><strong>pointRadius</strong>(ECQL) - (point) the size at which to draw the point graphic</li>
 * <li>
 * <strong>strokeColor</strong>(ECQL) - (line, point, polygon) the color to use when drawing a line
 * or the outline of a polygon or point graphic
 * </li>
 * <li><strong>strokeOpacity</strong>(ECQL) - (line, point, polygon) the opacity to use when drawing
 * the line/stroke</li>
 * <li><strong>strokeWidth</strong>(ECQL) - (line, point, polygon) the widh of the line/stroke</li>
 * <li>
 * <strong>strokeLinecap</strong>(ECQL) - (line, point, polygon) the style used when drawing the
 * end of a line.
 * <p>
 * Options:  butt (sharp square edge), round (rounded edge), and square (slightly elongated square edge).
 * Default is butt
 * </p>
 * </li>
 * <li>
 * <strong>strokeDashstyle</strong> - (line, point, polygon) A string describing how to draw the
 * line or an array of floats describing the line lengths and space lengths:
 * <ul>
 * <li>dot - translates to dash array: [0.1, 2 * strokeWidth]</li>
 * <li>dash - translates to dash array: [2 * strokeWidth, 2 * strokeWidth]</li>
 * <li>dashdot - translates to dash array: [3 * strokeWidth, 2 * strokeWidth, 0.1, 2 *
 * strokeWidth]</li>
 * <li>longdash - translates to dash array: [4 * strokeWidth, 2 * strokeWidth]</li>
 * <li>longdashdot - translates to dash array: [5 * strokeWidth, 2 * strokeWidth, 0.1, 2 *
 * strokeWidth]</li>
 * <li>{string containing spaces to delimit array elements} - Example: [1 2 3 1 2]</li>
 * </ul>
 * </li>
 * <li><strong>fontColor</strong>(ECQL) - (text) the color of the text drawn</li>
 * <li><strong>fontFamily</strong>(ECQL) - (text) the font of the text drawn</li>
 * <li><strong>fontSize</strong>(ECQL) - (text) the font size of the text drawn</li>
 * <li><strong>fontStyle</strong>(ECQL) - (text) the font style of the text drawn</li>
 * <li><strong>fontWeight</strong>(ECQL) - (text) the font weight of the text drawn</li>
 * <li><strong>haloColor</strong>(ECQL) - (text) the color of the halo around the text</li>
 * <li><strong>haloOpacity</strong>(ECQL) - (text) the opacity of the halo around the text</li>
 * <li><strong>haloRadius</strong>(ECQL) - (text) the radius of the halo around the text</li>
 * <li>
 * <strong>label</strong>(ECQL) - (text) the expression used to create the label e.  See the
 * section on labelling for more details
 * </li>
 * <li>
 * <strong>labelAlign</strong> - (Point Placement) the indicator of how to align the text with
 * respect to the geometry. This property must have 2 characters, the x-align and the y-align.
 * <p>
 * X-Align options:
 * </p>
 * <ul>
 * <li>l - align to the left of the geometric center</li>
 * <li>c - align on the center of the geometric center</li>
 * <li>r - align to the right of the geometric center</li>
 * </ul>
 * <p>
 * Y-Align options:
 * </p>
 * <ul>
 * <li>b - align to the bottom of the geometric center</li>
 * <li>m - align on the middle of the geometric center</li>
 * <li>t - align to the top of the geometric center</li>
 * </ul>
 * </li>
 *
 * <li><strong>labelRotation</strong>(ECQL) - (Point Placement) the rotation of the label</li>
 * <li><strong>labelXOffset</strong>(ECQL) - (Point Placement) the amount to offset the label along the
 * x axis.  negative number offset to the left</li>
 * <li><strong>labelYOffset</strong>(ECQL) - (Point Placement) the amount to offset the label along the
 * y axis.  negative number offset to the top of the printing</li>
 * <li><strong>labelAnchorPointX</strong>(ECQL) - (Point Placement) The point along the x axis that the
 * label is started at anchored). Offset and rotation is relative to this point.  Only one of
 * labelAnchorPointX/Y or labelAlign will be respected, since they are both ways of defining the anchor
 * Point</li>
 * <li><strong>labelAnchorPointY</strong>(ECQL) - (Point Placement) The point along the y axis that the
 * label is started at (anchored). Offset and rotation is relative to this point.  Only one of
 * labelAnchorPointX/Y or labelAlign will be respected, since they are both ways of defining the anchor
 * Point</li>
 * <li><strong>labelPerpendicularOffset</strong>(ECQL) - (Line Placement) If this property is defined
 * it will be assumed that the geometry is a line and this property defines how far from the center of the
 * line the label should be drawn.</li>
 * </ul>
 *
 * <h2 id="labels">Labelling: <a class="headerlink" href="#labels">¶</a></h2>
 * <p>
 * Labelling in this style format is done by defining a text symbolizer ("type":"text").  All text symbolizers
 * consist of:
 * </p>
 * <ul>
 * <li><a href="#labelproperties">Label Property</a></li>
 * <li><a href="#haloproperties">Halo Properties</a></li>
 * <li><a href="#otherproperties">Font/weight/style/color/opacity</a></li>
 * <li><a href="#placementproperties">Placement Properties</a></li>
 * <li><a href="#vendoroptions">Vendor Options</a></li>
 * </ul>
 *
 * <h3 id="labelproperties">Label Property <a class="headerlink" href="#labelproperties">¶</a></h3>
 * <p>
 * The label property defines what label will be drawn for a given feature.  The value is either a string
 * which will be the static label for all features that the symbolizer will be drawn on or a string surrounded
 * by [] which indicates that it is an ECQL Expression.  Examples:
 * </p>
 * <ul>
 * <li>Static label</li>
 * <li>[attributeName]</li>
 * <li>['Static Label Again']</li>
 * <li>[5]</li>
 * <li>5</li>
 * <li>env('java.home')</li>
 * <li>centroid(geomAtt)</li>
 * </ul>
 *
 * <h3 id="haloproperties">Halo Properties <a class="headerlink" href="#haloproperties">¶</a></h3>
 * <p>
 * A halo is a space around the drawn label text that is color (using the halo properties).  A label with a
 * halo is like the drawn label text with a buffer around the label text drawn using the halo properties. This
 * allows the label to be clearly visible regardless of the background.  For example if the text is black and
 * the halo is with, then the text will always be readable thanks to the white buffer around the label text.
 * </p>
 *
 * <h3 id="otherproperties">Font/weight/style/color/opacity
 * <a class="headerlink" href="#otherproperties">¶</a></h3>
 * <p>
 * The Font/weight/style/color/opacity properties define how the label text is drawn.  They are for the most
 * part equivalent to the similarly named css and SLD properties.
 * </p>
 *
 * <h3 id="placementproperties">Placement Properties
 * <a class="headerlink" href="#placementproperties">¶</a></h3>
 * <p>
 * An important part of defining a text symbolizer is defining where the text/label will be drawn.  The
 * placement properties are used for this purpose.  There are two types of placements, Point and Line
 * placement and <em>only one</em> type of placement can be used. The type of placement is determined by
 * inspecting the properties in the text symbolizer and if the <em>labelPerpendicularOffset</em> property is
 * defined then a line placement will be created for the text symbolizer.
 * </p>
 * <p>
 * It is important to realize that since only one type of placement can be used, an error will be reported if
 * <em>labelPerpendicularOffset</em> is defined in the text symbolizer along with
 * <em>any</em> of the point placement properties.
 * </p>
 * <p><strong>Point Placement</strong></p>
 * <p>
 * Point placement defines an <em>anchor point</em> which is the point to draw the text relative to. For
 * example an anchor point of 0.5, 0.5 ("labelAnchorPointX" : "0.5", "labelAnchorPointY" : "0.5") would
 * position the start of the label at the center of the geometry.
 * </p>
 * <p>
 * After <em>anchor point</em>, comes <em>displacement</em> displacement defines the distance from the anchor
 * point to start the label.  The combination of the two values determines the final location of the label.
 * </p>
 * <p>Lastly, there is a label rotation which defines the orientation of the label.</p>
 * <p>
 * There are two ways to define the anchor point, either the <em>labelAnchorPointX/Y</em> properties are set
 * or the <em>labelAlign</em> property is set.  If both are defined then the
 * <em>labelAlign</em> will be ignored.
 * </p>
 *
 * <h3 id="vendoroptions">Vendor Options <a class="headerlink" href="#vendoroptions">¶</a></h3>
 * <p>For text symbolizers the following vendor options are available:</p>
 * <ul>
 * <li><strong>allowOverruns</strong> (false): When false does not allow labels on lines to get beyond the
 * beginning/end of the line.  By default a partial overrun is tolerated, set to false to disallow it.
 * </li>
 * <li><strong>autoWrap</strong> (400): Number of pixels are which a long label should be split into
 * multiple lines. Works on all geometries, on lines it is mutually exclusive with the followLine option.
 * </li>
 * <li><strong>conflictResolution</strong> (true): Enables conflict resolution (default, true) meaning no
 * two labels will be allowed to overlap. Symbolizers with conflict resolution off are considered outside of
 * the conflict resolution game, they don’t reserve area and can overlap with other labels.
 * </li>
 * <li><strong>followLine</strong> (true): When true activates curved labels on linear geometries. The
 * label will follow the shape of the current line, as opposed to being drawn a tangent straight line
 * </li>
 * <li><strong>goodnessOfFit</strong> (90): Sets the percentage of the label that must sit inside the
 * geometry to allow drawing the label. Works only on polygons.
 * </li>
 * <li><strong>group</strong> (false): If true, geometries with the same labels are grouped and considered
 * a single entity to be labelled. This allows to avoid or control repeated labels.
 * </li>
 * <li><strong>maxDisplacement</strong> (400): The distance, in pixel, a label can be displaced from its
 * natural position in an attempt to find a position that does not conflict with already drawn labels.
 * </li>
 * <li><strong>spaceAround</strong> (50): The minimum distance between two labels, in pixels.</li>
 * </ul>
 * <p>Example</p>
 * <pre><code>
 * {
 *   "version" : "2",
 *   "*" : {
 *     "symbolizers" : [{
 *       "type" : "text",
 *       "fontColor":"#000000",
 *       "label": "[name]",
 *       "goodnessOfFit": 0.1,
 *       "spaceAround": 10
 *     }
 *   ]}
 * }
 * </code></pre>
 * <p>For more information, please refer to the
 * <a href="http://docs.geotools.org/latest/userguide/library/render/style.html#textsymbolizer">GeoTools
 * documentation</a>.
 * </p>
 *
 * <h2>ECQL references:</h2>
 * <ul>
 * <li><a href="http://docs.geoserver.org/stable/en/user/filter/ecql_reference.html#ecql-expr">
 * http://docs.geoserver.org/stable/en/user/filter/ecql_reference.html#ecql-expr</a></li>
 * <li><a href="http://udig.refractions.net/files/docs/latest/user/Constraint%20Query%20Language.html">
 * http://udig.refractions.net/files/docs/latest/user/Constraint%20Query%20Language.html</a></li>
 * <li><a
 * href="http://docs.geoserver.org/stable/en/user/filter/function_reference.html#filter-function-reference">
 * http://docs.geoserver.org/stable/en/user/filter/function_reference.html#filter-function-reference</a></li>
 * <li><a href="http://docs.geotools.org/stable/userguide/library/cql/ecql.html">
 * http://docs.geotools.org/stable/userguide/library/cql/ecql.html</a></li>
 * <li><a href="http://docs.geoserver.org/latest/en/user/tutorials/cql/cql_tutorial.html">
 * http://docs.geoserver.org/latest/en/user/tutorials/cql/cql_tutorial.html</a></li>
 * </ul>
 */
public final class MapfishStyleParserPlugin implements StyleParserPlugin {
    static final String JSON_VERSION = "version";
    private StyleBuilder sldStyleBuilder = new StyleBuilder();

    @Override
    public Optional<Style> parseStyle(
            @Nullable final Configuration configuration,
            @Nonnull final ClientHttpRequestFactory clientHttpRequestFactory,
            @Nonnull final String styleString) {
        final Optional<Style> styleOptional = tryParse(
                configuration, styleString, clientHttpRequestFactory);

        if (styleOptional.isPresent()) {
            return styleOptional;
        }
        return ParserPluginUtils.loadStyleAsURI(
                clientHttpRequestFactory, styleString, (final byte[] input) -> {
                    try {
                        return tryParse(
                                configuration, new String(input, Constants.DEFAULT_CHARSET),
                                clientHttpRequestFactory);
                    } catch (Throwable e) {
                        throw ExceptionUtils.getRuntimeException(e);
                    }
                });
    }

    private Optional<Style> tryParse(
            @Nullable final Configuration configuration,
            @Nonnull final String styleString,
            @Nonnull final ClientHttpRequestFactory clientHttpRequestFactory) {
        final String trimmed = styleString.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            final PJsonObject json = new PJsonObject(new JSONObject(styleString), "style");

            final String jsonVersion = json.optString(JSON_VERSION, "1");
            for (Versions version: Versions.values()) {
                if (version.versionNumber.equals(jsonVersion)) {
                    return Optional.of(version.parseStyle(
                            json, this.sldStyleBuilder, configuration, clientHttpRequestFactory));
                }
            }
        } else if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
            final SLDParserPlugin parser = new SLDParserPlugin();
            return parser.parseStyle(configuration, clientHttpRequestFactory, styleString);
        }
        return Optional.empty();
    }

    enum Versions {
        ONE("1") {
            @Override
            Style parseStyle(
                    @Nonnull final PJsonObject json,
                    @Nonnull final StyleBuilder styleBuilder,
                    @Nullable final Configuration configuration,
                    @Nonnull final ClientHttpRequestFactory requestFactory) {
                return new MapfishJsonStyleVersion1(
                        json, styleBuilder, configuration, requestFactory, DEFAULT_GEOM_ATT_NAME)
                        .parseStyle();
            }
        }, TWO("2") {
            @Override
            Style parseStyle(
                    @Nonnull final PJsonObject json,
                    @Nonnull final StyleBuilder styleBuilder,
                    @Nullable final Configuration configuration,
                    @Nonnull final ClientHttpRequestFactory requestFactory) {
                return new MapfishJsonStyleVersion2(json, styleBuilder, configuration, requestFactory)
                        .parseStyle();
            }
        };
        private final String versionNumber;

        Versions(final String versionNumber) {
            this.versionNumber = versionNumber;
        }

        abstract Style parseStyle(
                PJsonObject json, StyleBuilder styleBuilder, Configuration configuration,
                ClientHttpRequestFactory requestFactory);
    }
}
