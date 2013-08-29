/*
 * Copyright (C) 2013  Camptocamp
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

package org.mapfish.print.config;

//import org.apache.commons.httpclient.HostConfiguration;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.Constants;
import org.mapfish.print.InvalidValueException;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.config.layout.Layout;
import org.mapfish.print.config.layout.Layouts;
import org.mapfish.print.map.MapTileTask;
import org.mapfish.print.map.readers.MapReaderFactoryFinder;
import org.mapfish.print.map.readers.WMSServerInfo;
import org.mapfish.print.output.OutputFactory;
import org.pvalsecc.concurrent.OrderedResultsExecutor;
//import org.mapfish.print.output.OutputFormat;

/**
 * Bean mapping the root of the configuration file.
 */
public class Config implements Closeable {
    public static final Logger LOGGER = Logger.getLogger(Config.class);

    private Layouts layouts;
    private TreeSet<Integer> dpis;
    private TreeSet<Integer> scales;
    private String maxSvgWidth = "";
    private String maxSvgHeight = "";
    private double maxSvgW = Double.MAX_VALUE;
    private double maxSvgH = Double.MAX_VALUE;
    private boolean reloadConfig = false;
    
    private boolean integerSvg = true;
    
    private List<String> overlayLayers = null;
    
    private TreeSet<String> fonts = null;
    private List<HostMatcher> hosts = new ArrayList<HostMatcher>();
    private HashMap localHostForward;
    private TreeSet<Key> keys;

    private int globalParallelFetches = 5;
    private int perHostParallelFetches = 5;
    private int socketTimeout = 40*60*1000; // 40 minutes //3*60*1000;
    private int connectionTimeout = 40*60*1000; // 40 minutes //30*1000;

    private boolean tilecacheMerging = false;
    private boolean disableScaleLocking = false;
    
    private List<SecurityStrategy> security = Collections.emptyList();

    private String outputFilename = "mapfish-print.pdf";

    /**
     * How much of the asked map we tolerate to be outside of the printed area.
     * Used only in case of bbox printing (use by the PrintAction JS component).
     */
    private static final double BEST_SCALE_TOLERANCE = 0.98;

    /**
     * The bunch of threads that will be used to do the // fetching of the map
     * chunks
     */
    private OrderedResultsExecutor<MapTileTask> mapRenderingExecutor = null;
    private MultiThreadedHttpConnectionManager connectionManager;
    private TreeSet<String> formats; // private int svgMaxWidth = -1; private int svgMaxHeight = -1;

	private OutputFactory outputFactory;

	private MapReaderFactoryFinder mapReaderFactoryFinder;
    private String brokenUrlPlaceholder = Constants.ImagePlaceHolderConstants.THROW;

    public Config() {
        hosts.add(new LocalHostMatcher());
    }

    public void setOutputFactory(OutputFactory outputFactory) {
		this.outputFactory = outputFactory;
	}
    
    public Layout getLayout(String name) {
        return layouts.get(name);
    }

    public void setLayouts(Layouts layouts) {
        this.layouts = layouts;
    }

    public void setDpis(TreeSet<Integer> dpis) {
        this.dpis = dpis;
    }
    
    public void setMaxSvgWidth(String maxSvgWidth) {
    	this.maxSvgWidth = maxSvgWidth;
    	this.maxSvgW = Double.parseDouble(maxSvgWidth);
    }
    public String getMaxSvgWidth() {
    	return this.maxSvgWidth;
    }
    public void setMaxSvgHeight(String maxSvgHeight) {
    	this.maxSvgHeight = maxSvgHeight;
    	this.maxSvgH = Double.parseDouble(maxSvgHeight);
    }
    public String getMaxSvgHeight() {
    	return this.maxSvgHeight;
    }
    
    public double getMaxSvgW() {
    	return this.maxSvgW;
    }
    public double getMaxSvgH() {
    	return this.maxSvgH;
    }
    
    public TreeSet<Integer> getDpis() {
        return dpis;
    }

    public void printClientConfig(JSONWriter json) throws JSONException {
        json.key("scales");
        json.array();
        for (Integer scale : scales) {
            json.object();
            json.key("name").value("1:" + NumberFormat.getIntegerInstance().format(scale));
            json.key("value").value(scale.toString());
            json.endObject();
        }
        json.endArray();

        json.key("dpis");
        json.array();
        for (Integer dpi : dpis) {
            json.object();
            json.key("name").value(dpi.toString());
            json.key("value").value(dpi.toString());
            json.endObject();
        }
        json.endArray();

        json.key("outputFormats");
        json.array();
        for (String format : outputFactory.getSupportedFormats(this)) {
            json.object();
            json.key("name").value(format);
            json.endObject();
        }
        json.endArray();

        json.key("layouts");
        json.array();
        ArrayList<String> sortedLayouts = new ArrayList<String>();
        sortedLayouts.addAll(layouts.keySet());
        Collections.sort(sortedLayouts);
        for (int i = 0; i < sortedLayouts.size(); i++) {
            String key = sortedLayouts.get(i);
            json.object();
            json.key("name").value(key);
            layouts.get(key).printClientConfig(json);
            json.endObject();
        }
        json.endArray();
    }

