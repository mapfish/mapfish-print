package org.mapfish.print.map.style;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.Closeables;
import com.vividsolutions.jts.util.Assert;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.mapfish.print.Constants;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedReader;
import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Basic implementation for loading and parsing an SLD style.
 *
 * @author Jesse on 4/8/2014.
 */
public class SLDParserPlugin implements StyleParserPlugin {
    /**
     * The separator between the path or url segment for loading the sld and an index of the style to obtain.
     *
     * SLDs can contains multiple styles.  Because of this there needs to be a way to indicate which style
     * is referred to.  That is the purpose of the style index.
     */
    public static final String STYLE_INDEX_REF_SEPARATOR = "##";


    @Override
    public final Optional<Style> parseStyle(@Nullable final Configuration configuration,
                                            @Nonnull final ClientHttpRequestFactory clientHttpRequestFactory,
                                            @Nonnull final String styleString,
                                            @Nonnull final MapfishMapContext mapContext) throws Throwable {

        // try to load xml
        final ByteSource straightByteSource = ByteSource.wrap(styleString.getBytes(Constants.DEFAULT_CHARSET));
        final Optional<Style> styleOptional = tryLoadSLD(straightByteSource, null);

        if (styleOptional.isPresent()) {
            return styleOptional;
        }

        final Integer styleIndex = lookupStyleIndex(styleString).orNull();
        final String styleStringWithoutIndexReference = removeIndexReference(styleString);
        Function<byte[], Optional<Style>> loadFunction = new Function<byte[], Optional<Style>>() {
            @Override
            public Optional<Style> apply(final byte[] input) {
                final ByteSource bytes = ByteSource.wrap(input);
                try {
                    return tryLoadSLD(bytes, styleIndex);
                } catch (IOException e) {
                    throw ExceptionUtils.getRuntimeException(e);
                }
            }
        };

        return ParserPluginUtils.loadStyleAsURI(clientHttpRequestFactory, styleStringWithoutIndexReference, loadFunction);
    }

    private String removeIndexReference(final String styleString) {
        int styleIdentifier = styleString.lastIndexOf(STYLE_INDEX_REF_SEPARATOR);
        if (styleIdentifier > 0) {
            return styleString.substring(0, styleIdentifier);
        }
        return styleString;
    }

    private Optional<Integer> lookupStyleIndex(final String ref) {
        int styleIdentifier = ref.lastIndexOf(STYLE_INDEX_REF_SEPARATOR);
        if (styleIdentifier > 0) {
            return Optional.of(Integer.parseInt(ref.substring(styleIdentifier + 2)) - 1);
        }
        return Optional.absent();
    }

    private Optional<Style> tryLoadSLD(final ByteSource byteSource, final Integer styleIndex) throws IOException {
        Assert.isTrue(styleIndex == null || styleIndex > -1, "styleIndex must be > -1 but was: " + styleIndex);

        final CharSource charSource = byteSource.asCharSource(Constants.DEFAULT_CHARSET);
        BufferedReader readerXML = null;
        BufferedReader readerSLD = null;
        final Style[] styles;
        try {
            readerXML = charSource.openBufferedStream();

            // check if the XML is valid
            // this is only done in a separate step to avoid that fatal errors show up in the logs
            // by setting a custom error handler.
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setErrorHandler(new ErrorHandler());
            db.parse(new InputSource(readerXML));

            // then read the styles
            readerSLD = charSource.openBufferedStream();
            final SLDParser sldParser = new SLDParser(CommonFactoryFinder.getStyleFactory());
            sldParser.setInput(readerSLD);
            styles = sldParser.readXML();

        } catch (Throwable e) {
            return Optional.absent();
        } finally {
            Closeables.close(readerXML, true);
            Closeables.close(readerSLD, true);
        }

        if (styleIndex != null) {
            Assert.isTrue(styleIndex < styles.length, "There where " + styles.length + " styles in file but requested index was: " +
                                                      (styleIndex + 1));
        } else {
            Assert.isTrue(styles.length < 2, "There are " + styles.length + " therefore the styleRef must contain an index " +
                                             "identifying the style.  The index starts at 1 for the first style." +
                                             "\n\tExample: thinline.sld##1");
        }

        if (styleIndex == null) {
            return Optional.of(styles[0]);
        } else {
            return Optional.of(styles[styleIndex]);
        }
    }

    /**
     * A default error handler to avoid that error messages like "[Fatal Error] :1:1: Content is not allowed in prolog."
     * are directly printed to STDERR.
     */
    public static class ErrorHandler extends DefaultHandler {

        private static final Logger LOGGER = LoggerFactory.getLogger(StyleParser.class);

        /**
         * @param e Exception
         */
        public final void error(final SAXParseException e) throws SAXException {
            LOGGER.debug(e.getLocalizedMessage());
            super.error(e);
        }

        /**
         * @param e Exception
         */
        public final void fatalError(final SAXParseException e) throws SAXException {
            LOGGER.debug(e.getLocalizedMessage());
            super.fatalError(e);
        }

        /**
         * @param e Exception
         */
        public final void warning(final SAXParseException e) throws SAXException {
            //ignore
        }
    }
}
