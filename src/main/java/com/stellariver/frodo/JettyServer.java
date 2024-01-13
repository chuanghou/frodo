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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JettyServer {

    private static final Logger logger = Log.getLogger(Server.class);

    private static final JdkCompiler jdkCompiler = new JdkCompiler();

    static private final Pattern scriptPattern = Pattern.compile("public\\s+class\\s+[a-zA-Z0-9_]+\\s+implements Callable<String>\\s*\\{");

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
                    String rawCharset = httpRequest.getCharacterEncoding();
                    Charset charset = StringUtil.isBlank(rawCharset) ? StandardCharsets.UTF_8 : Charset.forName(rawCharset);
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

                    httpResponse.setContentType("application/json;charset=" + charset.name());
                    httpResponse.setCharacterEncoding(charset.name());
                    httpResponse.getWriter().write(process(requestBody));

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

    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
    static public String process(String request) {

        Matcher matcher = scriptPattern.matcher(request);

        if (matcher.find()) {
            matcher.group();
        } else {
            return "could not find class implements Callable<String> in \n\n" + wrapper(request);
        }

        if (matcher.find()) {
            return "find too many public class in \n\n" + wrapper(request);
        }

        try {
            Class<?> clazz = jdkCompiler.compile(request);
            Class<Callable<String>> callableClass = (Class<Callable<String>>) clazz;
            Callable<String> callable = callableClass.newInstance();
            return callable.call();
        } catch (Throwable throwable) {
            return printable(throwable);
        }

    }

    static private String wrapper(String code) {
        return "\\*------------------------------------------------------------------------*\\\n\n"
                + code
                + "\n\n\\*------------------------------------------------------------------------*\\\n";
    }

    static private String printable(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

}
