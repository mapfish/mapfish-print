package org.mapfish.print.servlet.job.impl;

import org.hibernate.annotations.Type;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.config.access.AccessAssertion;
import org.mapfish.print.config.access.AndAccessAssertion;
import org.mapfish.print.servlet.MapPrinterServlet;
import org.mapfish.print.servlet.ServletMapPrinterFactory;
import org.mapfish.print.servlet.job.PrintJobEntry;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.context.ApplicationContext;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Print Job Entry.
 */
@Embeddable
public class PrintJobEntryImpl implements PrintJobEntry {

    @Column(insertable = false, updatable = false)
    @Type(type = "org.hibernate.type.TextType")
    private String referenceId;

    @Column()
    @Type(type = "org.mapfish.print.servlet.job.impl.hibernate.PJsonObjectUserType")
    private PJsonObject requestData;

    @Column
    private long startTime;

    @Column()
    @Type(type = "org.mapfish.print.servlet.job.impl.hibernate.AccessAssertionUserType")
    private AccessAssertion access;

    /**
     * Constructor.
     */
    public PrintJobEntryImpl() {

    }

    /**
     * Constructor.
     *
     * @param referenceId reference of the report.
     * @param requestData the request data
     * @param startTime the time when the print job started.
     */
    public PrintJobEntryImpl(final String referenceId, final PJsonObject requestData, final long startTime) {
        this.referenceId = referenceId;
        this.requestData = requestData;
        this.startTime = startTime;
    }

    /**
     * Constructor.
     *
     * @param referenceId reference of the report.
     * @param requestData the request data
     * @param startTime the time when the print job started.
     * @param access the an access control object for downloading this report.  Typically this is
     *         combined access of the template and the configuration.
     */
    public PrintJobEntryImpl(
            final String referenceId, final PJsonObject requestData, final long startTime,
            final AccessAssertion access) {
        this.referenceId = referenceId;
        this.requestData = requestData;
        this.access = access;
        this.startTime = startTime;
    }

    @Override
    public final String getReferenceId() {
        return this.referenceId;
    }

    public final void setReferenceId(final String referenceId) {
        this.referenceId = referenceId;
    }

    @Override
    public final PJsonObject getRequestData() {
        return this.requestData;
    }

    public final void setRequestData(final PJsonObject requestData) {
        this.requestData = requestData;
    }

    @Override
    public final long getStartTime() {
        return this.startTime;
    }

    public final void setStartTime(final long startTime) {
        this.startTime = startTime;
    }

    @Override
    public final Date getStartDate() {
        return new Date(this.startTime);
    }

    @Override
    public final AccessAssertion getAccess() {
        return this.access;
    }

    public final void setAccess(final AccessAssertion access) {
        this.access = access;
    }

    @Override
    public final String getAppId() {
        return getRequestData().optString(
                MapPrinterServlet.JSON_APP,
                ServletMapPrinterFactory.DEFAULT_CONFIGURATION_FILE_KEY);
    }

    @Override
    public final long getTimeSinceStart() {
        return System.currentTimeMillis() - getStartTime();
    }

    @Override
    public final void assertAccess() {
        this.access.assertAccess(
                getClass().getSimpleName() + " for app '" + getAppId() +
                        "' for print job '" + getReferenceId() + "'", this);
    }

    /**
     * Configure the access permissions required to access this print job.
     *
     * @param template the containing print template which should have sufficient information to
     *         configure the access.
     * @param context the application context
     */
    public final void configureAccess(final Template template, final ApplicationContext context) {
        final Configuration configuration = template.getConfiguration();

        AndAccessAssertion accessAssertion = context.getBean(AndAccessAssertion.class);
        accessAssertion.setPredicates(configuration.getAccessAssertion(), template.getAccessAssertion());
        this.access = accessAssertion;
    }

}
