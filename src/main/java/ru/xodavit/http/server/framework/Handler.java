package ru.xodavit.http.server.framework;

import java.io.OutputStream;

@FunctionalInterface
public interface Handler {
  void handle(final Request request, final OutputStream response);
}
