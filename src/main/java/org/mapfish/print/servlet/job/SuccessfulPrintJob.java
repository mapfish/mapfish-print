/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.servlet.job;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Represents a successfully completed job.
 *
 * Created by Jesse on 3/18/14.
 */
public class SuccessfulPrintJob extends CompletedPrintJob {
    private static final String JSON_REPORT_URI = "reportURI";
    private final URI reportURI;

    /**
     * Constructor.
     *
     * @param referenceId reference of the report.
     * @param reportURI the uri for fetching the report.
     * @param appId the appId used for loading the configuration.
     * @param fileName the fileName to send to the client.
     */
    public SuccessfulPrintJob(final String referenceId, final URI reportURI, final String appId, final String fileName) {
        super(referenceId, appId, fileName);
        this.reportURI = reportURI;
    }

    /**
     * Construct a new instance from the values provided.
     *
     * @param metadata the metadata retrieved from the registry.  Only need it to get the extra information that is not stored by
     *                 parent class.
     * @param referenceId reference of the report.
     * @param appId the appId used for loading the configuration.
     * @param fileName the fileName to send to the client.
     */
    public static SuccessfulPrintJob load(final JSONObject metadata, final String referenceId, final String appId, final String fileName)
            throws JSONException {
        try {
        URI reportURI = new URI(metadata.getString(JSON_REPORT_URI));
        return new SuccessfulPrintJob(referenceId, reportURI, appId, fileName);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected final void addExtraParameters(final JSONObject metadata) throws JSONException {
        metadata.put(JSON_REPORT_URI, this.reportURI);
    }
}
