package org.mapfish.print.map.style;

import com.google.common.base.Optional;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.Closeables;
import com.vividsolutions.jts.util.Assert;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.mapfish.print.Constants;
import org.mapfish.print.config.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

/**
 * Basic implementation for loading and parsing an SLD style.
 *
 * @author Jesse on 4/8/2014.
 */
public abstract class AbstractSLDParserPlugin implements StyleParserPlugin {

    /**
     * The separator between the path or url segment for loading the sld and an index of the style to obtain.
     *
     * SLDs can contains multiple styles.  Because of this there needs to be a way to indicate which style
     * is referred to.  That is the purpose of the style index.
     */
    public static final String STYLE_INDEX_REF_SEPARATOR = "##";

    @Override
    public final Optional<Style> parseStyle(final Configuration configuration, final String styleString) throws Throwable {
        Integer styleIndex = lookupStyleIndex(styleString).orNull();
        String styleStringWithoutIndexReference = removeIndexReference(styleString);
        List<ByteSource> inputStream = getInputStreamSuppliers(configuration, styleStringWithoutIndexReference);
        for (ByteSource source : inputStream) {
            Optional<Style> style = tryLoadSLD(source, styleIndex);
            if (style.isPresent()) {
                return style;
            }
        }
        return Optional.absent();
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

    /**
     * Return a ByteSource for each known interpretation of the style string.
     *
     * @param configuration the configuration object being used.
     * @param styleString   the string (url, file, etc...) representing the style or the location to load the style from.
     */
    protected abstract List<ByteSource> getInputStreamSuppliers(Configuration configuration, String styleString);

    private Optional<Style> tryLoadSLD(final ByteSource byteSource, final Integer styleIndex) throws IOException {
        Assert.isTrue(styleIndex == null || styleIndex > -1, "styleIndex must be > -1 but was: " + styleIndex);

        final CharSource charSource = byteSource.asCharSource(Constants.DEFAULT_CHARSET);
        BufferedReader reader = null;
        final Style[] styles;
        try {
            reader = charSource.openBufferedStream();

            final SLDParser sldParser = new SLDParser(CommonFactoryFinder.getStyleFactory());
            sldParser.setInput(reader);
            styles = sldParser.readXML();

        } catch (Throwable e) {
            return Optional.absent();
        } finally {
            Closeables.close(reader, true);
        }

        if (styleIndex != null) {
            Assert.isTrue(styleIndex < styles.length, "There where " + styles.length + " styles in file but requested index was: " +
                                                      (styleIndex + 1));
        } else {
            Assert.isTrue(styles.length < 2, "There are " + styles.length + " therefore the styleRef must contain an index " +
                                             "identifying" +

                                             " the style.  The index starts at 1 for the first style.\n\tExample: thinline.sld##1");
        }

        if (styleIndex == null) {
            return Optional.of(styles[0]);
        } else {
            return Optional.of(styles[styleIndex]);
        }
    }

}