    public void setScales(TreeSet<Integer> scales) {
        this.scales = scales;
    }

    public boolean isScalePresent(int scale) {
        return scales.contains(scale);
    }

    public void setHosts(List<HostMatcher> hosts) {
        this.hosts = hosts;
    }

    public void setFonts(TreeSet<String> fonts) {
        this.fonts = fonts;
    }

    public TreeSet<String> getFonts() {
        return fonts;
    }

    public void setKeys(TreeSet<Key> keys) {
        this.keys = keys;
    }

    public TreeSet<Key> getKeys() {
        TreeSet<Key> k = keys;
        if(k == null) k = new TreeSet<Key>();
        return k;
    }

    /**
     * Make sure an URI is authorized
     */
    public boolean validateUri(URI uri) throws UnknownHostException, SocketException, MalformedURLException {
        for (int i = 0; i < hosts.size(); i++) {
            HostMatcher matcher = hosts.get(i);
            if (matcher.validate(uri)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("URI [" + uri + "] accepted by: " + matcher);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Called just after the config has been loaded to check it is valid.
     *
     * @throws InvalidValueException When there is a problem
     */
    public void validate() {
        if (layouts == null) throw new InvalidValueException("layouts", "null");
        layouts.validate();

        if (dpis == null) throw new InvalidValueException("dpis", "null");
        if (dpis.size() < 1) throw new InvalidValueException("dpis", "[]");

        if (scales == null) throw new InvalidValueException("scales", "null");
        if (scales.size() < 1) throw new InvalidValueException("scales", "[]");

        if (hosts == null) throw new InvalidValueException("hosts", "null");
        if (hosts.size() < 1) throw new InvalidValueException("hosts", "[]");

        if (globalParallelFetches < 1) {
            throw new InvalidValueException("globalParallelFetches", globalParallelFetches);
        }
        if (perHostParallelFetches < 1) {
            throw new InvalidValueException("perHostParallelFetches", perHostParallelFetches);
        }

        if (socketTimeout < 0) {
            throw new InvalidValueException("socketTimeout", socketTimeout);
        }
        if (connectionTimeout < 0) {
            throw new InvalidValueException("connectionTimeout", connectionTimeout);
        }

        for (Key key : getKeys()) {
            key.validate();
        }

    }

    /**
     * @return The first scale that is bigger or equal than the target.
     */
    public int getBestScale(double target) {
        if (this.disableScaleLocking) {
            Double forcedScale = target;
            return forcedScale.intValue();
        }
        else {
            target *= BEST_SCALE_TOLERANCE;
            for (Integer scale : scales) {
                if (scale >= target) {
                    return scale;
                }
            }
            return scales.last();
        }

    }

    public synchronized OrderedResultsExecutor<MapTileTask> getMapRenderingExecutor() {
        if (mapRenderingExecutor == null && globalParallelFetches > 1) {
            mapRenderingExecutor = new OrderedResultsExecutor<MapTileTask>(globalParallelFetches, "tilesReader");
            mapRenderingExecutor.start();
        }
        return mapRenderingExecutor;
    }

    /**
     * Stop all the threads and stuff used for this config.
     */
    public synchronized void close() {
        try {
            WMSServerInfo.clearCache();
        } finally {
            try {
                if (mapRenderingExecutor != null) {
                    mapRenderingExecutor.stop();
                }
            } finally {
                if (connectionManager != null) {
                    connectionManager.shutdown();
                }
            }
        }
    }

    public void setGlobalParallelFetches(int globalParallelFetches) {
        this.globalParallelFetches = globalParallelFetches;
    }

    public void setPerHostParallelFetches(int perHostParallelFetches) {
        this.perHostParallelFetches = perHostParallelFetches;
        System.getProperties().setProperty("http.maxConnections", Integer.toString(perHostParallelFetches));
    }

    /**
     * Get or create the http client to be used to fetch all the map data.
     */
    public HttpClient getHttpClient(URI uri) {
        MultiThreadedHttpConnectionManager connectionManager = getConnectionManager();
        HttpClient httpClient = new HttpClient(connectionManager);

        // httpclient is a bit pesky about loading everything in memory...
        // disabling the warnings.
        Logger.getLogger(HttpMethodBase.class).setLevel(Level.ERROR);

        // configure proxies for URI
        ProxySelector selector = ProxySelector.getDefault();

        List<Proxy> proxyList = selector.select(uri);
        Proxy proxy = proxyList.get(0);

        if (!proxy.equals(Proxy.NO_PROXY)) {
            InetSocketAddress socketAddress = (InetSocketAddress) proxy.address();
            String hostName = socketAddress.getHostName();
            int port = socketAddress.getPort();

            httpClient.getHostConfiguration().setProxy(hostName, port);
        }

        for(SecurityStrategy sec : security)
        if(sec.matches(uri)) {
        	sec.configure(uri, httpClient);
        	break;
        }
        return httpClient;
    }

    private synchronized MultiThreadedHttpConnectionManager getConnectionManager() {
        if(connectionManager == null) {
            connectionManager = new MultiThreadedHttpConnectionManager();
            final HttpConnectionManagerParams params = connectionManager.getParams();
            params.setDefaultMaxConnectionsPerHost(perHostParallelFetches);
            params.setMaxTotalConnections(globalParallelFetches);
            params.setSoTimeout(socketTimeout);
            params.setConnectionTimeout(connectionTimeout);
        }
        return connectionManager;
    }

    public void setTilecacheMerging(boolean tilecacheMerging) {
        this.tilecacheMerging = tilecacheMerging;
    }

    public boolean isTilecacheMerging() {
        return tilecacheMerging;
    }

    public void setDisableScaleLocking(boolean disableScaleLocking) {
        this.disableScaleLocking = disableScaleLocking;
    }

    public boolean isDisableScaleLocking() {
        return disableScaleLocking;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public String getOutputFilename(String layoutName) {
        Layout layout = layouts.get(layoutName);
        String name = null;
        if(layout != null) {
            name = layout.getOutputFilename();
            if (name != null) {
                name = PDFUtils.getValueFromString(name); // get the string if it has ${now} or ${now DATEFORMAT} in it
            }
        }
        return name == null ? outputFilename : name;
    }

    public String getOutputFilename() {
        return outputFilename;
    }

    public void setOutputFilename(String outputFilename) {
        this.outputFilename = outputFilename;
    }

    public TreeSet<String> getFormats() {
        if(formats == null) return new TreeSet<String>();
        return formats;
    }

    public void setFormats(TreeSet<String> formats) {
        this.formats = formats;
    }

    public void setOverlayLayers(List<String> overlayLayers) {
      this.overlayLayers = overlayLayers;
    }

    public List<String> getOverlayLayers() {
      return overlayLayers;
    }

	/**
	 * @return the integerSvg true if for example MapServer 5.6 or earlier is used where integers are put into the SVG
	 */
	public boolean getIntegerSvg() {
		return integerSvg;
	}

	/**
	 * @param integerSvg the integerSvg to set
	 */
	public void setIntegerSvg(boolean integerSvg) {
		this.integerSvg = integerSvg;
	}

    /**
     * @return the reloadConfig
     */
    public boolean getReloadConfig() {
        return reloadConfig;
    }

    /**
     * @param reloadConfig the reloadConfig to set
     */
    public void setReloadConfig(boolean reloadConfig) {
        this.reloadConfig = reloadConfig;
    }

    public void setSecurity(List<SecurityStrategy> security) {
        this.security = security;
    }

	public void setMapReaderFactoryFinder(
			MapReaderFactoryFinder mapReaderFactoryFinder) {
		this.mapReaderFactoryFinder = mapReaderFactoryFinder;
	}
	
	public MapReaderFactoryFinder getMapReaderFactoryFinder() {
		return mapReaderFactoryFinder;
	}

    public void setLocalHostForward(HashMap localHostForward) {
        this.localHostForward = localHostForward;
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    }

    public boolean localHostForwardIsHttps2http() {
        if (localHostForward != null) {
            Object https2http = localHostForward.get("https2http");
            if (https2http != null && https2http instanceof Boolean) {
                return (Boolean)https2http;
            }
        }
        return false;
    }

    public boolean localHostForwardIsFrom(String host) {
        if (localHostForward != null) {
            Object from = localHostForward.get("from");
            if (from != null && from instanceof List) {
                return ((List)from).indexOf(host) >= 0;
            }
        }
        return false;
    }

    public void setBrokenUrlPlaceholder(String brokenUrlPlaceholder) {
        this.brokenUrlPlaceholder = brokenUrlPlaceholder;
    }

    public String getBrokenUrlPlaceholder() {
        return brokenUrlPlaceholder;
    }
}
