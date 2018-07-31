package org.mapfish.print.metrics;

import com.codahale.metrics.MetricRegistry;
import com.readytalk.metrics.StatsDReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Will start a StatsD reporter if configured. Using those environment variables or Java system properties: -
 * STATSD_ADDRESS: the address:port of the statsd daemon (if not set, the feature is disabled) -
 * STATSD_PREFIX: the prefix to set (defaults to mapfish-print). Can use %h to insert the hostname. -
 * STATSD_PERIOD: the reporting period in seconds (defaults to 10)
 */
public class StatsDReporterInit {
    private static final String ADDRESS = "STATSD_ADDRESS";
    private static final String PREFIX = "STATSD_PREFIX";
    private static final String PERIOD = "STATSD_PERIOD";
    private static final Logger LOGGER = LoggerFactory.getLogger(StatsDReporterInit.class);

    @Autowired
    private MetricRegistry metricRegistry;

    private StatsDReporter reporter = null;

    private static String getHostname() {
        String hostname = System.getenv("HOSTNAME");  // should work on Unix
        if (hostname == null) {
            hostname = System.getenv("COMPUTERNAME");  // should work on Windows
        }
        if (hostname == null) {
            try {
                // last resort
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                hostname = "unknown";  // fallback
            }
        }
        return hostname.toLowerCase();
    }

    private String getConfig(final String name, final String def) {
        final String result = System.getenv(name);
        if (result == null) {
            return System.getProperty(name, def);
        }
        return result;
    }

    /**
     * Start the StatsD reporter, if configured.
     *
     * @throws URISyntaxException
     */
    @PostConstruct
    public final void init() throws URISyntaxException {
        final String address = getConfig(ADDRESS, null);
        if (address != null) {
            final URI uri = new URI("udp://" + address);
            final String prefix = getConfig(PREFIX, "mapfish-print").replace("%h", getHostname());
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
