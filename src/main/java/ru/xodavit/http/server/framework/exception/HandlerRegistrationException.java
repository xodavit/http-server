package ru.xodavit.http.server.framework.exception;

public class HandlerRegistrationException extends RuntimeException {
  public HandlerRegistrationException() {
  }

  public HandlerRegistrationException(String message) {
    super(message);
  }

  public HandlerRegistrationException(String message, Throwable cause) {
    super(message, cause);
  }

  public HandlerRegistrationException(Throwable cause) {
    super(cause);
  }

  public HandlerRegistrationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
