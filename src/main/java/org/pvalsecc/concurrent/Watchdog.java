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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Watch dog to check the process is still alive.
 */
@SuppressWarnings({"CallToSignalInsteadOfSignalAll"})
public abstract class Watchdog implements Runnable {
    public static final Log LOGGER = LogFactory.getLog(Watchdog.class);

    protected Thread thread = null;

    private boolean stop = false;

    private Set<Sheep> sheeps = new HashSet<Sheep>();

    private final long watchdogPeriod;

    private final Lock lock = new ReentrantLock();

    private final Condition condition = lock.newCondition();

    private static final long POLLING_TIME = 1000;

    protected Watchdog(long watchdogPeriod) {
        this.watchdogPeriod = watchdogPeriod;
    }

    /**
     * Declare a new activity to monitor.
     */
    public Sheep createSheep(String name, long worstFrequency) {
        Sheep sheep = new Sheep(name, worstFrequency);
        try {
            lock.lock();
            sheeps.add(sheep);
            return sheep;
        }
        finally {
            lock.unlock();
        }
    }

    public void start() {
        if (thread != null) {
            throw new RuntimeException("Watchdog started twice");
        }
        thread = new Thread(this);

        //noinspection CallToThreadSetPriority
        thread.setPriority(Thread.MAX_PRIORITY);

        thread.start();
    }

    public void stop() {
        if (thread == null) {
            return;
        }
        try {
            lock.lock();
            stop = true;
            condition.signal();
        }
        finally {
            lock.unlock();
        }

        try {
            thread.join(1000);
            thread = null;
        }
        catch (InterruptedException ignored) {
            //ignored
        }
    }

    @SuppressWarnings({"NestedTryStatement"})
    public void run() {
        try {
            lock.lock();

            long currentHeartbeat = System.currentTimeMillis();
            while (!stop) {
                if (stillAlive()) {
                    lock.unlock();
                    {
                        //noinspection OverlyBroadCatchBlock
                        try {
                            heartbeat();
                        }
                        catch (Exception e) {
                            LOGGER.error("Cannot send watchdog event", e);
                            return;
                        }
                    }
                    lock.lock();

                    currentHeartbeat += watchdogPeriod;
                    long toSleep = currentHeartbeat - System.currentTimeMillis();
                    if (toSleep > 0) {
                        condition.await(toSleep, TimeUnit.MILLISECONDS);
                    }
                } else {
                    //didn't have any sign of activity... wait for some small time
                    try {
                        condition.await(POLLING_TIME, TimeUnit.MILLISECONDS);
                        currentHeartbeat = System.currentTimeMillis();
                    }
                    catch (InterruptedException ignored) {
                        //ignored
                    }
                }
            }
        }
        catch (Throwable ex) {
            LOGGER.error("Un-expected exception", ex);
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * @return True if we have at least one sheep active and not in timeout
     */
    private boolean stillAlive() {
        try {
            lock.lock();
            if (sheeps.isEmpty()) {   //nothing to monitor, everything is fine...
                return true;
            }

            long now = System.currentTimeMillis();
            boolean alive = false;
            int nbInactif = 0;
            for (Sheep sheep : sheeps) {
                if (sheep.isActive()) {
                    if (sheep.isStillAlive(now)) {
                        alive = true;
                    } else {
                        timeout(sheep);
                    }
                } else {
                    nbInactif++;
                }
            }
            return alive || nbInactif == sheeps.size();
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Called every {@code #watchdogPeriod}ms if at least one sheep is OK
     */
    protected abstract void heartbeat() throws IOException, ClassNotFoundException;

    /**
     * Called when a sheep is not OK.
     */
    protected abstract void timeout(Sheep sheep);

    /**
     * Represents a monitored task.
     */
    public class Sheep {
        private final String name;

        private final long worstFrequency;

        private final AtomicLong lastSeen = new AtomicLong(0);

        private String lastMessage = null;

        private boolean registered = true;

        /**
         * @param name           The name of the activity.
         * @param worstFrequency The worst ever possible frequency that {@link #stillAlive(String)} may be called.
         */
        private Sheep(String name, long worstFrequency) {
            this.name = name;
            this.worstFrequency = worstFrequency;
        }

        /**
         * Just tells that the task is still alive and kicking.
         */
        public final void stillAlive(String message) {
            if (!registered) {
                throw new RuntimeException(name + " is already un-registered");
            }
            lastSeen.set(System.currentTimeMillis());
            lastMessage = message;
        }

        private boolean isStillAlive(long now) {
            return now - lastSeen.get() <= worstFrequency;  //not in timeout
        }

        /**
         * @return true if stillAlive has been called at least once.
         */
        private boolean isActive() {
            return lastMessage != null;
        }

        public String toString() {
            return name + " {" + (System.currentTimeMillis() - lastSeen.get()) + " > " + worstFrequency + "} lastMessage=" + lastMessage;
        }

        public void unregister() {
            registered = false;
            try {
                lock.lock();
                if (!sheeps.remove(this)) {
                    LOGGER.error("Unknown sheep being un-registered: " + this);
                }
            }
            finally {
                lock.unlock();
            }
        }
    }
}
