package org.mapfish.print.servlet.job.loader;

import com.google.common.io.Closer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

/**
 * Loads reports from file uris.
 * <p></p>
 */
public class FileReportLoader implements ReportLoader {
    @Override
    public final boolean accepts(final URI reportURI) {
        return reportURI.getScheme().equals("file");
    }

    @Override
    public final void loadReport(final URI reportURI, final OutputStream out) throws IOException {
        try (Closer closer = Closer.create()) {
            FileInputStream in = closer.register(new FileInputStream(reportURI.getPath()));
            FileChannel channel = closer.register(in.getChannel());
            channel.transferTo(0, Long.MAX_VALUE, Channels.newChannel(out));
        }
    }
}
