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

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * This bean will enable JMX reporting when added to application context.
 *
 * @author Jesse on 5/9/2014.
 */
public final class JmxMetricsReporter {

    @Autowired
    private MetricRegistry metricRegistry;
    private JmxReporter reporter;

    /**
     * Add jmx reporter on startup.
     */
    @PostConstruct
    public void init() {
        this.reporter = JmxReporter.forRegistry(this.metricRegistry).build();
        this.reporter.start();
    }

    /**
     * Stop JMX reporter.
     */
    @PreDestroy
    public void destroy() {
        this.reporter.stop();
    }
}
