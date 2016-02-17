/*
 * Copyright (C) 2016  Camptocamp
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
import com.readytalk.metrics.StatsDReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Will start a StatsD reporter if configured.
 * Using those environment variables or Java system properties:
 * - STATSD_ADDRESS: the address:port of the statsd daemon (if not set, the feature is disabled)
 * - STATSD_PREFIX: the prefix to set (defaults to mapfish-print)
 * - STATSD_PERIOD: the reporting period in seconds (defaults to 10)
 */
public class StatsDReporterInit {
    private static final String ADDRESS = "STATSD_ADDRESS";
    private static final String PREFIX = "STATSD_PREFIX";
    private static final String PERIOD = "STATSD_PERIOD";
    private static final Logger LOGGER = LoggerFactory.getLogger(StatsDReporterInit.class);

    @Autowired
    private MetricRegistry metricRegistry;

    private StatsDReporter reporter = null;

    private String getConfig(final String name, final String def) {
        final String result = System.getenv(name);
        if (result == null) {
            return System.getProperty(name, def);
        }
        return result;
    }

    /**
     * Start the StatsD reporter, if configured.
     * @throws URISyntaxException
     */
    @PostConstruct
    public final void init() throws URISyntaxException {
        final String address = getConfig(ADDRESS, null);
        if (address != null) {
            final URI uri = new URI("udp://" + address);
            final String prefix = getConfig(PREFIX, "mapfish-print");
            final int period = Integer.parseInt(getConfig(PERIOD, "10"));
            LOGGER.info("Starting a StatsD reporter targeting {} with prefix {} and period {}s",
                    uri, prefix, period);
            this.reporter = StatsDReporter.forRegistry(this.metricRegistry)
                    .prefixedWith(prefix)
                    .build(uri.getHost(), uri.getPort());
            this.reporter.start(period, TimeUnit.SECONDS);
        }
    }

    /**
     * Stop the StatsD reporter, if configured.
     */
    @PreDestroy
    public final void shutdown() {
        if (this.reporter != null) {
            LOGGER.info("Stopping the StatsD reporter");
            this.reporter.stop();
        }
    }
}
