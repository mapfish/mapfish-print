package org.mapfish.print.servlet.job.impl.hibernate;

import org.mapfish.print.servlet.job.impl.PrintJobResultImpl;

import java.net.URI;
import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Extension of Print Job Result that holds data as BLOB.
 */
@Entity
public class PrintJobResultExtImpl extends PrintJobResultImpl {

    @Column
    private byte[] data;

    /**
     * Default Constructor.
     */
    public PrintJobResultExtImpl() {
        this.data = null;
    }

    /**
     * Constructor.
     *
     * @param reportURI the report URI
     * @param fileName the file name
     * @param fileExtension the file extension
     * @param mimeType the mime type
     * @param data the data
     * @param referenceId the reference ID
     */
    public PrintJobResultExtImpl(
            final URI reportURI, final String fileName, final String fileExtension, final String mimeType,
            final byte[] data, final String referenceId) {
        super(reportURI, fileName, fileExtension, mimeType, referenceId);
        this.data = data;
    }


    public byte[] getData() {
        return this.data;
    }

    public void setData(final byte[] data) {
        this.data = data;
    }

}
