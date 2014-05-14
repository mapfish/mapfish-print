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
import com.codahale.metrics.log4j.InstrumentedAppender;
import org.apache.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

/**
 * Utility class for adding metrics instrumentation to logging framework.
 *
 * @author jesseeichar on 3/22/14.
 */
public class LoggingMetricsConfigurator {
    @Autowired
    private MetricRegistry metricRegistry;

    /**
     * Add an appender to Logback logging framework that will track the types of log messages made.
     */
    @PostConstruct
    public final void addMetricsAppenderToLogback() {
        final InstrumentedAppender instrumentedAppender = new InstrumentedAppender(metricRegistry);
        instrumentedAppender.activateOptions();
        LogManager.getRootLogger().addAppender(instrumentedAppender);
    }
}
