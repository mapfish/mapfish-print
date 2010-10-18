/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package integration;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.log4j.*;
import org.pvalsecc.misc.FileUtilities;
import org.pvalsecc.misc.UnitUtilities;

import java.io.File;
import java.io.IOException;
import java.util.TreeSet;

/**
 * Simple program to test a tomcat print module under heavy load.
 */
public class HeavyLoad {
    private static final int NB_THREADS = 1;
    private static final int TIMEOUT = 3 * 60 * 1000;

    private static final String DEFAULT = "default";

    static {
        BasicConfigurator.configure(new ConsoleAppender(
                new PatternLayout("%d{HH:mm:ss.SSS} [%t] %-5p %30.30c - %m%n")));
        Logger.getRootLogger().setLevel(Level.INFO);

        //httpclient is a bit pesky about loading everything in memory...
        //disabling the warnings.
        Logger.getLogger(HttpMethodBase.class).setLevel(Level.ERROR);
    }

    public static final Logger LOGGER = Logger.getLogger(HeavyLoad.class);
    private static String url;
    private static HttpClient httpClient;
    private static String spec;
    private static final TreeSet<ServerStats> servers = new TreeSet<ServerStats>();

    public static void main(String[] args) throws IOException {
        url = args[0] + "/create.json";
        final String specFile = args[1];

        for (int i = 2; i < args.length; i++) {
            String server = args[i];
            servers.add(new ServerStats(server));
        }
        if(servers.isEmpty()) {
            servers.add(new ServerStats(DEFAULT));
        }

        MultiThreadedHttpConnectionManager connectionManager =
                new MultiThreadedHttpConnectionManager();
        final HttpConnectionManagerParams params = connectionManager.getParams();
        params.setSoTimeout(TIMEOUT);
        params.setConnectionTimeout(TIMEOUT);
        params.setDefaultMaxConnectionsPerHost(NB_THREADS);
        params.setMaxTotalConnections(NB_THREADS);

        httpClient = new HttpClient(connectionManager);

        spec = FileUtilities.readWholeTextFile(new File(specFile));

        Thread[] threads = new Thread[NB_THREADS];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Reader(), "reader-" + i);
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
            }
        }
    }

    public static class Reader implements Runnable {

        public void run() {
            while (true) {
                doQuery();
            }
        }

        private static void doQuery() {

            final ServerStats server;
            synchronized (servers) {
                server = servers.first();
                servers.remove(server);
                server.addUsage();
                servers.add(server);
            }
            try {
                PostMethod method = new PostMethod(url);
                method.setRequestHeader("Connection", "close");
                method.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
                if(!server.server.equals(DEFAULT)) {
                    method.setRequestHeader("Cookie", "SRV=" + server.server + "; path=/");
                }
                method.setRequestEntity(new StringRequestEntity(spec, "application/json", "utf-8"));
                LOGGER.info("doing query... server=" + server.server);
                long start = System.currentTimeMillis();
                httpClient.executeMethod(method);
                if (method.getStatusCode() != 200) {
                    LOGGER.error("Invalid status code: " + method.getStatusCode());
                    System.exit(-1);
                }
                method.getResponseBody();
                final long duration = System.currentTimeMillis() - start;
                LOGGER.info("...query done in " + UnitUtilities.toElapsedTime(duration) + " server=" + server.updateStats(duration));
                method.releaseConnection();
            } catch (IOException e) {
                LOGGER.error("server=" + server, e);
                System.exit(-1);
            }
        }
    }

    private static class ServerStats implements Comparable<Object> {
        private final String server;
        private Integer curUsage = 0;
        private Integer totUsage = 0;
        private long totDuration = 0;

        public ServerStats(String server) {
            this.server = server;
        }


        public int compareTo(Object o) {
            if (!(o instanceof ServerStats)) {
                throw new RuntimeException();
            }
            final ServerStats other = (ServerStats) o;
            int thisVal = curUsage;
            int anotherVal = other.curUsage;
            int result = thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1);
            if (result == 0) {
                result = server.compareTo(other.server);
            }
            return result;
        }


        public synchronized String updateStats(long duration) {
            totUsage++;
            totDuration += duration;
            String result = toString();
            curUsage--;
            return result;
        }

        public synchronized String toString() {
            return String.format("%s avgTime=%s nbDone=%d curQ=%d",
                    server,
                    totUsage > 0 ? UnitUtilities.toElapsedTime(totDuration / totUsage) : "null",
                    totUsage, curUsage);
        }

        public synchronized void addUsage() {
            curUsage++;
        }
    }
}
