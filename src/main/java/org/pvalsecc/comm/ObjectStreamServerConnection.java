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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Iterator;

/**
 * The command arrives as one object and the answer as a stream of objects that can be un-marshalled by bunch.
 */
public abstract class ObjectStreamServerConnection<IN> extends ObjectServerConnection<IN> {
    /**
     * we don't want to make packets that are too small. That's the minimum size of one packet.
     */
    private static final int MIN_SIZE = 1024 * 1024;

    private Iterator<byte[]> iterator = null;

    private final ByteBuffer sizeMarshaller = ByteBuffer.allocate(4);

    private final MyByteArrayOutputStream byteStream = new MyByteArrayOutputStream(MIN_SIZE + MIN_SIZE / 10);

    public ObjectStreamServerConnection(SelectionKey key) {
        super(key);
    }

    /**
     * Fully asynchronous, the iterator will be kept way after this call has been returned.
     * So don't mess with what's being iterated!
     * <p/>
     * The returned ByteBuffers must be in read mode (call the flip method)
     */
    public void answer(Iterator<byte[]> iterator) throws IOException {
        if (this.iterator != null) {
            throw new RuntimeException("cannot send an answer while an answer is pending");
        }
        this.iterator = iterator;
        scheduleNextPacket();
    }

    protected boolean hasSomeMoreDataToSend() throws IOException {
        if (super.hasSomeMoreDataToSend()) {
            //first finish the current packet
            return true;
        } else {   //no more current packet, must create new ones.
            return scheduleNextPacket();
        }
    }

    /**
     * Take some more objects from the iterator, creates a packet with them and sends them over the network.
     *
     * @return True if something was scheduled.
     */
    private boolean scheduleNextPacket() throws IOException {

        if (iterator.hasNext()) {
            while (iterator.hasNext() && byteStream.size() < MIN_SIZE) {
                byte[] out = iterator.next();

                sizeMarshaller.rewind();
                sizeMarshaller.putInt(out.length);
                byteStream.write(sizeMarshaller.array());
                byteStream.write(out);
            }
            byteStream.flush();
            sendBunch(!iterator.hasNext());
            byteStream.reset();
            return true;
        } else {
            //no more data to send
            iterator = null;
            return false;
        }
    }

    private void sendBunch(boolean addTerminator) {
        ByteBuffer result = ByteBuffer.allocate(byteStream.size() + (addTerminator ? 4 : 0));
        result.put(byteStream.getInternalBuffer(), 0, byteStream.size());
        if (addTerminator) {   //the end of the stream is market by an empty object
            result.putInt(0);
        }
        result.flip();
        answer(result);
    }

    private static class MyByteArrayOutputStream extends ByteArrayOutputStream {
        public MyByteArrayOutputStream(int size) {
            super(size);
        }

        public byte[] getInternalBuffer() {
            return buf;
        }
    }
}
