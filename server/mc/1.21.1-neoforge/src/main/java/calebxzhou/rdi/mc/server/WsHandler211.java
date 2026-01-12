package calebxzhou.rdi.mc.server;

import calebxzhou.rdi.mc.common.WebSocketClient;
import calebxzhou.rdi.mc.common.WsMessage;
import calebxzhou.rdi.mc.common.WsMessageHandler;
import net.minecraft.server.dedicated.DedicatedServer;

/**
 * calebxzhou @ 2026-01-12 19:54
 */
public class WsHandler211 implements WsMessageHandler {
    private final DedicatedServer server;

    public WsHandler211(DedicatedServer server) {
        this.server = server;
    }

    @Override
    public void onMessage(WsMessage msg) {
        switch (msg.getChannel()){
            case Command -> {
                var cmd = msg.getData();
                var resp = server.runCommand(cmd);
                WebSocketClient.sendMessage(msg.getChannel(), resp);
            }
            default -> {}
        }
    }
}
