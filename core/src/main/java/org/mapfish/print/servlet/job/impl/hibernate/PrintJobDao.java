package org.mapfish.print.servlet.job.impl.hibernate;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.mapfish.print.servlet.job.PrintJobStatus;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

/**
 * JobEntryDao.
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

    private Session getSession() {
        return this.sf.getCurrentSession();
    }

    /**
     * Save Job Record.
     *
     * @param entry the entry
     */
    public final void save(final PrintJobStatusExtImpl entry) {
        getSession().merge(entry);
        getSession().flush();
        getSession().evict(entry);
    }

    /**
     * Get Job Record.
     *
     * @param id the id
     * @return the job
     */
    @Nullable
    public final PrintJobStatusExtImpl get(final String id) {
        final PrintJobStatusExtImpl result = get(id, false);
        if (result != null) {
            getSession().evict(result);
        }
        return result;
    }

    /**
     * Get Job Record.
     *
     * @param id the id
     * @param lock whether record should be locked for transaction
     * @return the job status or null.
     */
    @Nullable
    public final PrintJobStatusExtImpl get(final String id, final boolean lock) {
        final CriteriaBuilder builder = getSession().getCriteriaBuilder();
        final CriteriaQuery<PrintJobStatusExtImpl> criteria =
                builder.createQuery(PrintJobStatusExtImpl.class);
        final Root<PrintJobStatusExtImpl> root = criteria.from(PrintJobStatusExtImpl.class);
        root.alias("pj");
        root.fetch("result", JoinType.LEFT);
        criteria.where(builder.equal(root.get("referenceId"), id));
        final Query<PrintJobStatusExtImpl> query = getSession().createQuery(criteria);
        if (lock) {
            //LOCK means SELECT FOR UPDATE which prevents these records to be pulled by different
            // instances
            query.setLockMode("pj", LockMode.PESSIMISTIC_READ);
        } else {
            query.setReadOnly(true);  // make sure the object is not updated if there is no lock
        }
        return query.uniqueResult();
    }

    /**
     * get specific property value of job.
     *
     * @param id the id
     * @param property the property name/path
     * @return the property value
     */
    public final Object getValue(final String id, final String property) {
        final CriteriaBuilder builder = getSession().getCriteriaBuilder();
        final CriteriaQuery<Object> criteria = builder.createQuery(Object.class);
        final Root<PrintJobStatusExtImpl> root = criteria.from(PrintJobStatusExtImpl.class);
        criteria.select(root.get(property));
        criteria.where(builder.equal(root.get("referenceId"), id));
        return getSession().createQuery(criteria).uniqueResult();
    }

    /**
     * @param statuses the statuses to include (or none if all)
     * @return the total amount of jobs
     */
    public final long count(final PrintJobStatus.Status... statuses) {
        final CriteriaBuilder builder = getSession().getCriteriaBuilder();
        final CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        final Root<PrintJobStatusExtImpl> root = criteria.from(PrintJobStatusExtImpl.class);
        criteria.select(builder.count(root));
        if (statuses.length > 0) {
            criteria.where(root.get("status").in(Arrays.asList(statuses)));
        }
        return getSession().createQuery(criteria).uniqueResult();
    }

    /**
     * @param statuses the statuses to include (or none if all)
     * @return the jobs
     */
    public final List<PrintJobStatusExtImpl> get(final PrintJobStatus.Status... statuses) {
        final CriteriaBuilder builder = getSession().getCriteriaBuilder();
        final CriteriaQuery<PrintJobStatusExtImpl> criteria =
                builder.createQuery(PrintJobStatusExtImpl.class);
        final Root<PrintJobStatusExtImpl> root = criteria.from(PrintJobStatusExtImpl.class);
        if (statuses.length > 0) {
            criteria.where(root.get("status").in(Arrays.asList(statuses)));
        }
        return getSession().createQuery(criteria).list();
    }

    /**
     * @return total time spent printing
     */
    public final long getTotalTimeSpentPrinting() {
        final CriteriaBuilder builder = getSession().getCriteriaBuilder();
        final CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        final Root<PrintJobStatusExtImpl> root = criteria.from(PrintJobStatusExtImpl.class);
        criteria.where(root.get("completionTime").isNotNull());
        criteria.select(builder.sum(builder.diff(
                root.get("completionTime"), root.get("entry").get("startTime"))));
        final Long result = getSession().createQuery(criteria).uniqueResult();
        return result != null ? result : 0;
    }

    /**
     * Cancel old waiting jobs.
     *
     * @param starttimeThreshold threshold for start time
     * @param checkTimeThreshold threshold for last check time
     * @param message the error message
     */
    public final void cancelOld(
            final long starttimeThreshold, final long checkTimeThreshold, final String message) {
        final CriteriaBuilder builder = getSession().getCriteriaBuilder();
        final CriteriaUpdate<PrintJobStatusExtImpl> update =
                builder.createCriteriaUpdate(PrintJobStatusExtImpl.class);
        final Root<PrintJobStatusExtImpl> root = update.from(PrintJobStatusExtImpl.class);
        update.where(builder.and(
                builder.equal(root.get("status"), PrintJobStatus.Status.WAITING),
                builder.or(
                        builder.lessThan(root.get("entry").get("startTime"), starttimeThreshold),
                        builder.and(builder.isNotNull(root.get("lastCheckTime")),
                                    builder.lessThan(root.get("lastCheckTime"), checkTimeThreshold))
                )
        ));
        update.set(root.get("status"), PrintJobStatus.Status.CANCELLED);
        update.set(root.get("error"), message);
        getSession().createQuery(update).executeUpdate();
    }

    /**
     * Update the lastCheckTime of the given record.
     *
     * @param id the id
     * @param lastCheckTime the new value
     */
    public final void updateLastCheckTime(final String id, final long lastCheckTime) {
        final CriteriaBuilder builder = getSession().getCriteriaBuilder();
        final CriteriaUpdate<PrintJobStatusExtImpl> update =
                builder.createCriteriaUpdate(PrintJobStatusExtImpl.class);
        final Root<PrintJobStatusExtImpl> root = update.from(PrintJobStatusExtImpl.class);
        update.where(builder.equal(root.get("referenceId"), id));
        update.set(root.get("lastCheckTime"), lastCheckTime);
        getSession().createQuery(update).executeUpdate();
    }

    /**
     * Delete old jobs.
     *
     * @param checkTimeThreshold threshold for last check time
     * @return the number of jobs deleted
     */
    public final int deleteOld(final long checkTimeThreshold) {
        final CriteriaBuilder builder = getSession().getCriteriaBuilder();
        final CriteriaDelete<PrintJobStatusExtImpl> delete =
                builder.createCriteriaDelete(PrintJobStatusExtImpl.class);
        final Root<PrintJobStatusExtImpl> root = delete.from(PrintJobStatusExtImpl.class);
        delete.where(builder.and(builder.isNotNull(root.get("lastCheckTime")),
                                 builder.lessThan(root.get("lastCheckTime"), checkTimeThreshold)));
        return getSession().createQuery(delete).executeUpdate();
    }

    /**
     * Poll for the next N waiting jobs in line.
     *
     * @param size maximum amount of jobs to poll for
     * @return up to "size" jobs
     */
    public final List<PrintJobStatusExtImpl> poll(final int size) {
        final CriteriaBuilder builder = getSession().getCriteriaBuilder();
        final CriteriaQuery<PrintJobStatusExtImpl> criteria =
                builder.createQuery(PrintJobStatusExtImpl.class);
        final Root<PrintJobStatusExtImpl> root = criteria.from(PrintJobStatusExtImpl.class);
        root.alias("pj");
        criteria.where(builder.equal(root.get("status"), PrintJobStatus.Status.WAITING));
        criteria.orderBy(builder.asc(root.get("entry").get("startTime")));
        final Query<PrintJobStatusExtImpl> query = getSession().createQuery(criteria);
        query.setMaxResults(size);
        return query.getResultList();
    }

    /**
     * Get result report.
     *
     * @param reportURI the URI of the report
     * @return the result report.
     */
    public final PrintJobResultExtImpl getResult(final URI reportURI) {
        final CriteriaBuilder builder = getSession().getCriteriaBuilder();
        final CriteriaQuery<PrintJobResultExtImpl> criteria =
                builder.createQuery(PrintJobResultExtImpl.class);
        final Root<PrintJobResultExtImpl> root = criteria.from(PrintJobResultExtImpl.class);
        criteria.where(builder.equal(root.get("reportURI"), reportURI.toString()));
        return getSession().createQuery(criteria).uniqueResult();
    }

    /**
     * Delete a record.
     *
     * @param referenceId the reference ID.
     */
    public void delete(final String referenceId) {
        final CriteriaBuilder builder = getSession().getCriteriaBuilder();
        final CriteriaDelete<PrintJobStatusExtImpl> delete =
                builder.createCriteriaDelete(PrintJobStatusExtImpl.class);
        final Root<PrintJobStatusExtImpl> root = delete.from(PrintJobStatusExtImpl.class);
        delete.where(builder.equal(root.get("referenceId"), referenceId));
        getSession().createQuery(delete).executeUpdate();
    }
}
