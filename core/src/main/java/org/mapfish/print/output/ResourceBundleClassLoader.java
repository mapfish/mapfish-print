package org.mapfish.print.output;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This is use to load the utf-8 ResourceBundle files.
 */
public class ResourceBundleClassLoader extends ClassLoader {
    private final File configDir;

    /**
     * Construct.
     *
     * @param configDir the print application configuration directory.
     */
    public ResourceBundleClassLoader(final File configDir) {
        this.configDir = configDir;
    }

    @Override
    protected URL findResource(final String resource) {
        try {
            return new File(this.configDir, resource).toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getResourceAsStream(final String resource) {
        try {
            final InputStream is = super.getResourceAsStream(resource);
            byte[] ba = new byte[is.available()];
            is.read(ba);
            return new ByteArrayInputStream(new String(ba, "utf-8").getBytes("iso-8859-1"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
