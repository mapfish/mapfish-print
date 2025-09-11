package org.mapfish.print.servlet;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * Filter which checks the content size of requests.
 *
 * <p>This is to avoid that the server is flooded with overly huge requests.
 *
 * <p>You can tune this filter by setting the mapfish.maxContentLength (bytes) system property
 * before starting the JVM.
 */
public class RequestSizeFilter implements Filter {

  private static final int MAX_CONTENT_LENGTH = 1048576;
  private static final Logger LOGGER = LoggerFactory.getLogger(RequestSizeFilter.class);

  /** The maximum allowed content length of the request in bytes. */
  private int maxContentLength = MAX_CONTENT_LENGTH;

  @Override
  public final void doFilter(
      final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException {
    if (request.getContentLength() > this.maxContentLength) {
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      LOGGER.error("Request size exceeds limit: {} bytes", request.getContentLength());
      httpResponse.sendError(HttpStatus.BAD_REQUEST.value(), "Request size exceeds limit");
    } else {
      chain.doFilter(request, response);
    }
  }

  @Override
  public final void init(final FilterConfig config) {
    if (System.getProperty("mapfish.maxContentLength") != null) {
      this.maxContentLength = Integer.parseInt(System.getProperty("mapfish.maxContentLength"));
    } else if (config.getInitParameter("maxContentLength") != null) {
      this.maxContentLength = Integer.parseInt(config.getInitParameter("maxContentLength"));
    }
  }

  @Override
  public void destroy() {}
}
