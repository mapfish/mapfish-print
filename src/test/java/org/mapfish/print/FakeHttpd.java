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

package org.mapfish.print;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A fake HTTP server to be used for tests.
 */
public class FakeHttpd extends Thread {
    public static final Logger LOGGER = Logger.getLogger(FakeHttpd.class);
    private static final Pattern GET = Pattern.compile("^GET (.*) HTTP/\\d.\\d$");
    private int port;
    private final Map<String, HttpAnswerer> routings;
    private static final HttpAnswerer NOT_FOUND = new HttpAnswerer(404, "Not found", "text/plain", "Not found");
    private static final HttpAnswerer STOP = new HttpAnswerer(200, "STOPPING", "text/plain", "stopping", true);
    private final AtomicBoolean starting = new AtomicBoolean(false);

    public FakeHttpd(int port, Map<String, HttpAnswerer> routings) {
        super("FakeHttpd(" + port + ")");
        this.port = port;
        routings.put("/stop", STOP);
        this.routings = routings;
    }

    @Override
    public void start() {
        starting.set(true);
        super.start();
        synchronized (starting) {
            while (starting.get()) {
                try {
                    starting.wait();
                } catch (InterruptedException e) {
                    //ignored
                }
            }
        }
    }

    public void run() {
        try {
            LOGGER.info("starting");
            ServerSocket serverSocket = new ServerSocket(port);
            LOGGER.info("started");
            synchronized (starting) {
                starting.set(false);
                starting.notify();
            }

            while (true) {
                Socket socket = serverSocket.accept();
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintStream output = new PrintStream(socket.getOutputStream());
                final boolean stayAlive = handleHttp(input, output);
                input.close();
                output.close();
                socket.close();
                if (!stayAlive) {
                    break;
                }
            }
            serverSocket.close();
            LOGGER.info("stopped");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() throws IOException, InterruptedException {
        GetMethod method = new GetMethod("http://localhost:" + port + "/stop");
        HttpClient client = new HttpClient();
        client.executeMethod(method);
        join();
    }

    private boolean handleHttp(BufferedReader input, PrintStream output) throws IOException {
        String url = null;
        String curHeader;
        while (!(curHeader = input.readLine()).equals("")) {
            Matcher getMatcher = GET.matcher(curHeader);
            if (getMatcher.matches()) {
                url = getMatcher.group(1);
            }
        }
        if (url == null) {
            LOGGER.error("didn't receive a GET");
            return false;
        }

        LOGGER.debug("received a GET request: " + url);
        HttpAnswerer answer = routings.get(url);
        if (answer == null) {
            answer = NOT_FOUND;
        }

        return !answer.answer(output);
    }

    public static class HttpAnswerer {
        private final int status;
        private final String statusTxt;
        private final String contentType;
        private final String body;
        private final boolean stop;

        public HttpAnswerer(int status, String statusTxt, String contentType, String body) {
            this.status = status;
            this.statusTxt = statusTxt;
            this.contentType = contentType;
            this.body = body;
            this.stop = false;
        }

        private HttpAnswerer(int status, String statusTxt, String contentType, String body, boolean stop) {
            this.status = status;
            this.statusTxt = statusTxt;
            this.contentType = contentType;
            this.body = body;
            this.stop = stop;
        }

        protected boolean answer(PrintStream output) {
            output.println("HTTP/1.0 " + status + " " + statusTxt);
            output.println("Content-Type: " + contentType);
            output.println("");
            output.println(body);
            return stop;
        }
    }
}
