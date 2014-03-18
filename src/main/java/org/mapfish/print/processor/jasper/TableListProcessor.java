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
import net.sf.jasperreports.engine.JasperCompileManager;

import org.mapfish.print.json.PJsonArray;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.output.Values;
import org.mapfish.print.processor.AbstractProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Processor for creating a table.
 * 
 * @author Jesse
 */
public class TableListProcessor extends AbstractProcessor {
    private static final int DEFAULT_TABLE_WIDTH = 500;

	private static final int DEFAULT_BLUE = 230;

	private static final int DEFAULT_GREEN = 230;

	private static final int DEFAULT_RED = 230;

	private static final String DEFAULT_FONT = "DejaVu Sans";

	private static final int DEFAULT_TITLE_FONT_SIZE = 14;

	private static final int DEFAULT_FONT_SIZE = 12;

	private static final Logger LOGGER = LoggerFactory.getLogger(JasperReportBuilder.class);

    private String tableListRef;
    private String dynamicReportDirectory;
    private Map<String, Object> dynamicReport = null;

    @Override
	public final Map<String, Object> execute(final Values values) throws Exception {
        final Map<String, Object> output = new HashMap<String, Object>();
        final PJsonObject jsonTableList = values.getObject(this.tableListRef, PJsonObject.class);
        final List<Values> tableList = new ArrayList<Values>();

        if (jsonTableList != null) {
            final Iterator<String> iterTL = jsonTableList.keys();
            while (iterTL.hasNext()) {
                final String key = iterTL.next();
                final PJsonObject jsonTable = jsonTableList.getJSONObject(key);
                final PJsonArray jsonColumns = jsonTable.getJSONArray("columns");
                final PJsonArray jsonData = jsonTable.getJSONArray("data");
                final List<Map<String, String>> table = new ArrayList<Map<String, String>>();

                Map<String, Object> tableValues = new HashMap<String, Object>();
                tableValues.put("name", key);
                tableValues.put("displayName", jsonTable.optString("displayName", key));

                for (int i = 0; i < jsonData.size(); i++) {
                    final PJsonArray jsonRow = jsonData.getJSONArray(i);
                    final Map<String, String> row = new HashMap<String, String>();
                    for (int j = 0; j < jsonRow.size(); j++) {
                        row.put(jsonColumns.getString(j), jsonRow.getString(j));
                    }
                    table.add(row);
                }

                tableValues.put("table", table);
                tableList.add(new Values(tableValues));

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

                    File tableTempate = new File(this.dynamicReportDirectory, "table_" + key + ".jrxml");
                    if (!tableTempate.exists()) {
                        DynamicReportBuilder drb = new DynamicReportBuilder();
                        drb.setMargins(0, 0, 0, 0);
                        drb.setPrintBackgroundOnOddRows(true);
                        drb.setOddRowBackgroundStyle(oddRowStyle);
                        int width = dynamicReportOptInt("tableWidth", DEFAULT_TABLE_WIDTH)
                                    / jsonColumns.size();
                        for (int i = 0; i < jsonColumns.size(); i++) {
                            String column = jsonColumns.getString(i);
                            drb.addColumn(ColumnBuilder.getNew()
                                    .setColumnProperty(column, String.class.getName())
                                    .setStyle(detailStyle).setHeaderStyle(headerStyle)
                                    .setWidth(width).setTitle(column).build());
                        }
                        DynamicReport dr = drb.build();
                        DynamicJasperHelper.generateJRXML(dr, new ClassicLayoutManager(), null,
                                "UTF-8", tableTempate.getAbsolutePath());
                    }
                    File tableTempateBuild = new File(this.dynamicReportDirectory, "table_" + key
                                                                              + ".jasper");
                    if (!tableTempateBuild.exists()
                        || (tableTempate.lastModified() > tableTempateBuild.lastModified())) {
                        LOGGER.info("Building Jasper sub report: " + tableTempate.getAbsolutePath());
                        long start = System.currentTimeMillis();
                        JasperCompileManager.compileReportToFile(tableTempate.getAbsolutePath(),
                                tableTempateBuild.getAbsolutePath());
                        LOGGER.info("Report built in " + (System.currentTimeMillis() - start)
                                    + "ms.");
                    }
                }
            }
        }

        output.put("tableList", tableList);

        return output;
    }

    public final String getTableListRef() {
        return this.tableListRef;
    }

    public final void setTableListRef(final String tableListRef) {
        this.tableListRef = tableListRef;
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

    public final Map<String, Object> getDynamicReport() {
        return this.dynamicReport;
    }

    public final void setDynamicReport(final Map<String, Object> dynamicReport) {
        this.dynamicReport = dynamicReport;
    }

    public final String getDynamicReportDirectory() {
        return this.dynamicReportDirectory;
    }

    public final void setDynamicReportDirectory(final String dynamicReportDirectory) {
        this.dynamicReportDirectory = dynamicReportDirectory;
    }
}
