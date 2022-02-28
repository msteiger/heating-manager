package sma.service;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebServer {

    private final HttpHandler ROUTES = new RoutingHandler()
            .get("/", this::handleRootRequest)
            .get("/status", this::handleStatusRequest)
            .setFallbackHandler(this::handleNotFound);

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    private Supplier<Map<String, Object>> supplier;

    private Undertow server;


    public WebServer(Supplier<Map<String, Object>> dataProvider) {
        server = Undertow.builder()
                .addHttpListener(7474, "0.0.0.0")
                .setHandler(ROUTES)
                .build();
        this.supplier = dataProvider;
    }

    public void start() {
        server.start();
    }

    public void handleRootRequest(HttpServerExchange exchange) throws Exception {
        sendResponse(exchange, 200, Map.of("entries", Arrays.asList("status")));
    }

    public void handleStatusRequest(HttpServerExchange exchange) throws Exception {
        sendResponse(exchange, 200, supplier.get());
    }

    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private int status;
        private String statusText;
        private String message;
    }

    private void handleNotFound(HttpServerExchange exchange) {
        ErrorResponse dataObj = new ErrorResponse(404, "Page not found", exchange.getRequestPath());
        sendResponse(exchange, 404, dataObj);
    }

    private void sendResponse(HttpServerExchange exchange, int status, Object dataObj) {

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json;charset=utf-8");
        exchange.setStatusCode(status);

        try {
            String value = mapper.writeValueAsString(dataObj);
            exchange.getResponseSender().send(value);
        } catch (JsonProcessingException e) {
            log.error("Could not map data", e);
        }
    }
}
