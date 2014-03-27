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

import org.mapfish.print.config.WorkingDirectories;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;

/**
 * A PrintJob implementation that write results to files.
 * <p/>
 * @author jesseeichar on 3/18/14.
 */
public class FilePrintJob extends PrintJob implements PrintJobFactory {


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
        return null;
    }

    @Override
    public final PrintJob create() {
        return new FilePrintJob();
    }
}
