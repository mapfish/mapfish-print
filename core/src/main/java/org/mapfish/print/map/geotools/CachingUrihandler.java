package org.mapfish.print.map.geotools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import org.eclipse.emf.common.util.URI;
import org.geotools.xml.resolver.SchemaCache;
import org.geotools.xml.resolver.SchemaResolver;
import org.geotools.xsd.impl.HTTPURIHandler;
import org.mapfish.print.config.WorkingDirectories;
import org.mapfish.print.url.data.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/** An {@link org.eclipse.emf.ecore.resource.URIHandler} for caching XSDs or using local copies. */
class CachingUrihandler extends HTTPURIHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(CachingUrihandler.class);

  @Autowired private WorkingDirectories workingDirectories;

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
      URL resolvedUrl = new URL(null, resolved, new Handler());
      if (resolvedUrl.getProtocol().equals("file")) {
        return new FileInputStream(resolvedUrl.getPath());
      } else {
        throw new IOException("Don't know how to handle " + resolved);
      }
    }
  }
}
