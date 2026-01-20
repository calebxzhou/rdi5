package calebxzhou.rdi.mc.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;

/**
 * calebxzhou @ 2026-01-06 19:19
 */
@Mod("rdi")
public class RDIMain {
    public static ResourceLocation BG_RES = ResourceLocation.fromNamespaceAndPath("rdi", "textures/bg/1.jpg");
    public RDIMain() {
        LogManager.getLogger("rdi").info("❄❄❄❄❄❄❄❄RDI客户端核心模块已加载❄❄❄❄❄❄❄❄");
    }
}
