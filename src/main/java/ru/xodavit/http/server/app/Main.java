package ru.xodavit.http.server.app;

import ru.xodavit.http.server.framework.HttpServer;
import ru.xodavit.http.server.framework.resolver.argument.RequestHandlerMethodArgumentResolver;
import ru.xodavit.http.server.framework.resolver.argument.RequestHeaderHandlerMethodArgumentResolver;
import ru.xodavit.http.server.framework.resolver.argument.ResponseHandlerMethodArgumentResolver;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        final var server = new HttpServer(9999);

        server.autoRegisterHandlers("org.example.http.app");
        server.addArgumentResolver(
                new RequestHandlerMethodArgumentResolver(),
                new ResponseHandlerMethodArgumentResolver(),
                new RequestHeaderHandlerMethodArgumentResolver()
        );

        new Thread(() -> {
            try {
                Thread.sleep(10000);
                server.stop();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        server.listen();
    }
}
