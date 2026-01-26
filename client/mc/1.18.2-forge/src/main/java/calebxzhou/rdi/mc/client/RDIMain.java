package calebxzhou.rdi.mc.client;

import com.google.common.net.HostAndPort;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;

import static calebxzhou.rdi.mc.common.RDI.GAME_IP;
import static calebxzhou.rdi.mc.common.RDI.HOST_NAME;

/**
 * calebxzhou @ 2026-01-06 19:19
 */
@Mod("rdi")
public class RDIMain {
    public static Button JOIN_BUTTON = new Button(100,0,200,20,new TextComponent("进入地图：" + HOST_NAME),(btn)->{
        HostAndPort hp = HostAndPort.fromString(GAME_IP);
        ConnectScreen.startConnecting(
                new TitleScreen(),
                Minecraft.getInstance(),
                new ServerAddress(hp.getHost(), hp.getPort()),
                new ServerData("rdi", GAME_IP, false)
        );
    });
    public static ResourceLocation BG_RES = ResourceLocation.fromNamespaceAndPath("rdi", "textures/bg/1.jpg");
    public RDIMain() {
        LogManager.getLogger("rdi").info("❄❄❄❄❄❄❄❄RDI客户端核心模块已加载❄❄❄❄❄❄❄❄");
    }
}
