package ru.xodavit.http.server.app.handler;

import ru.xodavit.http.server.framework.HttpMethods;
import ru.xodavit.http.server.framework.annotation.RequestHeader;
import ru.xodavit.http.server.framework.annotation.RequestMapping;
import ru.xodavit.http.server.framework.exception.RequestHandleException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class SimpleHandler {
  @RequestMapping(method = HttpMethods.GET, path = "/courses")
  public void getCourses(
      OutputStream response,
      @RequestHeader("Username") String username
  ) {
    final var body = "{\"courses\": []}";
    try {
      response.write(
          (
              // language=HTTP
              "HTTP/1.1 200 OK\r\n" +
                  "Content-Length: " + body.length() + "\r\n" +
                  "Content-Type: application/json\r\n" +
                  "Connection: close\r\n" +
                  "\r\n" +
                  body
          ).getBytes(StandardCharsets.UTF_8)
      );
    } catch (IOException e) {
      throw new RequestHandleException(e);
    }
  }
}
