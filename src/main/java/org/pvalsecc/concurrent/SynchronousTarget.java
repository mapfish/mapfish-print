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

/**
 * Used to do something (callback) when every sub-task is done.
 */
public abstract class SynchronousTarget {
    /**
     * What will be executed when the job is completed.
     */
    private Runnable completedCallback;

    private boolean done = false;

    /**
     * Just for debug purpose...
     */
    private final String name;

    public SynchronousTarget(Runnable completedCallback, String name) {
        this.name = name;
        this.completedCallback = completedCallback;
    }

    public abstract void setTarget(int target);

    /**
     * Can be used to add a chained listener. It's the new listener's responsability to call the
     * previous (returned) listener.
     *
     * @param completedCallback the new listener
     * @return the previous listener
     */
    public Runnable registerCompletedCallback(Runnable completedCallback) {
        if (!isVirgin()) {
            throw new RuntimeException("Cannot add a sub barrier if it has already been used");
        }

        Runnable previous = this.completedCallback;
        this.completedCallback = completedCallback;
        return previous;
    }

    public synchronized boolean isDone() {
        return done;
    }

    protected synchronized void call() {
        if (done) {
            //may be called twice because addDone is not protected
            return;
        }
        done = true;
        if (completedCallback != null) {
            completedCallback.run();
        }
    }

    /**
     * @return True is the target as never been used.
     */
    protected abstract boolean isVirgin();

    public String getName() {
        return name;
    }
}
