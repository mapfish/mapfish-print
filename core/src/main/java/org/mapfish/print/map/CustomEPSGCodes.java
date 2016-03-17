package org.mapfish.print.map;

import org.geotools.factory.Hints;
import org.geotools.referencing.factory.epsg.FactoryUsingWKT;
import org.mapfish.print.ExceptionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


/**
 * This class is an authority factory that allows defining custom EPSG codes.
 */
public final class CustomEPSGCodes extends FactoryUsingWKT {
    /**
     * The path to the file containing the custom epsg codes.
     */
    public static final String CUSTOM_EPSG_CODES_FILE = "/epsg.properties";

    /**
     * Constructor.
     */
    public CustomEPSGCodes() {
        this(null);
    }

    /**
     * Constructor.
     *
     * @param hints hints to pass to the framework on construction
     */
    public CustomEPSGCodes(final Hints hints) {
        super(hints);
    }

    /**
     * Returns the URL to the property file that contains CRS definitions.     *
     * @return The URL to the epsg file containing custom EPSG codes
     */
    @Override
    protected URL getDefinitionsURL() {
        InputStream stream = null;
        try {
            URL url = CustomEPSGCodes.class.getResource(CUSTOM_EPSG_CODES_FILE);
            // quickly test url
            stream = url.openStream();
            stream.read();
            stream.close();
            return url;
        } catch (Throwable e) {
            throw new AssertionError("Unable to load /epsg.properties file from root of classpath.");
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    throw ExceptionUtils.getRuntimeException(e);
                }
            }
        }
    }

}
