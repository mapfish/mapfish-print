package org.mapfish.print.http;


import jsr166y.ForkJoinPool;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 
 * Cached ClientHttpRequestFactory.
 * 
 * @author Niels Charlier
 *
 */
public final class HttpRequestCache {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestCache.class);
    
    private static class CachedClientHttpResponse extends AbstractClientHttpResponse {
        
        private final File cachedFile;
        private final HttpHeaders headers;
        private final int status;
        private final String statusText;
        
        public CachedClientHttpResponse(final ClientHttpResponse originalResponse) throws IOException {
            this.headers = originalResponse.getHeaders();
            this.status = originalResponse.getRawStatusCode();
            this.statusText = originalResponse.getStatusText();
            this.cachedFile = File.createTempFile("cacheduri", null);
            InputStream is = originalResponse.getBody();
            try {
                OutputStream os = new FileOutputStream(this.cachedFile);
                try {
                    IOUtils.copy(is, os);
                } finally {
                    os.close();
                }
            } finally {
                is.close();
            }
        }

        @Override
        public InputStream getBody() throws IOException {
            return new FileInputStream(this.cachedFile);
        }

        @Override
        public HttpHeaders getHeaders() {
            return this.headers;
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return this.status;
        }

        @Override
        public String getStatusText() throws IOException {
            return this.statusText;
        }

        @Override
        public void close() {
            
        }
    }

    private static class CachedClientHttpRequest implements ClientHttpRequest, Callable<Void> {
        private final ClientHttpRequest originalRequest;
        private CachedClientHttpResponse response;
        
        public CachedClientHttpRequest(final ClientHttpRequest request) throws IOException {
            this.originalRequest = request;
        }
        
        @Override
        public HttpMethod getMethod() {
            return this.originalRequest.getMethod();
        }

        @Override
        public URI getURI() {
            return this.originalRequest.getURI();
        }

        @Override
        public HttpHeaders getHeaders() {
            return this.originalRequest.getHeaders();
        }

        @Override
        public OutputStream getBody() throws IOException {
            //body should be written before creating this object
            throw new UnsupportedOperationException();
        }

        @Override
        public ClientHttpResponse execute() throws IOException {
            if (this.response == null) {
                LOGGER.warn("Attempting to load cached URI before actual caching: " + this.originalRequest.getURI());
            } else {
                LOGGER.debug("Loading cached URI resource " + this.originalRequest.getURI());                
            }
            return this.response;
        }

        @Override
        public Void call() throws Exception {
            ClientHttpResponse originalResponse = this.originalRequest.execute();
            try {
                LOGGER.debug("Caching URI resource " + this.originalRequest.getURI());
                this.response = new CachedClientHttpResponse(originalResponse);            
            } finally {
                originalResponse.close();
            }
            return null;
        }
    }
    
    private List<CachedClientHttpRequest> requests = new ArrayList<CachedClientHttpRequest>();
    
    private CachedClientHttpRequest save(final CachedClientHttpRequest request) {
        this.requests.add(request);
        return request;
    }        
    
    /**
     * Cache a http request.
     * 
     * @param originalRequest the original request
     * @return the cashed http request
     * @throws IOException
     */
    public ClientHttpRequest register(final ClientHttpRequest originalRequest) throws IOException {
        return save(new CachedClientHttpRequest(originalRequest));
    }

    /**
     * Cache a URI. Returns a cashed HttpRequest.
     * 
     * @param factory the request factory
     * @param uri the uri
     * @return the cashed http request
     * @throws IOException
     */
    public ClientHttpRequest register(final MfClientHttpRequestFactory factory, final URI uri) throws IOException {
        return register(factory.createRequest(uri, HttpMethod.GET));
    }
    
    /**
     * Cache all requests at once.
     * 
     * @param requestForkJoinPool request fork join pool
     */
    public void cache(final ForkJoinPool requestForkJoinPool) {
        requestForkJoinPool.invokeAll(this.requests);
    }

}
