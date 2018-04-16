package org.mapfish.print.output;

import org.apache.commons.io.IOUtils;
import org.mapfish.print.Constants;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.http.MfClientHttpRequestFactoryImpl;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.processor.map.CreateMapProcessor;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;


/**
 *
 * The MapExportOutputFormat class.
 *
 * @author Niels
 *
 */
public class MapExportOutputFormat implements OutputFormat {

    private static final String MAP_SUBREPORT = "mapSubReport";

    @Autowired
    private ForkJoinPool forkJoinPool;

    @Autowired
    private MfClientHttpRequestFactoryImpl httpRequestFactory;

    private String fileSuffix;

    private String contentType;

    public final void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    @Override
    public final String getContentType() {
        return this.contentType;
    }

    public final void setFileSuffix(final String fileSuffix) {
        this.fileSuffix = fileSuffix;
    }

    @Override
    public final String getFileSuffix() {
        return this.fileSuffix;
    }

    private String getMapSubReportVariable(final Template template) {
        for (Processor<?, ?> processor : template.getProcessors()) {
            if (processor instanceof CreateMapProcessor) {
                String mapSubReport = ((CreateMapProcessor) processor).getOutputMapperBiMap().get(MAP_SUBREPORT);
                if (mapSubReport == null) {
                    return MAP_SUBREPORT;
                } else {
                    return mapSubReport;
                }
            }
        }
        // validation has already confirmed there is exactly one createmap processor
        // so this cannot happen
        return null;
    }

    @Override
    public final void print(final String jobId, final PJsonObject spec, final Configuration config,
                            final File configDir, final File taskDirectory, final OutputStream outputStream) throws Exception {
        final String templateName = spec.getString(Constants.JSON_LAYOUT_KEY);

        final Template template = config.getTemplate(templateName);
        if (template == null) {
            final String possibleTemplates = config.getTemplates().keySet().toString();
            throw new IllegalArgumentException("\nThere is no template with the name: " + templateName +
            ".\nAvailable templates: " + possibleTemplates);
        }

        final Values values = new Values(jobId, spec, template, taskDirectory, this.httpRequestFactory, null,
                this.fileSuffix);

        final ForkJoinTask<Values> taskFuture = this.forkJoinPool.submit(template.getProcessorGraph().createTask(values));

        try {
            taskFuture.get();
        } catch (InterruptedException exc) {
            // if cancel() is called on the current thread, this exception will be thrown.
            // in this case, also properly cancel the task future.
            taskFuture.cancel(true);
            Thread.currentThread().interrupt();
            throw new CancellationException();
        }

        String mapSubReport = values.getString(getMapSubReportVariable(template));

        //convert URI to file path
        try {
            mapSubReport = new File(new URI(mapSubReport)).getAbsolutePath();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e); //can't really happen
        }

        FileInputStream is = new FileInputStream(mapSubReport);
        try {
            IOUtils.copy(is, outputStream);
        } finally {
            is.close();
        }

    }
}
