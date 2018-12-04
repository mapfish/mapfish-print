package org.mapfish.print.servlet.job.impl;

import org.mapfish.print.config.WorkingDirectories;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.servlet.job.PrintJob;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

/**
 * A PrintJob implementation that write results to files.
 * <p></p>
 */
public class FilePrintJob extends PrintJob {

    @Autowired
    private WorkingDirectories workingDirectories;


    @Override
    protected final PrintResult withOpenOutputStream(final PrintAction function) throws Exception {
        final File reportFile = new File(this.workingDirectories.getReports(), getEntry().getReferenceId());
        FileOutputStream out = null;
        BufferedOutputStream bout = null;
        final Processor.ExecutionContext executionContext;
        try {
            out = new FileOutputStream(reportFile);
            bout = new BufferedOutputStream(out);
            executionContext = function.run(bout);
        } finally {
            try {
                if (bout != null) {
                    bout.close();
                }
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
        return new PrintResult(reportFile.toURI(), reportFile.length(), executionContext);
    }

    @Override
    protected MimeBodyPart getReportAttachment(final String mimeType) throws IOException, MessagingException {
        final MimeBodyPart result = new MimeBodyPart();
        result.attachFile(new File(this.workingDirectories.getReports(), getEntry().getReferenceId()),
                          mimeType, null);
        return result;
    }

    @Override
    protected void deleteReport() {
        new File(this.workingDirectories.getReports(), getEntry().getReferenceId()).delete();
    }
}
