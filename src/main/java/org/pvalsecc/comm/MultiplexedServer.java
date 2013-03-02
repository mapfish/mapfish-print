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
import org.pvalsecc.misc.SystemUtilities;
import org.pvalsecc.misc.UnitUtilities;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.TimeoutException;

/**
 * A class for handling multiple sockets from one thread.
 * <p/>
 * The protocol is managed by the objet returned by the factory method {@code newConnection}
 * called each time a new connection is established.
 *
 * @see org.pvalsecc.comm.ObjectServerConnection
 */
@SuppressWarnings({"OverlyBroadCatchBlock"})
public abstract class MultiplexedServer implements Runnable {
    public static final Log LOGGER = LogFactory.getLog(MultiplexedServer.class);

    private boolean stop = false;

    private Selector selector = null;

    private final Object selectorLOCK = new Object();

    private final FatalErrorReporter fatalErrorReporter;

    private InetSocketAddress address;

    private final String threadName;

    /**
     * The thread handling all the communication.
     */
    private Thread thread = null;

    private final Object socketCreatedLock = new Object();

    private boolean socketCreated = false;

    private long timeSending = 0;

    private long timeReceiving = 0;

    private long nbBytesSent = 0;

    private long nbBytesReceived = 0;

    private int nbReceived = 0;

    private int nbSent = 0;

    public MultiplexedServer(FatalErrorReporter fatalErrorReporter, InetSocketAddress address, String threadName) {
        this.fatalErrorReporter = fatalErrorReporter;
        this.address = address;
        this.threadName = threadName;
    }

    /**
     * Start (blocking) the thread and start to listen on the server socket.
     */
    public void start(long timeout) throws TimeoutException {
        thread = new Thread(this, threadName);
        thread.start();

        final long maxWait = System.currentTimeMillis() + timeout;

        synchronized (socketCreatedLock) {
            while (!socketCreated) {
                long toWait = maxWait - System.currentTimeMillis();
                if (toWait <= 0) {
                    throw new TimeoutException("Could not start the multiplexed server [" + threadName + "].");
                }
                try {
                    socketCreatedLock.wait(toWait);
                }
                catch (InterruptedException ignored) {
                    //ignored
                }
            }
        }
    }

