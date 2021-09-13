package ru.xodavit.http.server.framework.exception;

public class UnsupportedParameterException extends RuntimeException {
  public UnsupportedParameterException() {
  }

  public UnsupportedParameterException(String message) {
    super(message);
  }

  public UnsupportedParameterException(String message, Throwable cause) {
    super(message, cause);
  }

  public UnsupportedParameterException(Throwable cause) {
    super(cause);
  }

  public UnsupportedParameterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
