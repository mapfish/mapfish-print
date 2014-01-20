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

package org.mapfish.print;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.log4j.Logger;
import org.pvalsecc.misc.FileUtilities;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A fake HTTP server to be used for tests.
 */
public class FakeHttpd {

    public static class Route {
        final String route;
        final HttpAnswerer response;

        public Route(String route, HttpAnswerer response) {
            this.route = route;
            this.response = response;
        }

        /**
         * Return a route that returns a 200 response with text payload.
         *
         * @return a route that returns a 200 response with text payload
         */
        public static Route textResponse(String route, String response) {
            return new Route(route, new HttpAnswerer(200, "OK", "text/plain", response));
        }

        /**
         * Return a route that returns a 200 response with xml payload.
         *
         * @return a route that returns a 200 response with xml payload
         */
        public static Route xmlResponse(String route, byte[] response) {
            return new Route(route, new HttpAnswerer(200, "OK", "application/xml", response));
        }

        /**
         * Return a route that returns a 200 response with xml payload.
         *
         * @return a route that returns a 200 response with xml payload
         */
        public static Route xmlResponse(String route, String  response) {
            try {
                return xmlResponse(route, response.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Create a route that results in an error.
         *
         * @param route the path/route
         * @param code  the error code
         * @param msg   the error message
         * @return a route that results in an error.
         */
        public static Route errorResponse(String route, int code, String msg) {
            return new Route(route, new HttpAnswerer(code, msg, "text/plain", (byte[]) null));
        }
    }

    public static final Logger LOGGER = Logger.getLogger(FakeHttpd.class);
    private final static AtomicInteger portInc = new AtomicInteger(20732);
    private final HttpServer server;
    private int port;

    public FakeHttpd(Route... routes) {
        this.port = portInc.incrementAndGet();
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            addRoutes(routes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addRoutes(Route... routes) {
        if (routes != null) {
            for (Route route : routes) {
                this.server.createContext(route.route, route.response);
            }
        }
    }

    public int getPort() {
        return port;
    }

    public void start() {
        server.start();
    }


    public void shutdown() throws IOException, InterruptedException {
        server.stop(1);
    }

    public static class HttpAnswerer implements HttpHandler {
        private final int status;
        private final String statusTxt;
        private final String contentType;
        private final byte[] body;

        public HttpAnswerer(int status, String statusTxt, String contentType, InputStream inputStream) {
            this(status, statusTxt, contentType, streamToBytes(inputStream));
        }

        public HttpAnswerer(int status, String statusTxt, String contentType, byte[] bytes) {
            this.status = status;
            this.statusTxt = statusTxt;
            this.contentType = contentType;
            this.body = bytes;
        }

        private static byte[] streamToBytes(InputStream inputStream) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                FileUtilities.copyStream(inputStream, out);
            } catch (IOException e) {
                throw new Error(e);
            }

            return out.toByteArray();
        }

        public HttpAnswerer(int status, String statusTxt, String contentType, String body) {
            this.status = status;
            this.statusTxt = statusTxt;
            this.contentType = contentType;
            try {
                this.body = body.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new Error(e);
            }
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {

            LOGGER.debug("received a " + httpExchange.getRequestMethod() + " request: " + httpExchange.getRequestURI());

            if (contentType != null) {
                httpExchange.getResponseHeaders().add("Content-Type", contentType);
            }
            if (body == null) {
                httpExchange.sendResponseHeaders(status, 0);
                httpExchange.getResponseBody().close();
            } else {
                httpExchange.sendResponseHeaders(status, body.length);
                final OutputStream stream = httpExchange.getResponseBody();
                stream.write(body);
                stream.close();
            }
        }
    }
}
