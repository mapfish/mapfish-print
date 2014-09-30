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

package org.mapfish.print.processor.map.scalebar;

import com.google.common.collect.Lists;
import net.sf.jasperreports.engine.JRException;
import org.mapfish.print.attribute.ScalebarAttribute;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.InternalValue;
import org.mapfish.print.processor.jasper.JasperReportBuilder;
import org.mapfish.print.processor.jasper.MapSubReport;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Processor to create a scalebar for a map.
 */
public class CreateScalebarProcessor extends AbstractProcessor<CreateScalebarProcessor.Input, CreateScalebarProcessor.Output> {

    /**
     * Constructor.
     */
    protected CreateScalebarProcessor() {
        super(Output.class);
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors, final Configuration configuration) {
    }

    @Override
    public final Input createInputParameter() {
        return new Input();
    }

    @Override
    public final Output execute(final Input values, final ExecutionContext context) throws Exception {
        checkCancelState(context);

        final URI scalebarGraphicFile = createScalebarGraphic(values);

        checkCancelState(context);
        final URI scalebarSubReport = createScalebarSubReport(
                values.tempTaskDirectory, values.scalebar.getSize(),
                Lists.newArrayList(scalebarGraphicFile), values.map.getDpi());

        return new Output(scalebarGraphicFile, scalebarSubReport.toString());
    }

    private URI createScalebarGraphic(final Input values) throws IOException, ParserConfigurationException {
        final ScalebarGraphic scalebar = new ScalebarGraphic();
        return scalebar.render(values.map, values.scalebar, values.tempTaskDirectory, values.template);
    }

    private URI createScalebarSubReport(final File printDirectory,
                                   final Dimension size,
                                   final List<URI> graphics,
                                   final double dpi) throws IOException, JRException {
        final MapSubReport subReport = new MapSubReport(graphics, size, dpi);

        final File compiledReport = File.createTempFile("scalebar-report-",
                JasperReportBuilder.JASPER_REPORT_COMPILED_FILE_EXT, printDirectory);
        subReport.compile(compiledReport);

        return compiledReport.toURI();
    }

    /**
     * Input for the processor.
     */
    public static class Input {

        /**
         * The map the scalebar is created for.
         */
        public MapAttribute.MapAttributeValues map;

        /**
         * The parameters for the scalebar.
         */
        public ScalebarAttribute.ScalebarAttributeValues scalebar;

        /**
         * The path to the temporary directory for the print task.
         */
        @InternalValue
        public File tempTaskDirectory;
        /**
         * The containing template.
         */
        @InternalValue
        public Template template;
    }

    /**
     * Output for the processor.
     */
    public static final class Output {

        /**
         * The path to the scalebar graphic (for testing purposes).
         */
        @InternalValue
        public final URI graphic;

        /**
         * The path to the compiled sub-report for the scalebar.
         */
        public final String subReport;

        private Output(final URI graphic, final String subReport) {
            this.graphic = graphic;
            this.subReport = subReport;
        }
    }
}
