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

import ar.com.fdvs.dj.core.DynamicJasperHelper;
import ar.com.fdvs.dj.core.layout.ClassicLayoutManager;
import ar.com.fdvs.dj.domain.DynamicReport;
import ar.com.fdvs.dj.domain.Style;
import ar.com.fdvs.dj.domain.builders.ColumnBuilder;
import ar.com.fdvs.dj.domain.builders.DynamicReportBuilder;
import ar.com.fdvs.dj.domain.constants.Border;
import ar.com.fdvs.dj.domain.constants.Font;
import ar.com.fdvs.dj.domain.constants.HorizontalAlign;
import ar.com.fdvs.dj.domain.constants.VerticalAlign;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import org.mapfish.print.attribute.TableListAttribute.TableListAttributeValue;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.HasConfiguration;
import org.mapfish.print.config.WorkingDirectories;
import org.mapfish.print.output.Values;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.wrapper.json.PJsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mapfish.print.processor.jasper.JasperReportBuilder.JASPER_REPORT_COMPILED_FILE_EXT;
import static org.mapfish.print.processor.jasper.JasperReportBuilder.JASPER_REPORT_XML_FILE_EXT;

/**
 * Processor for creating a table.
 *
 * @author Jesse
 * @author sbrunner
 */
public class TableListProcessor extends AbstractProcessor<TableListProcessor.Input, TableListProcessor.Output>
        implements HasConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(TableListProcessor.class);

    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_DISPLAYNAME = "displayName";
    private static final String COLUMN_DATASOURCE = "dataSource";

    private static final int DEFAULT_TABLE_WIDTH = 500;
    private static final int DEFAULT_BLUE = 230;
    private static final int DEFAULT_GREEN = 230;
    private static final int DEFAULT_RED = 230;
    private static final String DEFAULT_FONT = "DejaVu Sans";
    private static final int DEFAULT_TITLE_FONT_SIZE = 14;
    private static final int DEFAULT_FONT_SIZE = 12;


    private Map<String, Object> dynamicReport = null;

    private File dynamicReportDirectory;
    
    private Configuration configuration;
    @Autowired
    private WorkingDirectories workingDirectories;

    /**
     * Constructor.
     */
    protected TableListProcessor() {
        super(Output.class);
    }

    @Override
    public final Input createInputParameter() {
        return new Input();
    }

    @Override
    public final Output execute(final Input values, final ExecutionContext context) throws Exception {
        final TableListAttributeValue[] jsonTableList = values.tableList;
        final List<Values> tableList = new ArrayList<Values>();

        if (jsonTableList != null) {

            for (TableListAttributeValue tableListAttributeValue : jsonTableList) {
                String id = tableListAttributeValue.id;
                createColumnDataSource(tableListAttributeValue, tableList);
                final String[] jsonColumns = tableListAttributeValue.columns;

                if (this.dynamicReport != null) {
                    Style detailStyle = new Style();
                    detailStyle.setFont(new Font(dynamicReportOptInt("fontSize", DEFAULT_FONT_SIZE),
                            dynamicReportOptString("font", DEFAULT_FONT), false));
                    detailStyle.setVerticalAlign(VerticalAlign.MIDDLE);

                    Style headerStyle = new Style();
                    headerStyle.setFont(new Font(dynamicReportOptInt("titleFontSize", DEFAULT_TITLE_FONT_SIZE),
                            dynamicReportOptString("titleFont", DEFAULT_FONT), true));
                    headerStyle.setHorizontalAlign(HorizontalAlign.CENTER);
                    headerStyle.setVerticalAlign(VerticalAlign.MIDDLE);
                    headerStyle.setBorderBottom(Border.PEN_1_POINT());

                    Style oddRowStyle = new Style();
                    oddRowStyle.setBackgroundColor(new Color(DEFAULT_RED, DEFAULT_GREEN, DEFAULT_BLUE));
                    oddRowStyle.setFont(new Font(dynamicReportOptInt("fontSize", DEFAULT_FONT_SIZE),
                            dynamicReportOptString("font", DEFAULT_FONT), false));
                    oddRowStyle.setVerticalAlign(VerticalAlign.MIDDLE);

                    File customTableTemplate = new File(this.dynamicReportDirectory, "table_" + id + JASPER_REPORT_XML_FILE_EXT);
                    if (customTableTemplate.exists()) {
                        // in case a custom template is provided, simply compile this file (if necessary)
                        File customTableTemplateBuild = this.workingDirectories.getBuildFileFor(this.configuration, customTableTemplate,
                                JASPER_REPORT_COMPILED_FILE_EXT, LOGGER);
                        
                        if (!customTableTemplateBuild.exists()
                                || (customTableTemplate.lastModified() > customTableTemplateBuild.lastModified())) {
                            JasperCompileManager.compileReportToFile(customTableTemplate.getAbsolutePath(),
                                    customTableTemplateBuild.getAbsolutePath());
                            
                        }
                    } else {
                        // if no template is provided, build a report on-the-fly
                        File tableTemplate = new File(values.tempTaskDirectory, "table_" + id + JASPER_REPORT_XML_FILE_EXT);
                        File tableTemplateBuild = new File(values.tempTaskDirectory, "table_" + id + JASPER_REPORT_COMPILED_FILE_EXT);

                        generateDynamicReport(jsonColumns, detailStyle, headerStyle, oddRowStyle, tableTemplate);
                        JasperCompileManager.compileReportToFile(tableTemplate.getAbsolutePath(),
                                tableTemplateBuild.getAbsolutePath());
                    }
                }
            }
        }

        return new Output(tableList);
    }

    private void generateDynamicReport(final String[] jsonColumns, final Style detailStyle, final Style headerStyle,
                                       final Style oddRowStyle, final File tableTemplate) throws JRException {
        DynamicReportBuilder drb = new DynamicReportBuilder();
        drb.setMargins(0, 0, 0, 0);
        drb.setPrintBackgroundOnOddRows(true);
        drb.setOddRowBackgroundStyle(oddRowStyle);
        int width = dynamicReportOptInt("tableWidth", DEFAULT_TABLE_WIDTH)
                    / jsonColumns.length;
        for (String column : jsonColumns) {
            drb.addColumn(ColumnBuilder.getNew()
                    .setColumnProperty(column, String.class.getName())
                    .setStyle(detailStyle).setHeaderStyle(headerStyle)
                    .setWidth(width).setTitle(column).build());
        }
        DynamicReport dr = drb.build();
        DynamicJasperHelper.generateJRXML(dr, new ClassicLayoutManager(), null,
                "UTF-8", tableTemplate.getAbsolutePath());
    }

    private void createColumnDataSource(final TableListAttributeValue jsonTable, final List<Values> tableList) {
        final String[] jsonColumns = jsonTable.columns;
        final PJsonArray[] jsonData = jsonTable.data;
        final List<Map<String, ?>> table = new ArrayList<Map<String, ?>>();

        Map<String, Object> tableValues = new HashMap<String, Object>();
        tableValues.put(COLUMN_NAME, jsonTable.id);
        tableValues.put(COLUMN_DISPLAYNAME, jsonTable.getDisplayName());

        for (PJsonArray jsonRow : jsonData) {
            final Map<String, String> row = new HashMap<String, String>();
            for (int j = 0; j < jsonRow.size(); j++) {
                row.put(jsonColumns[j], jsonRow.getString(j));
            }
            table.add(row);
        }

        tableValues.put(COLUMN_DATASOURCE, new JRMapCollectionDataSource(table));
        tableList.add(new Values(tableValues));
    }

    private String dynamicReportOptString(final String key, final String defaultValue) {
        if (this.dynamicReport.containsKey(key)) {
            return this.dynamicReport.get(key).toString();
        } else {
            return defaultValue;
        }
    }

    private int dynamicReportOptInt(final String key, final int defaultValue) {
        if (this.dynamicReport.containsKey(key)) {
            return ((Number) this.dynamicReport.get(key)).intValue();
        } else {
            return defaultValue;
        }
    }

    public final void setDynamicReport(final Map<String, Object> dynamicReport) {
        this.dynamicReport = dynamicReport;
    }

    @Override
    protected final void extraValidation(final List<Throwable> validationErrors) {
        // no validation needed
    }

    public final void setDynamicReportDirectory(final String dynamicReportDirectory) {
        this.dynamicReportDirectory = new File(this.configuration.getDirectory(), dynamicReportDirectory);
    }
    
    @Override
    public final void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Input of processor.
     */
    public static final class Input {
        /**
         * Data for constructing the table list of values.
         */
        public TableListAttributeValue[] tableList;
        
        /**
         * The path to the temporary directory for the print task.
         */
        public File tempTaskDirectory;
    }

    /**
     * Output of processor.
     */
    public static final class Output {
        /**
         * Resulting list of values for the table in the jasper report.
         */
        public final List<Values> tableList;

        private Output(final List<Values> tableList) {
            this.tableList = tableList;
        }
    }
}
