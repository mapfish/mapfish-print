package org.mapfish.print.processor.jasper;

import net.sf.jasperreports.engine.JRBand;
import net.sf.jasperreports.engine.JRElement;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRStyle;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignElement;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.HorizontalImageAlignEnum;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.ScaleImageEnum;
import net.sf.jasperreports.engine.type.StretchTypeEnum;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.engine.xml.JRXmlWriter;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.mapfish.print.Constants;
import org.mapfish.print.PrintException;
import org.mapfish.print.attribute.TableAttribute.TableAttributeValue;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationException;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.http.MfClientHttpRequestFactoryProvider;
import org.mapfish.print.wrapper.PArray;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mapfish.print.processor.jasper.JasperReportBuilder.JASPER_REPORT_COMPILED_FILE_EXT;
import static org.mapfish.print.processor.jasper.JasperReportBuilder.JASPER_REPORT_XML_FILE_EXT;

/**
 * <p>A processor for generating a table.</p>
 * <p>See also: <a href="attributes.html#!table">!table</a> attribute</p>
 * [[examples=verboseExample,datasource_dynamic_tables,customDynamicReport]]
 */
public final class TableProcessor extends AbstractProcessor<TableProcessor.Input, TableProcessor.Output> {

    private static final int SPACE_BETWEEN_COLS = 0;
    private static final int DEFAULT_MAX_COLUMNS = 9;
    private Map<String, TableColumnConverter<?>> columnConverterMap = new HashMap<>();
    private List<TableColumnConverter<?>> converters = new ArrayList<>();
    private boolean dynamic = false;
    private Integer reportWidth = null;
    private String jasperTemplate = null;
    private String firstHeaderStyle;
    private String lastHeaderStyle;
    private String headerStyle;
    private String firstDetailStyle;
    private String lastDetailStyle;
    private String detailStyle;
    private int maxColumns = DEFAULT_MAX_COLUMNS;
    private Set<String> excludeColumns = new HashSet<>();

    @Autowired
    private JasperReportBuilder jasperReportBuilder;
    private boolean defaultTemplate;

    /**
     * Constructor.
     */
    protected TableProcessor() {
        super(Output.class);
    }

    /**
     * The path to the JasperReports template that contains the template for the sub-report. If dynamic is
     * false then the template will be used without any changes. It will simply be compiled and used as is.
     * <p>
     * If dynamic is true then the template will be used to obtain the column styles and the size of the
     * subreport and to get the position of the first header and field element. The actual field and column
     * definitions will be dynamically generated from the table data that is provided.
     * </p>
     * This may be null if dynamic is false.  If it is null then the main template will likely use the
     * generated table datasource directly as its datasource for use in its detail section and the table will
     * be directly in the main template's detail section.  Or a later processor may use the table's datasource
     * in someway.
     *
     * @param jasperTemplate the template to use for rendering the table.
     */
    public void setJasperTemplate(final String jasperTemplate) {
        this.jasperTemplate = jasperTemplate;
    }

    /**
     * If true then the JasperReport template will be generated dynamically based on the columns in the table
     * attribute.
     * <p>Default: false</p>
     *
     * @param dynamic indicate if the template should be dynamically generated for each print
     *         request.
     */
    public void setDynamic(final boolean dynamic) {
        this.dynamic = dynamic;
    }

    /**
     * If dynamic is true, the page width of the table report can be adjusted with this property.
     *
     * @param reportWidth The report width to use.
     */
    public void setReportWidth(final Integer reportWidth) {
        this.reportWidth = reportWidth;
    }

    /**
     * Set strategies for converting the textual representation of each column to some other object (image,
     * other text, etc...).
     *
     * Note: The type returned by the column converter must match the type in the jasper template.
     *
     * @param columnConverters Map from column name -&gt; {@link TableColumnConverter}
     */
    public void setColumns(final Map<String, TableColumnConverter<?>> columnConverters) {
        this.columnConverterMap = columnConverters;
    }

