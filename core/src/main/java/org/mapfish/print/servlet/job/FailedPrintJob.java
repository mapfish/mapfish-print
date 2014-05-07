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

import java.util.Date;

/**
 * Represents a failed print job.
 *
 * @author jesseeichar on 3/18/14.
 */
public class FailedPrintJob extends CompletedPrintJob {
    private static final String JSON_ERROR = "errorMessage";
    private final String error;

    /**
     * Constructor.
     *
     * @param referenceId    reference of the report.
     * @param appId          the appId used for loading the configuration.
     * @param completionDate the date when the print job completed
     * @param fileName       the fileName to send to the client.
     * @param error          the error that occurred during running.
     */
    public FailedPrintJob(final String referenceId, final String appId, final Date completionDate, final String fileName,
                          final String error) {
        super(referenceId, appId, completionDate, fileName);
        this.error = error;
    }

    @Override
    protected final void addExtraParameters(final JSONObject metadata) throws JSONException {
        metadata.put(JSON_ERROR, this.error);
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
     */
    public static FailedPrintJob load(final JSONObject metadata, final String referenceId, final String appId,
                                      final Date completionDate, final String fileName) throws JSONException {
        String error = metadata.getString(JSON_ERROR);

        return new FailedPrintJob(referenceId, appId, completionDate, fileName, error);
    }


    public final String getError() {
        return this.error;
    }
}
