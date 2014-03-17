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

public class TableListProcessor extends AbstractProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(JasperReportBuilder.class);

    private String tableListRef;
    private String dynamicReportDirectory;
    private Map<String, Object> dynamicReport = null;

    @Override
    public Map<String, Object> execute(Values values) throws Exception {
        final Map<String, Object> output = new HashMap<String, Object>();
        final PJsonObject jsonTableList = (PJsonObject) values.getObject(tableListRef);
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

                if (dynamicReport != null) {
                    Style detailStyle = new Style();
                    detailStyle.setFont(new Font(dynamicReportOptInt("fontSize", 12),
                            dynamicReportOptString("font", "DejaVu Sans"), false));
                    detailStyle.setVerticalAlign(VerticalAlign.MIDDLE);
                    Style headerStyle = new Style();
                    headerStyle.setFont(new Font(dynamicReportOptInt("titleFontSize", 14),
                            dynamicReportOptString("titleFont", "DejaVu Sans"), true));
                    headerStyle.setHorizontalAlign(HorizontalAlign.CENTER);
                    headerStyle.setVerticalAlign(VerticalAlign.MIDDLE);
                    headerStyle.setBorderBottom(Border.PEN_1_POINT());
                    Style oddRowStyle = new Style();
                    oddRowStyle.setBackgroundColor(new Color(230, 230, 230));
                    oddRowStyle.setFont(new Font(dynamicReportOptInt("fontSize", 12),
                            dynamicReportOptString("font", "DejaVu Sans"), false));
                    oddRowStyle.setVerticalAlign(VerticalAlign.MIDDLE);

                    File tableTempate = new File(dynamicReportDirectory, "table_" + key + ".jrxml");
                    if (!tableTempate.exists()) {
                        DynamicReportBuilder drb = new DynamicReportBuilder();
                        drb.setMargins(0, 0, 0, 0);
                        drb.setPrintBackgroundOnOddRows(true);
                        drb.setOddRowBackgroundStyle(oddRowStyle);
                        int width = dynamicReportOptInt("tableWidth", 500)
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
                    File tableTempateBuild = new File(dynamicReportDirectory, "table_" + key
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

    public String getTableListRef() {
        return tableListRef;
    }

    public void setTableListRef(String tableListRef) {
        this.tableListRef = tableListRef;
    }

    private String dynamicReportOptString(String key, String defaultValue) {
        if (dynamicReport.containsKey(key)) {
            return dynamicReport.get(key).toString();
        } else {
            return defaultValue;
        }
    }

    private int dynamicReportOptInt(String key, int defaultValue) {
        if (dynamicReport.containsKey(key)) {
            return ((Number) dynamicReport.get(key)).intValue();
        } else {
            return defaultValue;
        }
    }

    public Map<String, Object> getDynamicReport() {
        return dynamicReport;
    }

    public void setDynamicReport(Map<String, Object> dynamicReport) {
        this.dynamicReport = dynamicReport;
    }

    public String getDynamicReportDirectory() {
        return dynamicReportDirectory;
    }

    public void setDynamicReportDirectory(String dynamicReportDirectory) {
        this.dynamicReportDirectory = dynamicReportDirectory;
    }
}
