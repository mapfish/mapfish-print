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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A queue of tasks run by a pool of thread. But with a twist.
 * <p/>
 * To avoid too many context switch and contention around the queue,
 * a few steps are taken:
 * <ul>
 * <li>There is one sub-queue per provider thread that is flushed
 * into the main queue every once in a while (see {@link #inputBufferSize}).</li>
 * <li>The consumer threads are taking a bunch of tasks at a time
 * (see {@link #outputBulkSize}).</li>
 * </ul>
 * Thanks to that, this class is well suited in presence of tons of micro tasks
 * and lots of CPU cores.
 * <p/>
 * See the tests for sample usages.
 */
public class ActiveExecutor {
    public static Log LOGGER = LogFactory.getLog(ActiveExecutor.class);

    /**
     * Just for logs.
     */
    private final String description;

    /**
     * The size of the per-provider sub-queue.
     */
    private final int inputBufferSize;

    /**
     * The number of tasks the counsumer threads are taking at a time.
     */
    private final int outputBulkSize;

    /**
     * The threads doing the job.
     */
    private final Consumer[] consumers;

    /**
     * The main queue.
     */
    private final FArrayBlockingQueue<Runnable> queue;

    /**
     * The per provider sub-queues.
     */
    private final ThreadLocal<List<Runnable>> inputBuffers = new ThreadLocal<List<Runnable>>() {
        protected List<Runnable> initialValue() {
            return new ArrayList<Runnable>(inputBufferSize);
        }
    };

    /**
     * If true, cancel the currently scheduled tasks.
     */
    private final AtomicBoolean cancel = new AtomicBoolean(false);

    /**
     * Approximative max queue size.
     */
    private int maxQueueSize = 0;

    private int nbQueueChanges = 0;

    private long sumQueueSizes = 0;

    public ActiveExecutor(String description, int nbThreads, int inputBufferSize,
                          int queueCapacity, int outputBulkSize) {
        if (inputBufferSize > queueCapacity) {
            throw new RuntimeException("inputBufferSize must be smaller than queueCapacity");
        }
        if (nbThreads < 1) {
            throw new RuntimeException("nbThreads must be bigger or equal to 1");
        }
        if (outputBulkSize < 1) {
            throw new RuntimeException("outputBulkSize must be bigger or equal to 1");
        }

        this.outputBulkSize = outputBulkSize;
        this.description = description;
        this.inputBufferSize = inputBufferSize;
        consumers = new Consumer[nbThreads];
        for (int i = 0; i < consumers.length; i++) {
            consumers[i] = new Consumer(description + " " + i);
        }
        queue = new FArrayBlockingQueue<Runnable>(queueCapacity);
    }

    /**
     * Start the consumer threads.
     */
    public void start() {
        for (int i = 0; i < consumers.length; i++) {
            Consumer consumer = consumers[i];
            consumer.start();
        }
    }

    /**
     * Stop the consumer threads. Must be called when all the provider
     * threads are not adding tasks anymore and have called {@link #flush()}.
     *
     * @param kill If true, cancel the currently scheduled tasks.
     */
    public void stop(boolean kill) throws InterruptedException {
        if (kill) {
            //don't do the waiting tasks.
            cancel.set(true);
            queue.clear();
        } else {
            flush();
        }
        queue.put(STOP);  //start a chain reaction.
    }

    /**
     * Wait until all the consumer threads are actually stopped.
     */
    public void join() throws InterruptedException {
        for (int i = 0; i < consumers.length; i++) {
            Consumer consumer = consumers[i];
            consumer.join();
        }
        if (LOGGER.isDebugEnabled() && nbQueueChanges > 0) {
            LOGGER.debug("Every consumers for [" + description +
                    "] stopped (maxQueue=" + maxQueueSize + " avgQueueSize=" +
                    (sumQueueSizes / nbQueueChanges) + ")");
        }
    }

    /**
     * Add a task.
     * <p/>
     * If {@link #inputBufferSize}>1, Make sure this thread calls
     * {@link #flush()} when you are done adding tasks.
     */
    public void addTask(Runnable task) throws InterruptedException {
        if (inputBufferSize <= 1) {
            queue.put(task);
            updateQueueStats(queue.size());
        } else {
            List<Runnable> inputBuffer = inputBuffers.get();
            inputBuffer.add(task);
            if (inputBuffer.size() >= inputBufferSize) {
                flush();
            }
        }
    }

    /**
     * Flush the per provider queue. Must be called by each provider thread if
     * {@link #inputBufferSize}>1.
     */
    public void flush() throws InterruptedException {
        if (inputBufferSize <= 1) return;
        List<Runnable> inputBuffer = inputBuffers.get();
        queue.put(inputBuffer);
        updateQueueStats(queue.size());
        inputBuffer.clear();
    }

    private void updateQueueStats(int queueSize) {
        maxQueueSize = Math.max(maxQueueSize, queueSize);
        nbQueueChanges++;
        sumQueueSizes += queueSize;
    }

    /**
     * One consumer thread.
     */
    private class Consumer extends Thread {
        public Consumer(String name) {
            super(name);
        }

        public void run() {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Consumer [" + getName() + "] started");

            List<Runnable> curTasks = new ArrayList<Runnable>(outputBulkSize);
            int maxBulkSize = 0;
            long nbTasks = 0;
            long nbRuns = 0;

            while (true) {
                try {
                    queue.blockingDrainTo(curTasks, outputBulkSize);
                    nbRuns++;
                    maxBulkSize = Math.max(maxBulkSize, curTasks.size());
                    for (int i = 0; i < curTasks.size(); i++) {
                        Runnable task = curTasks.get(i);
                        nbTasks++;
                        if (task == STOP || cancel.get()) {
                            queue.put(STOP); //tell the others to stop as well (continue the chain reaction)
                            if (LOGGER.isDebugEnabled())
                                LOGGER.debug("Consumer [" + getName() + "] stopped (maxBulkSize=" + maxBulkSize + " nbTasks=" + nbTasks + " avgBulk=" + (nbTasks / nbRuns) + ")");
                            return;
                        }
                        task.run();
                    }
                    curTasks.clear();
                } catch (InterruptedException e) {
                    //huh?
                }
            }
        }
    }

    /**
     * A special task for the consumer threads to tell them to stop.
     */
    private static final Runnable STOP = new Runnable() {
        public void run() {
            throw new RuntimeException("Not supposed to be called");
        }
    };
}
