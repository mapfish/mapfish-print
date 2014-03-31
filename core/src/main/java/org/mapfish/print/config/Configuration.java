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

package org.mapfish.print.config;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.map.style.StyleParser;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.annotation.Nonnull;

/**
 * The Main Configuration Bean.
 * <p/>
 * @author jesseeichar on 2/20/14.
 */
public class Configuration {
    private static final Map<String, String> GEOMETRY_NAME_ALIASES;
    static {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(Geometry.class.getSimpleName().toLowerCase(), Geometry.class.getSimpleName().toLowerCase());
        map.put("geom", Geometry.class.getSimpleName().toLowerCase());
        map.put("geometrycollection", Geometry.class.getSimpleName().toLowerCase());
        map.put("multigeometry", Geometry.class.getSimpleName().toLowerCase());

        map.put("line", LineString.class.getSimpleName().toLowerCase());
        map.put(LineString.class.getSimpleName().toLowerCase(), LineString.class.getSimpleName().toLowerCase());
        map.put("linearring", LineString.class.getSimpleName().toLowerCase());
        map.put("multilinestring", LineString.class.getSimpleName().toLowerCase());
        map.put("multiline", LineString.class.getSimpleName().toLowerCase());

        map.put("poly", Polygon.class.getSimpleName().toLowerCase());
        map.put(Polygon.class.getSimpleName().toLowerCase(), Polygon.class.getSimpleName().toLowerCase());
        map.put("multipolygon", Polygon.class.getSimpleName().toLowerCase());

        map.put(Point.class.getSimpleName().toLowerCase(), Point.class.getSimpleName().toLowerCase());
        map.put("multipoint", Point.class.getSimpleName().toLowerCase());
        GEOMETRY_NAME_ALIASES = map;
    }
    private boolean reloadConfig;
    private String proxyBaseUrl;
    private TreeSet<String> headers;
    private List<HostMatcher> hosts = new ArrayList<HostMatcher>();
    private List<SecurityStrategy> security = Collections.emptyList();
    private Map<String, Template> templates;
    private File configurationFile;
    private Map<String, Style> styles = new HashMap<String, Style>();
    private Map<String, Style> defaultStyle = new HashMap<String, Style>();

    @Autowired
    private StyleParser styleParser;

    /**
     * Print out the configuration that the client needs to make a request.
     *
     * @param json the output writer.
     *
     * @throws JSONException
     */
    public final void printClientConfig(final JSONWriter json) throws JSONException {
        json.key("layouts");
        json.array();
        for (String name : this.templates.keySet()) {
            json.object();
            json.key("name").value(name);
            this.templates.get(name).printClientConfig(json);
            json.endObject();
        }
        json.endArray();
    }

    public final boolean isReloadConfig() {
        return this.reloadConfig;
    }

    public final void setReloadConfig(final boolean reloadConfig) {
        this.reloadConfig = reloadConfig;
    }

    public final String getProxyBaseUrl() {
        return this.proxyBaseUrl;
    }

    public final void setProxyBaseUrl(final String proxyBaseUrl) {
        this.proxyBaseUrl = proxyBaseUrl;
    }

    public final TreeSet<String> getHeaders() {
        return this.headers;
    }

    public final void setHeaders(final TreeSet<String> headers) {
        this.headers = headers;
    }

    /**
     * Calculate the name of the pdf file to return to the user.
     *
     * @param layoutName the name of file from the configuration.
     */
    public final String getOutputFilename(final String layoutName) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public final List<HostMatcher> getHosts() {
        return this.hosts;
    }

    public final void setHosts(final List<HostMatcher> hosts) {
        this.hosts = hosts;
    }

    public final List<SecurityStrategy> getSecurity() {
        return this.security;
    }

    public final void setSecurity(final List<SecurityStrategy> security) {
        this.security = security;
    }

    public final Map<String, Template> getTemplates() {
        return this.templates;
    }

    /**
     * Retrieve the configuration of the named template.
     * @param name the template name;
     */
    public final Template getTemplate(final String name) {
        return this.templates.get(name);
    }

    public final void setTemplates(final Map<String, Template> templates) {
        this.templates = templates;
    }

    public final File getDirectory() {
        return this.configurationFile.getParentFile();
    }

    public final void setConfigurationFile(final File configurationFile) {
        this.configurationFile = configurationFile;
    }

    /**
     * Set the named styles defined in the configuration for this.
     *
     * @param styles the style definition.  StyleParser plugins will be used to load the style.
     */
    public final void setStyles(final Map<String, String> styles) {
        Map<String, Style> map = StyleParser.loadStyles(this, this.styleParser, styles);

        this.styles = map;
    }

    /**
     * Return the named style ot Optional.absent() if there is not a style with the given name.
     *
     * @param styleName the name of the style to look up
     */
    public final Optional<? extends Style> getStyle(final String styleName) {
        return Optional.fromNullable(this.styles.get(styleName));

    }

    /**
     * Get a default style.  If null a simple black line style will be returned.
     *
     * @param geometryType the name of the geometry type (point, line, polygon)
     */
     @Nonnull
     public final Style getDefaultStyle(@Nonnull final String geometryType) {
        String normalizedGeomName = GEOMETRY_NAME_ALIASES.get(geometryType.toLowerCase());
        if (normalizedGeomName == null) {
            normalizedGeomName = geometryType.toLowerCase();
        }
        Style style = this.defaultStyle.get(normalizedGeomName.toLowerCase());
        if (style == null) {
            StyleBuilder builder = new StyleBuilder();
            final Symbolizer symbolizer;
            if (normalizedGeomName.equalsIgnoreCase(Point.class.getSimpleName())) {
                symbolizer = builder.createPointSymbolizer();
            } else if (normalizedGeomName.equalsIgnoreCase(LineString.class.getSimpleName())) {
                symbolizer = builder.createLineSymbolizer(Color.black, 2);
            } else if (normalizedGeomName.equalsIgnoreCase(Polygon.class.getSimpleName())) {
                symbolizer = builder.createPolygonSymbolizer(Color.lightGray, Color.black, 2);
            } else {
                final Style geomStyle = this.defaultStyle.get(Geometry.class.getSimpleName().toLowerCase());
                if (geomStyle != null) {
                    return geomStyle;
                } else {
                    symbolizer = builder.createPointSymbolizer();
                }
            }
            style =  builder.createStyle(symbolizer);
        }
        return style;
    }

    /**
     * Set the default styles.  the case of the keys are not important.  The retrieval will be case insensitive.
     *
     * @param defaultStyle the mapping from geometry type name (point, polygon, etc...) to the style to use for that type.
     */
    public final void setDefaultStyle(final Map<String, Style> defaultStyle) {
        this.defaultStyle = Maps.newHashMapWithExpectedSize(defaultStyle.size());
        for (Map.Entry<String, Style> entry : defaultStyle.entrySet()) {
            String normalizedName = GEOMETRY_NAME_ALIASES.get(entry.getKey().toLowerCase());

            if (normalizedName == null) {
                normalizedName = entry.getKey().toLowerCase();
            }

            this.defaultStyle.put(normalizedName, entry.getValue());
        }
    }
}
