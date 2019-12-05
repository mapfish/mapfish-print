package org.mapfish.print.servlet.job.impl.hibernate;

import org.mapfish.print.servlet.job.loader.ReportLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * Loads reports from hibernate uris.
 *
 */
public class HibernateReportLoader implements ReportLoader {

    @Autowired
    private PrintJobDao dao;

    @Override
    public final boolean accepts(final URI reportURI) {
        return reportURI.getScheme().equals("hibernate");
    }

    @Override
    @Transactional
    public final void loadReport(final URI reportURI, final OutputStream out) throws IOException {
        out.write(this.dao.getResult(reportURI).getData());
    }
}
