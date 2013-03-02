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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Identifies a client connection within a multiplexed server.
 *
 * @see MultiplexedServer
 */
public abstract class ServerConnection {
    public static final Log LOGGER = LogFactory.getLog(ServerConnection.class);

    private final SelectionKey key;

    private final Queue<ByteBuffer> toSend = new LinkedList<ByteBuffer>();

    public ServerConnection(SelectionKey key) {
        this.key = key;
    }

    /**
     * Called from within the <strong>selecting</strong> thread when some data has been received.
     */
    protected abstract void received(ByteBuffer buffer);

    /**
     * Asynchronous. Just queue the data to be sent and tell the selector we want to send some data. When ready,
     * {@code send} will be called.
     */
    protected synchronized void answer(ByteBuffer buffer) {
        toSend.add(buffer);
        key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        key.selector().wakeup();
    }

    /**
     * Called when the socket is ready to send.
     */
    public synchronized int send(SocketChannel socket) throws IOException {
        ByteBuffer buffer = toSend.peek();
        int bytesSent = socket.write(buffer);
        if (!buffer.hasRemaining()) {
            toSend.remove();

            if (!hasSomeMoreDataToSend()) {   //no more data to send, set the selector back to read-only
                key.interestOps(SelectionKey.OP_READ);
            }
        }

        return bytesSent;
    }

    /**
     * @return True if we still have something to send
     * @throws IOException
     */
    protected boolean hasSomeMoreDataToSend() throws IOException {
        return !toSend.isEmpty();
    }

    /**
     * Called when a connection is closed.
     */
    protected abstract void closed();

    /**
     * Called when a communication error occurs.
     */
    protected void error(String message, Exception e) {
        LOGGER.error("[" + getChannelName() + "] " + message + " [" + toString() + "]", e);
        try {
            close();
        }
        catch (IOException e1) {
            LOGGER.error("Trouble closing the connection after an error", e1);
        }
    }

    /**
     * Used for logging to identify the channel
     */
    protected abstract String getChannelName();

    public SelectionKey getKey() {
        return key;
    }

    public String toString() {
        return getSocket().toString();
    }

    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
    public boolean equals(Object obj) {
        return this == obj;
    }

    public int hashCode() {
        return getClientAddress().hashCode();
    }

    private Socket getSocket() {
        SocketChannel channel = (SocketChannel) key.channel();
        return channel.socket();
    }

    protected void close() throws IOException {
        key.channel().close();
        key.cancel();
    }

    public InetSocketAddress getClientAddress() {
        return (InetSocketAddress) getSocket().getRemoteSocketAddress();
    }
}
