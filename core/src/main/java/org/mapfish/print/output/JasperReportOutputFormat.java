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

package org.mapfish.print.output;


import com.google.common.annotations.VisibleForTesting;

import jsr166y.ForkJoinPool;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import org.mapfish.print.Constants;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.config.WorkingDirectories;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.processor.jasper.JasperReportBuilder;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
/**
 * An PDF output format that uses Jasper reports to generate the result.
 *
 * @author Jesse
 * @author sbrunner
 */
public class JasperReportOutputFormat implements OutputFormat {
    private static final Logger LOGGER = LoggerFactory.getLogger(JasperReportOutputFormat.class);

    private static final String SUBREPORT_DIR = "SUBREPORT_DIR";
    private static final String SUBREPORT_TABLE_DIR = "SUBREPORT_TABLE_DIR";

    @Autowired
    private ForkJoinPool forkJoinPool;

    @Autowired
    private WorkingDirectories workingDirectories;
    
    @Autowired
    private MapfishParser parser;

    @Override
    public final String getContentType() {
        return "application/pdf";
    }

    @Override
    public final String getFileSuffix() {
        return "pdf";
    }

    @Override
    public final void print(final PJsonObject requestData, final Configuration config, final File configDir,
                final File taskDirectory, final OutputStream outputStream)
            throws Exception {
        final JasperPrint print = getJasperPrint(requestData, config, configDir, taskDirectory);
        JasperExportManager.exportReportToPdfStream(print, outputStream);
    }

    /**
     * Renders the jasper report.
     * 
     * @param requestData the data from the client, required for writing.
     * @param config the configuration object representing the server side configuration.
     * @param configDir the directory that contains the configuration, used for resolving resources like images etc...
     * @param taskDirectory the temporary directory for this printing task.
     * @return a jasper print object which can be used to generate a PDF or other outputs.
     */
    @VisibleForTesting
    protected final JasperPrint getJasperPrint(final PJsonObject requestData, final Configuration config, 
            final File configDir, final File taskDirectory)
            throws JRException, SQLException {
        final String templateName = requestData.getString(Constants.JSON_LAYOUT_KEY);

        final Template template = config.getTemplate(templateName);
        final Values values = new Values(requestData, template, this.parser, taskDirectory);

        final File jasperTemplateFile = new File(configDir, template.getJasperTemplate());
        final File jasperTemplateBuild = this.workingDirectories.getBuildFileFor(config, jasperTemplateFile,
                JasperReportBuilder.JASPER_REPORT_COMPILED_FILE_EXT, LOGGER);

        final File jasperTemplateDirectory = jasperTemplateBuild.getParentFile();

        values.put(SUBREPORT_DIR, jasperTemplateDirectory.getAbsolutePath());
        values.put(SUBREPORT_TABLE_DIR, taskDirectory.getAbsolutePath());

        this.forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

        final JasperPrint print;
        if (template.getIterValue() != null) {
            if (!values.containsKey(template.getIterValue())) {
                throw new IllegalArgumentException(template.getIterValue() + " is missing.  It must either an attribute or a processor " +
                                                   "output");
            }

            final Object iterator = values.getObject(template.getIterValue(), Object.class);
            if (!(iterator instanceof Iterable)) {
                throw new IllegalArgumentException(template.getIterValue() + " is supposed to be an iterable but was a "
                                                   + iterator.getClass());
            }
            final List<Map<String, ?>> dataSource = this.forkJoinPool.invoke(new ExecuteIterProcessorsTask(values, template));

            final JRDataSource jrDataSource = new JRMapCollectionDataSource(dataSource);

            print = JasperFillManager.fillReport(
                    jasperTemplateBuild.getAbsolutePath(),
                    values.getParameters(),
                    jrDataSource);
        } else if (template.getJdbcUrl() != null && template.getJdbcUser() != null && template.getJdbcPassword() != null) {
            Connection connection = DriverManager.getConnection(
                    template.getJdbcUrl(), template.getJdbcUser(), template.getJdbcPassword());

            print = JasperFillManager.fillReport(
                    jasperTemplateBuild.getAbsolutePath(),
                    values.getParameters(),
                    connection);
        } else if (template.getJdbcUrl() != null) {
            Connection connection = DriverManager.getConnection(template.getJdbcUrl());

            print = JasperFillManager.fillReport(
                    jasperTemplateBuild.getAbsolutePath(),
                    values.getParameters(),
                    connection);
        } else {
            print = JasperFillManager.fillReport(
                    jasperTemplateBuild.getAbsolutePath(),
                    values.getParameters(),
                    new JREmptyDataSource());
        }

        return print;
    }
}
