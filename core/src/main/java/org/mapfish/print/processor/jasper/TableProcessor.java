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
import ar.com.fdvs.dj.core.layout.LayoutManager;
import ar.com.fdvs.dj.domain.DynamicReport;
import ar.com.fdvs.dj.domain.Style;
import ar.com.fdvs.dj.domain.builders.ColumnBuilder;
import ar.com.fdvs.dj.domain.builders.FastReportBuilder;
import ar.com.fdvs.dj.domain.constants.Border;
import ar.com.fdvs.dj.domain.constants.Font;
import ar.com.fdvs.dj.domain.constants.HorizontalAlign;
import ar.com.fdvs.dj.domain.constants.ImageScaleMode;
import ar.com.fdvs.dj.domain.constants.Transparency;
import com.google.common.collect.Maps;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.mapfish.print.Constants;
import org.mapfish.print.PrintException;
import org.mapfish.print.attribute.TableAttribute.TableAttributeValue;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.InternalValue;
import org.mapfish.print.wrapper.PArray;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.Color;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mapfish.print.processor.jasper.JasperReportBuilder.JASPER_REPORT_COMPILED_FILE_EXT;
import static org.mapfish.print.processor.jasper.JasperReportBuilder.JASPER_REPORT_XML_FILE_EXT;

/**
 * A processor for generating a table.
 *
 * @author Jesse
 * @author sbrunner
 */
public final class TableProcessor extends AbstractProcessor<TableProcessor.Input, TableProcessor.Output> {

    private Map<String, TableColumnConverter> columnConverterMap = Maps.newHashMap();
    private boolean dynamic = false;
    private int width;
    @Autowired
    private JasperReportBuilder jasperReportBuilder;

    /**
     * Constructor.
     */
    protected TableProcessor() {
        super(Output.class);
    }

    /**
     * The width of the generated sub-report, only required if dynamic is true.
     *
     * @param width width of the sub-report
     */
    public void setWidth(final int width) {
        this.width = width;
    }

    /**
     * If true then the Jasper Report Template will be generated dynamically based on the columns in the table attribute.
     * <p>By default this is false because width is required if it is true</p>
     *
     * @param dynamic indicate if the template should be dynamically generated for each print request.
     */
    public void setDynamic(final boolean dynamic) {
        this.dynamic = dynamic;
    }

    /**
     * Set strategies for converting the textual representation of each column to some other object (image, other text, etc...).
     * <p/>
     * Note: The type returned by the column converter must match the type in the jasper template.
     *
     * @param columnConverters Map from column name -> {@link TableColumnConverter}
     */
    public void setColumns(final Map<String, TableColumnConverter> columnConverters) {
        this.columnConverterMap = columnConverters;
    }

    @Override
    public Input createInputParameter() {
        return new Input();
    }

    @Override
    public Output execute(final Input values, final ExecutionContext context) throws Exception {
        final TableAttributeValue jsonTable = values.table;
        final Collection<Map<String, ?>> table = new ArrayList<Map<String, ?>>();

        final String[] columnNames = jsonTable.columns;

        Map<String, Class<?>> columns = Maps.newHashMap();
        final PArray[] jsonData = jsonTable.data;
        for (final PArray jsonRow : jsonData) {
            checkCancelState(context);
            final Map<String, Object> row = new HashMap<String, Object>();
            for (int j = 0; j < jsonRow.size(); j++) {
                final String columnName = columnNames[j];
                Object rowValue = jsonRow.get(j);
                TableColumnConverter converter = this.columnConverterMap.get(columnName);
                if (converter != null) {
                    rowValue = converter.resolve(values.clientHttpRequestFactory, (String) rowValue);
                }
                Class<?> columnDef = columns.get(columnName);
                if (columnDef == null) {
                    Class<?> rowValueClass = null;
                    if (rowValue != null) {
                        rowValueClass = rowValue.getClass();
                    }
                    columns.put(columnName, rowValueClass);
                }
                row.put(columnName, rowValue);
            }
            table.add(row);
        }

        String subreport = null;
        if (this.dynamic) {
            subreport = generateSubReport(values, columns);
        }
        final JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(table);
        return new Output(dataSource, table.size(), subreport);
    }

