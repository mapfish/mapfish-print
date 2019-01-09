package org.mapfish.print.servlet.fileloader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

/**
 * Strategy for loading configuration files and files that are used in printing that are relative (or related)
 * to the configuration file.  For example, if a file is loaded from the file system as requested by a client,
 * (IE the file path was obtained by the client) then there must be restrictions placed on which files the
 * client may load (you can't allow him to load the full database or a passwords file for example).  The
 * simple way is to ensure the file is within the same or sub-directory of the config file directory.
 */
public interface ConfigFileLoaderPlugin {

    /**
     * Return the file object the uri refers to if it refers to a file.  Otherwise Optional.absent().
     *
     * @param fileUri the uri to use to resolve to a file (or not).
     */
    Optional<File> toFile(URI fileUri);

    /**
     * Returns the URI scheme that this loader supports.
     */
    String getUriScheme();

    /**
     * return the last modified time of the file URI.
     *
     * @param fileURI the uri of the config file to load.
     * @return return the last modified date of the file
     */
    Optional<Long> lastModified(URI fileURI);

    /**
     * Check if the configuration File exists.
     *
     * @param fileURI the uri of the file to load.
     */
    boolean isAccessible(URI fileURI);

    /**
     * Load the config data.
     *
     * @param fileURI the uri of the config file to load.
     * @return the file that make up the file.
     */
    byte[] loadFile(URI fileURI) throws IOException;

    /**
     * check if the file exists and can be accessed by the user/template/config/etc...
     *
     * @param configFileUri the uri of the configuration file
     * @param pathToSubResource a string representing a file that is accessible for use in printing
     *         templates within the configuration file.  In the case of a file based URI the path could be a
     *         relative path (relative to the configuration file) or an absolute path, but it must be an
     *         allowed file (you can't allow access to any file on the file system).
     */
    boolean isAccessible(URI configFileUri, String pathToSubResource) throws IOException;

    /**
     * Load the file related to the configuration file.
     *
     * @param configFileUri the uri of the configuration file
     * @param pathToSubResource a string representing a file that is accessible for use in printing
     *         templates within the configuration file.  In the case of a file based URI the path could be a
     *         relative path (relative to the configuration file) or an absolute path, but it must be an
     *         allowed file (you can't allow access to any file on the file system).
     */
    byte[] loadFile(URI configFileUri, String pathToSubResource) throws IOException;
}
