/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.output;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRPdfExporterParameter;
import org.mapfish.print.config.PDFConfig;

import java.io.OutputStream;

/**
 * An PDF output format that uses Jasper reports to generate the result.
 *
 * @author Jesse
 * @author sbrunner
 */
public final class JasperReportPDFOutputFormat extends AbstractJasperReportOutputFormat implements OutputFormat {

    @Override
    public String getContentType() {
        return "application/pdf";
    }

    @Override
    public String getFileSuffix() {
        return "pdf";
    }

    @Override
    protected void doExport(final OutputStream outputStream, final Print print) throws JRException {

        JRPdfExporter exporter = new JRPdfExporter(print.context);

        exporter.setParameter(JRExporterParameter.JASPER_PRINT, print.print);
        exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, outputStream);
        exporter.setParameter(JRPdfExporterParameter.PDF_VERSION, JRPdfExporterParameter.PDF_VERSION_1_7);
        final PDFConfig pdfConfig = print.values.getObject(Values.PDF_CONFIG, PDFConfig.class);
        exporter.setParameter(JRPdfExporterParameter.IS_COMPRESSED, pdfConfig.isCompressed());
        exporter.setParameter(JRPdfExporterParameter.METADATA_AUTHOR, pdfConfig.getAuthor());
        exporter.setParameter(JRPdfExporterParameter.METADATA_CREATOR, pdfConfig.getCreator());
        exporter.setParameter(JRPdfExporterParameter.METADATA_SUBJECT, pdfConfig.getSubject());
        exporter.setParameter(JRPdfExporterParameter.METADATA_TITLE, pdfConfig.getTitle());
        exporter.setParameter(JRPdfExporterParameter.METADATA_KEYWORDS, pdfConfig.getKeywordsAsString());

        exporter.exportReport();
    }
}
