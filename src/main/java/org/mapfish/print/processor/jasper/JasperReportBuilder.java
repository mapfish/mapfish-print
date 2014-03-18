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

package org.mapfish.print.processor.jasper;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import org.mapfish.print.output.Values;
import org.mapfish.print.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;

/**
 * A processor that actually compiles a jasper report.
 * 
 * @author Jesse
 */
public class JasperReportBuilder implements Processor {
    private static final Logger LOGGER = LoggerFactory.getLogger(JasperReportBuilder.class);

    private File directory = new File(".");

    @Override
	public final Map<String, Object> execute(final Values values) throws JRException {
        final FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(final File file, final String name) {
                return name.toLowerCase().endsWith(".jrxml");
            }
        };
        for (String jasperFileName : this.directory.list(filter)) {
            final File jasperFile = new File(this.directory.getAbsolutePath(), jasperFileName);
            final File buildFile = new File(this.directory.getAbsolutePath(),
                    jasperFileName.replaceAll("\\.jrxml$", ".jasper"));
            if (!buildFile.exists() || jasperFile.lastModified() > buildFile.lastModified()) {
                LOGGER.info("Building Jasper report: " + jasperFile.getAbsolutePath());
                long start = System.currentTimeMillis();
                JasperCompileManager.compileReportToFile(jasperFile.getAbsolutePath(),
                        buildFile.getAbsolutePath());
                LOGGER.info("Report built in " + (System.currentTimeMillis() - start) + "ms.");
            }
        }
        return null;
    }

    @Override
	public final Map<String, String> getOutputMapper() {
        return null;
    }

    public final String getDirectory() {
        return this.directory.getPath();
    }

    public final void setDirectory(final String directory) {
        this.directory = new File(directory);
    }
}
