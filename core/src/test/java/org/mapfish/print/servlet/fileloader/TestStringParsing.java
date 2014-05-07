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

package org.mapfish.print.servlet.fileloader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.TimeUnit;

/**
 * @author Jesse on 5/4/2014.
 */
public class TestStringParsing {
    public static void main(String[] args) throws Exception {

        for (int i = 0; i < 100000; i++) {
            parseRequestLineSimpleScan();
        }

        long start = System.nanoTime();
        for (int i = 0; i < 1000000; i++) {
            parseRequestLineSimpleScan();
        }
        long end = System.nanoTime();

        System.out.println("Time taken for 1 million requests: " + TimeUnit.NANOSECONDS.toMillis(end - start));
    }

    private static void parseRequestLineSimpleScan() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4 * 1024);
        ReadableByteChannel in = Channels.newChannel(new ByteArrayInputStream(("GET /home/xyz/mvn/yyy/sdfsfd/adfeccssdf/ewsfadfes/dfaef" +
                                                                               " HTTP/1.0\r\n").getBytes("UTF-8")));
        String method, path, protocol;

        buffer.clear();
        final int bytesRead = in.read(buffer);
        buffer.flip();

        StringBuilder builder = new StringBuilder();

        boolean eol = parseRequestLineSegment(buffer, builder);

        if (eol) {
            error400("Not a recognized http request, failed to parse request line " + builder);
        }

        method = builder.toString();
        builder.setLength(0);

        eol = parseRequestLineSegment(buffer, builder);

        if (eol) {
            error400("Not a recognized http request, failed to parse request line " + builder);
        }

        path = builder.toString();
        builder.setLength(0);

        eol = parseRequestLineSegment(buffer, builder);

        if (!eol) {
            error400("Not a recognized http request, expected eol after protocol segment" + builder);
        }

        protocol = builder.toString();
    }

    /**
     * Parse a portion of the http Request Line up-to
     * @param buffer
     * @param builder
     * @return
     */
    private static boolean parseRequestLineSegment(ByteBuffer buffer, StringBuilder builder) {
        char currentChar = (char) buffer.get();
        while(currentChar != ' ') {
            if (currentChar == '\r') {
                currentChar = (char) buffer.get();
                if (currentChar == '\n') {
                    return true;
                } else {
                    builder.append('\r');
                }
            }
            builder.append(currentChar);
            currentChar = (char) buffer.get();
        }

        // remove any extra spaces
        while (currentChar == ' ') {
            currentChar = (char) buffer.get();
        }

        return false;
    }

    private static void error400(String errorMessage) {
        throw new RuntimeException(errorMessage);
    }

}
