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

import java.util.ArrayList;

/**
 * Manage a group of {@link org.pvalsecc.concurrent.SynchronousTarget} and call the {@link #completedCallback} Runnable when done.
 */
@SuppressWarnings({"NonPrivateFieldAccessedInSynchronizedContext"})
public class SynchronousTargetGroup extends SynchronousTarget {
    protected ArrayList<SynchronousTarget> subs = new ArrayList<SynchronousTarget>(5);

    public SynchronousTargetGroup(String name, Runnable completedCallback) {
        super(completedCallback, name);
    }

    protected boolean isVirgin() {
        for (int i = 0; i < subs.size(); i++) {
            SynchronousTarget sub = subs.get(i);
            if (!sub.isVirgin()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Add a sub-target. Warning, can be done only if no target has been "touched" yet.
     *
     * @param sub The sub-target to add
     */
    public synchronized void add(SynchronousTarget sub) {
        if (!isVirgin()) {
            throw new RuntimeException("Cannot add a barrier");
        }
        subs.add(sub);
        //noinspection ResultOfObjectAllocationIgnored
        new SubListener(sub);
    }

    private synchronized void subDone(SynchronousTarget sub) {
        if (!subs.remove(sub)) {
            throw new RuntimeException("Inconsitency");
        }

        if (subs.size() == 0) {
            call();
        }
    }

    private class SubListener implements Runnable {

        private Runnable chained;

        private final SynchronousTarget sub;

        public SubListener(SynchronousTarget sub) {
            this.sub = sub;
            chained = sub.registerCompletedCallback(this);
        }

        public void run() {
            if (chained != null) {
                chained.run();
            }
            subDone(sub);
        }
    }

    public synchronized void setTarget(int target) {
        for (int i = 0; i < subs.size(); i++) {
            SynchronousTarget sub = subs.get(i);
            sub.setTarget(target);
        }
    }
}
