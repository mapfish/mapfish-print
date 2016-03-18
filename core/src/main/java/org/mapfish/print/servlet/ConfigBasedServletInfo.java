package org.mapfish.print.servlet;

import java.util.UUID;

/**
 * A servlet info that can be configured through the spring configuration (or programmatically).
 * <p></p>
 * A default random id will be created by default.
 *
 * @author Jesse on 4/26/2014.
 */
public final class ConfigBasedServletInfo implements ServletInfo {
    private String servletId = UUID.randomUUID().toString();

    public void setServletId(final String servletId) {
        this.servletId = servletId;
    }

    @Override
    public String getServletId() {
        return this.servletId;
    }
}
