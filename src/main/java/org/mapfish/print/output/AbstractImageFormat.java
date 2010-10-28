package org.mapfish.print.output;

import org.mapfish.print.RenderingContext;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;

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

    protected int calculateDPI(RenderingContext context, PJsonObject jsonSpec) {
        final int MISSING_VALUE = -1;
        int dpi = jsonSpec.optInt("dpi", MISSING_VALUE);
        dpi = Math.max(dpi, context.getGlobalParams().optInt("dpi", MISSING_VALUE));
        PJsonArray pages = jsonSpec.optJSONArray("pages");
        if (pages != null) {
            for (int i = 0; i < pages.size(); i++) {
                PJsonObject page = pages.getJSONObject(i);
                dpi = Math.max(dpi, page.optInt("dpi", MISSING_VALUE));
            }
        }
        if (dpi < 0) {
            throw new IllegalArgumentException("unable to calculation DPI of maps");
        }
        return dpi;
    }
}
