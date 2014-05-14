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

import com.google.common.io.ByteStreams;

import com.google.common.io.Files;
import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.Constants;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.MapPrinterFactory;
import org.mapfish.print.output.OutputFormat;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Main print servlet.
 */
public class OldAPIMapPrinterServlet extends BaseMapServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(OldAPIMapPrinterServlet.class);

    private static final String CONTEXT_TEMPDIR = "javax.servlet.context.tempdir";

    private static final String INFO_URL = "/info.json";
    private static final String PRINT_URL = "/print.pdf";
    private static final String CREATE_URL = "/create.json";

    private static final String TEMP_FILE_PREFIX = "mapfish-print";
    private static final String TEMP_FILE_SUFFIX = ".printout";

    private String app = null;

    private static final int TEMP_FILE_PURGE_SECONDS = 10 * 60;

    private File tempDir = null;
    /**
     * Tells if a thread is already purging the old temporary files or not.
     */
    private final AtomicBoolean purging = new AtomicBoolean(false);
    /**
     * Map of temporary files.
     */
    private final Map<String, TempFile> tempFiles = new HashMap<String, TempFile>();

    @Autowired
    private MapPrinterFactory printerFactory;
    @Qualifier("servletContext")
    @Autowired
    private ServletContext servletContext;


    /**
     * Handle post requests.
     *
     * @param httpServletRequest the request object
     * @param httpServletResponse the response object
     */
    @RequestMapping(value = "/**", method = RequestMethod.POST)
    public final void doPost(final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse) throws ServletException,
            IOException {
        final String additionalPath = httpServletRequest.getPathInfo();
        if (additionalPath.equals(PRINT_URL)) {
            createAndGetPDF(httpServletRequest, httpServletResponse);
        } else if (additionalPath.equals(CREATE_URL)) {
            createPDF(httpServletRequest, httpServletResponse, getBaseUrl(httpServletRequest));
        } else {
            error(httpServletResponse, "Unknown method: " + additionalPath, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Initialization method, called by spring.
     */
    @PostConstruct
    public final void init() throws ServletException {
        //get rid of the temporary files that were present before the servlet was started.
        File dir = getTempDir();
        for (File file : Files.fileTreeTraverser().children(dir)) {
            deleteFile(file);
        }
    }

    /**
     * Destroy method, called by spring.
     */
    @PreDestroy
    public final void destroy() {
        synchronized (this.tempFiles) {
            for (File file : this.tempFiles.values()) {
                deleteFile(file);
            }
            this.tempFiles.clear();
        }
    }

    /**
     * All in one method: create and returns the PDF to the client. Avoid to use
     * it, the accents in the spec are not all supported.
     *
     * @param httpServletRequest the request object
     * @param httpServletResponse the response object
     */
    @RequestMapping(PRINT_URL)
    public final void createAndGetPDF(final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse) {
        //get the spec from the query
        TempFile tempFile = null;
        String spec;
        try {
            httpServletRequest.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        if (httpServletRequest.getMethod().equals("POST")) {
            try {
                spec = getSpecFromPostBody(httpServletRequest);
            } catch (IOException e) {
                error(httpServletResponse, "Missing 'spec' in request body", HttpStatus.INTERNAL_SERVER_ERROR);
                return;
            }
        } else {
            spec = httpServletRequest.getParameter("spec");
        }
        if (spec == null) {
            error(httpServletResponse, "Missing 'spec' parameter", HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }

        try {
            tempFile = doCreatePDFFile(spec, httpServletRequest);
            sendPdfFile(httpServletResponse, tempFile, Boolean.parseBoolean(httpServletRequest.getParameter("inline")));
        } catch (NoSuchAppException e) {
            error(httpServletResponse, e.getMessage(), HttpStatus.NOT_FOUND);
            return;
        } catch (Throwable e) {
            error(httpServletResponse, e);
        } finally {
            deleteFile(tempFile);
        }
    }

    /**
     * Create the PDF and returns to the client (in JSON) the URL to get the PDF.
     * @param httpServletRequest the request object
     * @param httpServletResponse the response object
     * @param basePath the path of the webapp
     */
    protected final void createPDF(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse,
                             final String basePath) throws ServletException {
        TempFile tempFile = null;
        try {
            purgeOldTemporaryFiles();

            String spec = getSpecFromPostBody(httpServletRequest);
            try {
                tempFile = doCreatePDFFile(spec, httpServletRequest);
            } catch (NoSuchAppException e) {
                error(httpServletResponse, e.getMessage(), HttpStatus.NOT_FOUND);
                return;
            }
            if (tempFile == null) {
                error(httpServletResponse, "Missing 'spec' parameter", HttpStatus.INTERNAL_SERVER_ERROR);
                return;
            }
        } catch (Throwable e) {
            deleteFile(tempFile);
            error(httpServletResponse, e);
            return;
        }

        final String id = generateId(tempFile);
        httpServletResponse.setContentType("application/json; charset=utf-8");
        PrintWriter writer = null;
        try {
            writer = httpServletResponse.getWriter();
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
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        addTempFile(tempFile, id);
    }

    /**
     * Add a temporary file to the tracker of temporary files.
     *
     * @param tempFile the file to add.
     * @param id an id for looking it up again later.
     */
    protected final void addTempFile(final TempFile tempFile, final String id) {
        synchronized (this.tempFiles) {
            this.tempFiles.put(id, tempFile);
        }
    }

    /**
     * Read the json from the http request.
     *
     * @param httpServletRequest the request
     */
    protected final String getSpecFromPostBody(final HttpServletRequest httpServletRequest) throws IOException {
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
     * To get the PDF created previously and write it to the http response.
     *
     * @param req the http request
     * @param response the http response
     * @param id the id for the file
     */
    @RequestMapping("/{id}" + TEMP_FILE_SUFFIX)
    public final void getFile(@PathVariable final String id, final HttpServletRequest req, final HttpServletResponse response)
            throws IOException, ServletException {
        final TempFile file;
        synchronized (this.tempFiles) {
            file = this.tempFiles.get(id);
        }
        if (file == null) {
            error(response, "File with id=" + id + " unknown", HttpStatus.NOT_FOUND);
            return;
        }
        sendPdfFile(response, file, Boolean.parseBoolean(req.getParameter("inline")));
    }

    /**
     * To get (in JSON) the information about the available formats and CO.
     *
     * @param req the http request
     * @param resp the http response
     * @param basePath the path to the webapp
     */
    @RequestMapping(INFO_URL)
    public final void getInfo(final HttpServletRequest req, final HttpServletResponse resp, final String basePath)
            throws ServletException, IOException {
        this.app = req.getParameter("app");
        //System.out.println("app = "+app);

        final MapPrinter printer;
        try {
            printer = this.printerFactory.create(this.app);
        } catch (NoSuchAppException e) {
            error(resp, e.getMessage(), HttpStatus.NOT_FOUND);
            return;
        }
        resp.setContentType("application/json; charset=utf-8");
        final PrintWriter writer = resp.getWriter();

        try {
            final String var = req.getParameter("var");
            if (var != null) {
                writer.print(var + "=");
            }

            JSONWriter json = new JSONWriter(writer);
            try {
                json.object();
                {
                    printer.printClientConfig(json);
                    String urlToUseInSpec = basePath;

                    String proxyUrl = printer.getConfiguration().getProxyBaseUrl();
                    if (proxyUrl != null) {
                        urlToUseInSpec = proxyUrl;
                    }
                    json.key("printURL").value(urlToUseInSpec + PRINT_URL);
                    json.key("createURL").value(urlToUseInSpec + CREATE_URL);
                    if (this.app != null) {
                        json.key("app").value(this.app);
                    }
                }
                json.endObject();
            } catch (JSONException e) {
                throw new ServletException(e);
            }
            if (var != null) {
                writer.print(";");
            }
        } finally {
            writer.close();
        }
    }


    /**
     * Do the actual work of creating the PDF temporary file.
     *
     * @param spec the json specification
     * @param httpServletRequest the request
     */
    protected final TempFile doCreatePDFFile(final String spec, final HttpServletRequest httpServletRequest)
            throws IOException, ServletException,
            InterruptedException, NoSuchAppException {
        if (SPEC_LOGGER.isInfoEnabled()) {
            SPEC_LOGGER.info(spec.toString());
        }

        PJsonObject specJson = MapPrinter.parseSpec(spec);
        if (specJson.has("app")) {
            this.app = specJson.getString("app");
        } else {
            this.app = null;
        }

        MapPrinter mapPrinter = this.printerFactory.create(this.app);

        Map<String, String> headers = new HashMap<String, String>();
        TreeSet<String> configHeaders = mapPrinter.getConfiguration().getHeaders();
        if (configHeaders == null) {
            configHeaders = new TreeSet<String>();
            configHeaders.add("Referer");
            configHeaders.add("Cookie");
        }
        for (String header : configHeaders) {
            if (httpServletRequest.getHeader(header) != null) {
                headers.put(header, httpServletRequest.getHeader(header));
            }
        }

        final OutputFormat outputFormat = mapPrinter.getOutputFormat(specJson);
        //create a temporary file that will contain the PDF
        final File tempJavaFile = File.createTempFile(TEMP_FILE_PREFIX, "." + outputFormat.getFileSuffix() + TEMP_FILE_SUFFIX,
                getTempDir());
        TempFile tempFile = new TempFile(tempJavaFile, specJson, outputFormat);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(tempFile);
            mapPrinter.print(specJson, out, headers);

            return tempFile;
        } catch (Exception e) {
            deleteFile(tempFile);
            throw new RuntimeException(e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * copy the PDF into the output stream.
     *
     * @param httpServletResponse response obj.
     * @param tempFile the file to send to response
     * @param inline response should be in-lined.
     */
    protected final void sendPdfFile(final HttpServletResponse httpServletResponse, final TempFile tempFile, final boolean inline)
            throws IOException,
            ServletException {
        FileInputStream pdf = new FileInputStream(tempFile);
        final OutputStream response = httpServletResponse.getOutputStream();
        try {
            httpServletResponse.setContentType(tempFile.contentType());
            if (!inline) {
                final String fileName = tempFile.getOutputFileName(this.printerFactory.create(this.app));
                httpServletResponse.setHeader("Content-disposition", "attachment; filename=" + fileName);
            }
            ByteStreams.copy(pdf, response);
        } catch (NoSuchAppException e) {
            error(httpServletResponse, e.getMessage(), HttpStatus.NOT_FOUND);
            return;
        } finally {
            try {
                pdf.close();
            } finally {
                response.close();
            }
        }
    }

    /**
     * Get and cache the temporary directory to use for saving the generated PDF files.
     */
    protected final File getTempDir() {
        if (this.tempDir == null) {
            String tempDirPath = this.servletContext.getInitParameter("tempdir");
            if (tempDirPath != null) {
                this.tempDir = new File(tempDirPath);
            } else {
                this.tempDir = (File) this.servletContext.getAttribute(CONTEXT_TEMPDIR);
            }
            if (!this.tempDir.exists() && !this.tempDir.mkdirs()) {
                throw new RuntimeException("unable to create dir:" + this.tempDir);
            }

        }
        LOGGER.debug("Using '" + this.tempDir.getAbsolutePath() + "' as temporary directory");
        return this.tempDir;
    }

    /**
     * If the file is defined, delete it.
     *
     * @param file the file to delete
     */
    protected final void deleteFile(final File file) {
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
     *
     * @param tempFile the file to generate an id for
     */
    protected final String generateId(final File tempFile) {
        final String name = tempFile.getName();
        return name.substring(
                TEMP_FILE_PREFIX.length(),
                name.length() - TEMP_FILE_SUFFIX.length());
    }

    /**
     * Will purge all the known temporary files older than TEMP_FILE_PURGE_SECONDS.
     *
     */
    protected final void purgeOldTemporaryFiles() {
        if (!this.purging.getAndSet(true)) {
            final long minTime = System.currentTimeMillis() - TEMP_FILE_PURGE_SECONDS * TimeUnit.SECONDS.toMillis(1);
            synchronized (this.tempFiles) {
                Iterator<Map.Entry<String, TempFile>> it = this.tempFiles.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, TempFile> entry = it.next();
                    if (entry.getValue().creationTime < minTime) {
                        deleteFile(entry.getValue());
                        it.remove();
                    }
                }
            }
            this.purging.set(false);
        }
    }

    /**
     * Get the base url of the webapp.
     *
     * @param httpServletRequest the http request object.
     */
    protected final String getBaseUrl(final HttpServletRequest httpServletRequest) {
        final String additionalPath = httpServletRequest.getPathInfo();
        String fullUrl = httpServletRequest.getParameter("url");
        if (fullUrl != null) {
            return fullUrl.replaceFirst(additionalPath + "$", "");
        } else {
            return httpServletRequest.getRequestURL().toString()
                    .replaceFirst(additionalPath + "$", "");
        }
    }

    /**
     * Represents a pdf output file to send to the client.
     *
     * @author Jesse
     *
     */
    private static class TempFile extends File {
        private static final long serialVersionUID = 455104129549002361L;

        // CSOFF: VisibilityModifier
        final long creationTime;
        public final String printedLayoutName;
        public final String outputFileName;
        // CSON: VisibilityModifier
        private final String contentType;
        private final String suffix;

        public TempFile(final File tempFile, final PJsonObject jsonSpec, final OutputFormat format) {
            super(tempFile.getAbsolutePath());
            this.creationTime = System.currentTimeMillis();
            this.outputFileName = jsonSpec.optString(Constants.OUTPUT_FILENAME_KEY);
            this.printedLayoutName = jsonSpec.optString(Constants.JSON_LAYOUT_KEY, null);

            this.suffix = format.getFileSuffix();
            this.contentType = format.getContentType();
        }

        public String getOutputFileName(final MapPrinter mapPrinter) {
            if (this.outputFileName != null) {
                return formatFileName(this.suffix, this.outputFileName, new Date());
            } else {
                return formatFileName(this.suffix, mapPrinter.getOutputFilename(this.printedLayoutName, getName()), new Date());
            }
        }


        public static String formatFileName(final String fileSuffix, final String startingName, final Date date) {
            Matcher matcher = Pattern.compile("\\$\\{(.+?)\\}").matcher(startingName);
            HashMap<String, String> replacements = new HashMap<String, String>();
            while (matcher.find()) {
                String pattern = matcher.group(1);
                String key = "${" + pattern + "}";
                replacements.put(key, findReplacement(pattern, date));
            }
            String result = startingName;
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                result = result.replace(entry.getKey(), entry.getValue());
            }

            String suffix = fileSuffix;
            while (suffix.startsWith(".")) {
                suffix = suffix.substring(1);
            }
            if (suffix.isEmpty() || result.toLowerCase().endsWith("." + suffix.toLowerCase())) {
                return result;
            } else {
                return result + "." + suffix;
            }
        }

        public String contentType() {
            return this.contentType;
        }
    }
}
