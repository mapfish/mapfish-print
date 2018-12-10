package org.mapfish.print.servlet.job.impl;

import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.config.access.AccessAssertion;
import org.mapfish.print.config.access.AccessAssertionPersister;
import org.mapfish.print.servlet.job.JobQueue;
import org.mapfish.print.servlet.job.NoSuchReferenceException;
import org.mapfish.print.servlet.job.PrintJobEntry;
import org.mapfish.print.servlet.job.PrintJobResult;
import org.mapfish.print.servlet.job.PrintJobStatus;
import org.mapfish.print.servlet.registry.Registry;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Job Queue that uses Registry.
 */
public class RegistryJobQueue implements JobQueue {

    /**
     * Key for storing the number of print jobs currently running.
     */
    private static final String NEW_PRINT_COUNT = "newPrintCount";

    /**
     * The number of print requests made.
     */
    private static final String LAST_PRINT_COUNT = "lastPrintCount";

    /**
     * Total time spent printing.
     */
    private static final String TOTAL_PRINT_TIME = "totalPrintTime";
    /**
     * Number of print jobs done.
     */
    private static final String NB_PRINT_DONE = "nbPrintDone";
    /**
     * A registry tracking when the last time a metadata was check to see if it is done.
     */
    private static final String LAST_POLL = "lastPoll_";

    /**
     * prefix for storing metadata about a job in the registry.
     */
    private static final String RESULT_METADATA = "resultMetadata_";

    private static final String JSON_REQUEST_DATA = "requestData";
    private static final String JSON_FILENAME = "fileName";
    private static final String JSON_STATUS = "status";
    private static final String JSON_ACCESS_ASSERTION = "access";
    private static final String JSON_START_DATE = "startDate";
    private static final String JSON_COMPLETION_DATE = "completionDate";
    private static final String JSON_REQUEST_COUNT = "requestCount";
    private static final String JSON_ERROR = "errorMessage";
    private static final String JSON_REPORT_URI = "reportURI";
    private static final String JSON_MIME_TYPE = "mimeType";
    private static final String JSON_FILE_EXT = "fileExtension";

    @Autowired
    private Registry registry;

    @Qualifier("accessAssertionPersister")
    @Autowired
    private AccessAssertionPersister assertionPersister;

    @Override
    public final long getTimeToKeepAfterAccessInMillis() {
        return this.registry.getTimeToKeepAfterAccessInMillis();
    }

