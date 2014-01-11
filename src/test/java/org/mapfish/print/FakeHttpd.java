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

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.pvalsecc.misc.FileUtilities;

/**
 * A fake HTTP server to be used for tests.
 */
public class FakeHttpd {
    public static final Logger LOGGER = Logger.getLogger(FakeHttpd.class);
    private final HttpServer server;
    private int port;

    public FakeHttpd(int port, Map<String, HttpAnswerer> routings) {
        this.port = port;
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            for (Map.Entry<String, HttpAnswerer> entry : routings.entrySet()) {
                this.server.createContext(entry.getKey(), entry.getValue());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        server.start();
    }


    public void shutdown() throws IOException, InterruptedException {
        server.stop(1);
    }

    public int getPort() {
        return port;
    }

    public static class HttpAnswerer implements HttpHandler{
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

            LOGGER.debug("received a "+httpExchange.getRequestMethod()+" request: " + httpExchange.getRequestURI());

            if (contentType != null) {
                httpExchange.getResponseHeaders().add("Content-Type", contentType);
            }
            httpExchange.sendResponseHeaders(status, body.length);
            final OutputStream stream = httpExchange.getResponseBody();
            stream.write(body);
            stream.close();
        }
    }
}
