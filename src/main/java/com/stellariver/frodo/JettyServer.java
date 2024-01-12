package com.stellariver.frodo;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class JettyServer {

    private static final Logger logger = Log.getLogger(Server.class);

    static {
        CompletableFuture.runAsync(() -> {
            Server server = new Server(8080);
            server.setHandler(new AbstractHandler() {
                @Override
                public void handle(String target, Request request,
                                   HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {

                    byte[] bytes = IOUtils.toByteArray(httpRequest.getInputStream());
                    if (bytes.length == 0) {
                        httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }

                    CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
                    ByteBuffer buf = ByteBuffer.wrap(bytes);
                    String requestBody;
                    try {
                        requestBody = decoder.decode(buf).toString();
                    } catch(CharacterCodingException e){
                        httpResponse.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
                        logger.warn(e);
                        return;
                    }

                    try {
                        String responseBody = process(requestBody);
                        httpResponse.getWriter().write(responseBody);
                    } catch (Throwable throwable) {
                        logger.warn(throwable);
                        httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }

                    httpResponse.setContentType("application/json;charset=UTF-8");
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


    static public String process(String request) {
        return request + request;
    }



    public static void main(String[] args) throws IOException {
        System.in.read();
    }

}
