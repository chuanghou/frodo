package com.stellariver.frodo;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class JettyServer {

    private static final Logger logger = Log.getLogger(Server.class);

    static {
        CompletableFuture.runAsync(() -> {

            String rawPort = System.getProperty("frodo.port");
            Integer port = StringUtil.isBlank(rawPort) ? 24113 : Integer.parseInt(rawPort);
            Server server = new Server(port);

            server.setHandler(new AbstractHandler() {
                @Override
                public void handle(String target, Request request,
                                   HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
                    request.setHandled(true);
                    byte[] bytes = IOUtils.toByteArray(httpRequest.getInputStream());
                    Charset charset = Charset.forName(httpRequest.getCharacterEncoding());
                    CharsetDecoder decoder = charset.newDecoder();
                    ByteBuffer buf = ByteBuffer.wrap(bytes);
                    String requestBody;
                    try {
                        requestBody = decoder.decode(buf).toString();
                    } catch(CharacterCodingException e){
                        httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        logger.warn(e);
                        return;
                    }

                    try {
                        String responseBody = process(requestBody);
                        httpResponse.setContentType("application/json;charset=" + charset.name());
                        httpResponse.setCharacterEncoding(charset.name());
                        httpResponse.getWriter().write(responseBody);
                    } catch (Throwable throwable) {
                        logger.warn(throwable);
                        httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }

                }
            });

            try {
                server.start();
            } catch (Throwable throwable) {
                logger.warn(throwable);
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
