package org.mapfish.print.servlet.job.impl;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.net.URI;
import java.net.URISyntaxException;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.mapfish.print.PrintException;
import org.mapfish.print.servlet.job.PrintJobResult;
import org.mapfish.print.servlet.job.PrintJobStatus;

/** Print Job Result. */
@Entity
@Table(name = "print_job_results")
public class PrintJobResultImpl implements PrintJobResult {

  @Id private String reportURI;

  @Column private String mimeType;

  @Column private String fileExtension;

  @Column private String fileName;

  @OneToOne(targetEntity = PrintJobStatusImpl.class, fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "referenceId", insertable = false, updatable = false)
  private PrintJobStatus status = null;

  @Column(insertable = true, updatable = true)
  private String referenceId;

  /** Default Constructor. */
  public PrintJobResultImpl() {
    this.reportURI = null;
    this.mimeType = null;
    this.fileExtension = null;
    this.fileName = null;
    this.referenceId = null;
  }

  /**
   * Constructor.
   *
   * @param reportURI the report URI
   * @param fileName the file name
   * @param fileExtension the file extension
   * @param mimeType the mime type
   * @param referenceId the reference ID
   */
  public PrintJobResultImpl(
      final URI reportURI,
      final String fileName,
      final String fileExtension,
      final String mimeType,
      final String referenceId) {
    this.reportURI = reportURI.toString();
    this.mimeType = mimeType;
    this.fileName = fileName;
    this.fileExtension = fileExtension;
    this.referenceId = referenceId;
  }

  @Override
  public URI getReportURI() {
    try {
      return this.reportURI == null ? null : new URI(this.reportURI);
    } catch (URISyntaxException e) {
      throw new PrintException("Failed to create URI", e);
    }
  }

  @Override
  public String getReportURIString() {
    return this.reportURI;
  }

  @Override
  public String getMimeType() {
    return this.mimeType;
  }

  @Override
  public String getFileExtension() {
    return this.fileExtension;
  }

  @Override
  public String getFileName() {
    return this.fileName;
  }
}
