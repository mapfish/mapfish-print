package org.mapfish.print.servlet.job.impl.hibernate;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;
import org.mapfish.print.servlet.job.PrintJobStatus;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.List;

import javax.annotation.PostConstruct;

/**
 * JobEntryDao.
 *
 */
public class PrintJobDao {
    
    @Autowired
    private SessionFactory sf;
        
    /**
     * Initialize db manager. 
     */
    @PostConstruct
    public final void init() {
        this.sf.openSession();
    }
    
    public final Session getSession() {
        return this.sf.getCurrentSession();
    }
    
    /**
     * 
     * Save Job Record.
     * 
     * @param entry the entry
     */
    public final void save(final PrintJobStatusExtImpl entry) {  
        getSession().merge(entry);
    }
        
    /**
     * 
     * Get Job Record.
     * 
     * @param id the id
     * @return
     */
    public final PrintJobStatusExtImpl get(final String id) {
        return get(id, false);
    }
    
    /**
     * 
     * Get Job Record.
     * 
     * @param id the id
     * @param lock whether record should be locked for transaction
     * @return the job status.
     */
    public final PrintJobStatusExtImpl get(final String id, final boolean lock) {
        Criteria c = getSession().createCriteria(PrintJobStatusExtImpl.class);
        c.add(Restrictions.idEq(id));
        if (lock) { //LOCK means SELECT FOR UPDATE which prevents these records to be pulled by different instances
           c.setLockMode("pj", LockMode.PESSIMISTIC_READ);
           c.setFetchMode("result", FetchMode.SELECT);
        }
        return (PrintJobStatusExtImpl) c.uniqueResult();
    }
    
    /**
     * get specific property value of job.
     * 
     * @param id the id
     * @param property the property name/path
     * @return the property value
     */
    public final Object getValue(final String id, final String property) {
        Criteria c = getSession().createCriteria(PrintJobStatusExtImpl.class);
        c.add(Restrictions.idEq(id));
        c.setProjection(Projections.property(property));
        return c.uniqueResult();
    }
    
    /**
     * 
     * @param statuses the statuses to include (or none if all)
     * @return the total amount of jobs
     */
    public final int count(final PrintJobStatus.Status... statuses) {
        Criteria c = getSession().createCriteria(PrintJobStatusExtImpl.class);
        if (statuses.length > 0) {
            c.add(Restrictions.in("status" , statuses));
        }
        c.setProjection(Projections.rowCount());
        return ((Number) c.uniqueResult()).intValue();
    }
    
    /**
     * 
     * @param statuses the statuses to include (or none if all)
     * @return the jobs
     */
    @SuppressWarnings("unchecked")
    public final List<PrintJobStatusExtImpl> get(final PrintJobStatus.Status... statuses) {
        Criteria c = getSession().createCriteria(PrintJobStatusExtImpl.class);
        if (statuses.length > 0) {
            c.add(Restrictions.in("status" , statuses));
        }
        return (List<PrintJobStatusExtImpl>) c.list();
    }
    
    /**
     * 
     * @return total time spent printing
     */
    public final long getTotalTimeSpentPrinting() {
        Criteria c = getSession().createCriteria(PrintJobStatusExtImpl.class);
        c.add(Restrictions.isNotNull("completionTime"));
        c.setProjection(Projections.sqlProjection("sum(completionTime - startTime) as totalTime", 
                new String[] {"totalTime"}, new Type[] { LongType.INSTANCE }));
        Number result = (Number) c.uniqueResult();
        return result == null ? 0 : result.longValue();
    }
    
    /**
     * Cancel old waiting jobs.
     * 
     * @param starttimeThreshold threshold for start time
     * @param checkTimeThreshold threshold for last check time
     * @param message the error message
     */
    public final void cancelOld(final long starttimeThreshold, final long checkTimeThreshold, final String message) {
        Query query = getSession().createQuery("update PrintJobStatusExtImpl pj " + "set status=:newstatus, error=:msg "
                + "where pj.status = :oldstatus " + "and (startTime < :starttimethreshold "
                + "or lastCheckTime < :checktimethreshold)");
        query.setParameter("oldstatus", PrintJobStatus.Status.WAITING);
        query.setParameter("newstatus", PrintJobStatus.Status.CANCELLED);
        query.setParameter("msg", message);
        query.setParameter("starttimethreshold", starttimeThreshold);
        query.setParameter("checktimethreshold", checkTimeThreshold);
        query.executeUpdate();
    }

    /**
     * Delete old jobs.
     * 
     * @param checkTimeThreshold
     *            threshold for last check time
     */
    public final void deleteOld(final long checkTimeThreshold) {
        Query query = getSession()
                .createQuery("delete from PrintJobStatusExtImpl " + "where lastCheckTime < :checktimethreshold)");
        query.setParameter("checktimethreshold", checkTimeThreshold);
        query.executeUpdate();
    }

    /**
     * Poll for the next N waiting jobs in line.
     * 
     * @param size maximum amount of jobs to poll for
     * @return
     */
    @SuppressWarnings("unchecked")
    public final List<PrintJobStatusExtImpl> poll(final int size) {
        Query query = getSession()
                .createQuery("from PrintJobStatusExtImpl pj " + "where status = :status " + "order by startTime");
        query.setParameter("status", PrintJobStatus.Status.WAITING);
        query.setMaxResults(size);
        // LOCK but don't wait for release (since this is run continuously
        // anyway, no wait prevents deadlock)
        query.setLockMode("pj", LockMode.UPGRADE_NOWAIT);
        return (List<PrintJobStatusExtImpl>) query.list();
    }

    /**
     * Get result report.
     * 
     * @param reportURI
     *            the URI of the report
     * @return the result report.
     */
    public final PrintJobResultExtImpl getResult(final URI reportURI) {
        Criteria c = getSession().createCriteria(PrintJobResultExtImpl.class);
        c.add(Restrictions.idEq(reportURI.toString()));
        return (PrintJobResultExtImpl) c.uniqueResult();
    }

}
