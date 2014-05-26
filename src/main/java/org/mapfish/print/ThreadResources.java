package org.mapfish.print;

import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.mapfish.print.map.MapTileTask;
import org.pvalsecc.concurrent.OrderedResultsExecutor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Encapsulates resources that start and stop threads and need to be disposed and controlled.
 *
 * @author Jesse on 5/13/2014.
 */
public class ThreadResources {

    /**
     * The bunch of threads that will be used to do the // fetching of the map
     * chunks
     */
    private OrderedResultsExecutor<MapTileTask> mapRenderingExecutor = null;

    private MultiThreadedHttpConnectionManager connectionManager;
    private int perHostParallelFetches = 10;
    private int globalParallelFetches = 30;
    private int connectionTimeout = 30000;
    private int socketTimeout = 30000;

    @PostConstruct
    public void init() {
        this.connectionManager = new MultiThreadedHttpConnectionManager();

        final HttpConnectionManagerParams params = this.connectionManager.getParams();
        params.setDefaultMaxConnectionsPerHost(this.perHostParallelFetches);
        params.setMaxTotalConnections(this.globalParallelFetches);
        params.setSoTimeout(this.socketTimeout);
        params.setConnectionTimeout(this.connectionTimeout);

        mapRenderingExecutor = new OrderedResultsExecutor<MapTileTask>(globalParallelFetches, "tilesReader");
        mapRenderingExecutor.start();
    }

    @PreDestroy
    public void destroy() {
        try {
            this.connectionManager.shutdown();
        } finally {
            this.mapRenderingExecutor.stop();
        }
    }

    public void setPerHostParallelFetches(int perHostParallelFetches) {
        this.perHostParallelFetches = perHostParallelFetches;
    }

    public void setGlobalParallelFetches(int globalParallelFetches) {
        this.globalParallelFetches = globalParallelFetches;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public MultiThreadedHttpConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public OrderedResultsExecutor<MapTileTask> getMapRenderingExecutor() {
        return mapRenderingExecutor;
    }
}
