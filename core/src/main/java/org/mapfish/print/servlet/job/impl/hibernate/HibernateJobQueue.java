package org.mapfish.print.servlet.job.impl.hibernate;

import org.mapfish.print.servlet.job.JobQueue;
import org.mapfish.print.servlet.job.NoSuchReferenceException;
import org.mapfish.print.servlet.job.PrintJobEntry;
import org.mapfish.print.servlet.job.PrintJobResult;
import org.mapfish.print.servlet.job.PrintJobStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 
 * Db Job Manager.
 *
 */
@Transactional
public class HibernateJobQueue implements JobQueue {
    
    @Autowired
    private PrintJobDao dao;

    @Override
    public final long getTimeToKeepAfterAccessInMillis() {
        return -1;
    }
    
    @Override
    public final int getLastPrintCount() {
        return this.dao.count(PrintJobStatus.Status.FINISHED, PrintJobStatus.Status.CANCELLED, PrintJobStatus.Status.ERROR);
    }
    
    @Override
    public final int getWaitingJobsCount() {
        return this.dao.count(PrintJobStatus.Status.WAITING, PrintJobStatus.Status.RUNNING);
    }
    
    @Override
    public final int getNumberOfRequestsMade() {
        return this.dao.count();
    }
    
    @Override
    public final long getAverageTimeSpentPrinting() {
        return this.dao.getTotalTimeSpentPrinting() / Math.max(1, getLastPrintCount());
    }

    @Override
    @Transactional(readOnly = true)
    public final long timeSinceLastStatusCheck(final String referenceId) {
        return System.currentTimeMillis() - ((Number) this.dao.getValue(referenceId, "lastCheckTime")).longValue();
    }

    @Override
    public final PrintJobStatus get(final String referenceId, final boolean external) throws NoSuchReferenceException {
        long now = System.currentTimeMillis();
        PrintJobStatusExtImpl record = this.dao.get(referenceId);
        if (record == null) {
            throw new NoSuchReferenceException(referenceId);
        }
        record.setStatusTime(now);
        if (!record.isDone() && external) {
            record.setLastCheckTime(System.currentTimeMillis());
            this.dao.save(record);
        }
        return record;
    }

    @Override
    public final synchronized void add(final PrintJobEntry jobEntry) {
        this.dao.save(new PrintJobStatusExtImpl(jobEntry, getNumberOfRequestsMade()));
    }
    
    @Override
    public final synchronized void cancel(final String referenceId, final String message, final boolean forceFinal) 
            throws NoSuchReferenceException {
        PrintJobStatusExtImpl record = this.dao.get(referenceId, true);
        if (record == null) {
            throw new NoSuchReferenceException(referenceId);
        }
        if (!forceFinal && record.getStatus() == PrintJobStatus.Status.RUNNING) {
            record.setStatus(PrintJobStatus.Status.CANCELING);
        } else {
            record.setCompletionTime(System.currentTimeMillis());
            record.setStatus(PrintJobStatus.Status.CANCELLED);
        }
        record.setError(message);
        this.dao.save(record);
    }


    @Override
    public final synchronized void fail(final String referenceId, final String message) 
            throws NoSuchReferenceException {
        PrintJobStatusExtImpl record = this.dao.get(referenceId, true);
        if (record == null) {
            throw new NoSuchReferenceException(referenceId);
        }
        record.setCompletionTime(System.currentTimeMillis());
        record.setStatus(PrintJobStatus.Status.ERROR);
        record.setError(message);
        this.dao.save(record);
    }

    @Override
    public final synchronized void start(final String referenceId) throws NoSuchReferenceException {
        PrintJobStatusExtImpl record = this.dao.get(referenceId, true);
        if (record == null) {
            throw new NoSuchReferenceException(referenceId);
        }
        record.setStatus(PrintJobStatus.Status.RUNNING);
        record.setWaitingTime(0);
        this.dao.save(record);
    }

    @Override
    public final synchronized void done(final String referenceId, final PrintJobResult result) throws NoSuchReferenceException {
        PrintJobStatusExtImpl record = this.dao.get(referenceId, true);
        if (record == null) {
            throw new NoSuchReferenceException(referenceId);
        }
        record.setStatus(record.getStatus() == PrintJobStatus.Status.CANCELING ? PrintJobStatus.Status.CANCELLED : 
            PrintJobStatus.Status.FINISHED);
        record.setResult(result);
        record.setCompletionTime(System.currentTimeMillis());
        this.dao.save(record);
    }

    @Override
    public final synchronized void cancelOld(final long startTimeOut, final long abandonTimeout, final String message) {
        long now = System.currentTimeMillis();
        this.dao.cancelOld(now - startTimeOut, now - abandonTimeout, message);
    }

    @Override
    public final synchronized List<? extends PrintJobStatus> start(final int number) {
        List<PrintJobStatusExtImpl> list = this.dao.poll(number);
        for (PrintJobStatusExtImpl record : list) {
            record.setStatus(PrintJobStatus.Status.RUNNING);
            record.setWaitingTime(0);
            this.dao.save(record);
        }
        return list;
    }

    @Override
    public final List<? extends PrintJobStatus> toCancel() {
        return this.dao.get(PrintJobStatus.Status.CANCELING);
    }
    
}
