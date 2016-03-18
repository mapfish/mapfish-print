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
