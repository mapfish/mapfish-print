package org.mapfish.print.servlet;

import java.util.UUID;

/**
 * A servlet info that can be configured through the spring configuration (or programmatically).
 *
 * A default random id will be created by default.
 */
public final class ConfigBasedServletInfo implements ServletInfo {
    private String servletId = UUID.randomUUID().toString();

    @Override
    public String getServletId() {
        return this.servletId;
    }

    public void setServletId(final String servletId) {
        this.servletId = servletId;
    }
}
