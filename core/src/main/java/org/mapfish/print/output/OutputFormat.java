package org.mapfish.print.output;

import org.mapfish.print.config.Configuration;
import org.mapfish.print.wrapper.json.PJsonObject;

import java.io.File;
import java.io.OutputStream;

/**
 * Interface for exporting the generated PDF from MapPrinter.
 * <p></p>
 * User: jeichar
 * Date: Oct 18, 2010
 * Time: 1:49:41 PM
 */
public interface OutputFormat {
    /**
     * The content type of the output.
     */
    String getContentType();

    /**
     * The file suffix to use when writing to a file.
     */
    String getFileSuffix();

    /**
     * Performs the print and writes to the report in the correct format to the outputStream.
     *
     * @param jobId the job ID
     * @param spec the data from the client, required for writing.
     * @param config the configuration object representing the server side configuration.
     * @param configDir the directory that contains the configuration, used for resolving resources like images etc...
     * @param taskDirectory the temporary directory for this printing task.
     * @param outputStream the stream to write the result to
     */
    void print(String jobId, PJsonObject spec, Configuration config, File configDir, File taskDirectory,
               OutputStream outputStream) throws Exception;

}
