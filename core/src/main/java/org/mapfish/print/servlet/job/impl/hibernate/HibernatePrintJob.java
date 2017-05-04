package org.mapfish.print.servlet.job.impl.hibernate;

import org.mapfish.print.servlet.job.PrintJob;
import org.mapfish.print.servlet.job.PrintJobResult;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;

/**
 * A PrintJob implementation that write results to the database.
 * <p></p>
 */
public class HibernatePrintJob extends PrintJob {

    private byte[] data;

    @Override
    protected final URI withOpenOutputStream(final PrintAction function) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedOutputStream bout = new BufferedOutputStream(out);
        try {
            function.run(bout);
            this.data = out.toByteArray();
        } finally {
            bout.close();
        }
        return new URI("hibernate:" + getEntry().getReferenceId());
    }

    @Override
    protected final PrintJobResult createResult(final URI reportURI, final String fileName,
            final String fileExtension, final String mimeType) {
        return new PrintJobResultExtImpl(reportURI, fileName, fileExtension, mimeType, this.data);
    }
}
