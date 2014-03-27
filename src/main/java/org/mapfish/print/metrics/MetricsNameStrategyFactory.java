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

import com.codahale.metrics.httpclient.HttpClientMetricNameStrategies;
import com.codahale.metrics.httpclient.HttpClientMetricNameStrategy;

/**
 * Used as a factory for the spring configuration for configuring the {@link com.codahale.metrics.httpclient.InstrumentedHttpClient}.
 * <p/>
 * @author jesseeichar on 3/21/14.
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
     * Strategy for naming the http requests made for the metrics tracking.  Host, method and path will be tracked.
     */
    public static HttpClientMetricNameStrategy querylessUrlAndMethod() {
        return HttpClientMetricNameStrategies.QUERYLESS_URL_AND_METHOD;
    }
}
