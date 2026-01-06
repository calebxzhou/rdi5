package calebxzhou.rdi.mc.common;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static calebxzhou.rdi.mc.common.RDI.HOST_ID;
import static calebxzhou.rdi.mc.common.RDI.IHQ_URL;


/**
 * calebxzhou @ 2026-01-06 23:36
 */
public class WebSocketClient {
    private static final Logger lgr = LoggerFactory.getLogger("rdi-ws-client");
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rdi-ws-reconnect");
        t.setDaemon(true);
        return t;
    });

    private static volatile WebSocket currentWebSocket;
    private static volatile boolean shuttingDown = false;
    private static volatile String wsUrl;
    public static void start(){
// Convert http/https → ws/wss and append the path
        wsUrl = "ws://" + IHQ_URL + "/host/play/" + HOST_ID;
        shuttingDown = false;
        attemptConnect();
    }
    private WebSocketClient(){

    }

    private static void attemptConnect() {
        if (wsUrl == null || shuttingDown) {
            return;
        }

        lgr.info("Attempting WebSocket connection to {}", wsUrl);

        httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(URI.create(wsUrl), new Listener())
                .whenComplete((ws, throwable) -> {
                    if (throwable != null) {
                        lgr.error("Failed to connect WebSocket to {}", wsUrl, throwable);
                        scheduleReconnect();
                    } else {
                        currentWebSocket = ws;
                        lgr.info("WebSocket connection initiated successfully");
                    }
                });
    }

    private static void scheduleReconnect() {
        if (shuttingDown) {
            return;
        }
        reconnectExecutor.schedule(WebSocketClient::attemptConnect, 5, TimeUnit.SECONDS);
        lgr.info("Scheduled WebSocket reconnect in 5s");
    }

    private static class Listener implements WebSocket.Listener {
        @Override
        public void onOpen(WebSocket webSocket) {
            lgr.info("WebSocket connection opened: {}", wsUrl);
            webSocket.request(1);
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            lgr.info("Received text message: {}", data);
            webSocket.request(1);
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            lgr.info("Received binary message (length={})", data.remaining());
            webSocket.request(1);
            return WebSocket.Listener.super.onBinary(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            lgr.debug("Received ping");
            webSocket.request(1);
            return WebSocket.Listener.super.onPing(webSocket, message);
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            lgr.debug("Received pong");
            webSocket.request(1);
            return WebSocket.Listener.super.onPong(webSocket, message);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            lgr.info("WebSocket closed: {} – {}", statusCode, reason);
            currentWebSocket = null;
            scheduleReconnect();
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            lgr.error("WebSocket error", error);
            currentWebSocket = null;
            scheduleReconnect();
            WebSocket.Listener.super.onError(webSocket, error);
        }
    }

    public static void stop(){
        shuttingDown = true;

        WebSocket ws = currentWebSocket;
        if (ws != null) {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "server stopping");
            } catch (Exception ex) {
                lgr.warn("Failed to send WebSocket close frame", ex);
            }
        }

        reconnectExecutor.shutdownNow();
    }
}
