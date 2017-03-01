package org.mapfish.print.servlet.job.impl;

import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.servlet.job.PrintJobResult;

import java.net.URI;
import java.net.URISyntaxException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Print Job Result.
 *
 */
@Entity
@Table
public class PrintJobResultImpl implements PrintJobResult {

    @Column
    @Id
    private final String reportURI;

    @Column
    private final String mimeType;

    @Column
    private final String fileExtension;

    @Column
    private final String fileName;

    /**
     * Default Constructor.
     */
    public PrintJobResultImpl() {
        this.reportURI = null;
        this.mimeType = null;
        this.fileExtension = null;
        this.fileName = null;
    }

    /**
     * Constructor.
     * 
     * @param reportURI the report URI
     * @param fileName the file name
     * @param fileExtension the file extension
     * @param mimeType the mime type
     */
    public PrintJobResultImpl(final URI reportURI, final String fileName, final String fileExtension,
            final String mimeType) {
        this.reportURI = reportURI.toString();
        this.mimeType = mimeType;
        this.fileName = fileName;
        this.fileExtension = fileExtension;
    }

    @Override
    public final URI getReportURI() {
        try {
            return this.reportURI == null ? null : new URI(this.reportURI);
        } catch (URISyntaxException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }
    }

    @Override
    public final String getReportURIString() {
        return this.reportURI;
    }

    @Override
    public final String getMimeType() {
        return this.mimeType;
    }

    @Override
    public final String getFileExtension() {
        return this.fileExtension;
    }

    @Override
    public final String getFileName() {
        return this.fileName;
    }

}
