package org.mapfish.print.config;

import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.json.JSONException;
import org.json.JSONWriter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.mapfish.print.Constants;
import org.mapfish.print.config.access.AccessAssertion;
import org.mapfish.print.config.access.AlwaysAllowAssertion;
import org.mapfish.print.config.access.RoleAccessAssertion;
import org.mapfish.print.http.CertificateStore;
import org.mapfish.print.http.HttpCredential;
import org.mapfish.print.http.HttpProxy;
import org.mapfish.print.map.style.StyleParser;
import org.mapfish.print.map.style.json.ColorParser;
import org.mapfish.print.processor.http.matcher.URIMatcher;
import org.mapfish.print.processor.http.matcher.UriMatchers;
import org.mapfish.print.servlet.fileloader.ConfigFileLoaderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;


/**
 * The Main Configuration Bean.
 *
 */
public class Configuration implements ConfigurationObject {
    private static final Map<String, String> GEOMETRY_NAME_ALIASES;
    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    static {
        HashMap<String, String> map = new HashMap<>();
        map.put(Geometry.class.getSimpleName().toLowerCase(), Geometry.class.getSimpleName().toLowerCase());
        map.put("geom", Geometry.class.getSimpleName().toLowerCase());
        map.put("geometrycollection", Geometry.class.getSimpleName().toLowerCase());
        map.put("multigeometry", Geometry.class.getSimpleName().toLowerCase());

        map.put("line", LineString.class.getSimpleName().toLowerCase());
        map.put(LineString.class.getSimpleName().toLowerCase(),
                LineString.class.getSimpleName().toLowerCase());
        map.put("linearring", LineString.class.getSimpleName().toLowerCase());
        map.put("multilinestring", LineString.class.getSimpleName().toLowerCase());
        map.put("multiline", LineString.class.getSimpleName().toLowerCase());

        map.put("poly", Polygon.class.getSimpleName().toLowerCase());
        map.put(Polygon.class.getSimpleName().toLowerCase(), Polygon.class.getSimpleName().toLowerCase());
        map.put("multipolygon", Polygon.class.getSimpleName().toLowerCase());

        map.put(Point.class.getSimpleName().toLowerCase(), Point.class.getSimpleName().toLowerCase());
        map.put("multipoint", Point.class.getSimpleName().toLowerCase());

        map.put(Constants.Style.OverviewMap.NAME, Constants.Style.OverviewMap.NAME);
        GEOMETRY_NAME_ALIASES = map;
    }

    private Map<String, Template> templates;
    private File configurationFile;
    private Map<String, String> styles = new HashMap<>();
    private Map<String, Style> defaultStyle = new HashMap<>();
    private boolean throwErrorOnExtraParameters = true;
    private List<HttpProxy> proxies = new ArrayList<>();
    private PDFConfig pdfConfig = new PDFConfig();
    private List<HttpCredential> credentials = new ArrayList<>();
    private CertificateStore certificateStore;
    private String outputFilename;
    private String resourceBundle;
    private boolean defaultToSvg = false;
    private Set<String> jdbcDrivers = new HashSet<>();
    private Map<String, Style> namedStyles = new HashMap<>();
    private UriMatchers allowedReferers = null;
    private SmtpConfig smtp = null;

    /**
     * The color used to draw the WMS tiles error default: transparent pink.
     */
    private String transparentTileErrorColor = "rgba(255, 78, 78, 125)";
    /**
     * The color used to draw the other tiles error default: pink.
     */
    private String opaqueTileErrorColor = "rgba(255, 155, 155, 0)";

    @Autowired
    private StyleParser styleParser;
    @Autowired
    private ClientHttpRequestFactory clientHttpRequestFactory;
    @Autowired
    private ConfigFileLoaderManager fileLoaderManager;
    @Autowired
    private ApplicationContext context;

    private AccessAssertion accessAssertion = AlwaysAllowAssertion.INSTANCE;

    final PDFConfig getPdfConfig() {
        return this.pdfConfig;
    }

    /**
     * Configure various properties related to the reports generated as PDFs.
     *
     * @param pdfConfig the pdf configuration
     */
    public final void setPdfConfig(final PDFConfig pdfConfig) {
        this.pdfConfig = pdfConfig;
    }

    /**
     * Initialize some optionally wired fields.
     */
    @PostConstruct
    public final void init() {
        this.namedStyles = this.context.getBeansOfType(Style.class);
    }

