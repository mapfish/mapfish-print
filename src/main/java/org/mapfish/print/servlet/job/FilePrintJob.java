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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import javax.annotation.PostConstruct;

/**
 * A PrintJob implementation that write results to files.
 * <p/>
 * Created by Jesse on 3/18/14.
 */
public class FilePrintJob extends PrintJob implements PrintJobFactory {
    private File directory;


    /**
     * Set the directory that the will contain the report files.  By default it will be in the temporary folder.
     *
     * @param directory the directory to contain the report files
     */
    public final void setDirectory(final String directory) {
        this.directory = new File(directory);
    }

    /**
     * Check that the directory can be written to.
     */
    @PostConstruct
    public final void init() throws IOException {
        if (!this.directory.exists() && !this.directory.mkdirs()) {
            throw new IOException("Unable to write to report storage directory: " + this.directory +
                                  " Change the spring configuration file (default is mapfish-spirng-application-context.mxml)");
        }
    }


    @Override
    protected final URI withOpenOutputStream(final PrintAction function) throws Throwable {
        final File reportFile = new File(this.directory, getReferenceId());
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
        return null;
    }

    @Override
    public final PrintJob create() {
        return new FilePrintJob();
    }
}
