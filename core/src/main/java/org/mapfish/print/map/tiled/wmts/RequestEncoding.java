package org.mapfish.print.map.tiled.wmts;

/**
 * The types of encoding to use when making http requests.
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
