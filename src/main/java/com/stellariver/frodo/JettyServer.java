package com.stellariver.frodo;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JettyServer {

    static {
        CompletableFuture.runAsync(() -> {
            Server server = new Server(8080);
            server.setHandler(new AbstractHandler() {
                @Override
                public void handle(String target, Request request,
                                   HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
                    System.out.println(target);
                    System.out.println(httpRequest);
                    System.out.println(httpResponse);
                    httpResponse.setContentType("text/html; charset=utf-8");
                    httpResponse.getWriter().println("Hello World");
                    request.setHandled(true);
                }
            });
            try {
                server.start();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            try {
                server.join();
            } catch (InterruptedException ignore) {}
        }, Executors.newSingleThreadExecutor());
    }


    public static void main(String[] args) throws IOException {
        System.in.read();
    }

}
