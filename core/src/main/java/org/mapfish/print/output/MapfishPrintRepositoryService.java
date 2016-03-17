package org.mapfish.print.output;

import com.google.common.io.Closer;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.repo.FileRepositoryService;
import net.sf.jasperreports.repo.InputStreamResource;
import net.sf.jasperreports.repo.PersistenceService;
import net.sf.jasperreports.repo.PersistenceUtil;
import net.sf.jasperreports.repo.Resource;
import net.sf.jasperreports.repo.StreamRepositoryService;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.http.ConfigFileResolvingHttpRequestFactory;
import org.mapfish.print.http.MfClientHttpRequestFactoryImpl;
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
 *
 * @author Jesse on 8/28/2014.
 */
class MapfishPrintRepositoryService implements StreamRepositoryService {

    private final ConfigFileResolvingHttpRequestFactory httpRequestFactory;
    private JasperReportsContext jasperReportsContext;

    MapfishPrintRepositoryService(@Nonnull final Configuration configuration,
                                  @Nonnull final MfClientHttpRequestFactoryImpl httpRequestFactory) {
        this.httpRequestFactory = new ConfigFileResolvingHttpRequestFactory(httpRequestFactory, configuration);
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
            final ClientHttpResponse response = this.httpRequestFactory.createRequest(uri, HttpMethod.GET).execute();
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
        if (resourceType.isAssignableFrom(InputStreamResource.class)) {
            final InputStream inputStream = getInputStream(uri);
            if (inputStream != null) {
                final InputStreamResource resource = new InputStreamResource();
                resource.setInputStream(inputStream);
                return resourceType.cast(resource);
            }
        }

        final PersistenceUtil persistenceUtil = PersistenceUtil.getInstance(this.jasperReportsContext);
        PersistenceService persistenceService = persistenceUtil.getService(FileRepositoryService.class, resourceType);
        if (persistenceService != null) {
            return resourceType.cast(persistenceService.load(uri, this));
        }
        return null;
    }

    @Override
    public OutputStream getOutputStream(final String uri) {
        throw new UnsupportedOperationException();
    }

    private static class ResponseClosingStream extends InputStream {
        private final Closer closer;
        private final InputStream stream;

        public ResponseClosingStream(final ClientHttpResponse response) throws IOException {
            this.closer = Closer.create();
            this.closer.register(response);
            this.stream = this.closer.register(response.getBody());
        }

        @Override
        public void close() throws IOException {
            this.closer.close();
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
