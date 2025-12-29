package org.mapfish.print.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import org.mapfish.print.PrintException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/** Base class for MapPrinter servlets (deals with the configuration loading). */
public abstract class BaseMapServlet {
  private static final Logger LOGGER = LoggerFactory.getLogger(BaseMapServlet.class);
  private int cacheDurationInSeconds = 3600;

  /**
   * Send an error to the client with a message.
   *
   * @param httpServletResponse the response to send the error to.
   * @param message the message to send
   * @param code the error code
   */
  protected static void error(
      final HttpServletResponse httpServletResponse, final String message, final HttpStatus code) {
    try {
      httpServletResponse.setContentType("text/plain");
      httpServletResponse.setStatus(code.value());
      setNoCache(httpServletResponse);
      try (PrintWriter out = httpServletResponse.getWriter()) {
        out.println("Error while processing request:");
        out.println(message);
      }

      LOGGER.warn("Error while processing request: {}", message);
    } catch (IOException ex) {
      throw new PrintException("Failed to send an error", ex);
    }
  }

  /**
   * Disable caching of the response.
   *
   * @param response the response
   */
  protected static void setNoCache(final HttpServletResponse response) {
    response.setHeader("Cache-Control", "max-age=0, must-revalidate, no-cache, no-store");
  }

  /**
   * Returns the base URL of the print servlet.
   *
   * @param httpServletRequest the request
   */
  protected final StringBuilder getBaseUrl(final HttpServletRequest httpServletRequest) {
    StringBuilder baseURL = new StringBuilder();
    if (httpServletRequest.getContextPath() != null
        && !httpServletRequest.getContextPath().isEmpty()) {
      baseURL.append(httpServletRequest.getContextPath());
    }
    if (httpServletRequest.getServletPath() != null
        && !httpServletRequest.getServletPath().isEmpty()) {
      baseURL.append(httpServletRequest.getServletPath());
    }
    return baseURL;
  }

  /**
   * Set the cache duration for the queries that can be cached.
   *
   * @param cacheDurationInSeconds the duration
   */
  public final void setCacheDuration(final int cacheDurationInSeconds) {
    this.cacheDurationInSeconds = cacheDurationInSeconds;
  }

  /**
   * Enable caching of the response.
   *
   * @param response the response
   */
  protected void setCache(final HttpServletResponse response) {
    response.setHeader("Cache-Control", "max-age=" + this.cacheDurationInSeconds);
  }
}
