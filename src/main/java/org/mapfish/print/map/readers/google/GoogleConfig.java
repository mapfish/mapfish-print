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

package org.mapfish.print.map.readers.google;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.config.Config;
import org.mapfish.print.config.Key;
import org.mapfish.print.utils.PJsonArray;
import org.mapfish.print.utils.PJsonObject;

/**
 * User: jeichar
 * Date: 10/15/10
 * Time: 2:40 PM
 */
public class GoogleConfig {
    private final Logger logger;

    public final String format;
    public final String sensor;
    public final String maptype;
    public final GoogleURLSigner signer;
    public final String language;
    public final List<String> markers;
    public final String path;

    public GoogleConfig(RenderingContext context, PJsonObject params, Logger logger, URI baseUrl, boolean requireKey) {
        format = params.getString("format");
        sensor = params.getString("sensor");
        maptype = params.getString("maptype");
        language = params.optString("language");
        path = addEscapes(params.optString("path"));
        markers = getList(params, "markers");
        signer = createUriSigner(context, baseUrl, requireKey);
        this.logger = logger;
    }

    private List<String> getList(PJsonObject params, String s) {
        PJsonArray markerArray = params.optJSONArray(s);
        List<String> list;
        if(markerArray == null) {
            list = Collections.emptyList();
        } else {
            list = new ArrayList<String>(markerArray.size());
            for(int i=0; i< markerArray.size(); i++) {
                String base = markerArray.getString(i);
                base = addEscapes(base);
                list.add(base);
            }
        }
        return Collections.unmodifiableList(list);
    }

    private String addEscapes(String base) {
        if(base !=null) {
            return base;//.replace("|", "%7C");
        } else {
            return null;
        }
    }

    private GoogleURLSigner createUriSigner(RenderingContext context, URI baseUrl, boolean requireKey) {
        GoogleURLSigner signer = null;
        Config config = context.getConfig();
        for (Key key : config.getKeys()) {
            try {
                URI localhostURI = new URI("http://" + InetAddress.getLocalHost().getHostName());
                if (key.getHost().validate(baseUrl)){
                    if(key.getDomain().validate(localhostURI)) {
                        signer = new GoogleURLSigner(key);
                    } else {
                        if(logger.isDebugEnabled()) {
                            logger.debug("Failed domain matching: "+localhostURI+" to "+key.getDomain());
                        }
                    }
                } else {
                    if(logger.isDebugEnabled()) {
                        logger.debug("Failed host matching: "+baseUrl+" to "+key.getHost());
                    }
                }
            } catch (UnknownHostException e) {
                // continue assume not match
                if(logger.isDebugEnabled()) {
                    logger.debug("Failed host matching", e);
                }
            } catch (SocketException e) {
                // continue assume not match
                if(logger.isDebugEnabled()) {
                    logger.debug("Failed host matching", e);
                }
            } catch (MalformedURLException e) {
                // continue assume not match
                if(logger.isDebugEnabled()) {
                    logger.debug("Failed host matching", e);
                }
            } catch (URISyntaxException e) {
                throw new Error(e);
            }
        }
        if(signer==null && requireKey) {
            throw new RuntimeException(baseUrl+" is a google layer and therefore it needs a key" +
                    " obtained from google so be usable in a non-webbrowser application.  Add a keys section to the config.yaml file or use TiledGoogle type instead. ");
        }

        return signer;
    }


    URI signURI(URI uri) throws UnsupportedEncodingException,
            URISyntaxException {
        try {
            String[] path = uri.toString().substring(uri.toString().indexOf(uri.getPath())).split("#",2);
            String signedURI = uri.toString() + "&signature="+signer.signature(path[0]);

            if(path.length > 1) {
                signedURI += path[1];
            }

            return new URI(signedURI);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }}
