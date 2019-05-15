package org.mapfish.print.processor.map;

import net.sf.jasperreports.engine.JRException;
import org.mapfish.print.attribute.NorthArrowAttribute;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.http.MfClientHttpRequestFactoryProvider;
import org.mapfish.print.processor.jasper.ImagesSubReport;
import org.mapfish.print.processor.jasper.JasperReportBuilder;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.mapfish.print.Constants.PDF_DPI;


/**
 * <p>Processor to create a north-arrow for a map.</p>
 *
 * <p>The north-arrow is rotated according to the rotation
 * of the associated map.</p>
 *
 * <p>Example configuration:</p>
 *
 * <pre><code>
 * attributes:
 *   ...
 *   northArrow: !northArrow
 *     size: 50
 *     default:
 *       graphic: "file://NorthArrow_10.svg"
 *
 * processors:
 * ...
 *   - !createNorthArrow {}
 * </code></pre>
 * <p>See also: <a href="attributes.html#!northArrow">!northArrow</a> attribute</p>
 * [[examples=verboseExample,print_osm_new_york_nosubreports]]
 */
public class CreateNorthArrowProcessor
        extends AbstractProcessor<CreateNorthArrowProcessor.Input, CreateNorthArrowProcessor.Output> {

    /**
     * Constructor.
     */
    protected CreateNorthArrowProcessor() {
        super(Output.class);
    }

    @Override
    protected void extraValidation(
            final List<Throwable> validationErrors, final Configuration configuration) {
    }

    @Override
    public final Input createInputParameter() {
        return new Input();
    }

    @Override
    public final Output execute(final Input values, final ExecutionContext context) throws Exception {
        context.stopIfCanceled();

        final double dpiRatio = values.map.getDpi() / PDF_DPI;
        final Dimension size = new Dimension(
                (int) (values.northArrow.getSize().getWidth() * dpiRatio),
                (int) (values.northArrow.getSize().getHeight() * dpiRatio));

        final URI northArrowGraphicFile = NorthArrowGraphic.create(
                size,
                values.northArrow.getGraphic(),
                values.northArrow.getBackgroundColor(),
                values.map.getRotation(),
                values.tempTaskDirectory,
                values.clientHttpRequestFactoryProvider.get(),
                values.template.isAllowTransparency());

        context.stopIfCanceled();

        String strScalebarSubReport = null;
        if (values.northArrow.isCreateSubReport()) {
            final URI scalebarSubReport = createNorthArrowSubReport(
                    values.tempTaskDirectory, values.northArrow.getSize(),
                    Collections.singletonList(northArrowGraphicFile), values.map.getDpi());
            strScalebarSubReport = scalebarSubReport.toString();
        }

        return new Output(northArrowGraphicFile.toString(), strScalebarSubReport);
    }

    private URI createNorthArrowSubReport(
            final File printDirectory,
            final Dimension size,
            final List<URI> graphics,
            final double dpi) throws IOException, JRException {
        final ImagesSubReport subReport = new ImagesSubReport(graphics, size, dpi);

        final File compiledReport = File.createTempFile("north-arrow-report-",
                                                        JasperReportBuilder.JASPER_REPORT_COMPILED_FILE_EXT,
                                                        printDirectory);
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
        public MfClientHttpRequestFactoryProvider clientHttpRequestFactoryProvider;

        /**
         * The containing template.
         */
        public Template template;
    }

    /**
     * Output for the processor.
     */
    public static final class Output {

        /**
         * The path to the north arrow graphic (for testing purposes).
         */
        public final String northArrowGraphic;

        /**
         * The path to the compiled sub-report for the north arrow.
         */
        public final String northArrowSubReport;

        private Output(final String northArrowGraphic, final String northArrowSubReport) {
            this.northArrowGraphic = northArrowGraphic;
            this.northArrowSubReport = northArrowSubReport;
        }
    }
}
