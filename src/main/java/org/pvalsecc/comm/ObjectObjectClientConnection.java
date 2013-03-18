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

/**
 * Client side of a ObjectObjectServerConnection.
 * <p/>
 * Sends a command as an object and receives an answer as an object.
 */
public class ObjectObjectClientConnection<OUT, IN> extends ObjectClientConnection<OUT> {
    public ObjectObjectClientConnection(InetSocketAddress address) throws IOException {
        super(address);
    }

    /**
     * Synchronous.
     */
    public IN send(OUT request) throws IOException, ClassNotFoundException {
        sendRequest(request);

        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        int nb = socket.read(readBuffer);
        if (nb <= 0) {
            throw new IOException("Message not fully received");
        }
        readBuffer.flip();
        int len = readBuffer.getInt();
        if (len <= readBuffer.remaining()) {
            return (IN) ObjectServerConnection.unmarshall(readBuffer, len);
        } else {
            ByteBuffer message = ByteBuffer.allocate(len);
            message.put(readBuffer);
            while (message.remaining() > 0) {
                int nb2 = socket.read(message);
                if (nb2 <= 0) {
                    throw new IOException("Message not fully received");
                }
            }
            message.flip();
            return (IN) ObjectServerConnection.unmarshall(message, len);
        }
    }

    /**
     * Interrupt any pending call to "read".
     *
     * @throws IOException
     */
    public void interrupt() throws IOException {
        //not tested...
        socket.close();
    }
}
