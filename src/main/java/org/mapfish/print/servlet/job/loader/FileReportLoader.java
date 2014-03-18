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

package org.mapfish.print.servlet.job.loader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

/**
 * Loads reports from file uris.
 * <p/>
 * Created by Jesse on 3/18/14.
 */
public class FileReportLoader implements ReportLoader {
    @Override
    public final boolean accepts(final URI reportURI) {
        return reportURI.getAuthority().equals("file://");
    }

    @Override
    public final void loadReport(final URI reportURI, final OutputStream out) throws IOException {
        FileInputStream in = null;
        FileChannel channel = null;
        try {
            in = new FileInputStream(reportURI.getPath());
            channel = in.getChannel();
            channel.transferTo(0, Long.MAX_VALUE, Channels.newChannel(out));
        } finally {
            try {
                if (channel != null) {
                    channel.close();
                }
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
    }
}
