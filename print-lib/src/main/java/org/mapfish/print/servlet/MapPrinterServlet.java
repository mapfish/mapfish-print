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

package org.mapfish.print.servlet;

import com.lowagie.text.DocumentException;
import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.MapPrinter;
import org.pvalsecc.misc.FileUtilities;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main print servlet.
 */
public class MapPrinterServlet extends BaseMapServlet {
    private static final String INFO_URL = "/info.json";
    private static final String PRINT_URL = "/print.pdf";
    private static final String CREATE_URL = "/create.json";
    protected static final String TEMP_FILE_PREFIX = "mapfish-print";
    protected static final String TEMP_FILE_SUFFIX = ".pdf";
    private static final int TEMP_FILE_PURGE_SECONDS = 10 * 60;

    private File tempDir = null;

    /**
     * Tells if a thread is alread purging the old temporary files or not.
     */
    private AtomicBoolean purging = new AtomicBoolean(false);

    /**
     * Map of temporary files.
     */
    private final Map<String, TempFile> tempFiles = new HashMap<String, TempFile>();

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        //do the routing in function of the actual URL
        final String additionalPath = httpServletRequest.getPathInfo();
        if (additionalPath.equals(PRINT_URL)) {
            createAndGetPDF(httpServletRequest, httpServletResponse);
        } else if (additionalPath.equals(INFO_URL)) {
            getInfo(httpServletRequest, httpServletResponse, getBaseUrl(httpServletRequest));
        } else if (additionalPath.startsWith("/") && additionalPath.endsWith(TEMP_FILE_SUFFIX)) {
            getPDF(httpServletResponse, additionalPath.substring(1, additionalPath.length() - 4));
        } else {
            error(httpServletResponse, "Unknown method: " + additionalPath, 404);
        }
    }

    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        final String additionalPath = httpServletRequest.getPathInfo();
        if (additionalPath.equals(CREATE_URL)) {
            createPDF(httpServletRequest, httpServletResponse, getBaseUrl(httpServletRequest));
        } else {
            error(httpServletResponse, "Unknown method: " + additionalPath, 404);
        }
    }

    public void init() throws ServletException {
        //get rid of the temporary files that were present before the applet was started.
        File dir = getTempDir();
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; ++i) {
            File file = files[i];
            final String name = file.getName();
            if (name.startsWith(TEMP_FILE_PREFIX) &&
                    name.endsWith(TEMP_FILE_SUFFIX)) {
                deleteFile(file);
            }
        }
    }

    public void destroy() {
        synchronized (tempFiles) {
            for (File file : tempFiles.values()) {
                deleteFile(file);
            }
            tempFiles.clear();
        }
        super.destroy();
    }

    /**
     * All in one method: create and returns the PDF to the client. Avoid to use
     * it, the accents in the spec are not all supported.
     */
    protected void createAndGetPDF(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        //get the spec from the query
        try {
            httpServletRequest.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        final String spec = httpServletRequest.getParameter("spec");
        if (spec == null) {
            error(httpServletResponse, "Missing 'spec' parameter", 500);
            return;
        }

        File tempFile = null;
        try {
            tempFile = doCreatePDFFile(spec, httpServletRequest);
            sendPdfFile(httpServletResponse, tempFile);
        } catch (Throwable e) {
            error(httpServletResponse, e);
        } finally {
            deleteFile(tempFile);
        }
    }

    /**
     * Create the PDF and returns to the client (in JSON) the URL to get the PDF.
     */
    protected void createPDF(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, String basePath) throws ServletException {
        TempFile tempFile = null;
        try {
            purgeOldTemporaryFiles();

            String spec = getSpecFromPostBody(httpServletRequest);
            tempFile = doCreatePDFFile(spec, httpServletRequest);
            if (tempFile == null) {
                error(httpServletResponse, "Missing 'spec' parameter", 500);
                return;
            }
        } catch (Throwable e) {
            deleteFile(tempFile);
            error(httpServletResponse, e);
            return;
        }

        final String id = generateId(tempFile);
        httpServletResponse.setContentType("application/json; charset=utf-8");
        try {
            final PrintWriter writer = httpServletResponse.getWriter();
            JSONWriter json = new JSONWriter(writer);
            json.object();
            {
                json.key("getURL").value(basePath + "/" + id + TEMP_FILE_SUFFIX);
            }
            json.endObject();
        } catch (JSONException e) {
            deleteFile(tempFile);
            throw new ServletException(e);
        } catch (IOException e) {
            deleteFile(tempFile);
            throw new ServletException(e);
        }
        addTempFile(tempFile, id);
    }

    protected void addTempFile(TempFile tempFile, String id) {
        synchronized (tempFiles) {
            tempFiles.put(id, tempFile);
        }
    }

    protected String getSpecFromPostBody(HttpServletRequest httpServletRequest) throws IOException {
        BufferedReader data = httpServletRequest.getReader();
        StringBuilder spec = new StringBuilder();
        String cur;
        while ((cur = data.readLine()) != null) {
            spec.append(cur).append("\n");
        }
        return spec.toString();
    }

    /**
     * To get the PDF created previously.
     */
    protected void getPDF(HttpServletResponse httpServletResponse, String id) throws IOException {
        final File file;
        synchronized (tempFiles) {
            file = tempFiles.get(id);
        }
        if (file == null) {
            error(httpServletResponse, "File with id=" + id + " unknown", 404);
            return;
        }

        sendPdfFile(httpServletResponse, file);
    }

    /**
     * To get (in JSON) the information about the available formats and CO.
     */
    protected void getInfo(HttpServletRequest req, HttpServletResponse resp, String basePath) throws ServletException, IOException {
        MapPrinter printer = getMapPrinter();
        resp.setContentType("application/json; charset=utf-8");
        final PrintWriter writer = resp.getWriter();

        final String var = req.getParameter("var");
        if (var != null) {
            writer.print(var + "=");
        }

        JSONWriter json = new JSONWriter(writer);
        try {
            json.object();
            {
                printer.printClientConfig(json);
                json.key("printURL").value(basePath + PRINT_URL);
                json.key("createURL").value(basePath + CREATE_URL);
            }
            json.endObject();
        } catch (JSONException e) {
            throw new ServletException(e);
        }
        if (var != null) {
            writer.print(";");
        }
        writer.close();
    }


    /**
     * Do the actual work of creating the PDF temporary file.
     */
    protected TempFile doCreatePDFFile(String spec, HttpServletRequest httpServletRequest) throws IOException, DocumentException, ServletException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Generating PDF for spec=" + spec);
        }

        String referer = httpServletRequest.getHeader("Referer");
        //create a temporary file that will contain the PDF
        TempFile tempFile = new TempFile(File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX, getTempDir()));
        try {
            FileOutputStream out = new FileOutputStream(tempFile);

            getMapPrinter().print(spec, out, referer);
            out.close();
            return tempFile;
        } catch (IOException e) {
            deleteFile(tempFile);
            throw e;
        } catch (DocumentException e) {
            deleteFile(tempFile);
            throw e;
        } catch (ServletException e) {
            deleteFile(tempFile);
            throw e;
        }
    }

    /**
     * copy the PDF into the output stream
     */
    protected void sendPdfFile(HttpServletResponse httpServletResponse, File tempFile) throws IOException {
        FileInputStream pdf = new FileInputStream(tempFile);
        final OutputStream response = httpServletResponse.getOutputStream();
        httpServletResponse.setContentType("application/pdf");
        httpServletResponse.setHeader("Content-disposition", "attachment; filename=" + tempFile.getName());
        FileUtilities.copyStream(pdf, response);
        pdf.close();
        response.close();
    }

    /**
     * Send an error XXX to the client with an exception
     */
    protected void error(HttpServletResponse httpServletResponse, Throwable e) {
        try {
            httpServletResponse.setContentType("text/plain");
            httpServletResponse.setStatus(500);
            PrintWriter out = httpServletResponse.getWriter();
            out.println("Error while generating PDF:");
            e.printStackTrace(out);
            out.close();

            LOGGER.error("Error while generating PDF", e);
        } catch (IOException ex) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Send an error XXX to the client with a message
     */
    protected void error(HttpServletResponse httpServletResponse, String message, int code) {
        try {
            httpServletResponse.setContentType("text/plain");
            httpServletResponse.setStatus(code);
            PrintWriter out = httpServletResponse.getWriter();
            out.println("Error while generating PDF:");
            out.println(message);
            out.close();
            LOGGER.error("Error while generating PDF: " + message);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get and cache the temporary directory to use for saving the generated PDF files.
     */
    protected File getTempDir() {
        if (tempDir == null) {
            tempDir = (File) getServletContext().
                    getAttribute("javax.servlet.context.tempdir");
            LOGGER.debug("Using '" + tempDir.getAbsolutePath() + "' as temporary directory");
        }
        return tempDir;
    }

    /**
     * If the file is defined, delete it.
     */
    protected void deleteFile(File file) {
        if (file != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Deleting PDF file: " + file.getName());
            }
            if (!file.delete()) {
                LOGGER.warn("Cannot delete file:" + file.getAbsolutePath());
            }
        }
    }

    /**
     * Get the ID to use in function of the filename (filename without the prefix and the extension).
     */
    protected String generateId(File tempFile) {
        final String name = tempFile.getName();
        return name.substring(
                TEMP_FILE_PREFIX.length(),
                name.length() - TEMP_FILE_SUFFIX.length());
    }

    protected String getBaseUrl(HttpServletRequest httpServletRequest) {
        final String additionalPath = httpServletRequest.getPathInfo();
        String fullUrl = httpServletRequest.getParameter("url");
        if (fullUrl != null) {
            return fullUrl.replaceFirst(additionalPath + "$", "");
        } else {
            return httpServletRequest.getRequestURL().toString().replaceFirst(additionalPath + "$", "");
        }
    }

    /**
     * Will purge all the known temporary files older than TEMP_FILE_PURGE_SECONDS.
     */
    protected void purgeOldTemporaryFiles() {
        if (!purging.getAndSet(true)) {
            final long minTime = System.currentTimeMillis() - TEMP_FILE_PURGE_SECONDS * 1000L;
            synchronized (tempFiles) {
                Iterator<Map.Entry<String, TempFile>> it = tempFiles.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, TempFile> entry = it.next();
                    if (entry.getValue().creationTime < minTime) {
                        deleteFile(entry.getValue());
                        it.remove();
                    }
                }
            }
            purging.set(false);
        }
    }

    protected static class TempFile extends File {
        private final long creationTime;

        public TempFile(File tempFile) {
            super(tempFile.getAbsolutePath());
            creationTime = System.currentTimeMillis();
        }
    }
}