    /**
     * Set strategies for converting the textual representation of each cell to some other object (image,
     * other text, etc...).
     *
     * This is similar to the converters specified for a particular column. The difference is that these
     * converters are applied to every cell of the table (except for the cells of those columns that are
     * assigned a specific converter).
     *
     * @param converters A list of {@link TableColumnConverter}s.
     */
    public void setConverters(final List<TableColumnConverter<?>> converters) {
        this.converters = converters;
    }

    /**
     * The id of the style to apply to the first column in the table header.  This is optional.
     * <p>
     * The style must be a style element in the jasperTemplate.
     * </p>
     *
     * @param firstHeaderStyle a ref to a style in the japserTemplate
     */
    public void setFirstHeaderStyle(final String firstHeaderStyle) {
        this.firstHeaderStyle = firstHeaderStyle;
    }

    /**
     * The id of the style to apply to the last column in the table header.  This is optional.
     * <p>
     * The style must be a style element in the jasperTemplate.
     * </p>
     *
     * @param lastHeaderStyle a ref to a style in the japserTemplate
     */
    public void setLastHeaderStyle(final String lastHeaderStyle) {
        this.lastHeaderStyle = lastHeaderStyle;
    }

    /**
     * The id of the style to apply to the all columns in the table header except first and last columns. This
     * value is will be used as a default if either firstHeaderStyle or lastHeaderStyle is not defined. This
     * is required if dynamic is true and is not permitted if dynamic is false.
     * <p>
     * The style must be a style element in the jasperTemplate.
     * </p>
     *
     * @param headerStyle a ref to a style in the japserTemplate
     */
    public void setHeaderStyle(final String headerStyle) {
        this.headerStyle = headerStyle;
    }

    /**
     * The id of the style to apply to the first column in the table detail section.  This is optional.
     * <p>
     * The style must be a style element in the jasperTemplate.
     * </p>
     *
     * @param firstDetailStyle a ref to a style in the jasperTemplate
     */
    public void setFirstDetailStyle(final String firstDetailStyle) {
        this.firstDetailStyle = firstDetailStyle;
    }

    /**
     * The id of the style to apply to the last column in the table detail section.  This is optional.
     * <p>
     * The style must be a style element in the jasperTemplate.
     * </p>
     *
     * @param lastDetailStyle a ref to a style in the jasperTemplate
     */
    public void setLastDetailStyle(final String lastDetailStyle) {
        this.lastDetailStyle = lastDetailStyle;
    }

    /**
     * The id of the style to apply to the all columns in the table detail section except first and last
     * columns.  This value is will be used as a default if either firstDetailStyle or lastDetailStyle is not
     * defined.  This is required if dynamic is true and is not permitted if dynamic is false.
     * <p>
     * The style must be a style element in the jasperTemplate.
     * </p>
     *
     * @param detailStyle a ref to a style in the japserTemplate
     */
    public void setDetailStyle(final String detailStyle) {
        this.detailStyle = detailStyle;
    }

    /**
     * The maximum number of columns to allow.
     *
     * @param maxColumns maximum number of columns to allow.
     */
    public void setMaxColumns(final int maxColumns) {
        this.maxColumns = maxColumns;
    }

    /**
     * A set of column names to exclude from the table.
     *
     * @param excludeColumns a set of names of the columns to exclude from the table.
     */
    public void setExcludeColumns(final Set<String> excludeColumns) {
        this.excludeColumns = excludeColumns;
    }

    @Override
    public Input createInputParameter() {
        return new Input();
    }

