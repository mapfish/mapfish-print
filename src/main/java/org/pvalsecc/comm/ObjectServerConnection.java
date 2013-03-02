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

import org.pvalsecc.misc.SystemUtilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * Server side of the object stream.
 * <p/>
 * Receives commands as object, the answer protocol is implemented in child classes.
 *
 * @see org.pvalsecc.comm.ObjectObjectServerConnection
 * @see org.pvalsecc.comm.ObjectStreamServerConnection
 */
public abstract class ObjectServerConnection<IN>
        extends ServerConnection {
    private int length = 0;

    private ByteBuffer message = null;

    public ObjectServerConnection(SelectionKey key) {
        super(key);
    }

    /**
     * Called from within the <strong>selecting</strong> thread when some data has been received.
     */
    protected final void received(ByteBuffer buffer) {
        if (message == null) {   //first bloc
            length = buffer.getInt();
            if (buffer.remaining() >= length) {   //complete
                process(buffer);
                length = 0;
            } else {   //need to get other blocs
                message = ByteBuffer.allocate(length);
                message.put(buffer.array(), buffer.position(), buffer.remaining());
            }
        } else {   //continuation
            message.put(buffer.array(), buffer.position(), buffer.remaining());
            if (message.remaining() == 0) {   //complete
                message.flip();
                process(message);
                length = 0;
                message = null;
            }
        }
    }

    /**
     * Called from within the <strong>selecting</strong> thread when all the data chunk
     * to unmarshall has been received.
     */
    private void process(ByteBuffer buffer) {
        try {

            Object object = unmarshall(buffer, buffer.remaining());
            received((IN) object);
        }
        catch (IOException e) {
            error("Cannot unmarshall the message", e);
        }
        catch (ClassNotFoundException e) {
            error("Cannot unmarshall the message", e);
        }
    }

    public static Object unmarshall(ByteBuffer buffer, int length) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = null;
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer.array(), buffer.position(), length);
            //noinspection IOResourceOpenedButNotSafelyClosed
            objectInputStream = new ObjectInputStream(byteArrayInputStream);
            int availableBefore = byteArrayInputStream.available();
            Object result = objectInputStream.readObject();
            int availableAfter = byteArrayInputStream.available();
            buffer.position(buffer.position() + availableBefore - availableAfter);
            return result;
        }
        finally {
            SystemUtilities.safeClose(objectInputStream);
        }
    }

    /**
     * Called from within the <strong>selecting</strong> thread.
     */
    protected abstract void received(IN object);

    public static ByteBuffer marshall(Object object) throws IOException {
        ObjectOutputStream objectOutputStream = null;
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            //noinspection IOResourceOpenedButNotSafelyClosed
            objectOutputStream = new ObjectOutputStream(byteStream);
            objectOutputStream.writeObject(object);
            byte[] bytes = byteStream.toByteArray();
            ByteBuffer result = ByteBuffer.allocate(bytes.length + 4);
            result.putInt(bytes.length);
            result.put(bytes);
            return result;
        }
        finally {
            SystemUtilities.safeClose(objectOutputStream);
        }
    }
}