    /**
     * Either use the provided value (renderAsSvg) or if it is null then use {@link #defaultToSvg}.
     *
     * @param renderAsSvg the value to use if non-null.
     */
    public final boolean renderAsSvg(final Boolean renderAsSvg) {
        return renderAsSvg == null ? this.defaultToSvg : renderAsSvg;
    }

    /**
     * If true then all vector layers (and other parts of the system that can be either SVG or Bitmap, like
     * scalebar) will be rendered as SVG (unless layer specifically indicates useSvg as false).
     * <p>
     * The default is false.
     * </p>
     *
     * @param defaultToSvg whether or not to create svg layers by default
     */
    public final void setDefaultToSvg(final boolean defaultToSvg) {
        this.defaultToSvg = defaultToSvg;
    }

    /**
     * The configuration for locating a custom certificate store.
     */
    @Nullable
    public final CertificateStore getCertificateStore() {
        return this.certificateStore;
    }

    /**
     * The configuration for locating a custom certificate store.  This is only required if the default
     * certificate store which ships with all java installations does not contain the certificates needed by
     * this server.  Usually it is to accept a self-signed certificate, for example on a test server.
     *
     * @param certificateStore The configuration for locating a custom certificate store
     */
    public final void setCertificateStore(final CertificateStore certificateStore) {
        this.certificateStore = certificateStore;
    }

    /**
     * Get the http credentials.  Should also getProxies since {@link org.mapfish.print.http.HttpProxy} is a
     * subclass of {@link org.mapfish.print.http.HttpCredential}.
     */
    public final List<HttpCredential> getCredentials() {
        return this.credentials;
    }

    /**
     * Http credentials to be used when making http requests.
     * <p>
     * If a proxy needs credentials you don't need to configure it here because the proxy configuration object
     * also has options for declaring the credentials.
     * </p>
     *
     * @param credentials the credentials
     */
    public final void setCredentials(final List<HttpCredential> credentials) {
        this.credentials = credentials;
    }

    /**
     * Get the http proxies used by in all requests in this syste.
     *
     * @see org.mapfish.print.http.ConfigFileResolvingHttpRequestFactory
     */
    public final List<HttpProxy> getProxies() {
        return this.proxies;
    }

    /**
     * Configuration for proxying http requests.  Each proxy can be configured with authentication and with
     * the uris that they apply to.
     *
     * See {@link org.mapfish.print.http.HttpProxy} for details on how to configure them.
     *
     * @param proxies the proxy configuration objects
     */
    public final void setProxies(final List<HttpProxy> proxies) {
        this.proxies = proxies;
    }

    /**
     * Print out the configuration that the client needs to make a request.
     *
     * @param json the output writer.
     * @throws JSONException
     */
    public final void printClientConfig(final JSONWriter json) throws JSONException {
        json.key("layouts");
        json.array();
        final Map<String, Template> accessibleTemplates = getTemplates();
        accessibleTemplates.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(entry -> {
            json.object();
            json.key("name").value(entry.getKey());
            entry.getValue().printClientConfig(json);
            json.endObject();
                });
        json.endArray();
        json.key("smtp").object();
        json.key("enabled").value(smtp != null);
        if (smtp != null) {
            json.key("storage").object();
            json.key("enabled").value(smtp.getStorage() != null);
            json.endObject();
        }
        json.endObject();
    }

    public final String getOutputFilename() {
        return this.outputFilename;
    }

    /**
     * The default output file name of the report.  This can be overridden by {@link
     * org.mapfish.print.config.Template#setOutputFilename(String)} and the outputFilename parameter in the
     * request JSON.
     * <p>
     * This can be a string and can also have a date section in the string that will be filled when the report
     * is created for example a section with ${&lt;dateFormatString&gt;} will be replaced with the current
     * date formatted in the way defined by the &lt;dateFormatString&gt; string.  The format rules are the
     * rules in
     * <a href="http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html">
     * java.text.SimpleDateFormat</a> (do a google search if the link above is broken).
     * </p>
     * <p>
     * Example: <code>outputFilename: print-${dd-MM-yyyy}</code> should output:
     * <code>print-22-11-2014.pdf</code>
     * </p>
     * <p>
     * Note: the suffix will be appended to the end of the name.
     * </p>
     *
     * @param outputFilename default output file name of the report.
     */
    public final void setOutputFilename(final String outputFilename) {
        this.outputFilename = outputFilename;
    }

