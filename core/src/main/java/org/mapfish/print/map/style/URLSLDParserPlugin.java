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

package org.mapfish.print.map.style;

import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.io.Closer;
import com.google.common.io.Resources;
import org.mapfish.print.FileUtils;
import org.mapfish.print.config.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Style parser plugin that loads styles from a file containing SLD xml.
 * <p/>
 * Since an SLD can have multiple styles, the string can end with ##&lt;number>.  if that number is there
 * then the identified style will be retrieved from the parsed file.
 *
 * @author Jesse on 3/26/14.
 */
public final class URLSLDParserPlugin extends AbstractSLDParserPlugin {


    @Autowired
    private ClientHttpRequestFactory requestFactory;

    @Override
    protected List<ByteSource> getInputStreamSuppliers(final Configuration configuration, final String styleString) {
        List<ByteSource> options = Lists.newArrayList();

        try {
            final URL url = FileUtils.testForLegalFileUrl(configuration, new URL(styleString));
            if (url.toExternalForm().startsWith("file:/")) {
                options.add(Resources.asByteSource(url));
            } else {
                addUrlByteSource(options, url);
            }
            return options;
        } catch (MalformedURLException e) {
            return options;
        }
    }

    private void addUrlByteSource(final List<ByteSource> options, final URL url) {
        options.add(new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {

                return new InputStream() {
                    private final Closer closer = Closer.create();
                    private final InputStream in;
                    {
                        try {
                            final ClientHttpRequestFactory requestFactory1 = URLSLDParserPlugin.this.requestFactory;
                            final ClientHttpRequest request = requestFactory1.createRequest(url.toURI(), HttpMethod.GET);
                            ClientHttpResponse response = this.closer.register(request.execute());
                            this.in = closer.register(response.getBody());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    @Override
                    public int read() throws IOException {
                        return this.in.read();
                    }

                    @Override
                    public int read(@Nonnull final byte[] b) throws IOException {
                        return this.in.read(b);
                    }

                    @Override
                    public int read(@Nonnull final byte[] b, final int off, final int len) throws IOException {
                        return this.in.read(b, off, len);
                    }

                    @Override
                    public long skip(final long n) throws IOException {
                        return this.in.skip(n);
                    }

                    @Override
                    public int available() throws IOException {
                        return this.in.available();
                    }

                    @Override
                    public synchronized void mark(final int readLimit) {
                        this.in.mark(readLimit);
                    }

                    @Override
                    public synchronized void reset() throws IOException {
                        this.in.reset();
                    }

                    @Override
                    public boolean markSupported() {
                        return this.in.markSupported();
                    }

                    @Override
                    public void close() throws IOException {
                        this.closer.close();
                    }
                };
            }
        });
    }
}
