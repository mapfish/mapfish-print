package org.springframework.core;

import java.io.IOException;

public class NestedIOException extends IOException {

  static {
    NestedExceptionUtils.class.getName();
  }

  public NestedIOException(final String msg) {
    super(msg);
  }

  public NestedIOException(final String msg, final Throwable cause) {
    super(msg, cause);
  }

  /**
   * @return something
   */
  @Override
  public String getMessage() {
    return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
  }
}
