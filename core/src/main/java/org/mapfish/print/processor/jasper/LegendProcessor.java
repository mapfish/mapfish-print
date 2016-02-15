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

import com.google.common.collect.Lists;
import com.google.common.io.Closer;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRTableModelDataSource;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.LegendAttribute.LegendAttributeValue;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.InternalValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * <p>Create a legend.</p>
 * <p>See also: <a href="attributes.html#!legend">!legend</a> attribute</p>
 * [[examples=verboseExample,legend_cropped]]
 *
 * @author Jesse
 * @author sbrunner
 */
public final class LegendProcessor extends AbstractProcessor<LegendProcessor.Input, LegendProcessor.Output> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LegendProcessor.class);
    private static final String NAME_COLUMN = "name";
    private static final String ICON_COLUMN = "icon";
    private static final String REPORT_COLUMN = "report";
    private static final String LEVEL_COLUMN = "level";
    @Autowired
    private JasperReportBuilder jasperReportBuilder;

    // CSOFF:MagicNumber
    private Dimension missingImageSize = new Dimension(24, 24);
    // CSON:MagicNumber
    private BufferedImage missingImage;
    private Color missingImageColor = Color.PINK;
    private String template;
    private Integer maxWidth = null;
    private Double dpi = Constants.PDF_DPI;

    /**
     * Constructor.
     */
    protected LegendProcessor() {
        super(Output.class);
    }

    /**
     * The path to the Jasper Report template for rendering the legend data.
     *
     * @param template path to the template file
     */
    public void setTemplate(final String template) {
        this.template = template;
    }

    /**
     * The maximum width in pixels for the legend graphics.
     * If this parameter is set, the legend graphics are cropped to the given maximum
     * width. In this case a sub-report is created containing the graphic.
     * For reference see the example `legend_dynamic`.
     *
     * @param maxWidth The max. width.
     */
    public void setMaxWidth(final Integer maxWidth) {
        this.maxWidth = maxWidth;
    }

    /**
     * The DPI value that is used for the legend graphics.
     * Note: This parameter is only considered when `maxWidth` is set.
     *
     * @param dpi The DPI value.
     */
    public void setDpi(final Double dpi) {
        this.dpi = dpi;
    }

    @Override
    public Input createInputParameter() {
        return new Input();
    }

    @Override
    public Output execute(final Input values, final ExecutionContext context) throws Exception {
        final List<Object[]> legendList = new ArrayList<Object[]>();
        final String[] legendColumns = {NAME_COLUMN, ICON_COLUMN, REPORT_COLUMN, LEVEL_COLUMN};
        final LegendAttributeValue legendAttributes = values.legend;
        fillLegend(values.clientHttpRequestFactory, legendAttributes, legendList, 0, context, values.tempTaskDirectory);
        final Object[][] legend = new Object[legendList.size()][];

        final JRTableModelDataSource dataSource = new JRTableModelDataSource(new TableDataSource(legendColumns,
                legendList.toArray(legend)));

        String compiledTemplatePath = compileTemplate(values.template.getConfiguration());

        return new Output(dataSource, legendList.size(), compiledTemplatePath);
    }

    private String compileTemplate(final Configuration configuration) throws JRException {
        if (this.template != null) {
            final File file = new File(configuration.getDirectory(), this.template);
            return this.jasperReportBuilder.compileJasperReport(configuration, file).getAbsolutePath();
        }
        return null;
    }

    private void fillLegend(final MfClientHttpRequestFactory clientHttpRequestFactory,
                            final LegendAttributeValue legendAttributes,
                            final List<Object[]> legendList,
                            final int level,
                            final ExecutionContext context,
                            final File tempTaskDirectory) throws IOException, URISyntaxException, JRException {
        int insertNameIndex = legendList.size();
        final URL[] icons = legendAttributes.icons;
        Closer closer = Closer.create();
        if (icons != null) {
            for (URL icon : icons) {
                BufferedImage image = null;
                try {
                    checkCancelState(context);
                    final ClientHttpRequest request = clientHttpRequestFactory.createRequest(icon.toURI(), HttpMethod.GET);
                    final ClientHttpResponse httpResponse = closer.register(request.execute());
                    if (httpResponse.getStatusCode() == HttpStatus.OK) {
                        image = ImageIO.read(httpResponse.getBody());
                        if (image == null) {
                            LOGGER.warn("The URL: " + icon + " is NOT an image format that can be decoded");
                        }
                    } else {
                        LOGGER.warn("Failed to load image from: " + icon + " due to server side error.\n\tResponse Code: " +
                                    httpResponse.getStatusCode() + "\n\tResponse Text: " + httpResponse.getStatusText());
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to load image from: " + icon, e);
                } finally {
                    closer.close();
                }

                if (image == null) {
                    image = this.getMissingImage();
                }

                String report = null;
                if (this.maxWidth != null) {
                    // if a max width is given, create a sub-report containing the cropped graphic
                    report = createSubReport(image, tempTaskDirectory).toString();
                }
                final Object[] iconRow = {null, image, report, level};
                legendList.add(iconRow);
            }
        }

        if (legendAttributes.classes != null) {
            for (LegendAttributeValue value : legendAttributes.classes) {
                fillLegend(clientHttpRequestFactory, value, legendList, level + 1, context, tempTaskDirectory);
            }
        }

        if (!legendList.isEmpty()) {
            legendList.add(insertNameIndex, new Object[]{legendAttributes.name, null, null, level});
        }
    }

    private URI createSubReport(final BufferedImage originalImage,
                                final File tempTaskDirectory) throws IOException, JRException {
        assert (this.maxWidth != null);

        double scaleFactor = getScaleFactor();
        BufferedImage image = originalImage;
        if (image.getWidth() * scaleFactor > this.maxWidth) {
            image = cropToMaxWidth(image, scaleFactor);
        }

        URI imageFile = writeToFile(image, tempTaskDirectory);

        final ImagesSubReport subReport = new ImagesSubReport(
                Lists.newArrayList(imageFile),
                new Dimension((int) (image.getWidth() * scaleFactor), (int) (image.getHeight() * scaleFactor)),
                this.dpi);

        final File compiledReport = File.createTempFile("legend-report-",
                JasperReportBuilder.JASPER_REPORT_COMPILED_FILE_EXT, tempTaskDirectory);
        subReport.compile(compiledReport);

        return compiledReport.toURI();
    }

    private BufferedImage cropToMaxWidth(final BufferedImage image, final double scaleFactor) {
        int width = (int) Math.round(this.maxWidth / scaleFactor);
        return image.getSubimage(0, 0, width, image.getHeight());
    }

    private double getScaleFactor() {
        return Constants.PDF_DPI / this.dpi;
    }

    private URI writeToFile(final BufferedImage image, final File tempTaskDirectory) throws IOException {
        File path = File.createTempFile("legend-", ".png", tempTaskDirectory);
        ImageIO.write(image, "png", path);
        return path.toURI();
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors, final Configuration configuration) {
        // no checks needed
    }

    private synchronized BufferedImage getMissingImage() {
        if (this.missingImage == null) {
            this.missingImage = new BufferedImage(this.missingImageSize.width, this.missingImageSize.height, BufferedImage.TYPE_INT_RGB);
            final Graphics2D graphics = this.missingImage.createGraphics();

            try {
                graphics.setBackground(this.missingImageColor);
                graphics.clearRect(0, 0, this.missingImageSize.width, this.missingImageSize.height);
            } finally {
                graphics.dispose();
            }
        }
        return this.missingImage;
    }

    /**
     * The Input Parameter object for {@link org.mapfish.print.processor.jasper.LegendProcessor}.
     */
    public static final class Input {
        /**
         * The template that contains this processor.
         */
        @InternalValue
        public Template template;
        /**
         * A factory for making http requests.  This is added to the values by the framework and therefore
         * does not need to be set in configuration
         */
        @InternalValue
        public MfClientHttpRequestFactory clientHttpRequestFactory;
        /**
         * The path to the temporary directory for the print task.
         */
        @InternalValue
        public File tempTaskDirectory;
        /**
         * The data required for creating the legend.
         */
        public LegendAttributeValue legend;
    }

    /**
     * The Output object of the legend processor method.
     */
    public static final class Output {
        /**
         * The datasource for the legend object in the report.
         */
        public final JRTableModelDataSource legend;
        /**
         * The path to the compiled subreport.
         */
        public final String legendSubReport;
        /**
         * The number of rows in the legend.
         */
        public final int numberOfLegendRows;

        Output(final JRTableModelDataSource legend, final int numberOfLegendRows, final String legendSubReport) {
            this.legend = legend;
            this.numberOfLegendRows = numberOfLegendRows;
            this.legendSubReport = legendSubReport;
        }
    }
}