    public final Map<String, Template> getTemplates() {
        return this.templates.entrySet().stream().filter(input -> {
            try {
                accessAssertion.assertAccess("Configuration", this);
                input.getValue().assertAccessible(input.getKey());
                return true;
            } catch (AccessDeniedException | AuthenticationCredentialsNotFoundException e) {
                return false;
            }
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Set the configuration of the named template.
     *
     * @param templates the templates;
     */
    public final void setTemplates(final Map<String, Template> templates) {
        this.templates = templates;
    }

    /**
     * Retrieve the configuration of the named template.
     *
     * @param name the template name;
     */
    public final Template getTemplate(final String name) {
        final Template template = this.templates.get(name);
        if (template != null) {
            this.accessAssertion.assertAccess("Configuration", this);
            template.assertAccessible(name);
        } else {
            LOGGER.warn("Template '%s' does not exist.  Options are: %s", name, this.templates.keySet());
        }
        return template;
    }

    public final File getDirectory() {
        return this.configurationFile.getAbsoluteFile().getParentFile();
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
        this.styles = styles;
    }

    /**
     * Return the named style ot Optional.absent() if there is not a style with the given name.
     *
     * @param styleName the name of the style to look up
     */
    public final Optional<? extends Style> getStyle(final String styleName) {
        final String styleRef = this.styles.get(styleName);
        if (styleRef != null) {
            return this.styleParser.loadStyle(this, this.clientHttpRequestFactory, styleRef);
        } else {
            return Optional.empty();
        }


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
            style = this.namedStyles.get(normalizedGeomName.toLowerCase());
        }

        if (style == null) {
            StyleBuilder builder = new StyleBuilder();
            final Symbolizer symbolizer;
            if (isPointType(normalizedGeomName)) {
                symbolizer = builder.createPointSymbolizer();
            } else if (isLineType(normalizedGeomName)) {
                symbolizer = builder.createLineSymbolizer(Color.black, 2);
            } else if (isPolygonType(normalizedGeomName)) {
                symbolizer = builder.createPolygonSymbolizer(Color.lightGray, Color.black, 2);
            } else if (normalizedGeomName.equalsIgnoreCase(Constants.Style.Raster.NAME)) {
                symbolizer = builder.createRasterSymbolizer();
            } else if (normalizedGeomName.startsWith(Constants.Style.OverviewMap.NAME)) {
                symbolizer = createMapOverviewStyle(normalizedGeomName, builder);
            } else {
                final Style geomStyle = this.defaultStyle.get(Geometry.class.getSimpleName().toLowerCase());
                if (geomStyle != null) {
                    return geomStyle;
                } else {
                    symbolizer = builder.createPointSymbolizer();
                }
            }
            style = builder.createStyle(symbolizer);
        }
        return style;
    }

    private boolean isPolygonType(@Nonnull final String normalizedGeomName) {
        return normalizedGeomName.equalsIgnoreCase(Polygon.class.getSimpleName())
                || normalizedGeomName.equalsIgnoreCase(MultiPolygon.class.getSimpleName());
    }

    private boolean isLineType(@Nonnull final String normalizedGeomName) {
        return normalizedGeomName.equalsIgnoreCase(LineString.class.getSimpleName())
                || normalizedGeomName.equalsIgnoreCase(MultiLineString.class.getSimpleName())
                || normalizedGeomName.equalsIgnoreCase(LinearRing.class.getSimpleName());
    }

    private boolean isPointType(@Nonnull final String normalizedGeomName) {
        return normalizedGeomName.equalsIgnoreCase(Point.class.getSimpleName())
                || normalizedGeomName.equalsIgnoreCase(MultiPoint.class.getSimpleName());
    }

    private Symbolizer createMapOverviewStyle(
            @Nonnull final String normalizedGeomName,
            @Nonnull final StyleBuilder builder) {
        Stroke stroke = builder.createStroke(Color.blue, 2);
        final Fill fill = builder.createFill(Color.blue, 0.2);
        String overviewGeomType = Polygon.class.getSimpleName();

        if (normalizedGeomName.contains(":")) {
            final String[] parts = normalizedGeomName.split(":");
            overviewGeomType = parts[1];
        }

        if (isPointType(overviewGeomType)) {
            final Mark mark = builder.createMark(StyleBuilder.MARK_CIRCLE, fill, stroke);
            Graphic graphic = builder.createGraphic(null, mark, null);
            graphic.setSize(builder.literalExpression(Constants.Style.POINT_SIZE));
            return builder.createPointSymbolizer(graphic);
        }
        if (isLineType(overviewGeomType)) {
            return builder.createLineSymbolizer(stroke);
        }
        return builder.createPolygonSymbolizer(stroke, fill);
    }

    /**
     * Set the default styles.  the case of the keys are not important.  The retrieval will be case
     * insensitive.
     *
     * @param defaultStyle the mapping from geometry type name (point, polygon, etc...) to the style
     *         to use for that type.
     */
    public final void setDefaultStyle(final Map<String, Style> defaultStyle) {
        this.defaultStyle = new HashMap<>(defaultStyle.size());
        for (Map.Entry<String, Style> entry: defaultStyle.entrySet()) {
            String normalizedName = GEOMETRY_NAME_ALIASES.get(entry.getKey().toLowerCase());

            if (normalizedName == null) {
                normalizedName = entry.getKey().toLowerCase();
            }

            this.defaultStyle.put(normalizedName, entry.getValue());
        }
    }

    /**
     * If true and the request JSON has extra parameters in the layers definition, exceptions will be thrown.
     * Otherwise the information will be logged.
     */
    public final boolean isThrowErrorOnExtraParameters() {
        return this.throwErrorOnExtraParameters;
    }

    /**
     * If true and the request JSON has extra parameters in the layers definition, exceptions will be thrown.
     * Otherwise the information will be logged.
     *
     * @param throwErrorOnExtraParameters the value
     */
    public final void setThrowErrorOnExtraParameters(final boolean throwErrorOnExtraParameters) {
        this.throwErrorOnExtraParameters = throwErrorOnExtraParameters;
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        validationErrors.addAll(validate());
    }

    /**
     * Validate that the configuration is valid.
     *
     * @return any validation errors.
     */
    public final List<Throwable> validate() {
        List<Throwable> validationErrors = new ArrayList<>();
        this.accessAssertion.validate(validationErrors, this);

        for (String jdbcDriver: this.jdbcDrivers) {
            try {
                Class.forName(jdbcDriver);
            } catch (ClassNotFoundException e) {
                try {
                    Configuration.class.getClassLoader().loadClass(jdbcDriver);
                } catch (ClassNotFoundException e1) {
                    validationErrors.add(new ConfigurationException(String.format(
                            "Unable to load JDBC driver: %s ensure that the web application has the jar " +
                                    "on its classpath", jdbcDriver)));
                }
            }
        }

        if (this.configurationFile == null) {
            validationErrors.add(new ConfigurationException("Configuration file is field on configuration " +
                                                                    "object is null"));
        }
        if (this.templates.isEmpty()) {
            validationErrors.add(new ConfigurationException("There are not templates defined."));
        }
        for (Template template: this.templates.values()) {
            template.validate(validationErrors, this);
        }

        for (HttpProxy proxy: this.proxies) {
            proxy.validate(validationErrors, this);
        }

        try {
            ColorParser.toColor(this.opaqueTileErrorColor);
        } catch (RuntimeException ex) {
            validationErrors.add(new ConfigurationException("Cannot parse opaqueTileErrorColor", ex));
        }

        try {
            ColorParser.toColor(this.transparentTileErrorColor);
        } catch (RuntimeException ex) {
            validationErrors.add(new ConfigurationException("Cannot parse transparentTileErrorColor", ex));
        }

        if (smtp != null) {
            smtp.validate(validationErrors, this);
        }

        return validationErrors;
    }

    /**
     * check if the file exists and can be accessed by the user/template/config/etc...
     *
     * @param pathToSubResource a string representing a file that is accessible for use in printing
     *         templates within the configuration file.  In the case of a file based URI the path could be a
     *         relative path (relative to the configuration file) or an absolute path, but it must be an
     *         allowed file (you can't allow access to any file on the file system).
     */
    public final boolean isAccessible(final String pathToSubResource) throws IOException {
        return this.fileLoaderManager.isAccessible(this.configurationFile.toURI(), pathToSubResource);
    }

    /**
     * Load the file related to the configuration file.
     *
     * @param pathToSubResource a string representing a file that is accessible for use in printing
     *         templates within the configuration file.  In the case of a file based URI the path could be a
     *         relative path (relative to the configuration file) or an absolute path, but it must be an
     *         allowed file (you can't allow access to any file on the file system).
     */
    public final byte[] loadFile(final String pathToSubResource) throws IOException {
        return this.fileLoaderManager.loadFile(this.configurationFile.toURI(), pathToSubResource);
    }

    /**
     * Set file loader manager.
     *
     * @param fileLoaderManager new manager.
     */
    public final void setFileLoaderManager(final ConfigFileLoaderManager fileLoaderManager) {
        this.fileLoaderManager = fileLoaderManager;
    }

    /**
     * Set the JDBC drivers that are required to connect to the databases in the configuration.  JDBC drivers
     * are needed (for example) when database sources are used in templates.  For example if in one of the
     * template you have:
     *
     * <pre><code>
     *     jdbcUrl: "jdbc:postgresql://localhost:5432/morges_dpfe"
     * </code></pre>
     * <p>
     * then you need to add:
     *
     * <pre><code>
     *     jdbcDrivers: [org.postgresql.Driver]
     * </code>
     * </pre>
     * <p>
     * or
     *
     * <pre><code>
     *     jdbcDrivers:
     *       - org.postgresql.Driver
     * </code></pre>
     *
     * @param jdbcDrivers the set of JDBC drivers to load before performing a print (this ensures they
     *         are registered with the JVM)
     */
    public final void setJdbcDrivers(final Set<String> jdbcDrivers) {
        this.jdbcDrivers = jdbcDrivers;
    }

    /**
     * The roles required to access this configuration/app.  If empty or not set then it is a
     * <em>public</em> app.  If there are many roles then a user must have one of the roles in order to
     * access the configuration/app.
     *
     * The security (how authentication/authorization is done) is configured in the
     * /WEB-INF/classes/mapfish-spring-security.xml
     * <p>
     * Any user without the required role will get an error when trying to access any of the templates and no
     * templates will be listed in the capabilities requests.
     * </p>
     *
     * @param access the roles needed to access this
     */
    public final void setAccess(final List<String> access) {
        final RoleAccessAssertion assertion = new RoleAccessAssertion();
        assertion.setRequiredRoles(access);
        this.accessAssertion = assertion;
    }

    public final AccessAssertion getAccessAssertion() {
        return this.accessAssertion;
    }

    /**
     * Get the color used to draw the WMS tiles error default: transparent pink.
     */
    public final String getTransparentTileErrorColor() {
        return this.transparentTileErrorColor;
    }

    /**
     * Color used for tiles in error on transparent layers.
     *
     * @param transparentTileErrorColor The color
     */
    public final void setTransparentTileErrorColor(final String transparentTileErrorColor) {
        this.transparentTileErrorColor = transparentTileErrorColor;
    }

    /**
     * Get the color used to draw the other tiles error default: pink.
     */
    public final String getOpaqueTileErrorColor() {
        return this.opaqueTileErrorColor;
    }

    /**
     * Color used for tiles in error on opaque layers.
     *
     * @param opaqueTileErrorColor The color
     */
    public final void setOpaqueTileErrorColor(final String opaqueTileErrorColor) {
        this.opaqueTileErrorColor = opaqueTileErrorColor;
    }

    /**
     * Get the resource bundle name.
     */
    public final String getResourceBundle() {
        return this.resourceBundle;
    }

    /**
     * Set the resource bundle name.
     *
     * @param resourceBundle the resource bundle name
     */
    public final void setResourceBundle(final String resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    /**
     * @return the list of referer checks (null = no check)
     */
    public final UriMatchers getAllowedReferersImpl() {
        return this.allowedReferers;
    }

    /**
     * The matchers used to authorize the incoming requests in function of the referer. For example:
     * <pre><code>
     * allowedReferers:
     *   - !hostnameMatch
     *     host: example.com
     *     allowSubDomains: true
     * </code></pre>
     * <p>
     * By default, the referer is not checked
     *
     * @param matchers the list of matcher to use to check if a referer is permitted or null for no
     *         check
     * @see org.mapfish.print.processor.http.matcher.URIMatcher
     */
    public final void setAllowedReferers(@Nullable final List<? extends URIMatcher> matchers) {
        this.allowedReferers = matchers != null ? new UriMatchers(matchers) : null;
    }

    public SmtpConfig getSmtp() {
        return smtp;
    }

    public void setSmtp(final SmtpConfig smtp) {
        this.smtp = smtp;
    }
}
