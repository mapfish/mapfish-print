package org.mapfish.print.output;

/**
 * User: jeichar
 * Date: 10/21/10
 * Time: 11:18 AM
 */
abstract class AbstractImageFormat implements OutputFormat {
    protected static final float MARGIN = 20;

    protected final String format;

    protected AbstractImageFormat(String format) {
        this.format = format;
    }

    public String contentType() {

        return "image/" + format;
    }

    public String fileSuffix() {
        return format;
    }
}
