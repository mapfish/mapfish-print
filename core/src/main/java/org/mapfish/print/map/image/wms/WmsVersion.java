package org.mapfish.print.map.image.wms;

import org.geotools.ows.wms.WMS1_0_0;
import org.geotools.ows.wms.WMS1_1_0;
import org.geotools.ows.wms.WMS1_1_1;
import org.geotools.ows.wms.WMS1_3_0;
import org.geotools.ows.wms.request.GetMapRequest;

import java.net.URL;

/**
 * An enumeration of all the supported WMS versions.
 */
public enum WmsVersion {
    /**
     * Version for WMS 1.0.0.
     */
    V1_0_0 {
        @Override
        public GetMapRequest getGetMapRequest(final URL baseURL) {
            return new WMS1_0_0.GetMapRequest(baseURL);
        }
    },
    /**
     * Version for WMS 1.1.0.
     */
    V1_1_0 {
        @Override
        public GetMapRequest getGetMapRequest(final URL baseURL) {
            return new WMS1_1_0.GetMapRequest(baseURL);
        }
    },
    /**
     * Version for WMS 1.1.1.
     */
    V1_1_1 {
        @Override
        public GetMapRequest getGetMapRequest(final URL baseURL) {
            return new WMS1_1_1.GetMapRequest(baseURL);
        }
    },
    /**
     * Version for WMS 1.3.0.
     */
    V1_3_0 {
        @Override
        public GetMapRequest getGetMapRequest(final URL baseURL) {
            return new WMS1_3_0.GetMapRequest(baseURL);
        }
    };

    /**
     * Find the correct version enum object based on the version string.
     *
     * @param versionString the version string.
     */
    public static WmsVersion lookup(final String versionString) {
        for (WmsVersion wmsVersion: values()) {
            if (versionString.equals(wmsVersion.versionString())) {
                return wmsVersion;
            }
        }

        StringBuilder msg = new StringBuilder("\n'").append(versionString)
                .append("' is not one of the supported WMS versions.  Supported versions include: ");
        for (WmsVersion wmsVersion: values()) {
            msg.append("\n\t* ").append(wmsVersion.versionString());
        }
        throw new IllegalArgumentException(msg.toString());
    }

    /**
     * Get the WMS version string usable in making WMS requests.
     */
    public String versionString() {
        return name().substring(1).replace('_', '.');
    }

    /**
     * Return a getMap request that is configured for the correct version.
     *
     * @param baseURL the url to use as the basis for making the request. It has to have the host and
     *         full path but query parameters are optional.
     */
    public abstract GetMapRequest getGetMapRequest(URL baseURL);
}
