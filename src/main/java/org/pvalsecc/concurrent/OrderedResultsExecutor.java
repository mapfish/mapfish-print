package org.pvalsecc.concurrent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Take tasks and execute them in //. Each task generates a result and the
 * results are sent to a resultCollector.
 *
 * The guaranties are:
 * <ul>
 * <li>{@link #addTask} is thread safe
 * <li>the results are sent to the resultCollector in the order their tasks have
 * been added.
 * <li>{@link org.pvalsecc.concurrent.OrderedResultsExecutor.ResultCollector#handle(Object)}
 * is called only one result at a time for a given instance of {@link org.pvalsecc.concurrent.OrderedResultsExecutor.ResultCollector}.
 * No // call of this method for a given object.
 * </ul>
 */
public class OrderedResultsExecutor<RESULT> {
    public static Log LOGGER = LogFactory.getLog(OrderedResultsExecutor.class);

    /**
     * The base name for the executor threads.
     */
    private final String name;

    /**
     * The executor threads.
     */
    private final Thread[] threads;

    /**
     * The sequence used to attribute the order of the tasks.
     */
    private AtomicLong nextSequenceNumber = new AtomicLong(0L);

    /**
     * Queue of tasks to do. Protected by itself.
     */
    private final Queue<InternalTask<RESULT>> queue;

    /**
     * Ordered structure used to store the results the time they are in order.
     */
    private final SortedSet<InternalTask<RESULT>> output = Collections.synchronizedSortedSet(new TreeSet<InternalTask<RESULT>>());

    /**
     * number of the next task to be sent out. Protected by {@link  #nextOutputLock}
     */
    private long nextOutput = 1L;

    /**
     * The lock to protect {@link #nextOutput}.
     */
    private final Object nextOutputLock = new Object();


    public OrderedResultsExecutor(int nbThreads, String name) {
        this.name = name;
        this.threads = new Thread[nbThreads];
        queue = new LinkedList<InternalTask<RESULT>>();
    }

    /**
     * Start the executor threads.
     */
    public void start() {
        for (int i = 0; i < threads.length; i++) {
            if(threads[i]==null) {
                Thread thread = threads[i] = new Thread(new Runner(), name+i);
                thread.setDaemon(true);
                thread.start();
            }
        }
    }

    /**
     * Stop the executor threads.
     */
    public void stop() {
        synchronized (queue) {
            for (int i = 0; i < threads.length; ++i) {
                  //null task means "die!"
                queue.add(new InternalTask<RESULT>(null, null, 0));
            }
            queue.notifyAll();
        }

        for (int i = 0; i < threads.length; i++) {
            Thread thread = threads[i];
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (InterruptedException e) {
                    //retry
                }
            }
            threads[i]=null;
        }
    }

    /**
     * Adds a task whose result will be sent to the given resultCollector.
     */
    public void addTask(Task<RESULT> command, ResultCollector<RESULT> resultCollector) {
        synchronized (queue) {
            queue.add(new InternalTask<RESULT>(command, resultCollector, nextSequenceNumber.incrementAndGet()));
            queue.notify();
        }
    }

    private void addOutput(InternalTask<RESULT> task) {
        output.add(task);
        while (true) {
            InternalTask<RESULT> first;
            synchronized (nextOutputLock) {
                if(output.isEmpty()) {
                    //next one not yet available
                    return;
                }
                first = output.first();
                if (first.sequenceNumber != nextOutput) {
                    //next one not yet available
                    return;
                }

                //it wouldn't be a good idea to take the resultCollector lock here.
                //That would serialize the calls between resultCollectors
            }

            //"the dangerous point"

            synchronized (first.resultCollector) {
                synchronized (nextOutputLock) {
                    if(first.sequenceNumber !=nextOutput) {
                        //Another thread took over us during "the dangerous point" with the same nextOutputValue
                        continue;
                    }
                    //now we are sure we can output "first"
                    ++nextOutput;
                    output.remove(first);
                }
                first.resultCollector.handle(first.result);
            }
        }
    }

    /**
     * One executor thread.
     */
    public class Runner implements Runnable {
        public void run() {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Runner [" + name + "] started");
            while (true) {
                //gets a task to be executed
                InternalTask<RESULT> cur;
                synchronized (queue) {
                    while ((cur = queue.poll()) == null) {
                        try {
                            queue.wait();
                        } catch (InterruptedException e) {
                            //ignored
                        }
                    }
                }

                if (cur.task == null) {
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("Runner [" + name + "] stopped");
                    return;  //received the signal to stop
                }

                //runs it and schedule its result
                cur.setState(ExecutionState.RUNNING);
                try {
                    final RESULT process = cur.task.process();
                    cur.setResult(process);
                    addOutput(cur);
                } catch (Throwable t) {
                    cur.setState(ExecutionState.ERROR);
                    cur.setError(t);
                } finally {
                    cur.setState(ExecutionState.DONE);
                }
            }
        }
    }

    private enum ExecutionState {
        PENDING, RUNNING, DONE, ERROR
    }

    /**
     * Internal structure which represents a task and it's related information.
     * @param <RESULT>
     */
    private static class InternalTask<RESULT> implements Comparable<InternalTask<RESULT>> {
        private final Task<RESULT> task;
        private final ResultCollector<RESULT> resultCollector;
        private final long sequenceNumber;
        private ExecutionState state;
        private RESULT result = null;
        private Throwable error;

        public InternalTask(Task<RESULT> task, ResultCollector<RESULT> resultCollector, long sequenceNumber) {
            this.task = task;
            this.resultCollector = resultCollector;
            this.sequenceNumber = sequenceNumber;
            this.state = ExecutionState.PENDING;
        }

        public void setResult(RESULT result) {
            if (this.result != null) {
                throw new RuntimeException("Synchronization bug");
            }
            this.result = result;
        }

        public int compareTo(InternalTask<RESULT> o) {
            return (sequenceNumber < o.sequenceNumber ? -1 : (sequenceNumber == o.sequenceNumber ? 0 : 1));
        }

        public synchronized void setState(ExecutionState state) {
            this.state = state;
        }

        public synchronized void setError(Throwable error) {
            this.error = error;
        }
    }

    /**
     * Definition of a task.
     */
    public static interface Task<RESULT> {
        /**
         * Called in parallel and in random order to do the processing. The
         * implementation must be thread safe.
         * @return The result of the task.
         */
        RESULT process();
    }

    /**
     * Definition of a result collector.
     */
    public static interface ResultCollector<RESULT> {
        /**
         * Will be called sequentially (no // execution for the same instance)
         * with each task's result, in the order the task have been scheduled.
         */
        public void handle(RESULT result);
    }
}
