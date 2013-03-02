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

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Very much similar to the JDK <code>ArrayBlockingQueue</code> class.
 * <p/>
 * When the producer threads are blocked due to a queue full event,
 * then the consumer threads will not signal the producers until a
 * lower mark is reached again. The point is to give maximum CPU
 * resources to the consumer threads for a while.
 */
@SuppressWarnings({"CallToSignalInsteadOfSignalAll", "LockAcquiredButNotSafelyReleased", "ObjectEquality", "AssignmentToMethodParameter"})
public final class FArrayBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
    /**
     * The queued items
     */
    private final E[] items;

    /**
     * items index for next take, poll or remove
     */
    private int takeIndex;

    /**
     * items index for next put, offer, or add.
     */
    private int putIndex;

    /**
     * Number of items in the queue
     */
    private int count;

    /**
     * See JAVAdoc of this class
     */
    private int lowMark;

    /**
     * @see #lowMark
     */
    private boolean movingBackToLowMark;

    /*
     * Concurrency control uses the classic two-condition algorithm
     * found in any textbook.
     */

    /**
     * Main lock guarding all access
     */
    private final ReentrantLock lock;

    /**
     * Condition for waiting takes
     */
    private final Condition notEmpty;

    /**
     * Condition for waiting puts
     */
    private final Condition notFull;

    /**
     * Circularly increment i.
     */
    private int inc(int i) {
        return (++i == items.length) ? 0 : i;
    }

    /**
     * Insert element at current put position, advance, and signal.
     * Call only when holding lock.
     */
    private void insert(E x) {
        items[putIndex] = x;
        putIndex = inc(putIndex);
        ++count;

        if (count == items.length) {
            movingBackToLowMark = true;
        }

        notEmpty.signal();
    }

    /**
     * Extract element at current take position, advance, and signal.
     * Call only when holding lock.
     */
    private E extract() {
        final E[] items = this.items;
        E x = items[takeIndex];
        items[takeIndex] = null;
        takeIndex = inc(takeIndex);
        --count;
        wakeupProducersIfNeeded();

        return x;
    }

    /**
     * Call only when holding lock.
     */
    private void wakeupProducersIfNeeded() {
        if (!movingBackToLowMark) // regular mode
        {
            notFull.signalAll();
        } else // producers are waiting
        {
            if (count <= lowMark) {
                movingBackToLowMark = false;
                notFull.signalAll();
            }
        }
    }

    /**
     * Utility for remove and iterator.remove: Delete item at position i.
     * Call only when holding lock.
     */
    void removeAt(int i) {
        final E[] items = this.items;
        // if removing front item, just advance
        if (i == takeIndex) {
            items[takeIndex] = null;
            takeIndex = inc(takeIndex);
        } else {
            // slide over all others up through putIndex.
            for (; ;) {
                int nexti = inc(i);
                if (nexti != putIndex) {
                    items[i] = items[nexti];
                    i = nexti;
                } else {
                    items[i] = null;
                    putIndex = i;
                    break;
                }
            }
        }
        --count;
        notFull.signal();
    }

    /**
     * Creates an <tt>ArrayBlockingQueue</tt> with the given (fixed)
     * capacity and default access policy.
     *
     * @param capacity the capacity of this queue
     * @throws IllegalArgumentException if <tt>capacity</tt> is less than 1
     */
    public FArrayBlockingQueue(int capacity) {
        this(capacity, false);
    }

    /**
     * Creates an <tt>ArrayBlockingQueue</tt> with the given (fixed)
     * capacity and the specified access policy.
     *
     * @param capacity the capacity of this queue
     * @param fair     if <tt>true</tt> then queue accesses for threads blocked
     *                 on insertion or removal, are processed in FIFO order; if <tt>false</tt>
     *                 the access order is unspecified.
     * @throws IllegalArgumentException if <tt>capacity</tt> is less than 1
     */
    private FArrayBlockingQueue(int capacity, boolean fair) {
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.items = (E[]) new Object[capacity];
        lock = new ReentrantLock(fair);
        notEmpty = lock.newCondition();
        notFull = lock.newCondition();

        // This is still experimental: for the time being 1/3 is hardcoded.
        {
            //lowMark = capacity - (capacity / 3);
            lowMark = capacity / 3;
        }
    }

    /**
     * Inserts the specified element at the tail of this queue if possible,
     * returning immediately if this queue is full.
     *
     * @param o the element to add.
     * @return <tt>true</tt> if it was possible to add the element to
     *         this queue, else <tt>false</tt>
     * @throws NullPointerException if the specified element is <tt>null</tt>
     */
    public boolean offer(E o) {
        if (o == null) {
            throw new NullPointerException();
        }
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count == items.length) {
                return false;
            } else {
                insert(o);
                return true;
            }
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Inserts the specified element at the tail of this queue, waiting if
     * necessary up to the specified wait time for space to become available.
     *
     * @param o       the element to add
     * @param timeout how long to wait before giving up, in units of
     *                <tt>unit</tt>
     * @param unit    a <tt>TimeUnit</tt> determining how to interpret the
     *                <tt>timeout</tt> parameter
     * @return <tt>true</tt> if successful, or <tt>false</tt> if
     *         the specified waiting time elapses before space is available.
     * @throws InterruptedException if interrupted while waiting.
     * @throws NullPointerException if the specified element is <tt>null</tt>.
     */
    public boolean offer(E o, long timeout, TimeUnit unit)
            throws InterruptedException {

        if (o == null) {
            throw new NullPointerException();
        }
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            long nanos = unit.toNanos(timeout);
            for (; ;) {
                if (count != items.length) {
                    insert(o);
                    return true;
                }
                if (nanos <= 0) {
                    return false;
                }
                try {
                    nanos = notFull.awaitNanos(nanos);
                }
                catch (InterruptedException ie) {
                    notFull.signal(); // propagate to non-interrupted thread
                    throw ie;
                }
            }
        }
        finally {
            lock.unlock();
        }
    }

    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (count == 0) {
                return null;
            }
            return extract();
        }
        finally {
            lock.unlock();
        }
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            long nanos = unit.toNanos(timeout);
            for (; ;) {
                if (count != 0) {
                    return extract();
                }
                if (nanos <= 0) {
                    return null;
                }
                try {
                    nanos = notEmpty.awaitNanos(nanos);
                }
                catch (InterruptedException ie) {
                    notEmpty.signal(); // propagate to non-interrupted thread
                    throw ie;
                }

            }
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Removes a single instance of the specified element from this
     * queue, if it is present.
     */
    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }
        final E[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int i = takeIndex;
            int k = 0;
            for (; ;) {
                if (k++ >= count) {
                    return false;
                }
                if (o.equals(items[i])) {
                    removeAt(i);
                    return true;
                }
                i = inc(i);
            }

        }
        finally {
            lock.unlock();
        }
    }

    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (count == 0) ? null : items[takeIndex];
        }
        finally {
            lock.unlock();
        }
    }

    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            try {
                while (count == 0) {
                    notEmpty.await();
                }
            }
            catch (InterruptedException ie) {
                notEmpty.signal(); // propagate to non-interrupted thread
                throw ie;
            }
            return extract();
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Adds the specified element to the tail of this queue, waiting if
     * necessary for space to become available.
     *
     * @param o the element to add
     * @throws InterruptedException if interrupted while waiting.
     * @throws NullPointerException if the specified element is <tt>null</tt>.
     */
    public void put(E o) throws InterruptedException {
        if (o == null) {
            throw new NullPointerException();
        }
        final E[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            try {
                while (movingBackToLowMark || count == items.length) {
                    notFull.await();
                }
            }
            catch (InterruptedException ie) {
                notFull.signal(); // propagate to non-interrupted thread
                throw ie;
            }

            insert(o);
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Like {@link #put(Object)}, but for multiple entries.
     */
    public void put(List<E> objects) throws InterruptedException {
        final E[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {

            try {
                for (int i = 0; i < objects.size(); i++) {
                    E o = objects.get(i);

                    while (movingBackToLowMark || count == items.length) {
                        notFull.await();
                    }

                    insert(o);
                }
            }
            catch (InterruptedException ie) {
                notFull.signal(); // propagate to non-interrupted thread
                throw ie;
            }
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * @return an approximative number of elements in this queue.
     */
    public int size() {
        return count;
    }

    // this doc comment is a modified copy of the inherited doc comment,
    // without the reference to unlimited queues.
    /**
     * Returns the number of elements that this queue can ideally (in
     * the absence of memory or resource constraints) accept without
     * blocking. This is always equal to the initial capacity of this queue
     * less the current <tt>size</tt> of this queue.
     * <p>Note that you <em>cannot</em> always tell if
     * an attempt to <tt>add</tt> an element will succeed by
     * inspecting <tt>remainingCapacity</tt> because it may be the
     * case that a waiting consumer is ready to <tt>take</tt> an
     * element out of an otherwise full queue.
     */
    public int remainingCapacity() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return items.length - count;
        }
        finally {
            lock.unlock();
        }
    }

    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        final E[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int i = takeIndex;
            int k = 0;
            while (k++ < count) {
                if (o.equals(items[i])) {
                    return true;
                }
                i = inc(i);
            }
            return false;
        }
        finally {
            lock.unlock();
        }
    }

    public Object[] toArray() {
        final E[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] a = new Object[count];
            int k = 0;
            int i = takeIndex;
            while (k < count) {
                a[k++] = items[i];
                i = inc(i);
            }
            return a;
        }
        finally {
            lock.unlock();
        }
    }

    public <T> T[] toArray(T[] a) {
        final E[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (a.length < count) {
                a = (T[]) java.lang.reflect.Array.newInstance(
                        a.getClass().getComponentType(),
                        count
                );
            }

            int k = 0;
            int i = takeIndex;
            while (k < count) {
                a[k++] = (T) items[i];
                i = inc(i);
            }
            if (a.length > count) {
                a[count] = null;
            }
            return a;
        }
        finally {
            lock.unlock();
        }
    }

    public String toString() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return super.toString();
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Atomically removes all of the elements from this queue.
     * The queue will be empty after this call returns.
     */
    public void clear() {
        final E[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int i = takeIndex;
            int k = count;
            while (k-- > 0) {
                items[i] = null;
                i = inc(i);
            }
            count = 0;
            putIndex = 0;
            takeIndex = 0;
            notFull.signalAll();
        }
        finally {
            lock.unlock();
        }
    }

    public int drainTo(Collection<? super E> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        final E[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int i = takeIndex;
            int n = 0;
            int max = count;
            while (n < max) {
                c.add(items[i]);
                items[i] = null;
                i = inc(i);
                ++n;
            }
            if (n > 0) {
                count = 0;
                putIndex = 0;
                takeIndex = 0;
                notFull.signalAll();
            }
            return n;
        }
        finally {
            lock.unlock();
        }
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        if (maxElements <= 0) {
            return 0;
        }
        final E[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int i = takeIndex;
            int n = 0;
            int max = (maxElements < count) ? maxElements : count;
            while (n < max) {
                c.add(items[i]);
                items[i] = null;
                i = inc(i);
                ++n;
            }
            if (n > 0) {
                count -= n;
                takeIndex = i;
                notFull.signalAll();
            }
            return n;
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Same as drainTo, but blocks until at least one element is available.
     */
    public int blockingDrainTo(Collection<? super E> c, int maxElements) throws InterruptedException {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        if (maxElements <= 0) {
            return 0;
        }
        final E[] items = this.items;
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == 0) {
                notEmpty.await();
            }

            int i = takeIndex;
            int n = 0;
            int max = (maxElements < count) ? maxElements : count;
            while (n < max) {
                c.add(items[i]);
                items[i] = null;
                i = inc(i);
                ++n;
            }
            if (n > 0) {
                count -= n;
                takeIndex = i;
                wakeupProducersIfNeeded();
            }
            return n;
        }
        catch (InterruptedException ie) {
            notEmpty.signal(); // propagate to non-interrupted thread
            throw ie;
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Returns an iterator over the elements in this queue in proper sequence.
     * The returned <tt>Iterator</tt> is a "weakly consistent" iterator that
     * will never throw {@link java.util.ConcurrentModificationException},
     * and guarantees to traverse elements as they existed upon
     * construction of the iterator, and may (but is not guaranteed to)
     * reflect any modifications subsequent to construction.
     *
     * @return an iterator over the elements in this queue in proper sequence.
     */
    public Iterator<E> iterator() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return new Itr();
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Iterator for ArrayBlockingQueue
     */
    private class Itr implements Iterator<E> {
        /**
         * Index of element to be returned by next,
         * or a negative number if no such.
         */
        private int nextIndex;

        /**
         * nextItem holds on to item fields because once we claim
         * that an element exists in hasNext(), we must return it in
         * the following next() call even if it was in the process of
         * being removed when hasNext() was called.
         */
        private E nextItem;

        /**
         * Index of element returned by most recent call to next.
         * Reset to -1 if this element is deleted by a call to remove.
         */
        private int lastRet;

        Itr() {
            lastRet = -1;
            if (count == 0) {
                nextIndex = -1;
            } else {
                nextIndex = takeIndex;
                nextItem = items[takeIndex];
            }
        }

        public boolean hasNext() {
            /*
             * No sync. We can return true by mistake here
             * only if this iterator passed across threads,
             * which we don't support anyway.
             */
            return nextIndex >= 0;
        }

        /**
         * Check whether nextIndex is valid; if so setting nextItem.
         * Stops iterator when either hits putIndex or sees null item.
         */
        private void checkNext() {
            if (nextIndex == putIndex) {
                nextIndex = -1;
                nextItem = null;
            } else {
                nextItem = items[nextIndex];
                if (nextItem == null) {
                    nextIndex = -1;
                }
            }
        }

        public E next() {
            final ReentrantLock lock = FArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                if (nextIndex < 0) {
                    throw new NoSuchElementException();
                }
                lastRet = nextIndex;
                E x = nextItem;
                nextIndex = inc(nextIndex);
                checkNext();
                return x;
            }
            finally {
                lock.unlock();
            }
        }

        public void remove() {
            final ReentrantLock lock = FArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                int i = lastRet;
                if (i == -1) {
                    throw new IllegalStateException();
                }
                lastRet = -1;

                int ti = takeIndex;
                removeAt(i);
                // back up cursor (reset to front if was first element)
                nextIndex = (i == ti) ? takeIndex : i;
                checkNext();
            }
            finally {
                lock.unlock();
            }
        }
    }
}
