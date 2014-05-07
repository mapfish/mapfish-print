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

import com.google.common.base.Optional;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.servlet.registry.Registry;

import java.util.Date;

/**
 * Represent a print job that has completed.  Contains the information about the print job.
 * @author jesseeichar on 3/18/14.
 */
public abstract class CompletedPrintJob {
    /**
     * prefix for storing metadata about a job in the registry.
     */
    private static final String RESULT_METADATA = "resultMetadata_";

    private static final String JSON_APP = "appId";
    private static final String JSON_FILENAME = "fileName";
    private static final String JSON_SUCCESS = "success";
    private static final String JSON_COMPLETION_DATE = "completionDate";
    private final String referenceId;
    private final String appId;
    private final String fileName;
    private Date completionDate;

    /**
     * Constructor.
     *
     * @param referenceId reference of the report.
     * @param appId       the appId used for loading the configuration.
     * @param fileName    the fileName to send to the client.
     * @param completionDate the time when the print job ended.
     */
    public CompletedPrintJob(final String referenceId, final String appId, final Date completionDate, final String fileName) {
        this.referenceId = referenceId;
        this.appId = appId;
        this.completionDate = completionDate;
        this.fileName = fileName;
    }

    /**
     * Store the data of a print job in the registry.
     *
     * @param registry the registry to writer to
     */
    public final void store(final Registry registry) throws JSONException {
        JSONObject metadata = new JSONObject();
        metadata.put(JSON_APP, this.appId);
        metadata.put(JSON_FILENAME, this.fileName);
        metadata.put(JSON_SUCCESS, this instanceof SuccessfulPrintJob);
        metadata.put(JSON_COMPLETION_DATE, this.completionDate.getTime());
        addExtraParameters(metadata);
        registry.put(RESULT_METADATA + this.referenceId, metadata);
    }

    /**
     * Add extra information to the metadata object being put in the registry.
     *
     * @param metadata the json object that contains the metadata
     */
    protected abstract void addExtraParameters(JSONObject metadata) throws JSONException;

    /**
     * Construct a print job by reading the data from a registry.
     *
     * @param referenceId the reference id of the report to get information about.
     * @param registry    the registry to read from.
     */
    public static Optional<? extends CompletedPrintJob> load(final String referenceId, final Registry registry) throws JSONException {
        if (registry.containsKey(RESULT_METADATA + referenceId)) {
            JSONObject metadata = registry.getJSON(RESULT_METADATA + referenceId);

            String appId = metadata.optString(JSON_APP, null);
            String fileName = metadata.getString(JSON_FILENAME);
            Date completionDate = new Date(metadata.getLong(JSON_COMPLETION_DATE));
            CompletedPrintJob report;
            if (metadata.getBoolean(JSON_SUCCESS)) {
                report = SuccessfulPrintJob.load(metadata, referenceId, appId, completionDate, fileName);
            } else {
                report = FailedPrintJob.load(metadata, referenceId, appId, completionDate, fileName);
            }
            return Optional.of(report);
        } else {
            return Optional.absent();
        }
    }

    public final String getFileName() {
        return this.fileName;
    }

    public final Date getCompletionDate() {
        return this.completionDate;
    }
}
