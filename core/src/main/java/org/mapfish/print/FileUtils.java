package org.mapfish.print;

import org.mapfish.print.config.Configuration;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Methods for interacting with files.  Such things and verifying the files are in the correct directory,
 * Converting URLs to file objects.
 *
 * @author Jesse on 4/8/2014.
 */
public final class FileUtils {
    private FileUtils() {
        // intentionally empty
    }
    /**
     * Check if the url is a file url which is either relative to the configuration directory or refers to a file in the
     * configuration directory.
     * <p/>
     * The correct file url is returned or the original if not a file url.  If an illegal file url (one that does not refer to a
     * file within the configuration directory or is relative to the configuration directory) then an exception is thrown.
     *
     * @param url           the url to test
     * @param configuration the configuration to test relativity.
     */
    public static URL testForLegalFileUrl(final Configuration configuration, final URL url) {
        final String protocol = url.getProtocol();
        if (protocol.equalsIgnoreCase("file")) {
            try {

                File file = new File(configuration.getDirectory(), url.toExternalForm().substring("file://".length()));
                if (file.exists() && file.isFile()) {
                    URL tmpUrl = file.getAbsoluteFile().toURI().toURL();
                    assertFileIsInConfigDir(configuration, file);
                    return tmpUrl;
                } else {
                    file = new File(url.getFile());
                    if (file.exists() && file.isFile()) {
                        URL tmpUrl = file.getAbsoluteFile().toURI().toURL();
                        assertFileIsInConfigDir(configuration, file);
                        return tmpUrl;
                    } else {
                        throw new IllegalArgumentException("File urls must refer to a file within the configuration directory");
                    }
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return url;
    }

    /**
     * Verify that the file is within the configuration directory.
     *
     * @param configuration the configuration to test relativity.
     * @param file the file to test
     */
    public static void assertFileIsInConfigDir(final Configuration configuration, final File file) {
        final String configurationDir = configuration.getDirectory().getAbsolutePath();
        if (!file.getAbsolutePath().startsWith(configurationDir)) {
            throw new IllegalArgumentException("The url is a file url but indicates a file that is not within the" +
                                               " configurationDirectory: " + file.getAbsolutePath());
        }
    }
}
