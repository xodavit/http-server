package ru.xodavit.http.server.framework;

import io.github.classgraph.ClassGraph;
import lombok.extern.java.Log;
import ru.xodavit.http.server.framework.annotation.RequestMapping;
import ru.xodavit.http.server.framework.exception.*;
import ru.xodavit.http.server.framework.guava.Bytes;
import ru.xodavit.http.server.framework.resolver.argument.HandlerMethodArgumentResolver;

import javax.net.ServerSocketFactory;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Log
public class HttpServer {

    private static final byte[] CRLF = new byte[]{'\r', '\n'};
    private static final byte[] CRLFCRLF = new byte[]{'\r', '\n', '\r', '\n'};
    private final static int headersLimit = 4096;
    private final static long bodyLimit = 10 * 1024 * 1024;

    private final ServerSocket serverSocket;
    private final ExecutorService executorService;
    // state -> NOT_STARTED, STARTED, STOP, STOPPED
    private volatile boolean isStopped = false;

    // GET, "/search", handler
    private final Map<String, Map<String, HandlerMethod>> routes = new HashMap<>();
    // 404 Not Found ->
    // 500 Internal Server error ->
    private final Handler notFoundHandler = (request, response) -> {
        // language=JSON
        final var body = "{\"status\": \"error\"}";
        try {
            response.write(
                    (
                            // language=HTTP
                            "HTTP/1.1 404 Not Found\r\n" +
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
    };
    private final Handler internalErrorHandler = (request, response) -> {
        // language=JSON
        final var body = "{\"status\": \"error\"}";
        try {
            response.write(
                    (
                            // language=HTTP
                            "HTTP/1.1 500 Internal Server Error\r\n" +
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
    };
    private final List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();


    public HttpServer(ServerSocketFactory socketFactory, int port) throws IOException {
        this.serverSocket = socketFactory.createServerSocket(port);
        //this.executorService = Executors.newFixedThreadPool(64);
        this.executorService = Executors.newFixedThreadPool(64, r -> {
            final var thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
    }
    public HttpServer(int port) throws IOException {
        this(ServerSocketFactory.getDefault(), port);
    }
//    public HttpServer() {
//    }


    public void listen() {
        log.log(Level.INFO, "server started at port: " + serverSocket.getLocalPort());

        try {
            while (!isStopped && !Thread.currentThread().isInterrupted()) {
                final var socket = serverSocket.accept();
                executorService.submit(() -> handle(socket));
            }
        } catch (IOException e) {
            throw new ServerException(e);
        }
    }

    public void stop() {
        log.log(Level.INFO, "server stop..");
        this.isStopped = true;
        executorService.shutdownNow();
        log.info("executorService closes...");
        var terminated = executorService.shutdownNow().size();
        log.info(String.format("%s connections interrupted.", terminated));
        try {
            log.info("socket closes...");
            serverSocket.close();
        } catch (IOException e) {
            log.severe(e.getMessage());
        }
        log.info("Server has been stopped :)");
    }

    public void handle(final Socket socket) {
        try (
                socket;
                final var in = new BufferedInputStream(socket.getInputStream());
                final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            log.log(Level.INFO, "connected: " + socket.getPort());
            final var buffer = new byte[headersLimit];
            in.mark(headersLimit);

            final var read = in.read(buffer);

            try {
                final var requestLineEndIndex = Bytes.indexOf(buffer, CRLF, 0, read) + CRLF.length;
                if (requestLineEndIndex == 1) {
                    throw new MalformedRequestException("request line end not found");
                }

                final var requestLineParts = new String(buffer, 0, requestLineEndIndex).trim().split(" ");
                if (requestLineParts.length != 3) {
                    throw new MalformedRequestException("request line must contains 3 parts");
                }

                final var method = requestLineParts[0];
                // TODO: uri split ? -> URLDecoder
                final var uri = requestLineParts[1];

                final var headersEndIndex = Bytes.indexOf(buffer, CRLFCRLF, requestLineEndIndex, read) + CRLFCRLF.length;
                if (headersEndIndex == 3) {
                    throw new MalformedRequestException("headers too big");
                }

                var lastIndex = requestLineEndIndex;
                final var headers = new HashMap<String, String>();
                while (lastIndex < headersEndIndex - CRLF.length) {
                    final var headerEndIndex = Bytes.indexOf(buffer, CRLF, lastIndex, headersEndIndex) + CRLF.length;
                    if (headerEndIndex == 1) {
                        throw new MalformedRequestException("can't find header end index");
                    }
                    final var header = new String(buffer, lastIndex, headerEndIndex - lastIndex);
                    final var headerParts = Arrays.stream(header.split(":", 2))
                            .map(String::trim)
                            .collect(Collectors.toList());

                    if (headerParts.size() != 2) {
                        throw new MalformedRequestException("Invalid header: " + header);
                    }

                    headers.put(headerParts.get(0), headerParts.get(1));
                    lastIndex = headerEndIndex;
                }

                final var contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));

                if (contentLength > bodyLimit) {
                    throw new RequestBodyTooLarge();
                }

                in.reset();
                in.skipNBytes(headersEndIndex);
                final var body = in.readNBytes(contentLength);

                // TODO: annotation monkey
                final var request = Request.builder()
                        .method(method)
                        .path(uri)
                        .headers(headers)
                        .body(body)
                        .build();

                final var response = out;

                final var handlerMethod = Optional.ofNullable(routes.get(request.getMethod()))
                        .map(o -> o.get(request.getPath()))
                        .orElse(new HandlerMethod(notFoundHandler, notFoundHandler.getClass().getMethod("handle", Request.class, OutputStream.class)));

                try {
                    final var invokableMethod = handlerMethod.getMethod();
                    final var invokableHandler = handlerMethod.getHandler();

                    final var arguments = new ArrayList<>(invokableMethod.getParameterCount());
                    for (final var parameter : invokableMethod.getParameters()) {
                        var resolved = false;
                        for (final var argumentResolver : argumentResolvers) {
                            if (!argumentResolver.supportsParameter(parameter)) {
                                continue;
                            }

                            final var argument = argumentResolver.resolveArgument(parameter, request, response);
                            arguments.add(argument);
                            resolved = true;
                            break;
                        }
                        if (!resolved) {
                            throw new UnsupportedParameterException(parameter.getType().getName());
                        }
                    }

                    invokableMethod.invoke(invokableHandler, arguments.toArray());
                } catch (Exception e) {
                    internalErrorHandler.handle(request, response);
                }
            } catch (MalformedRequestException e) {
                // language=HTML
                final var html = "<h1>Mailformed request</h1>";
                out.write(
                        (
                                // language=HTTP
                                "HTTP/1.1 400 Bad Request\r\n" +
                                        "Server: nginx\r\n" +
                                        "Content-Length: " + html.length() + "\r\n" +
                                        "Content-Type: text/html; charset=UTF-8\r\n" +
                                        "Connection: close\r\n" +
                                        "\r\n" +
                                        html
                        ).getBytes(StandardCharsets.UTF_8)
                );
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                // TODO:
            }
        } catch (IOException e) {
            e.printStackTrace();
            // TODO:
        }
    }



    public void get(String path, Handler handler) {
        registerHandler(HttpMethods.GET, path, handler);
    }

    public void post(String path, Handler handler) {
        registerHandler(HttpMethods.POST, path, handler);
    }

    public void autoRegisterHandlers(String pkg) {
        try (final var scanResult = new ClassGraph().enableAllInfo().acceptPackages(pkg).scan()) {
            for (final var classInfo : scanResult.getClassesWithMethodAnnotation(RequestMapping.class.getName())) {
                final var handler = classInfo.loadClass().getConstructor().newInstance();
                for (final var method : handler.getClass().getMethods()) {
                    if (method.isAnnotationPresent(RequestMapping.class)) {
                        final RequestMapping mapping = method.getAnnotation(RequestMapping.class);

                        final var handlerMethod = new HandlerMethod(handler, method);
                        Optional.ofNullable(routes.get(mapping.method()))
                                .ifPresentOrElse(
                                        map -> map.put(mapping.path(), handlerMethod),
                                        () -> routes.put(mapping.method(), new HashMap<>(Map.of(mapping.path(), handlerMethod)))
                                );
                    }
                }
            }
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public void registerHandler(String method, String path, Handler handler) {
        try {
            final var handle = handler.getClass().getMethod("handle", Request.class, OutputStream.class);
            final var handlerMethod = new HandlerMethod(handler, handle);
            Optional.ofNullable(routes.get(method))
                    .ifPresentOrElse(
                            map -> map.put(path, handlerMethod),
                            () -> routes.put(method, new HashMap<>(Map.of(path, handlerMethod)))
                    );
        } catch (NoSuchMethodException e) {
            throw new HandlerRegistrationException(e);
        }
//    final var map = routes.get(method);
//    if (map != null) {
//      map.put(path, handler);
//      return;
//    }
//    routes.put(method, new HashMap<>(Map.of(path, handler)));
    }

    public void addArgumentResolver(HandlerMethodArgumentResolver... resolvers) {
        argumentResolvers.addAll(List.of(resolvers));
    }
}

