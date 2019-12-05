package org.mapfish.print.metrics;

import com.codahale.metrics.httpclient.HttpClientMetricNameStrategies;
import com.codahale.metrics.httpclient.HttpClientMetricNameStrategy;

/**
 * Used as a factory for the spring configuration for configuring the {@link
 * com.codahale.metrics.httpclient.InstrumentedHttpClients}.
 *
 */
public final class MetricsNameStrategyFactory {
    private MetricsNameStrategyFactory() {
        // intentionally blank.
    }

    /**
     * Strategy for naming the http requests made for the metrics tracking.  Host and method will be tracked.
     */
    public static HttpClientMetricNameStrategy hostAndMethod() {
        return HttpClientMetricNameStrategies.HOST_AND_METHOD;
    }

    /**
     * Strategy for naming the http requests made for the metrics tracking.  Only the http method is tracked
     */
    public static HttpClientMetricNameStrategy methodOnly() {
        return HttpClientMetricNameStrategies.METHOD_ONLY;
    }

    /**
     * Strategy for naming the http requests made for the metrics tracking.  Host, method and path will be
     * tracked.
     */
    public static HttpClientMetricNameStrategy querylessUrlAndMethod() {
        return HttpClientMetricNameStrategies.QUERYLESS_URL_AND_METHOD;
    }
}
