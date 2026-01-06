package calebxzhou.rdi.mc.server;

import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * calebxzhou @ 2026-01-06 13:41
 */
@Mod("rdi")
@Mod.EventBusSubscriber(modid = "rdi")
public class RDIMain {
    private static final Logger lgr = LogManager.getLogger("rdi");
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

    public RDIMain() {

    }

    @SubscribeEvent
    public static void started(ServerStartedEvent e) {
        String hostId = System.getenv("HOST_ID");
        if (hostId == null) {
            hostId = System.getProperty("rdi.host.id");
        }
        if (hostId == null || hostId.isBlank()) {
            lgr.warn("No HOST_ID provided – stopping");
            e.getServer().stopServer();
            return;
        }

        String ihqUrl = System.getProperty("rdi.ihq.url");
        if (ihqUrl == null) {
            ihqUrl = "host.docker.internal:65231";
        }

        // Convert http/https → ws/wss and append the path
        wsUrl = "ws://" + ihqUrl + "/host/play/" + hostId;

        shuttingDown = false;
        attemptConnect();
    }

    @SubscribeEvent
    public static void starting(ServerStartingEvent e) {
        DedicatedServer server = (DedicatedServer) e.getServer();

        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                String gameRuleEnv = System.getenv("GAME_RULE_" + key.getId());

                if (gameRuleEnv != null) {
                    GameRules.Value<?> rule = server.getGameRules().getRule(key);

                    if (rule instanceof GameRules.BooleanValue booleanValue) {
                        booleanValue.set(Boolean.parseBoolean(gameRuleEnv), server);
                        lgr.info("SET GAME RULE {}={}  B", key, gameRuleEnv);
                    } else if (rule instanceof GameRules.IntegerValue integerValue) {
                        integerValue.set(Integer.parseInt(gameRuleEnv), server);
                        lgr.info("SET GAME RULE {}={}  I", key, gameRuleEnv);
                    }
                }
            }
        });
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent e) {
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
        reconnectExecutor.schedule(RDIMain::attemptConnect, 5, TimeUnit.SECONDS);
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

}