    public void run() {
        try {
            createSocket();

            synchronized (socketCreatedLock) {
                socketCreated = true;
                socketCreatedLock.notifyAll();
            }

            while (!stop) {
                int nbKeys = 0;
                try {
                    nbKeys = selector.select();
                }
                catch (IOException e) {
                    LOGGER.error(e);
                }

                if (nbKeys > 0) {
                    Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();

                        if (key.isAcceptable()) {
                            createNewClientConnection(key);
                        } else if (key.isWritable()) {
                            long startTime = System.nanoTime();
                            readyToSend(key);
                            timeSending += System.nanoTime() - startTime;
                        } else if (key.isReadable()) {
                            long startTime = System.nanoTime();
                            readyToReceive(key);
                            timeReceiving += System.nanoTime() - startTime;
                        }

                        it.remove();
                    }
                }
            }
        }
        catch (RuntimeException ex) {
            LOGGER.error("The MultiplexedServer [" + threadName + "] caught an unexpected exception.", ex);
            fatalErrorReporter.report(ex);
        }
        finally {
            if (selector != null) {
                for (SelectionKey key : selector.keys()) {
                    SystemUtilities.safeClose(key.channel());
                }
                SystemUtilities.safeClose(selector);
            }

            LOGGER.info("[" + threadName + "] inTime=" + UnitUtilities.toElapsedNanoTime(timeReceiving) + " outTime=" + UnitUtilities.toElapsedNanoTime(timeSending) + " in=" + nbBytesReceived + "B out=" + UnitUtilities.toComputerSize(nbBytesSent) + "B inNb=" + nbReceived + " outNb=" + nbSent);
        }
    }

    private void readyToReceive(SelectionKey key) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        //noinspection ConstantConditions
        SocketChannel socket = (SocketChannel) key.channel();
        ServerConnection connection = (ServerConnection) key.attachment();

        int size;
        try {
            size = socket.read(buffer);
        }
        catch (IOException e) {
            //message logging and call to "closed" is done in "error" method
            connection.error("Cannot receive data", e);
            key.cancel();
            SystemUtilities.safeClose(socket);
            return;
        }

        if (size >= 0) {
            if (size != buffer.position()) {
                throw new RuntimeException("[" + threadName + "] Inconsistent buffer: " + size + "!=" + buffer.position());
            }
            nbBytesReceived += size;
            nbReceived++;
            buffer.flip();
            connection.received(buffer);
        } else {
            LOGGER.info("[" + threadName + "] Connection closed by " + connection);
            connection.closed();
            key.cancel();
        }
    }

    private void readyToSend(SelectionKey key) {
        SocketChannel socket = (SocketChannel) key.channel();
        ServerConnection connection = (ServerConnection) key.attachment();

        try {
            nbBytesSent += connection.send(socket);
            nbSent++;
        }
        catch (IOException e) {
            connection.error("Cannot send data", e);
            key.cancel();
            SystemUtilities.safeClose(socket);
        }
    }

    private void createNewClientConnection(SelectionKey key) {
        SocketChannel socket = null;
        try {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            socket = serverSocketChannel.accept();
        }
        catch (IOException e) {
            LOGGER.error("Cannot accept the connection from a new client", e);
            SystemUtilities.safeClose(socket);
            return;
        }

        SelectionKey newKey;
        try {
            socket.configureBlocking(false);
            newKey = socket.register(selector, SelectionKey.OP_READ);
        }
        catch (IOException e) {
            LOGGER.error("Cannot add a new client socket to the selector", e);
            SystemUtilities.safeClose(socket);
            return;
        }

        ServerConnection connection = newConnection(newKey);
        newKey.attach(connection);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[" + threadName + "] New connection from " + connection);
        }
    }

    /**
     * Attempt to create the listening socket for at most 5 minutes.
     */
    private void createSocket() {
        while (!stop) {
            ServerSocketChannel serverSocketChannel = null;

            try {
                synchronized (selectorLOCK) {
                    if (!stop) {
                        selector = Selector.open();
                    }
                }

                if (!stop) {
                    serverSocketChannel = ServerSocketChannel.open();
                    //serverSocketChannel.socket().setReuseAddress(true);
                    serverSocketChannel.configureBlocking(false);
                    serverSocketChannel.socket().bind(address);
                    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                    LOGGER.info("[" + threadName + "] Start to listen on " + address);
                }

                break;
            }
            catch (IOException e) {
                //noinspection StringContatenationInLoop
                LOGGER.warn("Cannot start to listen on " + threadName + " (will try again later)", e);

                SystemUtilities.safeClose(serverSocketChannel);

                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException ignored) {
                    //ignored
                }
            }
        }
    }

    /**
     * Called each time a new client is connected.
     */
    protected abstract ServerConnection newConnection(SelectionKey key);

    /**
     * Close the socket and stop the thread. Wait for the thread to actually stop.
     */
    public void stop() {
        stop = true;

        while (thread != null && thread.isAlive()) {
            synchronized (selectorLOCK) {
                if (selector != null) {
                    selector.wakeup();
                }
            }

            try {
                thread.join(100);
                //break;
            }
            catch (InterruptedException e) {
                LOGGER.warn(e);
            }
        }
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public InetSocketAddress getConnectingAddress() throws UnknownHostException {
        return new InetSocketAddress(InetAddress.getLocalHost(), address.getPort());
    }

    public InetSocketAddress getConnectingAddress(ServerConnection connection) {
        return new InetSocketAddress(((SocketChannel) connection.getKey().channel()).socket().getLocalAddress(), address.getPort());
    }
}