    @Override
    public final synchronized void add(final PrintJobEntry jobEntry) {
        this.registry.incrementInt(NEW_PRINT_COUNT, 1);
        try {
            store(new PrintJobStatusImpl(jobEntry, getNumberOfRequestsMade()));
        } catch (JSONException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
        this.registry.put(LAST_POLL + jobEntry.getReferenceId(), System.currentTimeMillis());
    }

    @Override
    public final synchronized void start(final String referenceId) throws NoSuchReferenceException {
        try {
            PrintJobStatusImpl jobStatus = load(referenceId);
            if (jobStatus.getStatus() == PrintJobStatus.Status.WAITING) {
                jobStatus.setStatus(PrintJobStatus.Status.RUNNING);
                jobStatus.setWaitingTime(0);
                store(jobStatus);
            }
        } catch (JSONException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }

    @Override
    public final synchronized void done(final String referenceId, final PrintJobResult result)
            throws NoSuchReferenceException {
        try {
            PrintJobStatusImpl status = load(referenceId);
            if (!status.isDone()) {
                this.registry.incrementInt(NB_PRINT_DONE, 1);
                this.registry.incrementLong(TOTAL_PRINT_TIME, status.getElapsedTime());
                this.registry.incrementInt(LAST_PRINT_COUNT, 1);
            }

            status.setCompletionTime(System.currentTimeMillis());
            status.setStatus(PrintJobStatus.Status.FINISHED);
            status.setResult(result);
            store(status);
        } catch (JSONException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }

    @Override
    public final synchronized void cancel(
            final String referenceId, final String message, final boolean forceFinal)
            throws NoSuchReferenceException {
        try {
            PrintJobStatusImpl status = load(referenceId);

            if (!forceFinal && status.getStatus() == PrintJobStatus.Status.RUNNING) {
                status.setStatus(PrintJobStatus.Status.CANCELING);
            } else {
                if (!status.isDone()) {
                    this.registry.incrementInt(NB_PRINT_DONE, 1);
                    this.registry.incrementLong(TOTAL_PRINT_TIME, status.getElapsedTime());
                    this.registry.incrementInt(LAST_PRINT_COUNT, 1);
                }
                // even if the job is already finished, we store it as "cancelled" in the registry,
                // so that all subsequent status requests return "cancelled"
                status.setCompletionTime(System.currentTimeMillis());
                status.setStatus(PrintJobStatus.Status.CANCELLED);
            }

            status.setError(message);
            store(status);
        } catch (JSONException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }

    @Override
    public final synchronized void fail(final String referenceId, final String message)
            throws NoSuchReferenceException {
        try {
            PrintJobStatusImpl status = load(referenceId);
            if (!status.isDone()) {
                this.registry.incrementInt(NB_PRINT_DONE, 1);
                this.registry.incrementLong(TOTAL_PRINT_TIME, status.getElapsedTime());
                this.registry.incrementInt(LAST_PRINT_COUNT, 1);
            }

            // even if the job is already finished, we store it as "cancelled" in the registry,
            // so that all subsequent status requests return "cancelled"
            status.setCompletionTime(System.currentTimeMillis());
            status.setStatus(PrintJobStatus.Status.ERROR);
            status.setError(message);
            store(status);
        } catch (JSONException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }

    @Override
    public final long getNumberOfRequestsMade() {
        return this.registry.opt(NEW_PRINT_COUNT, 0);
    }

    @Override
    public final long timeSinceLastStatusCheck(final String referenceId) {
        long lastPoll = this.registry.opt(LAST_POLL + referenceId, 0L);
        if (lastPoll == 0) {
            return 0;
        }
        return System.currentTimeMillis() - lastPoll;
    }

    @Override
    public final long getAverageTimeSpentPrinting() {
        return this.registry.opt(TOTAL_PRINT_TIME, 0L) / this.registry.opt(NB_PRINT_DONE, 1).longValue();
    }

    @Override
    public final long getLastPrintCount() {
        return this.registry.opt(LAST_PRINT_COUNT, 0);
    }

    @Override
    public final long getWaitingJobsCount() {
        return getNumberOfRequestsMade() - getLastPrintCount();
    }

    @Override
    public final PrintJobStatusImpl get(final String referenceId, final boolean external)
            throws NoSuchReferenceException {
        try {
            PrintJobStatusImpl status = load(referenceId);
            status.setStatusTime(System.currentTimeMillis());

            if (!status.isDone() && external) {
                // remember when the status was polled for the last time
                this.registry.put(LAST_POLL + referenceId, System.currentTimeMillis());
            }

            return status;
        } catch (JSONException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }

    /**
     * Store the data of a print job in the registry.
     *
     * @param printJobStatus the print job status
     */
    private void store(final PrintJobStatus printJobStatus) throws JSONException {
        JSONObject metadata = new JSONObject();
        metadata.put(JSON_REQUEST_DATA, printJobStatus.getEntry().getRequestData().getInternalObj());
        metadata.put(JSON_STATUS, printJobStatus.getStatus().toString());
        metadata.put(JSON_START_DATE, printJobStatus.getStartTime());
        metadata.put(JSON_REQUEST_COUNT, printJobStatus.getRequestCount());
        if (printJobStatus.getCompletionDate() != null) {
            metadata.put(JSON_COMPLETION_DATE, printJobStatus.getCompletionTime());
        }
        metadata.put(JSON_ACCESS_ASSERTION, this.assertionPersister.marshal(printJobStatus.getAccess()));
        if (printJobStatus.getError() != null) {
            metadata.put(JSON_ERROR, printJobStatus.getError());
        }
        if (printJobStatus.getResult() != null) {
            metadata.put(JSON_REPORT_URI, printJobStatus.getResult().getReportURIString());
            metadata.put(JSON_FILENAME, printJobStatus.getResult().getFileName());
            metadata.put(JSON_FILE_EXT, printJobStatus.getResult().getFileExtension());
            metadata.put(JSON_MIME_TYPE, printJobStatus.getResult().getMimeType());
        }
        this.registry.put(RESULT_METADATA + printJobStatus.getReferenceId(), metadata);
    }

    private PrintJobStatusImpl load(final String referenceId) throws JSONException, NoSuchReferenceException {
        if (this.registry.containsKey(RESULT_METADATA + referenceId)) {
            JSONObject metadata = this.registry.getJSON(RESULT_METADATA + referenceId);

            PrintJobStatus.Status status = PrintJobStatus.Status.valueOf(metadata.getString(JSON_STATUS));

            PJsonObject requestData = new PJsonObject(metadata.getJSONObject(JSON_REQUEST_DATA), "spec");
            Long startTime = metadata.getLong(JSON_START_DATE);
            long requestCount = metadata.getLong(JSON_REQUEST_COUNT);

            JSONObject accessJSON = metadata.getJSONObject(JSON_ACCESS_ASSERTION);
            final AccessAssertion accessAssertion = this.assertionPersister.unmarshal(accessJSON);

            PrintJobStatusImpl report = new PrintJobStatusImpl(
                    new PrintJobEntryImpl(referenceId, requestData, startTime, accessAssertion),
                    requestCount);
            report.setStatus(status);

            if (metadata.has(JSON_COMPLETION_DATE)) {
                report.setCompletionTime(metadata.getLong(JSON_COMPLETION_DATE));
            }

            if (metadata.has(JSON_ERROR)) {
                report.setError(metadata.getString(JSON_ERROR));
            }

            if (metadata.has(JSON_REPORT_URI)) {
                URI reportURI;
                try {
                    reportURI = new URI(metadata.getString(JSON_REPORT_URI));
                } catch (URISyntaxException e) {
                    throw ExceptionUtils.getRuntimeException(e);
                }
                String fileName = metadata.getString(JSON_FILENAME);
                String fileExt = metadata.getString(JSON_FILE_EXT);
                String mimeType = metadata.getString(JSON_MIME_TYPE);

                PrintJobResult result =
                        new PrintJobResultImpl(reportURI, fileName, fileExt, mimeType, referenceId);
                report.setResult(result);
            }

            return report;
        } else {
            throw new NoSuchReferenceException(referenceId);
        }
    }

    @Override
    public final void cancelOld(final long startTimeOut, final long abandonTimeout, final String message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final List<? extends PrintJobStatus> start(final int number) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final List<? extends PrintJobStatus> toCancel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(final String referenceId) {
        this.registry.delete(referenceId);
    }
}
