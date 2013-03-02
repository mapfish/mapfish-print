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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Used to do something (callback) when every sub-task is done.
 * <p/>
 * The target number of jobs doesn't have to be known in advance, but has to be specified
 * only once with {@code setTarget}.
 * <p/>
 * When every task kinds are completed, the {@code completedCallback} Runnable is called
 * either from {@code addJobDone()} or from {@code setTarget()}.
 * <p/>
 * This is not really a barrier as defined in the list of synchronization methods...
 */
@SuppressWarnings({"NonPrivateFieldAccessedInSynchronizedContext"})
public class SynchronousSimpleTarget extends SynchronousTarget {

    private AtomicInteger count = new AtomicInteger(0);

    /**
     * -1 when unknown.
     */
    private AtomicInteger target = new AtomicInteger(-1);

    public SynchronousSimpleTarget(String name, Runnable completedCallback) {
        super(completedCallback, name);
    }

    protected boolean isVirgin() {
        return count.get() == 0;
    }

    /**
     * Not protected to avoid contention.
     *
     * @return The total number done.
     */
    public int addDone(int nb) {
        int currentCount = count.addAndGet(nb);

        int currentTarget = target.get();
        if (currentTarget >= 0) {   // we know the target for this kind of job
            if (currentCount == currentTarget) {   //this job is done
                call();
            } else if (currentCount > currentTarget) {
                throw new RuntimeException("Job overtook its target [" + getName() + "]");
            }
        }

        return currentCount;
    }

    /**
     * Use this if you want to set a fixed target for the given job kind.
     */
    public synchronized void setTarget(int target) {

        if (this.target.get() >= 0) {
            throw new RuntimeException("Target already set");
        }

        this.target.set(target);

        if (count.get() == target) {
            call();
        }
    }

    public int getCount() {
        return count.get();
    }

    public boolean isTargetKnown() {
        return target.get() >= 0;
    }

    public int getTarget() {
        return target.get();
    }
}