    @Override
    public Output execute(final Input values, final ExecutionContext context) throws Exception {
        final TableAttributeValue jsonTable = values.table;
        final Collection<Map<String, ?>> table = new ArrayList<>();

        final String[] columnNames = jsonTable.columns;

        // this map needs to be linked so it keeps order
        Map<String, Class<?>> columns = new LinkedHashMap<>();
        final PArray[] jsonData = jsonTable.data;
        for (final PArray jsonRow: jsonData) {
            context.stopIfCanceled();
            final Map<String, Object> row = new HashMap<>();
            for (int j = 0; j < jsonRow.size(); j++) {
                final String columnName = columnNames[j];
                Object rowValue = jsonRow.get(j);
                if (rowValue == JSONObject.NULL) {
                    rowValue = null;
                }
                TableColumnConverter<?> converter = this.columnConverterMap.get(columnName);
                if (converter != null) {
                    rowValue = converter
                            .resolve(values.clientHttpRequestFactoryProvider.get(), (String) rowValue);
                } else {
                    rowValue = tryConvert(values.clientHttpRequestFactoryProvider.get(), rowValue);
                }
                if (columns.size() < this.maxColumns && !this.excludeColumns.contains(columnName)) {
                    Class<?> columnDef = columns.get(columnName);
                    if (columnDef == null) {
                        Class<?> rowValueClass = null;
                        if (rowValue != null) {
                            rowValueClass = rowValue.getClass();
                        }
                        columns.put(columnName, rowValueClass);
                    }
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

    /**
     * If converters are set on a table, this function tests if these can convert a cell value. The first
     * converter, which claims that it can convert, will be used to do the conversion.
     */
    private Object tryConvert(
            final MfClientHttpRequestFactory clientHttpRequestFactory,
            final Object rowValue) throws URISyntaxException, IOException {
        if (this.converters.isEmpty()) {
            return rowValue;
        }

        String value = String.valueOf(rowValue);
        for (TableColumnConverter<?> converter: this.converters) {
            if (converter.canConvert(value)) {
                return converter.resolve(clientHttpRequestFactory, value);
            }
        }

        return rowValue;
    }

    private String generateSubReport(
            final Input input,
            final Map<String, Class<?>> columns) throws JRException, IOException {
        byte[] bytes = loadJasperTemplate(input.template.getConfiguration());
        final JasperDesign templateDesign = JRXmlLoader.load(new ByteArrayInputStream(bytes));

        if (this.reportWidth != null) {
            templateDesign.setPageWidth(this.reportWidth);
        }

        int headerHeight = templateDesign.getColumnHeader().getHeight();
        final JRDesignSection detailSection = (JRDesignSection) templateDesign.getDetailSection();
        int detailHeight = detailSection.getBands()[0].getHeight();

        final JRElement sampleHeaderEl = templateDesign.getColumnHeader().getElements()[0];
        int headerPosX = sampleHeaderEl.getX();
        int headerPosY = sampleHeaderEl.getY();
        final JRElement sampleDetailEl = detailSection.getBands()[0].getElements()[0];
        int detailPosX = sampleDetailEl.getX();
        int detailPosY = sampleDetailEl.getY();
        clearFields(templateDesign);
        removeDetailBand(templateDesign);
        JRDesignBand headerBand = new JRDesignBand();
        headerBand.setHeight(headerHeight);
        templateDesign.setColumnHeader(headerBand);

        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(detailHeight);
        detailSection.addBand(detailBand);

        final int columnWidth;
        final int numColumns = columns.size();
        if (columns.isEmpty()) {
            columnWidth = templateDesign.getPageWidth();
        } else {
            columnWidth =
                    (templateDesign.getPageWidth() - (SPACE_BETWEEN_COLS * (numColumns - 1))) / numColumns;
        }

        int i = 0;
        for (Map.Entry<String, Class<?>> entry: columns.entrySet()) {
            i++;

            JRStyle columnDetailStyle;
            JRStyle columnHeaderStyle;
            if (i == 1) {
                columnDetailStyle = getStyle(templateDesign, this.firstDetailStyle, this.detailStyle);
                columnHeaderStyle = getStyle(templateDesign, this.firstHeaderStyle, this.headerStyle);
            } else if (i == numColumns) {
                columnDetailStyle = getStyle(templateDesign, this.lastDetailStyle, this.detailStyle);
                columnHeaderStyle = getStyle(templateDesign, this.lastHeaderStyle, this.headerStyle);
            } else {
                columnDetailStyle = templateDesign.getStylesMap().get(this.detailStyle);
                columnHeaderStyle = templateDesign.getStylesMap().get(this.headerStyle);
            }
            String columnName = entry.getKey();
            Class<?> valueClass = String.class;
            if (entry.getValue() != null) {
                valueClass = entry.getValue();
            }
            // Create a Column Field
            JRDesignField field = new JRDesignField();
            field.setName(columnName);
            if (this.converters.isEmpty()) {
                // if there are no cell converters, the type for all cells in a column should be the same.
                // so we can set a specific type. otherwise we have to set a generic type (Object.class).
                field.setValueClass(valueClass);
            } else {
                field.setValueClass(Object.class);
            }
            templateDesign.addField(field);

            // Add a Header Field to the headerBand
            JRDesignTextField colHeaderField = new JRDesignTextField();
            colHeaderField.setX(headerPosX);
            colHeaderField.setY(headerPosY);
            colHeaderField.setWidth(columnWidth);
            colHeaderField.setHeight(headerHeight);
            colHeaderField.setHorizontalTextAlign(HorizontalTextAlignEnum.LEFT);
            colHeaderField.setStyle(columnHeaderStyle);
            colHeaderField.setStretchWithOverflow(true);
            colHeaderField.setStretchType(StretchTypeEnum.ELEMENT_GROUP_HEIGHT);

            JRDesignExpression headerExpression = new JRDesignExpression();
            headerExpression.setText('"' + columnName + '"');
            colHeaderField.setExpression(headerExpression);
            headerBand.addElement(colHeaderField);

            // Add fields to the detailBand
            if (this.converters.isEmpty()) {
                // if no converters are used, create a field depending on the type
                JRDesignElement designElement;
                if (RenderedImage.class.isAssignableFrom(valueClass)) {
                    designElement = createImageElement(templateDesign, columnName);
                    addElement(detailBand, designElement, detailPosX, detailPosY,
                               columnWidth, detailHeight, columnDetailStyle);
                } else {
                    JRDesignTextField textField = createTextField(columnName);
                    addElement(detailBand, textField, detailPosX, detailPosY,
                               columnWidth, detailHeight, columnDetailStyle);
                }
            } else {
                // image element
                JRDesignElement imageElement = createImageElement(templateDesign, columnName);
                // condition: use this element for images
                JRDesignExpression printWhenExpression = new JRDesignExpression();
                printWhenExpression.setText("new Boolean($F{" + columnName +
                                                    "}.getClass().equals(java.awt.image.BufferedImage" +
                                                    ".class))");
                imageElement.setPrintWhenExpression(printWhenExpression);

                addElement(detailBand, imageElement, detailPosX, detailPosY,
                           columnWidth, detailHeight, columnDetailStyle);

                // text field element
                JRDesignTextField textField = createTextField(columnName);
                // condition: use this element for non-images
                printWhenExpression = new JRDesignExpression();
                printWhenExpression.setText("new Boolean(!$F{" + columnName +
                                                    "}.getClass().equals(java.awt.image.BufferedImage" +
                                                    ".class))");
                textField.setPrintWhenExpression(printWhenExpression);

                addElement(detailBand, textField, detailPosX, detailPosY,
                           columnWidth, detailHeight, columnDetailStyle);
            }

            headerPosX = headerPosX + columnWidth + SPACE_BETWEEN_COLS;
            detailPosX = detailPosX + columnWidth + SPACE_BETWEEN_COLS;
        }

        final File jrxmlFile =
                File.createTempFile("table-", JASPER_REPORT_XML_FILE_EXT, input.tempTaskDirectory);
        JRXmlWriter.writeReport(templateDesign, jrxmlFile.getAbsolutePath(), Constants.DEFAULT_ENCODING);

        final File buildFile =
                File.createTempFile("table-", JASPER_REPORT_COMPILED_FILE_EXT, input.tempTaskDirectory);
        if (!buildFile.delete()) {
            throw new PrintException("Unable to delete the build file: " + buildFile);
        }
        return this.jasperReportBuilder.compileJasperReport(buildFile, jrxmlFile).getAbsolutePath();
    }

    private JRDesignTextField createTextField(final String columnName) {
        JRDesignTextField textField = new JRDesignTextField();
        textField.setHorizontalTextAlign(HorizontalTextAlignEnum.LEFT);
        JRDesignExpression expression = new JRDesignExpression();
        expression.setText("$F{" + columnName + "}");
        textField.setExpression(expression);
        textField.setStretchWithOverflow(true);
        return textField;
    }

    private JRDesignElement createImageElement(
            final JasperDesign templateDesign,
            final String columnName) {
        JRDesignImage designImage = new JRDesignImage(templateDesign);
        designImage.setScaleImage(ScaleImageEnum.RETAIN_SHAPE);
        designImage.setHorizontalImageAlign(HorizontalImageAlignEnum.LEFT);
        JRDesignExpression expression = new JRDesignExpression();
        expression.setText("$F{" + columnName + "}");
        designImage.setExpression(expression);
        return designImage;
    }

    private void addElement(
            final JRDesignBand detailBand,
            final JRDesignElement designElement, final int detailPosX, final int detailPosY,
            final int columnWidth, final int detailHeight, final JRStyle columnDetailStyle) {
        designElement.setStretchType(StretchTypeEnum.ELEMENT_GROUP_HEIGHT);
        designElement.setX(detailPosX);
        designElement.setY(detailPosY);
        designElement.setWidth(columnWidth);
        designElement.setHeight(detailHeight);
        designElement.setStyle(columnDetailStyle);
        detailBand.addElement(designElement);
    }

    private void removeDetailBand(final JasperDesign templateDesign) {
        final JRDesignSection detailSection = (JRDesignSection) templateDesign.getDetailSection();
        final List<JRBand> bandsList = new ArrayList<>(detailSection.getBandsList());
        for (JRBand jrBand: bandsList) {
            detailSection.removeBand(jrBand);
        }
    }

    private void clearFields(final JasperDesign templateDesign) {
        final List<JRField> fieldsList = new ArrayList<>(templateDesign.getFieldsList());
        for (JRField jrField: fieldsList) {
            templateDesign.removeField(jrField);
        }
    }

    private JRStyle getStyle(
            final JasperDesign templateDesign,
            final String specificStyle,
            final String defaultStyle) {
        JRStyle columnDetailStyle;
        if (specificStyle != null) {
            columnDetailStyle = templateDesign.getStylesMap().get(specificStyle);
        } else {
            columnDetailStyle = templateDesign.getStylesMap().get(defaultStyle);
        }
        return columnDetailStyle;
    }

    @Override
    protected void extraValidation(
            final List<Throwable> validationErrors, final Configuration configuration) {
        final boolean styleRefDeclared =
                this.firstHeaderStyle != null || this.lastHeaderStyle != null || this.headerStyle != null ||
                        this.firstDetailStyle != null || this.lastDetailStyle != null ||
                        this.detailStyle != null;
        if (styleRefDeclared && this.jasperTemplate == null) {
            validationErrors.add(new ConfigurationException(
                    "if a style is declared a 'jasperTemplate' must also be declared (in !tableProcessor)."));
        }
        if (styleRefDeclared && !this.dynamic) {
            validationErrors.add(new ConfigurationException(
                    "if a style is declared dynamic must be true (in !tableProcessor)."));
        }
        if (this.dynamic) {
            if (this.jasperTemplate == null) {
                try {
                    this.jasperTemplate =
                            TableProcessor.class.getResource("dynamic-table-default.jrxml").toURI()
                                    .toString();
                    this.firstDetailStyle = "column_style_1";
                    this.detailStyle = "column_style_2";
                    this.lastDetailStyle = "column_style_3";
                    this.firstHeaderStyle = "header_style_1";
                    this.headerStyle = "header_style_2";
                    this.lastHeaderStyle = "header_style_3";
                    this.defaultTemplate = true;

                } catch (URISyntaxException e) {
                    throw new Error(e);
                }
            }
            if (this.headerStyle == null) {
                validationErrors.add(new ConfigurationException(
                        "'headerStyle' property must be declared if !tableProcessor is dynamic."));
            }
            if (this.detailStyle == null) {
                validationErrors.add(new ConfigurationException(
                        "'detailStyle' property must be declared if !tableProcessor is dynamic."));
            }

            try {
                byte[] bytes = loadJasperTemplate(configuration);
                final JasperDesign templateDesign = JRXmlLoader.load(new ByteArrayInputStream(bytes));
                final Map<String, JRStyle> stylesMap = templateDesign.getStylesMap();
                if (templateDesign.getColumnHeader() == null) {
                    validationErrors.add(new ConfigurationException(
                            "JasperTemplate must have a column band defined for height and positioning " +
                                    "information"));
                } else if (templateDesign.getColumnHeader().getElements().length == 0) {
                    validationErrors.add(new ConfigurationException(
                            "column header band must have at least one element defined for to height and " +
                                    "positioning information"));
                }

                final JRDesignSection detailSection = (JRDesignSection) templateDesign.getDetailSection();
                if (detailSection.getBands().length == 0) {
                    validationErrors.add(new ConfigurationException(
                            "JasperTemplate must have a detail band defined for height and positioning " +
                                    "information"));
                } else if (detailSection.getBands()[0].getElements().length == 0) {
                    validationErrors.add(new ConfigurationException(
                            "detail band must have at least one element defined for to height and " +
                                    "positioning information"));
                }

                checkStyleExists(validationErrors, stylesMap, this.firstDetailStyle);
                checkStyleExists(validationErrors, stylesMap, this.detailStyle);
                checkStyleExists(validationErrors, stylesMap, this.lastDetailStyle);
                checkStyleExists(validationErrors, stylesMap, this.firstHeaderStyle);
                checkStyleExists(validationErrors, stylesMap, this.headerStyle);
                checkStyleExists(validationErrors, stylesMap, this.lastHeaderStyle);
            } catch (Throwable e) {
                validationErrors.add(e);
            }

        }
    }

    private byte[] loadJasperTemplate(final Configuration configuration) throws IOException {
        if (this.defaultTemplate) {
            try (InputStream is = new URL(this.jasperTemplate).openStream()) {
                return IOUtils.toByteArray(is);
            }
        } else {
            return configuration.loadFile(this.jasperTemplate);
        }
    }

    private void checkStyleExists(
            final List<Throwable> validationErrors,
            final Map<String, JRStyle> stylesMap,
            final String styleRef) {
        if (styleRef != null && !stylesMap.containsKey(styleRef)) {
            validationErrors.add(new ConfigurationException(
                    "No style with id: '" + styleRef + "' exists in " + this.jasperTemplate));
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
        public MfClientHttpRequestFactoryProvider clientHttpRequestFactoryProvider;
        /**
         * The directory to write the generated table to (if dynamic).
         */
        public File tempTaskDirectory;
        /**
         * The template containing this table processor.
         */
        public Template template;
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
        public final JRMapCollectionDataSource tableDataSource;

        /**
         * The number of rows in the table.
         */
        public final int numberOfTableRows;

        /**
         * The path to the generated sub-report.  If dynamic is false then this will be null.
         */
        public final String tableSubReport;

        private Output(
                final JRMapCollectionDataSource dataSource,
                final int numberOfTableRows,
                final String subReport) {
            this.tableDataSource = dataSource;
            this.numberOfTableRows = numberOfTableRows;
            this.tableSubReport = subReport;
        }
    }

}
