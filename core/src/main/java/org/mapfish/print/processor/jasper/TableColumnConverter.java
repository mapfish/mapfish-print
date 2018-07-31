package org.mapfish.print.processor.jasper;

import org.mapfish.print.config.ConfigurationObject;
import org.mapfish.print.http.MfClientHttpRequestFactory;

/**
 * Converter to convert the value of a table cell (a string) into a different type (e.g. an image).
 *
 * @param <R> The resulting type
 */
public interface TableColumnConverter<R> extends ConfigurationObject {
    /**
     * Convert the value.
     *
     * @param requestFactory for fetching file and http resources.
     * @param text the cell value.
     */
    R resolve(
            MfClientHttpRequestFactory requestFactory,
            String text);

    /**
     * Returns true if the converter can convert the given input.
     *
     * @param text the input to convert.
     */
    boolean canConvert(String text);
}
