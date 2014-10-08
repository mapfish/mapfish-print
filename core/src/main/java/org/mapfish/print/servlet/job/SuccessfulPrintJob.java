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
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.config.access.AccessAssertion;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

/**
 * Represents a successfully completed job.
 *
 * @author jesseeichar on 3/18/14.
 */
public final class SuccessfulPrintJob extends PrintJobStatus {
    private static final String JSON_REPORT_URI = "reportURI";
    private static final String JSON_MIME_TYPE = "mimeType";
    private static final String JSON_FILE_EXT = "fileExtension";
    private final URI reportURI;
    private final String mimeType;
    private final String fileExtension;

    /**
     * Constructor.
     *
     * @param referenceId    reference of the report.
     * @param reportURI      the uri for fetching the report.
     * @param appId          the appId used for loading the configuration.
     * @param completionDate the date when the print job completed
     * @param fileName       the fileName to send to the client.
     * @param mimeType       the mimetype of the printed file
     * @param fileExtension  the file extension (to be added to the filename)
     * @param access         the an access control object for downloading this report.  Typically this is combined access of the
     *                       template and the configuration.
     */
    // CSOFF: ParameterNumber
    public SuccessfulPrintJob(final String referenceId, final URI reportURI, final String appId, final Date completionDate,
                              final String fileName, final String mimeType, final String fileExtension, final AccessAssertion access) {
        // CSON: ParameterNumber
        super(referenceId, appId, completionDate, fileName, access);
        this.reportURI = reportURI;
        this.mimeType = mimeType;
        this.fileExtension = fileExtension;
    }

    public URI getURI() {
        return this.reportURI;
    }

    public String getMimeType() {
        return this.mimeType;
    }

    public String getFileExtension() {
        return this.fileExtension;
    }

    /**
     * Construct a new instance from the values provided.
     *
     * @param metadata       the metadata retrieved from the registry.  Only need it to get the extra information that is not stored by
     *                       parent class.
     * @param referenceId    reference of the report.
     * @param appId          the appId used for loading the configuration.
     * @param completionDate the date when the print job completed
     * @param fileName       the fileName to send to the client.
     * @param reportAccess   the an access control object for downloading this report.  Typically this is combined access of the
     *                       template and the configuration.
     */
    public static SuccessfulPrintJob load(final JSONObject metadata, final String referenceId, final String appId,
                                          final Date completionDate, final String fileName, final AccessAssertion reportAccess)
            throws JSONException {
        try {
            URI reportURI = new URI(metadata.getString(JSON_REPORT_URI));
            String fileExt = metadata.getString(JSON_FILE_EXT);
            String mimeType = metadata.getString(JSON_MIME_TYPE);

            return new SuccessfulPrintJob(referenceId, reportURI, appId, completionDate, fileName, mimeType, fileExt, reportAccess);
        } catch (URISyntaxException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }

    @Override
    protected void addExtraParameters(final JSONObject metadata) throws JSONException {
        metadata.put(JSON_REPORT_URI, this.reportURI);
        metadata.put(JSON_FILE_EXT, this.fileExtension);
        metadata.put(JSON_MIME_TYPE, this.mimeType);
    }
}
