package org.mapfish.print.http;

import java.io.InputStream;
import jakarta.annotation.Nonnull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.util.StreamUtils;

public class ErrorResponseClientHttpResponse extends AbstractClientHttpResponse {
  private final Exception exception;

  /** HTTP code use in response for non HTTP errors, (Not Acceptable). */
  private static final int FAKE_HTTP_ERROR_CODE = 406;

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
    return FAKE_HTTP_ERROR_CODE;
  }

  @Override
  @Nonnull
  public String getStatusText() {
    return String.format(
        "Not true HTTP code, %s: %s, see above error",
        this.exception.getClass().getSimpleName(), this.exception.getMessage());
  }

  @Override
  public void close() {}
}
