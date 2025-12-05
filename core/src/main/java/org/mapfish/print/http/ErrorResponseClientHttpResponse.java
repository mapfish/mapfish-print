package org.mapfish.print.http;

import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nonnull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

public class ErrorResponseClientHttpResponse implements ClientHttpResponse {
  private final Exception exception;

  /** HTTP code use in response for non HTTP errors, (Not Acceptable). */
  private static final HttpStatusCode FAKE_HTTP_ERROR_CODE = HttpStatusCode.valueOf(406);

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
  public HttpStatusCode getStatusCode() throws IOException {
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
