package ru.xodavit.http.server.framework.resolver.argument;

import ru.xodavit.http.server.framework.Request;
import ru.xodavit.http.server.framework.annotation.RequestHeader;
import ru.xodavit.http.server.framework.exception.UnsupportedParameterException;

import java.io.OutputStream;
import java.lang.reflect.Parameter;
import java.util.Optional;

public class RequestHeaderHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {
  private final Class<RequestHeader> annotation = RequestHeader.class;

  @Override
  public boolean supportsParameter(Parameter parameter) {
    return parameter.getType().isAssignableFrom(String.class) && parameter.isAnnotationPresent(annotation);
  }

  @Override
  public Object resolveArgument(Parameter parameter, Request request, OutputStream response) {
    if (!supportsParameter(parameter)) {
      // this should never happen
      throw new UnsupportedParameterException(parameter.getType().getName());
    }

    final RequestHeader annotation = parameter.getAnnotation(this.annotation);
    return Optional
        .ofNullable(
            request.getHeaders().get(annotation.value())
        ).orElseThrow(
            () -> new UnsupportedParameterException(annotation.value())
        );
  }
}
