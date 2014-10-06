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
import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.Constants;
import org.mapfish.print.ExceptionUtils;
import org.mapfish.print.MapPrinter;
import org.mapfish.print.MapPrinterFactory;
import org.mapfish.print.attribute.Attribute;
import org.mapfish.print.attribute.map.MapAttribute;
import org.mapfish.print.attribute.map.MapAttribute.MapAttributeValues;
import org.mapfish.print.attribute.map.ZoomLevels;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.servlet.BaseMapServlet;
import org.mapfish.print.servlet.MapPrinterServlet;
import org.mapfish.print.servlet.NoSuchAppException;
import org.mapfish.print.servlet.job.JobManager;
import org.mapfish.print.servlet.job.NoSuchReferenceException;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mapfish.print.servlet.ServletMapPrinterFactory.DEFAULT_CONFIGURATION_FILE_KEY;

/**
 * Servlet with the old print API.
 */
@Controller
public class OldAPIMapPrinterServlet extends BaseMapServlet {
    static final String REPORT_SUFFIX = ".printout";
    private static final String DEP_SEG = "/dep";
    private static final String INFO_URL = "/info.json";
    private static final String DEP_INFO_URL = DEP_SEG + INFO_URL;
    private static final String PRINT_URL = "/print.pdf";
    private static final String DEP_PRINT_URL = DEP_SEG + PRINT_URL;
    private static final String CREATE_URL = "/create.json";
    private static final String DEP_CREATE_URL = DEP_SEG + CREATE_URL;

    private static final int HALF_SECOND = 500;
    static final String JSON_PRINT_URL = "printURL";
    static final String JSON_CREATE_URL = "createURL";

    @Autowired
    private MapPrinterFactory printerFactory;

    @Autowired
    private MapPrinterServlet primaryApiServlet;
    @Autowired
    private JobManager jobManager;

    /**
     * Print the report from a POST request.
     *
     * @param requestData         the request spec as POST body
     * @param httpServletRequest  the request object
     * @param httpServletResponse the response object
     */
    @RequestMapping(value = DEP_PRINT_URL, method = RequestMethod.POST)
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
     * @param spec                the request spec as GET parameter
     * @param httpServletRequest  the request object
     * @param httpServletResponse the response object
     */
    @RequestMapping(value = DEP_PRINT_URL, method = RequestMethod.GET)
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
     * @param baseUrl             the base url to the servlet
     * @param spec                if spec is form data then this will be nonnull
     * @param requestData         the request spec as POST body
     * @param httpServletRequest  the request object
     * @param httpServletResponse the response object
     */
    @RequestMapping(value = DEP_CREATE_URL + "**", method = RequestMethod.POST)
    public final void createReportPost(
            @RequestParam(value = "url", defaultValue = "") final String baseUrl,
            @RequestParam(value = "spec", required = false) final String spec,
            @RequestBody final String requestData,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse) throws IOException, JSONException {
        if (Strings.isNullOrEmpty(requestData)) {
            // TODO in case the POST body is empty, status code 415 is returned automatically, so we never get here
            error(httpServletResponse, "Missing 'spec' parameter", HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }
        String baseUrlPath = getBaseUrl(DEP_CREATE_URL,
                URLDecoder.decode(baseUrl, Constants.DEFAULT_ENCODING), httpServletRequest);
        String specData = spec == null ? requestData : spec;
        createPDF(httpServletRequest, httpServletResponse, baseUrlPath, specData);
    }

    /**
     * All in one method: create and returns the PDF to the client.
     *
     * @param httpServletRequest  the request object
     * @param httpServletResponse the response object
     * @param spec                the request spec
     */
    private void createAndGetPDF(final HttpServletRequest httpServletRequest,
                                 final HttpServletResponse httpServletResponse, final String spec) {
        try {
            httpServletRequest.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }

        try {
            String jobRef = doCreatePDFFile(spec, httpServletRequest, httpServletResponse);
            this.primaryApiServlet.getReport(jobRef, false, httpServletResponse);
        } catch (NoSuchAppException e) {
            error(httpServletResponse, e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Throwable e) {
            error(httpServletResponse, e);
        }
    }

    /**
     * Create the PDF and returns to the client (in JSON) the URL to get the PDF.
     *
     * @param httpServletRequest  the request object
     * @param httpServletResponse the response object
     * @param basePath            the path of the webapp
     * @param spec                the request spec
     */
    protected final void createPDF(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse,
                                   final String basePath, final String spec) throws IOException, JSONException {
        String jobRef;
        try {
            try {
                jobRef = doCreatePDFFile(spec, httpServletRequest, httpServletResponse);
                httpServletResponse.setContentType("application/json; charset=utf-8");
                PrintWriter writer = null;
                try {
                    writer = httpServletResponse.getWriter();
                    JSONWriter json = new JSONWriter(writer);
                    json.object();
                    {
                        json.key("getURL").value(basePath + "/" + jobRef + REPORT_SUFFIX);
                    }
                    json.endObject();
                } finally {
                    if (writer != null) {
                        writer.close();
                    }
                }
            } catch (NoSuchAppException e) {
                error(httpServletResponse, e.getMessage(), HttpStatus.NOT_FOUND);
                return;
            }
        } catch (Throwable e) {
            error(httpServletResponse, e);
            return;
        }

    }

    /**
     * To get the PDF created previously and write it to the http response.
     *
     * @param inline   if true then inline the response
     * @param response the http response
     * @param id       the id for the file
     */
    @RequestMapping(DEP_SEG + "/{id:.+}" + REPORT_SUFFIX)
    public final void getFile(@PathVariable final String id,
                              @RequestParam(value = "inline", defaultValue = "false") final boolean inline,
                              final HttpServletResponse response)
            throws IOException, ServletException {
        this.primaryApiServlet.getReport(id, inline, response);
    }

    /**
     * To get (in JSON) the information about the available formats and CO.
     *
     * @param baseUrl  the path to the webapp
     * @param jsonpVar if given the result is returned as a variable assignment
     * @param req      the http request
     * @param resp     the http response
     */
    @RequestMapping(DEP_INFO_URL)
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

        writeInfoLayouts(json, printer.getConfiguration());

        String urlToUseInSpec = getBaseUrl(DEP_INFO_URL, baseUrl, req);
        json.key(JSON_PRINT_URL).value(urlToUseInSpec + PRINT_URL);
        json.key(JSON_CREATE_URL).value(urlToUseInSpec + CREATE_URL);
    }

    private void writeInfoLayouts(final JSONWriter json, final Configuration configuration) throws JSONException {
        Double maxDpi = null;
        double[] dpiSuggestions = null;
        ZoomLevels zoomLevels = null;

        json.key("layouts");
        json.array();
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
                
                MapAttributeValues mapValues = map.createValue(template);
                json.key("map");
                json.object();
                {
                    json.key("width").value(mapValues.getMapSize().width);
                    json.key("height").value(mapValues.getMapSize().height);
                }
                json.endObject();
                
                // get the zoom levels and dpi values from the first template
                if (maxDpi == null) {
                    maxDpi = map.getMaxDpi();
                    dpiSuggestions = map.getDpiSuggestions();
                }
                if (zoomLevels == null) {
                    zoomLevels = mapValues.getZoomLevels();
                }
            }
            json.endObject();
        }
        json.endArray();

