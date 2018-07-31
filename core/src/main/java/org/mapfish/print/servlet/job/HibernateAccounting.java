package org.mapfish.print.servlet.job;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.mapfish.print.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Store accounting info in the DB.
 */
public class HibernateAccounting extends Accounting {
    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateAccounting.class);

    @Autowired
    private SessionFactory sf;

    @Autowired
    private PlatformTransactionManager txManager;


    @Override
    public JobTracker startJob(final PrintJobEntry entry, final Configuration configuration) {
        return new JobTracker(entry, configuration);
    }

    /**
     * A JobTracker specialization for storing in the DB.
     */
    public class JobTracker extends Accounting.JobTracker {
        /**
         * Constructor.
         *
         * @param entry the job description.
         * @param configuration the job configuration.
         */
        protected JobTracker(final PrintJobEntry entry, final Configuration configuration) {
            super(entry, configuration);
        }

        @Override
        public long onJobSuccess(final PrintJob.PrintResult printResult) {
            final long duractionUSec = super.onJobSuccess(printResult);
            final HibernateAccountingEntry record1 = new HibernateAccountingEntry(
                    this.entry, PrintJobStatus.Status.FINISHED, this.configuration);
            final HibernateAccountingEntry record = record1;
            record.setProcessingTimeMS(duractionUSec / 1000000L);
            record.setFileSize(printResult.fileSize);
            record.setStats(printResult.executionContext.getStats());
            insertRecord(record);
            return duractionUSec;
        }

        @Override
        public void onJobCancel() {
            super.onJobCancel();
            final HibernateAccountingEntry record = new HibernateAccountingEntry(
                    this.entry, PrintJobStatus.Status.CANCELLED, this.configuration);
            insertRecord(record);
        }

        @Override
        public void onJobError() {
            super.onJobError();
            final HibernateAccountingEntry record = new HibernateAccountingEntry(
                    this.entry, PrintJobStatus.Status.ERROR, this.configuration);
            insertRecord(record);
        }

        private void insertRecord(final HibernateAccountingEntry tuple) {
            try {
                final TransactionTemplate tmpl = new TransactionTemplate(HibernateAccounting.this.txManager);
                tmpl.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(final TransactionStatus status) {
                        final Session currentSession = HibernateAccounting.this.sf.getCurrentSession();
                        currentSession.merge(tuple);
                        currentSession.flush();
                        currentSession.evict(tuple);
                    }
                });
            } catch (HibernateException ex) {
                LOGGER.warn("Cannot save accounting information", ex);
            }
        }
    }
}
