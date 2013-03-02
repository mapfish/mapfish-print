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
package org.pvalsecc.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class FileUtilities {
    /**
     * Takes the content of the input stream and returns a string containing
     * the full content using the platform's default encoding.
     */
    public static String readWholeTextStream(InputStream in) throws IOException {
        final int size = in.available();
        StringBuilder result = new StringBuilder(size);
        byte b[] = new byte[size];
        int actual;
        while ((actual = in.read(b, 0, size)) >= 0) {
            if (actual > 0) {
                result.append(new String(b, 0, actual));
            }
        }
        return result.toString();
    }

    /**
     * Takes the content of the input stream and returns a string containing
     * the full content using the given encoding.
     */
    public static String readWholeTextStream(InputStream in, String encoding) throws IOException {
        final int size = 1024;
        StringBuilder result = new StringBuilder(size);
        byte b[] = new byte[size];
        int actual;
        while ((actual = in.read(b, 0, size)) >= 0) {
            if (actual > 0) {
                result.append(new String(b, 0, actual, encoding));
            }
        }
        return result.toString();
    }

    public static String readWholeTextFile(File file) throws IOException {
        return readWholeTextStream(new FileInputStream(file));
    }

    public static String readWholeTextFile(File file, String encoding) throws IOException {
        return readWholeTextStream(new FileInputStream(file), encoding);
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte b[] = new byte[4096];
        int size;
        while ((size = in.read(b)) > 0) {
            out.write(b, 0, size);
        }
    }
}
