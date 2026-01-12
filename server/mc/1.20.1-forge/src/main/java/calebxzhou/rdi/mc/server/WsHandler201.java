package calebxzhou.rdi.mc.server;

import calebxzhou.rdi.mc.common.WebSocketClient;
import calebxzhou.rdi.mc.common.WsMessage;
import calebxzhou.rdi.mc.common.WsMessageHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;

/**
 * calebxzhou @ 2026-01-12 18:08
 */
public class WsHandler201 implements WsMessageHandler {
    private final DedicatedServer server;

    public WsHandler201(DedicatedServer server) {
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
