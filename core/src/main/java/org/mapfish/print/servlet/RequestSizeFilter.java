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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * Filter which checks the content size of requests.
 * 
 * This is to avoid that the server is flooded with overly
 * huge requests.
 */
public class RequestSizeFilter implements Filter {

    private static final int MAX_CONTENT_LENGTH = 1048576;
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestSizeFilter.class);
    
    /**
     * The maximum allowed content length of the request in bytes.
     */
    private int maxContentLength = MAX_CONTENT_LENGTH;

    @Override
    public final void doFilter(final ServletRequest request, final ServletResponse response,
            final FilterChain chain) throws IOException, ServletException {
        if (request.getContentLength() > this.maxContentLength) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            LOGGER.error("Request size exceeds limit: " + request.getContentLength() + " bytes");
            httpResponse.sendError(HttpStatus.BAD_REQUEST.value(), "Request size exceeds limit");
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public final void init(final FilterConfig config) throws ServletException {
        if (config.getInitParameter("maxContentLength") != null) {
            this.maxContentLength = Integer.parseInt(config.getInitParameter("maxContentLength"));
        }
    }
    
    @Override
    public void destroy() { }

}
