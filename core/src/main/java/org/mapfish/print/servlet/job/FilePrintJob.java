package org.mapfish.print.servlet.job;

import org.mapfish.print.config.WorkingDirectories;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;

/**
 * A PrintJob implementation that write results to files.
 * <p></p>
 * @author jesseeichar on 3/18/14.
 */
public class FilePrintJob extends PrintJob {


    @Autowired
    private WorkingDirectories workingDirectories;


    @Override
    protected final URI withOpenOutputStream(final PrintAction function) throws Throwable {
        final File reportFile = new File(this.workingDirectories.getReports(), getReferenceId());
        FileOutputStream out = null;
        BufferedOutputStream bout = null;
        try {
            out = new FileOutputStream(reportFile);
            bout = new BufferedOutputStream(out);
            function.run(bout);
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
        return reportFile.toURI();
    }
}
