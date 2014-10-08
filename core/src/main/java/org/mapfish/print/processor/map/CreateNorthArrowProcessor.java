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

package org.mapfish.print.processor.map;

import com.google.common.collect.Lists;
import net.sf.jasperreports.engine.JRException;
import org.mapfish.print.attribute.NorthArrowAttribute;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.InternalValue;
import org.mapfish.print.processor.jasper.JasperReportBuilder;
import org.mapfish.print.processor.jasper.MapSubReport;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;


/**
 * Processor to create a north-arrow for a map.
 *
 * <p>The north-arrow is rotated according to the rotation
 * of the associated map.</p>
 *
 * <p>Example configuration:</p>
 *
 * <pre><code>
 * attributes:
 *   ...
 *   northArrowDef: !northArrow
 *     size: 50
 *     default:
 *       graphic: "file://NorthArrow_10.svg"
 *
 * processors:
 * ...
 *   - !createNorthArrow
 *     inputMapper: {
 *       mapDef: map,
 *       northArrowDef: northArrow
 *     }
 *     outputMapper: {
 *       subReport: northArrowSubReport
 *     }
 * </code></pre>
 */
public class CreateNorthArrowProcessor extends AbstractProcessor<CreateNorthArrowProcessor.Input, CreateNorthArrowProcessor.Output> {

    /**
     * Constructor.
     */
    protected CreateNorthArrowProcessor() {
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

        final double dpiRatio = values.map.getDpi() / values.map.getRequestorDPI();
        final Dimension size = new Dimension(
                (int) (values.northArrow.getSize().getWidth() * dpiRatio),
                (int) (values.northArrow.getSize().getHeight() * dpiRatio));

        final URI northArrowGraphicFile = NorthArrowGraphic.create(
                size,
                values.northArrow.getGraphic(),
                values.northArrow.getBackgroundColor(),
                values.map.getRotation(),
                values.tempTaskDirectory,
                values.clientHttpRequestFactory);

        checkCancelState(context);
        final URI scalebarSubReport = createNorthArrowSubReport(
                values.tempTaskDirectory, values.northArrow.getSize(),
                Lists.newArrayList(northArrowGraphicFile), values.map.getDpi());

        return new Output(northArrowGraphicFile, scalebarSubReport.toString());
    }

    private URI createNorthArrowSubReport(final File printDirectory,
                                   final Dimension size,
                                   final List<URI> graphics,
                                   final double dpi) throws IOException, JRException {
        final MapSubReport subReport = new MapSubReport(graphics, size, dpi);

        final File compiledReport = File.createTempFile("north-arrow-report-",
                JasperReportBuilder.JASPER_REPORT_COMPILED_FILE_EXT, printDirectory);
        subReport.compile(compiledReport);

        return compiledReport.toURI();
    }

    /**
     * Input for the processor.
     */
    public static class Input {
        /**
         * The map the north arrow is created for.
         */
        public MapAttribute.MapAttributeValues map;

        /**
         * The parameters for the north arrow.
         */
        public NorthArrowAttribute.NorthArrowAttributeValues northArrow;

        /**
         * The path to the temporary directory for the print task.
         */
        public File tempTaskDirectory;

        /**
         * The factory to use for making http requests.
         */
        public MfClientHttpRequestFactory clientHttpRequestFactory;
    }

    /**
     * Output for the processor.
     */
    public static final class Output {

        /**
         * The path to the north arrow graphic (for testing purposes).
         */
        @InternalValue
        public final URI graphic;

        /**
         * The path to the compiled sub-report for the north arrow.
         */
        public final String subReport;

        private Output(final URI graphic, final String subReport) {
            this.graphic = graphic;
            this.subReport = subReport;
        }
    }
}
