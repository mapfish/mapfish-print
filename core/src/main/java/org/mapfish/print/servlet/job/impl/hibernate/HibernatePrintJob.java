package org.mapfish.print.servlet.job.impl.hibernate;

import org.mapfish.print.processor.Processor;
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
    protected final PrintResult withOpenOutputStream(final PrintAction function) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Processor.ExecutionContext executionContext;
        try (BufferedOutputStream bout = new BufferedOutputStream(out)) {
            executionContext = function.run(bout);
            this.data = out.toByteArray();
        }
        return new PrintResult(new URI("hibernate:" + getEntry().getReferenceId()),
                this.data.length, executionContext);
    }

    @Override
    protected final PrintJobResult createResult(final URI reportURI, final String fileName,
            final String fileExtension, final String mimeType, final String referenceId) {
        return new PrintJobResultExtImpl(reportURI, fileName, fileExtension, mimeType, this.data,
                referenceId);
    }
}
