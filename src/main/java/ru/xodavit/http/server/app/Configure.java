package ru.xodavit.http.server.app;

import lombok.extern.java.Log;

import ru.xodavit.http.server.framework.Server;
import ru.xodavit.http.server.framework.resolver.argument.RequestHandlerMethodArgumentResolver;
import ru.xodavit.http.server.framework.resolver.argument.RequestHeaderHandlerMethodArgumentResolver;
import ru.xodavit.http.server.framework.resolver.argument.ResponseHandlerMethodArgumentResolver;

import java.io.IOException;

@Log
public class Configure {
    public static void main(String[] args) throws IOException {
        final var server = new Server();
        server.autoRegisterHandlers("ru.xodavit.http.server.app");
        server.addArgumentResolver(
                new RequestHandlerMethodArgumentResolver(),
                new ResponseHandlerMethodArgumentResolver(),
                new RequestHeaderHandlerMethodArgumentResolver()
        );

//        new Thread(() -> {
//            try {
//                Thread.sleep(10000);
//                server.stop();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }).start();

        server.listen(9999);
    }
}
