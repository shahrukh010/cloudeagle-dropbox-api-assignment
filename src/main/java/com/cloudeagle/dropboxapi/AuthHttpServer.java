package com.cloudeagle.dropboxapi;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * AuthHttpServer
 * <p>
 * Starts a simple embedded HTTP server that listens for the OAuth redirect,
 * extracts the "code" query parameter, and returns it to the caller.
 * <p>
 * Usage:
 * AuthHttpServer server = new AuthHttpServer(45678, "/callback");
 * server.start();                      // start listening
 * Desktop.getDesktop().browse(new URI(authUrl)); // open browser
 * String code = server.waitForCode(120, TimeUnit.SECONDS); // wait up to timeout
 * server.stop();
 */
public class AuthHttpServer {

    private final HttpServer server;
    private final CompletableFuture<String> codeFuture = new CompletableFuture<>();
    private final int port;
    private final String path; // e.g., "/callback"

    public AuthHttpServer(int port, String path) throws IOException {
        this.port = port;
        this.path = path.startsWith("/") ? path : "/" + path;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(this.path, new CallbackHandler());
        server.setExecutor(Executors.newCachedThreadPool());
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    /**
     * Blocks until code is received or timeout occurs.
     *
     * @param timeout timeout value
     * @param unit    time unit
     * @return the authorization code
     * @throws TimeoutException     if timeout occurs
     * @throws ExecutionException   if handler failed
     * @throws InterruptedException if interrupted
     */
    public String waitForCode(long timeout, TimeUnit unit)
            throws TimeoutException, ExecutionException, InterruptedException {
        return codeFuture.get(timeout, unit);
    }

    private class CallbackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // Only handle GET
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendPlain(exchange, 405, "Method not allowed");
                    return;
                }

                URI requestUri = exchange.getRequestURI();
                String query = requestUri.getRawQuery();
                // parse query params
                Map<String, List<String>> params = splitQuery(query);
                if (params.containsKey("code")) {
                    String code = params.get("code").get(0);
                    // complete the future if not already
                    codeFuture.complete(code);

                    // reply a friendly HTML page to the browser
                    String html = "<html><body><h3>Authorization complete</h3>"
                            + "<p>You can close this window and return to the application.</p></body></html>";
                    sendHtml(exchange, 200, html);
                } else if (params.containsKey("error")) {
                    String err = params.get("error").get(0);
                    codeFuture.completeExceptionally(new RuntimeException("Authorization error: " + err));
                    sendPlain(exchange, 400, "Authorization error: " + err);
                } else {
                    sendPlain(exchange, 400, "Missing code");
                }
            } catch (Exception ex) {
                codeFuture.completeExceptionally(ex);
                sendPlain(exchange, 500, "Internal server error");
            }
        }

        private void sendPlain(HttpExchange ex, int status, String msg) throws IOException {
            byte[] bytes = msg.getBytes("UTF-8");
            ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            ex.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }

        private void sendHtml(HttpExchange ex, int status, String html) throws IOException {
            byte[] bytes = html.getBytes("UTF-8");
            ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            ex.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }

        private Map<String, List<String>> splitQuery(String query) throws UnsupportedEncodingException {
            final Map<String, List<String>> query_pairs = new java.util.HashMap<>();
            if (query == null || query.isEmpty()) return query_pairs;
            final String[] pairs = query.split("&");
            for (String pair : pairs) {
                final int idx = pair.indexOf("=");
                final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
                final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : "";
                query_pairs.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(value);
            }
            return query_pairs;
        }
    }
}
