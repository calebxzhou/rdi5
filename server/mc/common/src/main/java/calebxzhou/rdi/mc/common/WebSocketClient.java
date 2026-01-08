package calebxzhou.rdi.mc.common;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rdi-ws-reconnect");
        t.setDaemon(true);
        return t;
    });

    private static volatile WebSocket currentWebSocket;
    private static volatile boolean shuttingDown = false;
    private static volatile String wsUrl;

    public static void start() {
        // Convert http/https to ws/wss and append the path
        wsUrl = "ws://" + IHQ_URL + "/host/play/" + HOST_ID;
        shuttingDown = false;
        attemptConnect();
    }

    private WebSocketClient() {

    }

    private static void attemptConnect() {
        if (wsUrl == null || shuttingDown) {
            return;
        }

        lgr.info("Attempting WebSocket connection to {}", wsUrl);
        try {
            WebSocket ws = new WebSocketFactory()
                    .setConnectionTimeout(CONNECT_TIMEOUT_MS)
                    .createSocket(wsUrl)
                    .addListener(new Listener());
            currentWebSocket = ws;
            ws.connectAsynchronously();
        } catch (IOException ex) {
            lgr.error("Failed to connect WebSocket to {}", wsUrl, ex);
            scheduleReconnect();
        }
    }

    private static void scheduleReconnect() {
        if (shuttingDown) {
            return;
        }
        reconnectExecutor.schedule(WebSocketClient::attemptConnect, 5, TimeUnit.SECONDS);
        lgr.info("Scheduled WebSocket reconnect in 5s");
    }

    private static class Listener extends WebSocketAdapter {
        @Override
        public void onConnected(WebSocket webSocket, Map<String, List<String>> headers) {
            lgr.info("WebSocket connection opened: {}", wsUrl);
        }

        @Override
        public void onTextMessage(WebSocket webSocket, String text) {
            lgr.info("Received text message: {}", text);
        }

        @Override
        public void onBinaryMessage(WebSocket webSocket, byte[] binary) {
            lgr.info("Received binary message (length={})", binary.length);
        }

        @Override
        public void onPingFrame(WebSocket webSocket, WebSocketFrame frame) {
            lgr.debug("Received ping");
        }

        @Override
        public void onPongFrame(WebSocket webSocket, WebSocketFrame frame) {
            lgr.debug("Received pong");
        }

        @Override
        public void onDisconnected(WebSocket webSocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
            String reason = serverCloseFrame != null ? serverCloseFrame.getCloseReason() : "unknown";
            int code = serverCloseFrame != null ? serverCloseFrame.getCloseCode() : -1;
            lgr.info("WebSocket closed: {} - {}", code, reason);
            currentWebSocket = null;
            scheduleReconnect();
        }

        @Override
        public void onConnectError(WebSocket webSocket, WebSocketException exception) {
            lgr.error("WebSocket connect error", exception);
            currentWebSocket = null;
            scheduleReconnect();
        }

        @Override
        public void onError(WebSocket webSocket, WebSocketException cause) {
            lgr.error("WebSocket error", cause);
            currentWebSocket = null;
            scheduleReconnect();
        }
    }

    public static void stop() {
        shuttingDown = true;

        WebSocket ws = currentWebSocket;
        if (ws != null) {
            try {
                ws.disconnect(1000, "server stopping");
            } catch (Exception ex) {
                lgr.warn("Failed to send WebSocket close frame", ex);
            }
        }

        reconnectExecutor.shutdownNow();
    }
}
