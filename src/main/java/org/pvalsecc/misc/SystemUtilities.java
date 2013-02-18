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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;

public class SystemUtilities {
    public static final Log LOGGER = LogFactory.getLog(SystemUtilities.class);

    public static void safeClose(SelectableChannel selectableChannel) {
        try {
            if (selectableChannel != null)
                selectableChannel.close();
        } catch (IOException e) {
            LOGGER.warn("Cannot close", e);
        }
    }

    public static void safeClose(Selector selector) {
        try {
            if (selector != null)
                selector.close();
        } catch (IOException e) {
            LOGGER.warn("Cannot close", e);
        }
    }

    public static void safeClose(InputStream stream) {
        try {
            if (stream != null)
                stream.close();
        } catch (IOException e) {
            LOGGER.warn("Cannot close", e);
        }
    }

    public static void safeClose(OutputStream stream) {
        try {
            if (stream != null)
                stream.close();
        } catch (IOException e) {
            LOGGER.warn("Cannot close", e);
        }
    }
}
