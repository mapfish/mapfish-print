package org.mapfish.print.output;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.repo.FileRepositoryService;
import net.sf.jasperreports.repo.InputStreamResource;
import net.sf.jasperreports.repo.PersistenceService;
import net.sf.jasperreports.repo.PersistenceUtil;
import net.sf.jasperreports.repo.Resource;
import net.sf.jasperreports.repo.StreamRepositoryService;
import org.mapfish.print.http.MfClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.Nonnull;

/**
 * The class responsible for accessing resources and streams when generating jasper reports.
 */
class MapfishPrintRepositoryService implements StreamRepositoryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapfishPrintRepositoryService.class);

    private final MfClientHttpRequestFactory httpRequestFactory;
    private JasperReportsContext jasperReportsContext;

    MapfishPrintRepositoryService(
            @Nonnull final MfClientHttpRequestFactory httpRequestFactory) {
        this.httpRequestFactory = httpRequestFactory;
        this.jasperReportsContext = DefaultJasperReportsContext.getInstance();
    }

    @Override
    public InputStream getInputStream(final String uriString) {
        URI uri;
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            uri = new File(uriString).toURI();
        }
        try {
            final ClientHttpResponse response =
                    this.httpRequestFactory.createRequest(uri, HttpMethod.GET).execute();
            return new ResponseClosingStream(response);
        } catch (IOException e) {
            return null;
        }
    }


    @Override
    public Resource getResource(final String uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveResource(final String uri, final Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K extends Resource> K getResource(final String uri, final Class<K> resourceType) {
        try {
            if (resourceType.isAssignableFrom(InputStreamResource.class)) {
                final InputStream inputStream = getInputStream(uri);
                if (inputStream != null) {
                    final InputStreamResource resource = new InputStreamResource();
                    resource.setInputStream(inputStream);
                    return resourceType.cast(resource);
                }
            }

            final PersistenceUtil persistenceUtil = PersistenceUtil.getInstance(this.jasperReportsContext);
            PersistenceService persistenceService =
                    persistenceUtil.getService(FileRepositoryService.class, resourceType);
            if (persistenceService != null) {
                return resourceType.cast(persistenceService.load(uri, this));
            }
        } catch (IllegalStateException e) {
            LOGGER.info("Resource not found {} ({}).", uri, e.toString());
        } catch (Exception e) {
            LOGGER.trace("Error on getting resource {}", uri, e);
        }
        return null;
    }

    @Override
    public OutputStream getOutputStream(final String uri) {
        throw new UnsupportedOperationException();
    }

    private static final class ResponseClosingStream extends InputStream {
        private final InputStream stream;
        private final ClientHttpResponse response;

        private ResponseClosingStream(final ClientHttpResponse response) throws IOException {
            this.response = response;
            this.stream = response.getBody();
        }

        @Override
        public void close() throws IOException {
            this.response.close();
            super.close();
        }

        @Override
        public int read() throws IOException {
            return this.stream.read();
        }

        @Override
        public int read(final byte[] b) throws IOException {
            return this.stream.read(b);
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            return this.stream.read(b, off, len);
        }

        @Override
        public long skip(final long n) throws IOException {
            return this.stream.skip(n);
        }

        @Override
        public int available() throws IOException {
            return this.stream.available();
        }

        @Override
        public void mark(final int readlimit) {
            this.stream.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            this.stream.reset();
        }

        @Override
        public boolean markSupported() {
            return this.stream.markSupported();
        }
    }
}
