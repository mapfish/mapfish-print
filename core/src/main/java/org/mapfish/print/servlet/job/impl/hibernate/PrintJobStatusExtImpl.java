package org.mapfish.print.servlet.job.impl.hibernate;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import org.mapfish.print.servlet.job.PrintJobEntry;
import org.mapfish.print.servlet.job.impl.PrintJobStatusImpl;

/** Extension of PrintJob Status that holds last check time. */
@Entity
public class PrintJobStatusExtImpl extends PrintJobStatusImpl {

  @Nullable @Column private Long lastCheckTime = System.currentTimeMillis();

  /** Constructor. */
  public PrintJobStatusExtImpl() {}

  /**
   * Constructor.
   *
   * @param entry the print job entry.
   * @param requestCount the request count
   */
  public PrintJobStatusExtImpl(final PrintJobEntry entry, final long requestCount) {
    super(entry, requestCount);
    if (entry.getRequestData().optJSONObject("smtp") != null) {
      // If the result is to be sent by email, we don't expect the client to check the status of the
      // job.
      lastCheckTime = null;
    }
  }

  public Long getLastCheckTime() {
    return this.lastCheckTime;
  }

  public void setLastCheckTime(final long lastCheckTime) {
    this.lastCheckTime = lastCheckTime;
  }
}
