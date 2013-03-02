/*
 * Copyright (C) 2008 Patrick Valsecchi
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  U
 */
package org.pvalsecc.comm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ObjectClientConnection<OUT> {
    protected SocketChannel socket;

    public ObjectClientConnection(InetSocketAddress address) throws IOException {
        socket = SocketChannel.open(address);
    }

    protected void sendRequest(OUT request) throws IOException {
        ByteBuffer writeBuffer = ObjectServerConnection.marshall(request);
        writeBuffer.flip();
        while (writeBuffer.hasRemaining()) {
            socket.write(writeBuffer);
        }
    }

    public void close() throws IOException {
        socket.close();
    }

    public InetSocketAddress getAddress() {
        return (InetSocketAddress) socket.socket().getRemoteSocketAddress();
    }

    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) socket.socket().getLocalSocketAddress();
    }
}
