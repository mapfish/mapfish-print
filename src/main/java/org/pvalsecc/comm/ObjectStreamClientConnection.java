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
public class ObjectStreamClientConnection<OUT> extends ObjectClientConnection<OUT> {
    public ObjectStreamClientConnection(InetSocketAddress address) throws IOException {
        super(address);
    }

    public void getData(OUT request, Stream answer) throws Exception {
        sendRequest(request);
        receiveReply(answer);
    }

    /**
     * Reads the answer (synchrone)
     */
    private void receiveReply(Stream answer)
            throws Exception {
        ByteBuffer readBuffer = ByteBuffer.allocate(1024 * 60);
        ByteBuffer previousReadBuffer = ByteBuffer.allocate(1024 * 60);

        while (true) {
            int nb = socket.read(readBuffer);
            if (nb <= 0) {
                throw new IOException("Message not fully received");
            }
            readBuffer.flip();   //switch the buffer to the read mode and place the cursor at the beginning
            int len = 0;
            while (readBuffer.remaining() >= 4) {
                len = readBuffer.getInt();
                if (len == 0) {   //received the end of stream marker
                    if (readBuffer.remaining() != 0) {
                        throw new RuntimeException("Receving data after the END marker");
                    }
                    return;
                } else {
                    if (readBuffer.remaining() >= len) {   //still have a complete object
                        byte[] result = new byte[len];
                        readBuffer.get(result);
                        answer.onObjectReceived(result);
                        len = 0;
                    } else {   //the object is not fully received yet
                        break;
                    }
                }
            }

            previousReadBuffer.rewind();
            if (len + 4 > previousReadBuffer.remaining()) {
                previousReadBuffer = ByteBuffer.allocate(len + 4);
            }

            //put the remaining of the current object in the next
            if (len > 0) {
                previousReadBuffer.putInt(len);
            }
            previousReadBuffer.put(readBuffer);

            //swap the buffers
            ByteBuffer temp = previousReadBuffer;
            previousReadBuffer = readBuffer;
            readBuffer = temp;
        }
    }

    public interface Stream {
        void onObjectReceived(byte[] object) throws Exception;
    }
}
