package ru.xodavit.http.server;

public class Main {
    public static void main(String[] args) {
        System.out.println("hello from lib");
        Server server = new Server();
        server.start();
    }

}
class Server {
    void start() {
        System.out.println("server is started");
    }
}
