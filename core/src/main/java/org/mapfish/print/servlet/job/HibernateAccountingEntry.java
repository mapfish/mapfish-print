package org.mapfish.print.servlet.job;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.mapfish.print.Constants;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.processor.ExecutionStats;
import org.mapfish.print.servlet.MapPrinterServlet;
import org.mapfish.print.wrapper.ObjectMissingException;
import org.mapfish.print.wrapper.PObject;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entity for the print_accountings table.
 */
@Entity
@Table(name = "print_accountings")
@TypeDef(name = "jsonb-node", typeClass = JsonBinaryType.class, defaultForType = ObjectNode.class)
public class HibernateAccountingEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateAccountingEntry.class);

    @Id
    @Column(name = "reference_id")
    @Type(type = "org.hibernate.type.TextType")
    private String referenceId;

    @Column(nullable = false, name = "app_id")
    @Type(type = "org.hibernate.type.TextType")
    private String appId;

    @Column
    @Type(type = "org.hibernate.type.TextType")
    private String referer;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PrintJobStatus.Status status;

    @Column(nullable = false, name = "completion_time")
    private Date completionTime = new Date();

    @Column(name = "processing_time_ms")
    private Long processingTimeMS = null;

    @Column(name = "total_time_ms", nullable = false)
    private long totalTimeMS;

    @Column(nullable = false, name = "output_format")
    @Type(type = "org.hibernate.type.TextType")
    private String outputFormat;

    @Column(nullable = false)
    @Type(type = "org.hibernate.type.TextType")
    private String layout;

    @Column(name = "file_size")
    private Long fileSize = null;

    @Column(columnDefinition = "jsonb")
    private ObjectNode stats = null;

    @Column(nullable = false)
    private boolean mapExport;

    /**
     * Default constructor (used only by Hibernate).
     */
    public HibernateAccountingEntry() {
    }

    /**
     * Constructor that initialize the fields that depends on the job description.
     *
     * @param entry The job description
     * @param status The status
     * @param configuration The configuration
     */
    public HibernateAccountingEntry(
            final PrintJobEntry entry, final PrintJobStatus.Status status,
            final Configuration configuration) {
        this.referenceId = entry.getReferenceId();
        this.appId = entry.getAppId();
        final PJsonObject specJson = entry.getRequestData();
        try {
            final PObject headers = specJson.
                    getObject(MapPrinterServlet.JSON_ATTRIBUTES).
                    getObject(MapPrinterServlet.JSON_REQUEST_HEADERS).
                    getObject(MapPrinterServlet.JSON_REQUEST_HEADERS);
            if (headers.has("referer")) {
                this.referer = headers.getArray("referer").getString(0);
            }
        } catch (ObjectMissingException ex) {
            LOGGER.info("Cannot get the referer", ex);
        }

        try {
            this.outputFormat = specJson.getString(MapPrinterServlet.JSON_OUTPUT_FORMAT);
        } catch (ObjectMissingException ex) {
            LOGGER.info("Cannot get the output format", ex);
        }

        try {
            this.layout = specJson.getString(Constants.JSON_LAYOUT_KEY);
            this.mapExport = configuration.getTemplate(this.layout).isMapExport();
        } catch (ObjectMissingException ex) {
            LOGGER.info("Cannot get the layout", ex);
        }

        this.status = status;

        this.totalTimeMS = System.currentTimeMillis() - entry.getStartTime();
    }

    public String getReferenceId() {
        return this.referenceId;
    }

    public String getAppId() {
        return this.appId;
    }

    public String getReferer() {
        return this.referer;
    }

    public PrintJobStatus.Status getStatus() {
        return this.status;
    }

    public Long getProcessingTimeMS() {
        return this.processingTimeMS;
    }

    public void setProcessingTimeMS(final Long processingTimeMS) {
        this.processingTimeMS = processingTimeMS;
    }

    public long getTotalTimeMS() {
        return this.totalTimeMS;
    }

    public Date getCompletionTime() {
        return this.completionTime;
    }

    public String getOutputFormat() {
        return this.outputFormat;
    }

    public String getLayout() {
        return this.layout;
    }

    public Long getFileSize() {
        return this.fileSize;
    }

    public void setFileSize(final Long fileSize) {
        this.fileSize = fileSize;
    }

    public ObjectNode getStats() {
        return this.stats;
    }

    public void setStats(final ExecutionStats stats) {
        this.stats = stats.toJson();
    }

    public boolean getMapExport() {
        return this.mapExport;
    }
}
