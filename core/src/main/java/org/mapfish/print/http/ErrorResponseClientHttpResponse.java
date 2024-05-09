package org.mapfish.print.http;

import java.io.InputStream;
import javax.annotation.Nonnull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.util.StreamUtils;

public class ErrorResponseClientHttpResponse extends AbstractClientHttpResponse {
  private final Exception exception;

  public ErrorResponseClientHttpResponse(final Exception e) {
    assert e != null;
    this.exception = e;
  }

  @Override
  @Nonnull
  public HttpHeaders getHeaders() {
    return new HttpHeaders();
  }

  @Override
  @Nonnull
  public InputStream getBody() {
    return StreamUtils.emptyInput();
  }

  @Override
  public int getRawStatusCode() {
    return 500;
  }

  @Override
  @Nonnull
  public String getStatusText() {
    return exception.getMessage();
  }

  @Override
  public void close() {}
}
