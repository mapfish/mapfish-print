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
package org.pvalsecc.concurrent;

import java.util.concurrent.TimeoutException;

public class BlockingSimpleTarget extends SynchronousSimpleTarget {
    private boolean completed = false;

    private final Object sync = new Object();

    public BlockingSimpleTarget(String name) {
        super(name, null);
        registerCompletedCallback(new Runnable() {
            public void run() {
                synchronized (sync) {
                    completed = true;
                    sync.notifyAll();
                }
            }
        });
    }

    public BlockingSimpleTarget(int target, String name) {
        this(name);
        setTarget(target);
    }

    public void waitForCompletion(long timeout) throws TimeoutException {
        long maxTime = System.currentTimeMillis() + timeout;
        synchronized (sync) {
            while (!completed) {
                try {
                    long toWait = maxTime - System.currentTimeMillis();
                    if (toWait <= 0) {
                        throw new TimeoutException();
                    }
                    sync.wait(toWait);
                }
                catch (InterruptedException ignored) {
                    //ignored
                }
            }
        }
    }
}
