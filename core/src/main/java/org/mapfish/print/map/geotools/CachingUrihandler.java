/*
 * Copyright (C) 2016  Camptocamp
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

package org.mapfish.print.map.geotools;

import org.eclipse.emf.common.util.URI;
import org.geotools.xml.impl.HTTPURIHandler;
import org.geotools.xml.resolver.SchemaCache;
import org.geotools.xml.resolver.SchemaResolver;
import org.mapfish.print.config.WorkingDirectories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import javax.annotation.PostConstruct;

/**
 * An {@link org.eclipse.emf.ecore.resource.URIHandler} for caching XSDs or using local copies.
 */
class CachingUrihandler extends HTTPURIHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CachingUrihandler.class);

    @Autowired
    private WorkingDirectories workingDirectories;

    private SchemaResolver resolver;

    @PostConstruct
    public final void init() {
        this.resolver = new SchemaResolver(new SchemaCache(getCacheLocation(), true, true));
    }

    private File getCacheLocation() {
        return new File(this.workingDirectories.getWorking(), "xsdCache");
    }

    @Override
    public boolean canHandle(final URI uri) {
        // We don't cache WFS DescribeFeatureType since it can change. We'll assume the
        // http://{...}.xsd are not changing during the lifetime of the webapp.
        return super.canHandle(uri) && uri.path().endsWith(".xsd");
    }

    @Override
    public InputStream createInputStream(final URI uri, final Map<?, ?> options) throws IOException {
        final String resolved = this.resolver.resolve(uri.toString());
        LOGGER.debug("Resolved {} to {}", uri, resolved);
        final URI resolvedUri = URI.createURI(resolved);
        if (super.canHandle(resolvedUri)) {
            return super.createInputStream(resolvedUri, options);
        } else {
            URL resolvedUrl = new URL(resolved);
            if (resolvedUrl.getProtocol().equals("file")) {
                return new FileInputStream(resolvedUrl.getPath());
            } else {
                throw new IOException("Don't know how to handle " + resolved);
            }
        }
    }
}
