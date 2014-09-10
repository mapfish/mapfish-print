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

package org.mapfish.print.http;

import org.apache.http.HttpHost;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;
import org.mapfish.print.config.Configuration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import javax.net.ssl.SSLContext;

/**
 * A ssl socket factory that obtains the keystore from the current configuration.
 *
 * @author Jesse on 9/4/2014.
 */
public final class MfSSLSocketFactory implements LayeredConnectionSocketFactory {
    private LayeredConnectionSocketFactory defaultFactory = SSLConnectionSocketFactory.getSystemSocketFactory();

    @Override
    public Socket createLayeredSocket(final Socket socket,
                                      final String target,
                                      final int port,
                                      final HttpContext context) throws IOException {
        LayeredConnectionSocketFactory factory = getSSLSocketFactory();
        return factory.createLayeredSocket(socket, target, port, context);
    }


    @Override
    public Socket createSocket(final HttpContext context) throws IOException {
        LayeredConnectionSocketFactory factory = getSSLSocketFactory();
        return factory.createSocket(context);
    }

    @Override
    public Socket connectSocket(final int connectTimeout,
                                final Socket sock,
                                final HttpHost host,
                                final InetSocketAddress remoteAddress,
                                final InetSocketAddress localAddress,
                                final HttpContext context) throws IOException {
        LayeredConnectionSocketFactory factory = getSSLSocketFactory();
        return factory.connectSocket(connectTimeout, sock, host, remoteAddress, localAddress, context);
    }

    private LayeredConnectionSocketFactory getSSLSocketFactory() {
        final Configuration currentConfiguration = MfClientHttpRequestFactoryImpl.getCurrentConfiguration();
        if (currentConfiguration == null || currentConfiguration.getCertificateStore() == null) {
            return this.defaultFactory;
        }
        SSLContext context = currentConfiguration.getCertificateStore().getSSLContext();
        return new SSLConnectionSocketFactory(context,
                SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
    }

}
