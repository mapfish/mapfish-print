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
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

/**
 * A bean that will add several gauges and metric sets for tracking the JVM stats.
 *
 * @author Jesse on 5/9/2014.
 */
public final class JvmMetricsConfigurator {

    @Autowired
    private MetricRegistry metricRegistry;

    /**
     * Add several jvm metrics.
     */
    @PostConstruct
    public void init() {
        this.metricRegistry.register(name("gc"), new GarbageCollectorMetricSet());
        this.metricRegistry.register(name("memory"), new MemoryUsageGaugeSet());
        this.metricRegistry.register(name("thread-states"), new ThreadStatesGaugeSet());
        this.metricRegistry.register(name("fd-usage"), new FileDescriptorRatioGauge());
    }

    private String name(final String metricName) {
        return "jvm-" + metricName;
    }
}
