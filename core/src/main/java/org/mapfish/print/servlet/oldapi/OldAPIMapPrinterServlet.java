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

package org.mapfish.print.servlet.oldapi;


import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.Constants;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.MapPrinterFactory;
import org.mapfish.print.attribute.Attribute;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.attribute.map.MapAttribute.MapAttributeValues;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.config.WorkingDirectories;
import org.mapfish.print.output.OutputFormat;
import org.mapfish.print.servlet.BaseMapServlet;
import org.mapfish.print.servlet.NoSuchAppException;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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

import static org.mapfish.print.servlet.ServletMapPrinterFactory.DEFAULT_CONFIGURATION_FILE_KEY;

/**
 * Servlet with the old print API.
 */
@Controller
public class OldAPIMapPrinterServlet extends BaseMapServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(OldAPIMapPrinterServlet.class);

    private static final String INFO_URL = "/info.json";
    private static final String PRINT_URL = "/print.pdf";
    private static final String CREATE_URL = "/create.json";

    private static final String TEMP_FILE_PREFIX = "mapfish-print";
    private static final String TEMP_FILE_SUFFIX = ".printout";

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
    @Autowired
    private WorkingDirectories workingDirectories;


    /**
     * Print the report from a POST request.
     *
     * @param requestData the request spec as POST body
     * @param httpServletRequest the request object
     * @param httpServletResponse the response object
     */
    @RequestMapping(value = PRINT_URL, method = RequestMethod.POST)
    public final void printReportPost(
            @RequestBody final String requestData,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse) throws ServletException,
            IOException {
        if (Strings.isNullOrEmpty(requestData)) {
            error(httpServletResponse, "Missing 'spec' parameter", HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }
        createAndGetPDF(httpServletRequest, httpServletResponse, requestData);
    }

    /**
     * Print the report from a GET request. Avoid to use
     * it, the accents in the spec are not all supported.
     * 
     * @param spec the request spec as GET parameter
     * @param httpServletRequest the request object
     * @param httpServletResponse the response object
     */
    @RequestMapping(value = PRINT_URL, method = RequestMethod.GET)
    public final void printReport(
            @RequestParam(value = "spec", defaultValue = "") final String spec,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse) throws ServletException {
        if (Strings.isNullOrEmpty(spec)) {
            error(httpServletResponse, "Missing 'spec' parameter", HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }
        createAndGetPDF(httpServletRequest, httpServletResponse, spec);
    }

    /**
     * Create the report from a POST request.
     * 
     * @param baseUrl the base url to the servlet
     * @param requestData the request spec as POST body
     * @param httpServletRequest the request object
     * @param httpServletResponse the response object
     */
    @RequestMapping(value = CREATE_URL + "**" , method = RequestMethod.POST)
    public final void createReportPost(
            @RequestParam(value = "url", defaultValue = "") final String baseUrl,
            @RequestBody final String requestData,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse) throws ServletException, UnsupportedEncodingException {
        if (Strings.isNullOrEmpty(requestData)) {
            // TODO in case the POST body is empty, status code 415 is returned automatically, so we never get here
            error(httpServletResponse, "Missing 'spec' parameter", HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }
        String baseUrlPath = getBaseUrl(CREATE_URL,
                URLDecoder.decode(baseUrl, Constants.DEFAULT_ENCODING), httpServletRequest);
        createPDF(httpServletRequest, httpServletResponse, baseUrlPath, requestData);
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
     * All in one method: create and returns the PDF to the client.
     *
     * @param httpServletRequest the request object
     * @param httpServletResponse the response object
     * @param spec the request spec
     */
    public final void createAndGetPDF(final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse, final String spec) {
        try {
            httpServletRequest.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        TempFile tempFile = null;
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
     * @param spec the request spec
     */
    protected final void createPDF(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse,
                             final String basePath, final String spec) throws ServletException {
        TempFile tempFile = null;
        try {
            purgeOldTemporaryFiles();
            
            try {
                tempFile = doCreatePDFFile(spec, httpServletRequest);
            } catch (NoSuchAppException e) {
                error(httpServletResponse, e.getMessage(), HttpStatus.NOT_FOUND);
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
     * @param baseUrl  the path to the webapp
     * @param jsonpVar if given the result is returned as a variable assignment
     * @param req      the http request
     * @param resp     the http response
     */
    @RequestMapping(INFO_URL)
    public final void getInfo(
            @RequestParam(value = "url", defaultValue = "") final String baseUrl,
            @RequestParam(value = "var", defaultValue = "") final String jsonpVar,
            final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {

        final MapPrinter printer;
        try {
            printer = this.printerFactory.create(DEFAULT_CONFIGURATION_FILE_KEY);
        } catch (NoSuchAppException e) {
            error(resp, e.getMessage(), HttpStatus.NOT_FOUND);
            return;
        }
        resp.setContentType("application/json; charset=utf-8");
        final PrintWriter writer = resp.getWriter();

        try {
            if (!Strings.isNullOrEmpty(jsonpVar)) {
                writer.print("var " + jsonpVar + "=");
            }

            JSONWriter json = new JSONWriter(writer);
            try {
                json.object();
                writeInfoJson(json, baseUrl, printer, req);
                json.endObject();
            } catch (JSONException e) {
                throw new ServletException(e);
            }
            if (!Strings.isNullOrEmpty(jsonpVar)) {
                writer.print(";");
            }
        } catch (UnsupportedOperationException exc) {
            error(resp, exc.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception exc) {
            error(resp, "Unexpected error, please see the server logs", HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            writer.close();
        }
    }

    private void writeInfoJson(final JSONWriter json, final String baseUrl,
            final MapPrinter printer, final HttpServletRequest req)
            throws JSONException {
        // TODO scales
        json.key("scales");
        json.array();
        json.endArray();
        
        // TODO dpis
        json.key("dpis");
        json.array();
        json.endArray();
        
        json.key("outputFormats");
        json.array();
        {
            for (String format : printer.getOutputFormatsNames()) {
                json.object();
                json.key("name").value(format);
                json.endObject();
            }
        }
        json.endArray();
        
        json.key("layouts");
        json.array();
        writeInfoLayouts(json, printer.getConfiguration());
        json.endArray();

        String urlToUseInSpec = getBaseUrl(INFO_URL, baseUrl, req);
        json.key("printURL").value(urlToUseInSpec + PRINT_URL);
        json.key("createURL").value(urlToUseInSpec + CREATE_URL);
    }

    private void writeInfoLayouts(final JSONWriter json, final Configuration configuration) throws JSONException {
        for (String name : configuration.getTemplates().keySet()) {
            json.object();
            {
                json.key("name").value(name);
                json.key("rotation").value(true);
                
                Template template = configuration.getTemplates().get(name);
                
                // find the map attribute
                MapAttribute map = null;
                for (Attribute attribute : template.getAttributes().values()) {
                    if (attribute instanceof MapAttribute) {
                        if (map != null) {
                            throw new UnsupportedOperationException("Template '" + name + "' contains "
                                    + "more than one map configuration. The legacy API "
                                    + "supports only one map per template.");
                        } else {
                            map = (MapAttribute) attribute;
                        }
                    }
                }
                if (map == null) {
                    throw new UnsupportedOperationException("Template '" + name + "' contains "
                            + "no map configuration.");
                }
                
                json.key("map");
                json.object();
                {
                    MapAttributeValues mapValues = map.createValue(template);
                    json.key("width").value(mapValues.getMapSize().width);
                    json.key("height").value(mapValues.getMapSize().height);
                }
                json.endObject();
            }
            json.endObject();
        }
    }

    private String getBaseUrl(final String suffix, final String baseUrl, final HttpServletRequest req) {
        String urlToUseInSpec = null;
        if (!Strings.isNullOrEmpty(baseUrl) && baseUrl.endsWith(suffix)) {
            urlToUseInSpec = baseUrl.replace(suffix, "");
        } else {
            urlToUseInSpec = super.getBaseUrl(req).toString();
        }
        return urlToUseInSpec;
    }

    /**
     * Do the actual work of creating the PDF temporary file.
     *
     * @param spec the json specification in the old API format
     * @param httpServletRequest the request
     */
    protected final TempFile doCreatePDFFile(final String spec, final HttpServletRequest httpServletRequest)
            throws IOException, ServletException,
            InterruptedException, NoSuchAppException {
        if (SPEC_LOGGER.isInfoEnabled()) {
            SPEC_LOGGER.info(spec.toString());
        }

        MapPrinter mapPrinter = this.printerFactory.create(DEFAULT_CONFIGURATION_FILE_KEY);
        PJsonObject specJson = parseSpec(spec, mapPrinter);

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
     * Parse request and convert it to the new API.
     *
     * @param requestDataRaw the json in string form with no processing done
     * @param mapPrinter the mapPrinter to use for printing
     */
    public static PJsonObject parseSpec(final String requestDataRaw, final MapPrinter mapPrinter) {
        try {
            String requestData = requestDataRaw;
            if (requestData.startsWith("spec=")) {
                requestData = requestData.substring("spec=".length());
            }

            try {
                return OldAPIRequestConverter.convert(requestData, mapPrinter.getConfiguration());
            } catch (RuntimeException e) {
                try {
                    String decoded = URLDecoder.decode(requestData, Constants.DEFAULT_ENCODING);
                    if (decoded.startsWith("spec=")) {
                        decoded = decoded.substring("spec=".length());
                    }
                    return OldAPIRequestConverter.convert(decoded, mapPrinter.getConfiguration());
                } catch (UnsupportedEncodingException uee) {
                    throw new RuntimeException(e);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error parsing request data: " + requestDataRaw);
            throw new RuntimeException("Error parsing request data", e);
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
                final String fileName = tempFile.getOutputFileName(this.printerFactory.create(DEFAULT_CONFIGURATION_FILE_KEY));
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
            this.tempDir = this.workingDirectories.getReportsOldApi();
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
