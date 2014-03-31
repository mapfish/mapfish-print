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

package org.mapfish.print.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlet.InstrumentedFilterContextListener;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import static org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext;

/**
 * Allows the AdminServlet to get access to the MetricRegistry so it can display the statistics via the admin servlet.
 * <p/>
 * @author jesseeichar on 3/21/2014.
 */
public class MapfishPrintInstrumentedFilterContextListener extends InstrumentedFilterContextListener {
    private ServletContext servletContext;

    @Override
    protected final MetricRegistry getMetricRegistry() {
        final WebApplicationContext webApplicationContext = getWebApplicationContext(this.servletContext);
        return webApplicationContext.getBean(MetricRegistry.class);
    }

    @Override
    public final void contextInitialized(final ServletContextEvent event) {
        this.servletContext = event.getServletContext();
        super.contextInitialized(event);
    }

}
