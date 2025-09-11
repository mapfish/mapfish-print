package org.mapfish.print.map.geotools;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import jakarta.annotation.Nonnull;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.emf.ecore.resource.URIHandler;
import org.geotools.api.data.FeatureSource;
import org.geotools.data.collection.CollectionFeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.gml2.GMLConfiguration;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Parser;
import org.mapfish.print.FileUtils;
import org.mapfish.print.PrintException;
import org.mapfish.print.URIUtils;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.map.AbstractLayerParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

/** Parses GML from the request data. */
public final class GmlLayer extends AbstractFeatureSourceLayer {
  /**
   * Constructor.
   *
   * @param executorService the thread pool for doing the rendering.
   * @param featureSourceSupplier a function that creates the feature source. This will only be
   *     called once.
   * @param styleSupplier a function that creates the style for styling the features. This will only
   *     be called once.
   * @param renderAsSvg is the layer rendered as SVG?
   * @param params the parameters for this layer
   */
  public GmlLayer(
      final ExecutorService executorService,
      final FeatureSourceSupplier featureSourceSupplier,
      final StyleSupplier<FeatureSource<?, ?>> styleSupplier,
      final boolean renderAsSvg,
      final AbstractLayerParams params) {
    super(executorService, featureSourceSupplier, styleSupplier, renderAsSvg, params);
  }

  @Override
  public LayerContext prepareRender(
      final MapfishMapContext transformer,
      final MfClientHttpRequestFactory clientHttpRequestFactory) {
    return new LayerContext(DEFAULT_SCALING, null, null);
  }

  /**
   * Renders GML layers.
   *
   * <p>Type: <code>gml</code>
   */
  public static final class Plugin extends AbstractFeatureSourceLayerPlugin<GmlParam> {

    private static final String TYPE = "gml";

    private static final GMLConfiguration GML_2_PARSER = new GMLConfiguration();
    private static final org.geotools.gml3.GMLConfiguration GML_3_PARSER =
        new org.geotools.gml3.GMLConfiguration();
    private static final org.geotools.gml3.v3_2.GMLConfiguration GML_32_PARSER =
        new org.geotools.gml3.v3_2.GMLConfiguration(true);

    @Autowired private URIHandler cachingUrihandler;

    /** Constructor. */
    public Plugin() {
      super(TYPE);
    }

    @Override
    public GmlParam createParameter() {
      return new GmlParam();
    }

    @Nonnull
    @Override
    public GmlLayer parse(@Nonnull final Template template, @Nonnull final GmlParam param) {
      return new GmlLayer(
          this.forkJoinPool,
          createFeatureSourceSupplier(template, param.url),
          createStyleFunction(template, param.style),
          template.getConfiguration().renderAsSvg(param.renderAsSvg),
          param);
    }

    private FeatureSourceSupplier createFeatureSourceSupplier(
        final Template template, final String url) {
      return new FeatureSourceSupplier() {
        @Nonnull
        @Override
        public FeatureSource<?, ?> load(
            @Nonnull final MfClientHttpRequestFactory requestFactory,
            @Nonnull final MapfishMapContext mapContext) {
          SimpleFeatureCollection featureCollection;
          try {
            featureCollection = createFeatureSource(template, requestFactory, url);
          } catch (IOException e) {
            throw new PrintException("Failed to create feature source for " + url, e);
          }
          if (featureCollection == null) {
            throw new IllegalArgumentException(url + " does not reference a GML file");
          }
          return new CollectionFeatureSource(featureCollection);
        }
      };
    }

    private SimpleFeatureCollection createFeatureSource(
        final Template template,
        final MfClientHttpRequestFactory httpRequestFactory,
        final String gmlString)
        throws IOException {
      try {
        URL url = new URL(gmlString);
        FileUtils.testForLegalFileUrl(template.getConfiguration(), url);
        try {
          final String gmlData = URIUtils.toString(httpRequestFactory, url.toURI());
          final int endIndex = 200;
          String startOfData = gmlData.substring(0, endIndex);
          if (startOfData.contains("\"http://www.opengis.net/gml/3.2\"")) {
            return parseGml32(gmlData);
          } else {
            return parseGml3(gmlData);
          }
        } catch (URISyntaxException e) {
          throw new PrintException("Failed to create URI for " + url, e);
        }
      } catch (MalformedURLException e) {
        return null;
      }
    }

    private SimpleFeatureCollection parseGml3(final String gmlData) throws IOException {
      final SimpleFeatureCollection featureCollection =
          getSimpleFeatureCollection(gmlData, createParser(GML_3_PARSER));
      if (featureCollection != null) {
        return featureCollection;
      }
      return parseGml2(gmlData);
    }

    private SimpleFeatureCollection getSimpleFeatureCollection(
        final String gmlData, final Parser parser) throws IOException {
      parser.setStrict(false);
      parser.setRootElementType(new QName("http://www.opengis.net/wfs", "FeatureCollection"));
      try {
        final Object featureCollection = parser.parse(new StringReader(gmlData));
        if (featureCollection instanceof SimpleFeatureCollection) {
          return (SimpleFeatureCollection) featureCollection;
        }
      } catch (SAXException | ParserConfigurationException e) {
        // do nothing try to load as gml2
      }
      return null;
    }

    private SimpleFeatureCollection parseGml2(final String gmlData) throws IOException {
      final SimpleFeatureCollection featureCollection =
          getSimpleFeatureCollection(gmlData, createParser(GML_2_PARSER));
      if (featureCollection != null) {
        return featureCollection;
      }
      return parseGml32(gmlData);
    }

    private SimpleFeatureCollection parseGml32(final String gmlData) throws IOException {
      Parser gmlV32Parser = createParser(GML_32_PARSER);
      gmlV32Parser.setStrict(false);
      gmlV32Parser.setRootElementType(
          new QName("http://www.opengis.net/wfs/2.0", "FeatureCollection"));
      try {
        final Object featureCollection = gmlV32Parser.parse(new StringReader(gmlData));
        if (featureCollection instanceof SimpleFeatureCollection) {
          return (SimpleFeatureCollection) featureCollection;
        } else {
          throw new RuntimeException("unable to parse gml: \n\n" + gmlData);
        }

      } catch (SAXException | ParserConfigurationException e) {
        throw new PrintException("Failed to parse Gml32 " + gmlData, e);
      }
    }

    private Parser createParser(final Configuration configuration) {
      final Parser parser = new Parser(configuration);
      parser.getURIHandlers().add(0, this.cachingUrihandler);
      return parser;
    }
  }

  /** The parameters for creating a layer that renders Gml formatted data. */
  public static class GmlParam extends AbstractVectorLayerParam {
    /**
     * An url to the gml or the raw Gml data.
     *
     * <p>The url can be a file url, however if it is it must be relative to the configuration
     * directory.
     */
    public String url;
  }
}