        json.key("dpis");
        json.array();
        {
            if (dpiSuggestions != null) {
                for (Double dpi : dpiSuggestions) {
                    json.object();
                    {
                        json.key("name").value(Integer.toString(dpi.intValue()));
                        json.key("value").value(Integer.toString(dpi.intValue()));
                    }
                    json.endObject();
                }
            }
        }
        json.endArray();

        json.key("scales");
        json.array();
        {
            if (zoomLevels != null) {
                {
                    for (int i = 0; i < zoomLevels.size(); i++) {
                        double scale = zoomLevels.get(i);
                        json.object();
                        {
                            String scaleValue = new DecimalFormat("#.##").format(scale);
                            json.key("name").value("1:" + scaleValue);
                            json.key("value").value(scaleValue);
                        }
                        json.endObject();
                        
                    }
                }
            }
        }
        json.endArray();
    }

    private String getBaseUrl(final String suffix, final String baseUrl, final HttpServletRequest req) {
        String urlToUseInSpec;
        if (!Strings.isNullOrEmpty(baseUrl) && baseUrl.endsWith(suffix)) {
            urlToUseInSpec = baseUrl.replace(suffix, DEP_SEG);
        } else if (!Strings.isNullOrEmpty(baseUrl)) {
            urlToUseInSpec = removeLastSlash(baseUrl);
        } else {
            urlToUseInSpec = removeLastSlash(super.getBaseUrl(req).toString()) + DEP_SEG;
        }

        urlToUseInSpec = removeLastSlash(urlToUseInSpec);
        return urlToUseInSpec;
    }

    private String removeLastSlash(final String urlToUseInSpec) {
        if (urlToUseInSpec.endsWith("/")) {
            return urlToUseInSpec.substring(1);
        }
        return urlToUseInSpec;
    }

    /**
     * Do the actual work of creating the PDF temporary file.
     *
     * @param spec               the json specification in the old API format
     * @param httpServletRequest the request
     */
    private String doCreatePDFFile(final String spec,
                                   final HttpServletRequest httpServletRequest,
                                   final HttpServletResponse httpServletResponse)
            throws IOException, ServletException,
            InterruptedException, NoSuchAppException, NoSuchReferenceException {
        if (SPEC_LOGGER.isInfoEnabled()) {
            SPEC_LOGGER.info("\nOLD-API:\n" + spec);
        }

        PJsonObject specJson = MapPrinterServlet.parseJson(spec, httpServletResponse);
        String appId;
        if (specJson.has("app")) {
            appId = specJson.getString("app");
        } else {
            appId = DEFAULT_CONFIGURATION_FILE_KEY;
        }
        MapPrinter mapPrinter = this.printerFactory.create(appId);
        PJsonObject updatedSpecJson = null;
        try {
            updatedSpecJson = OldAPIRequestConverter.convert(specJson, mapPrinter.getConfiguration());

            String format = updatedSpecJson.optString(MapPrinterServlet.JSON_OUTPUT_FORMAT, "pdf");
            final String jobReferenceId = this.primaryApiServlet.createAndSubmitPrintJob(appId, format,
                    updatedSpecJson.getInternalObj().toString(), httpServletRequest,
                    httpServletResponse);
            boolean isDone = false;
            while (!isDone) {
                Thread.sleep(HALF_SECOND);
                isDone = this.jobManager.isDone(jobReferenceId);
            }

            return jobReferenceId;
        } catch (JSONException e) {
            throw ExceptionUtils.getRuntimeException(e);
        }

    }

}
