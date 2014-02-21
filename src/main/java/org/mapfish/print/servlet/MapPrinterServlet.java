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

package org.mapfish.print.servlet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mapfish.print.Constants;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.servlet.queue.BasicQueue;
import org.mapfish.print.servlet.queue.Queue;
import org.mapfish.print.servlet.registry.BasicRegistry;
import org.mapfish.print.servlet.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

public class MapPrinterServlet extends BaseMapServlet {
    private static final long serialVersionUID = -5038318057436063687L;
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseMapServlet.class);

    // can be /app/capabilities.json
    private static final String CAPABILITIES_URL = "/capabilities.json";
    private static final String CREATE_URL = "/create";
    private static final String STATUS_URL = "/status/";
    private static final String RESULT_URL = "/get/";

    /* The registry keys */
    private static final String RESULT_DATA = "resultdata";
    private static final String RESULT_METADATA = "resultmetadata";
    private static final String NEW_PRINT_COUNT = "newprintcount";
    private static final String LAST_PRINT_COUNT = "lastprintcount";
    private static final String TOTAL_PRINT_TIME = "totalprinttime";
    private static final String NB_PRINT_DONE = "nbprintdone";
    private static final String LAST_POOL = "lastpool";

    /**
     * The layout tag in the json job, status and metadata.
     */
    private static final String JSON_ERROR = "error";
    private static final String JSON_APP = "app";
    private static final String JSON_REF = "ref";
    private static final String JSON_COUNT = "count";
    private static final String JSON_SPEC = "spec";
    private static final String JSON_DONE = "done";
    private static final String JSON_TIME = "time";
    private static final String JSON_HEADERS = "headers";
    private static final String JSON_FILENAME = "filename";

    private static final Queue queue = new BasicQueue();
    private static final Registry registry = new BasicRegistry();


    public MapPrinterServlet() {
        PrintThread thread = new PrintThread();
        thread.start();
    }

    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws ServletException, IOException {
        // do the routing in function of the actual URL
        String additionalPath = httpServletRequest.getPathInfo().trim();
        if (additionalPath.isEmpty()) {
            // handle an odd case where path info returns an empty string
            additionalPath = httpServletRequest.getServletPath();
        }
        if (additionalPath.equals(CAPABILITIES_URL)) {
            getInfo(null, httpServletRequest, httpServletResponse, getBaseUrl(httpServletRequest));
        } else if (additionalPath.endsWith(CAPABILITIES_URL)) {
            final String app = additionalPath.substring(1, additionalPath.length() - CAPABILITIES_URL.length());
            getInfo(app, httpServletRequest, httpServletResponse, getBaseUrl(httpServletRequest));
        } else if (additionalPath.startsWith(STATUS_URL)) {
            getStatus(additionalPath, httpServletResponse);
        } else if (additionalPath.startsWith(RESULT_URL)) {
            getFile(additionalPath, httpServletRequest, httpServletResponse);
        } else {
            error(httpServletResponse, "Unknown method: " + additionalPath, 404);
        }
    }

    private void getStatus(String additionalPath, HttpServletResponse httpServletResponse) {
        int index = additionalPath.lastIndexOf("/");
        int ref = Integer.parseInt(additionalPath.substring(index + 1));
        boolean done = registry.containsKey(RESULT_METADATA + ref);

        if (!done) {
            registry.setLong(LAST_POOL + ref, new Date().getTime());
        }

        PrintWriter writer = null;
        try {
            httpServletResponse.setContentType("application/json; charset=utf-8");
            writer = httpServletResponse.getWriter();
            JSONWriter json = new JSONWriter(writer);
            json.object();
            {
                json.key(JSON_DONE).value(done);
                JSONObject metadata = registry.getJSON(RESULT_METADATA + ref);
                if (metadata != null && metadata.has(JSON_ERROR)) {
                    json.key(JSON_ERROR).value(metadata.getString(JSON_ERROR));
                }
                if (!done) {
                    json.key(JSON_COUNT).value(registry.getInteger(LAST_PRINT_COUNT));
                    json.key(JSON_TIME).value(
                            registry.getLong(TOTAL_PRINT_TIME) /
                            registry.getInteger(NB_PRINT_DONE));
                }
            }
            json.endObject();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws ServletException, IOException {
        final String additionalPath = httpServletRequest.getPathInfo();
        if (additionalPath.equals(CREATE_URL)) {
            queueJob(httpServletRequest, httpServletResponse);
        } else {
            error(httpServletResponse, "Unknown method: " + additionalPath, 404);
        }
    }

    protected Map<String, String> getHeaders(HttpServletRequest httpServletRequest) {
        @SuppressWarnings("rawtypes")
        Enumeration headersName = httpServletRequest.getHeaderNames();
        Map<String, String> headers = new HashMap<String, String>();
        while (headersName.hasMoreElements()) {
            String name = headersName.nextElement().toString();
            headers.put(name, httpServletRequest.getHeader(name));
        }
        return headers;
    }

    protected void queueJob(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        String spec = null;
        try {
            httpServletRequest.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        if (httpServletRequest.getMethod() == "POST") {
            try {
                spec = getSpecFromPostBody(httpServletRequest);
            } catch (IOException e) {
                error(httpServletResponse, "Missing 'spec' in request body", 500);
                return;
            }
        } else {
            spec = httpServletRequest.getParameter(JSON_SPEC);
        }
        if (spec == null) {
            error(httpServletResponse, "Missing 'spec' parameter", 500);
            return;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Queue spec=" + spec);
        }

        PJsonObject specJson = MapPrinter.parseSpec(spec);

        int ref = new Random().nextInt();
        PrintWriter writer = null;
        try {
            int count = registry.getInteger(NEW_PRINT_COUNT) + 1;
            registry.setInteger(NEW_PRINT_COUNT, count);

            specJson.getInternalObj().put(JSON_REF, ref);
            specJson.getInternalObj().put(JSON_COUNT, count);
            JSONObject job = new JSONObject();
            job.put(JSON_REF, ref);
            job.put(JSON_COUNT, count);
            job.put(JSON_SPEC, specJson.getInternalObj());
            job.put(JSON_HEADERS, getHeaders(httpServletRequest));
            queue.push(job);

            httpServletResponse.setContentType("application/json; charset=utf-8");

            writer = httpServletResponse.getWriter();
            JSONWriter json = new JSONWriter(writer);
            json.object();
            {
                json.key("ref").value(ref);
            }
            json.endObject();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Create the PDF and returns to the client (in JSON) the URL to get the
     * PDF.
     * @throws JSONException
     */
    protected void createPDF(PJsonObject job) throws ServletException, JSONException {
        final String ref = job.getString(JSON_REF);
        long duration = new Date().getTime() - registry.getLong(LAST_POOL + ref);

        if (duration > 1000 * 60) {
            return;
        }

        final String app = job.optString(JSON_APP);
        final MapPrinter mapPrinter = getMapPrinter(job.getString(JSON_APP));
        final PJsonObject spec = job.getJSONObject(JSON_SPEC);
        final String outputFileName = spec.optString(Constants.OUTPUT_FILENAME_KEY);

        byte[] pdf = null;
        try {
            long start = new Date().getTime();
            pdf = doCreatePDFFile(job, mapPrinter);
            registry.setLong(TOTAL_PRINT_TIME,
                    registry.getLong(TOTAL_PRINT_TIME) +
                    (new Date().getTime() - start) / 1000);
            registry.setLong(NB_PRINT_DONE,
                    registry.getLong(NB_PRINT_DONE) + 1);
        } catch (Throwable e) {
            JSONObject status = new JSONObject();
            status.put(JSON_ERROR, e.getMessage());
            registry.setJSON(RESULT_METADATA + ref, status);
            return;
        }
        finally {
            registry.setString(LAST_PRINT_COUNT, job.getString(JSON_COUNT));
        }
        registry.setBytes(RESULT_DATA + ref, pdf);
        JSONObject metadata = new JSONObject();
        metadata.put(JSON_APP, app);
        metadata.put(JSON_FILENAME, getFileName(outputFileName, new Date()));
        registry.setJSON(RESULT_METADATA + ref, metadata);
    }


    public static String getFileName(String fileName, Date date) {
        Matcher matcher = Pattern.compile("\\$\\{(.+?)\\}").matcher(fileName);
        HashMap<String,String> replacements = new HashMap<String,String>();
        while(matcher.find()) {
            String pattern = matcher.group(1);
            String key = "${"+pattern+"}";
            replacements.put(key, findReplacement(pattern, date));
        }
        String result = fileName;
        for(Map.Entry<String,String> entry: replacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        return result;
    }

    protected String getSpecFromPostBody(HttpServletRequest httpServletRequest) throws IOException {
        if (httpServletRequest.getParameter("spec") != null) {
            return httpServletRequest.getParameter("spec");
        }
        BufferedReader data = httpServletRequest.getReader();
        try {
            StringBuilder spec = new StringBuilder();
            String cur;
            while ((cur = data.readLine()) != null) {
                spec.append(cur).append("\n");
            }
            return spec.toString();
        } finally {
            if (data != null) {
                data.close();
            }
        }
    }

    /**
     * To get the PDF created previously.
     */
    protected void getFile(String additionalPath, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
        int index = additionalPath.lastIndexOf("/");
        int ref = Integer.parseInt(additionalPath.substring(index + 1));
        byte[] pdf = registry.getBytes(RESULT_DATA);
        PJsonObject metadata = new PJsonObject(registry.getJSON(RESULT_METADATA), "metadata");

        if (pdf == null) {
            error(httpServletResponse, "Print with ref=" + ref + " unknown", 404);
            return;
        }
        sendPdfFile(metadata, httpServletResponse, pdf,
                Boolean.parseBoolean(httpServletRequest.getParameter("inline")));
    }

    /**
     * To get (in JSON) the information about the available formats and CO.
     */
    protected void getInfo(String app, HttpServletRequest req, HttpServletResponse resp, String basePath) throws ServletException,
    IOException {
        MapPrinter printer = getMapPrinter(app);
        resp.setContentType("application/json; charset=utf-8");
        final PrintWriter writer = resp.getWriter();

        try {
            JSONWriter json = new JSONWriter(writer);
            try {
                json.object();
                {
                    printer.printClientConfig(json);

                    if (app != null) {
                        json.key(JSON_APP).value(app);
                    }
                }
                json.endObject();
            } catch (JSONException e) {
                throw new ServletException(e);
            }
        } finally {
            writer.close();
        }
    }

    /**
     * Do the actual work of creating the PDF temporary file.
     *
     * @throws InterruptedException
     * @throws JRException
     * @throws ColumnBuilderException
     */
    protected byte[] doCreatePDFFile(PJsonObject job, MapPrinter mapPrinter)
            throws IOException, ServletException, InterruptedException {

        Map<String, String> headers = new HashMap<String, String>();
        TreeSet<String> configHeaders = mapPrinter.getConfiguration().getHeaders();
        if (configHeaders == null) {
            configHeaders = new TreeSet<String>();
            configHeaders.add("Referer");
            configHeaders.add("Cookie");
        }
        PJsonObject jobHeaders = job.getJSONObject(JSON_HEADERS);
        for (String header : configHeaders) {
            String headerValue = jobHeaders.optString(header);
            if (headerValue != null) {
                headers.put(header, headerValue);
            }
        }

        final PJsonObject spec = job.getJSONObject(JSON_SPEC);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mapPrinter.print(spec, out, headers);
        return out.toByteArray();
    }

    /**
     * copy the PDF into the output stream
     */
    protected void sendPdfFile(PJsonObject metadata, HttpServletResponse httpServletResponse, byte[] file, boolean inline)
            throws IOException, ServletException {
        InputStream pdf = new ByteArrayInputStream(file);
        final OutputStream response = httpServletResponse.getOutputStream();
        try {
            httpServletResponse.setContentType("application/pdf");
            if (!inline) {
                final String fileName = metadata.getString(JSON_FILENAME);
                httpServletResponse.setHeader("Content-disposition", "attachment; filename=" + fileName);
            }
            ByteStreams.copy(pdf, response);
        } finally {
            try {
                pdf.close();
            } finally {
                response.close();
            }
        }
    }

    private class PrintThread extends Thread {
        @Override
        public void run() {
            while (true) {
                if (queue.isEmpty()) {
                    try {
                        sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    JSONObject job = queue.get();
                    try {
                        createPDF(new PJsonObject(job, "job"));
                    } catch (ServletException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
