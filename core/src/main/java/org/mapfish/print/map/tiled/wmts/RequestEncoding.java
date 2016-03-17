package org.mapfish.print.map.tiled.wmts;

/**
 * The types of encoding to use when making http requests.
 *
 * @author Jesse on 4/3/14.
 */
public enum RequestEncoding {
    /**
     * Use query parameters for the WMTS tile parameters.
     */
    KVP,
    /**
     * Use Rest format for encoding the WMTS tile parameters.
     */
    REST
}
