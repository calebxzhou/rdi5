package calebxzhou.rdi.mc.client;


import calebxzhou.rdi.mc.common.RDI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConnectingScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;

import static calebxzhou.rdi.mc.common.RDI.GAME_IP;

/**
 * calebxzhou @ 2026-01-06 19:19
 */
@Mod("rdi")
public class RDIMain {
    public static Button JOIN_BUTTON = new Button(100, 0, 200, 20, "进入地图：" + RDI.HOST_NAME, (btn) -> {
        Minecraft.getInstance().setScreen(
                new ConnectingScreen(
                        new MainMenuScreen(),
                        Minecraft.getInstance(),
                        new ServerData("rdi", GAME_IP, false)
                ));
    });
    public static net.minecraft.util.ResourceLocation BG_RES = new ResourceLocation("rdi", "textures/bg/1.jpg");

    public RDIMain() {
        LogManager.getLogger("rdi").info("❄❄❄❄❄❄❄❄RDI客户端核心模块已加载❄❄❄❄❄❄❄❄");
    }
}
