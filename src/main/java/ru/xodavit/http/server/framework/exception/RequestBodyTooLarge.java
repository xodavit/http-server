package ru.xodavit.http.server.framework.exception;

public class RequestBodyTooLarge extends RuntimeException {
  public RequestBodyTooLarge() {
  }

  public RequestBodyTooLarge(String message) {
    super(message);
  }

  public RequestBodyTooLarge(String message, Throwable cause) {
    super(message, cause);
  }

  public RequestBodyTooLarge(Throwable cause) {
    super(cause);
  }

  public RequestBodyTooLarge(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