    private String generateSubReport(
            final Input input,
            final Map<String, Class<?>> columns) throws JRException, ClassNotFoundException, IOException {
        FastReportBuilder reportBuilder = new FastReportBuilder();
        final int columnWidth = this.width / columns.size();
        Style detailStyle = createDetailStyle();
        Style headerStyle = createHeaderStyle();
        Style oddRowStyle = createOddRowStyle();
        reportBuilder.setOddRowBackgroundStyle(oddRowStyle);
        reportBuilder.setPrintBackgroundOnOddRows(true);
        for (Map.Entry<String, Class<?>> entry : columns.entrySet()) {
            String columnName = entry.getKey();
            Class<?> valueClass = String.class;
            if (entry.getValue() != null) {
                valueClass = entry.getValue();
            }
            ColumnBuilder column = ColumnBuilder.getNew()
                    .setColumnProperty(columnName, valueClass)
                    .setTitle(columnName)
                    .setWidth(columnWidth)
                    .setStyle(detailStyle)
                    .setHeaderStyle(headerStyle)
                    .setFixedWidth(false);
            if (RenderedImage.class.isAssignableFrom(valueClass)) {
                column.setColumnType(ColumnBuilder.COLUMN_TYPE_IMAGE)
                        .setImageScaleMode(ImageScaleMode.FILL_PROPORTIONALLY);
            }
            reportBuilder.addColumn(column.build());
        }
        DynamicReport dr = reportBuilder
                .setPrintBackgroundOnOddRows(true)
                .setUseFullPageWidth(true)
                .build();

        final File jrxmlFile = File.createTempFile("table-", JASPER_REPORT_XML_FILE_EXT, input.tempTaskDirectory);
        LayoutManager layoutManager = new ClassicLayoutManager();
        Map params = Maps.newHashMap();
        DynamicJasperHelper.generateJRXML(dr, layoutManager, params, Constants.DEFAULT_ENCODING, jrxmlFile.getAbsolutePath());

        final File buildFile = File.createTempFile("table-", JASPER_REPORT_COMPILED_FILE_EXT, input.tempTaskDirectory);
        if (!buildFile.delete()) {
            throw new PrintException("Unable to delete the build file: " + buildFile);
        }
        return this.jasperReportBuilder.compileJasperReport(buildFile, jrxmlFile).getAbsolutePath();
    }

    private Style createOddRowStyle() {
        // CSOFF: MagicNumber
        Style oddRowStyle = new Style();
        oddRowStyle.setBorder(Border.NO_BORDER());
        Color veryLightGrey = new Color(230, 230, 230);
        // CSON: MagicNumber
        oddRowStyle.setBackgroundColor(veryLightGrey);
        oddRowStyle.setTransparency(Transparency.OPAQUE);
        return oddRowStyle;
    }

    private Style createHeaderStyle() {
        // CSOFF: MagicNumber
        Style titleStyle = new Style();
        Font font = new Font();
        font.setFontName("DejaVu Sans");
        font.setBold(true);
        titleStyle.setFont(font);
        titleStyle.setBorderBottom(Border.PEN_1_POINT());
        titleStyle.setHorizontalAlign(HorizontalAlign.LEFT);
        // CSON: MagicNumber
        return titleStyle;
    }

    private Style createDetailStyle() {
        final Style style = new Style();
        style.setHorizontalAlign(HorizontalAlign.LEFT);
        Font font = new Font();
        font.setFontName("DejaVu Sans");
        style.setFont(font);
        return style;
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors) {
        if (this.dynamic && this.width < 1) {
            validationErrors.add(new ConfigurationException("Size must be set if !tableProcessor is dynamic."));
        }
    }

    /**
     * Input object for execute.
     */
    public static final class Input {
        /**
         * A factory for making http requests.  This is added to the values by the framework and therefore
         * does not need to be set in configuration
         */
        @InternalValue
        public MfClientHttpRequestFactory clientHttpRequestFactory;
        /**
         * The directory to write the generated table to (if dynamic).
         */
        @InternalValue
        public File tempTaskDirectory;
        /**
         * Data for constructing the table Datasource.
         */
        public TableAttributeValue table;
    }

    /**
     * The Output of the processor.
     */
    public static final class Output {
        /**
         * The table datasource.
         */
        public final JRMapCollectionDataSource table;

        /**
         * The number of rows in the table.
         */
        public final int numberOfTableRows;

        /**
         * The path to the generated sub-report.  If dynamic is false then this will be null.
         */
        public final String tableSubReport;

        private Output(final JRMapCollectionDataSource dataSource,
                       final int numberOfTableRows,
                       final String subReport) {
            this.table = dataSource;
            this.numberOfTableRows = numberOfTableRows;
            this.tableSubReport = subReport;
        }
    }
}
