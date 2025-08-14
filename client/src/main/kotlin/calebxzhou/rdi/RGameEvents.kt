package calebxzhou.rdi

import calebxzhou.rdi.cmd.DebugCommand
import calebxzhou.rdi.service.EnglishStorage
import calebxzhou.rdi.service.Mcmod
import calebxzhou.rdi.service.RGuiHud
import calebxzhou.rdi.service.RKeyBinds
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.sendCommand
import com.mojang.blaze3d.platform.InputConstants
import icyllis.modernui.mc.BlurHandler
import net.minecraft.client.resources.language.ClientLanguage
import net.minecraft.util.HttpUtil
import net.minecraft.world.level.GameType
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent
import net.neoforged.neoforge.client.event.*
import net.neoforged.neoforge.event.RegisterCommandsEvent

@EventBusSubscriber(modid = "rdi")
class RGameEvents {
    companion object {


        @SubscribeEvent
        @JvmStatic
        fun onRenderTick(e: RenderFrameEvent.Pre) {
        }

        @SubscribeEvent
        @JvmStatic
        fun onCommandRegister(e: RegisterCommandsEvent) {
            if (Const.DEBUG) {
                lgr.info("注册调试命令")
                e.dispatcher.register(DebugCommand.cmd)
            }
        }

        @SubscribeEvent
        @JvmStatic
        fun onLocalServerStart(e: ClientPlayerNetworkEvent.LoggingIn) {
            //启动局域网

            mc.singleplayerServer?.publishServer(GameType.SURVIVAL, Const.DEBUG, HttpUtil.getAvailablePort())
        }
        @SubscribeEvent
        @JvmStatic
        fun loadComplete(e: FMLLoadCompleteEvent) {

            RDI.Companion.modIdChineseName += "tfc" to "群峦传说"
            RDI.Companion.modIdChineseName += "firmalife" to "群峦人生"
            RDI.Companion.modIdChineseName += "ae2" to "应用能源2"
            RDI.Companion.modIdChineseName += "create" to "机械动力"
            RDI.Companion.modIdChineseName += "create_connected" to "机械动力·创意传动"
            RDI.Companion.modIdChineseName += "vinery" to "葡园酒香"
            RDI.Companion.modIdChineseName += "aether" to "天境"
            RDI.Companion.modIdChineseName += "farmersdelight" to "农夫乐事"
            RDI.Companion.modIdChineseName += "chefsdelight" to "厨师乐事"
            RDI.Companion.modIdChineseName += "aethersdelight" to "天境乐事"
            RDI.Companion.modIdChineseName += "oceansdelight" to "海洋乐事"
            RDI.Companion.modIdChineseName += "cuisinedelight" to "料理乐事"
            RDI.Companion.modIdChineseName += "computercraft" to "电脑"
            RDI.Companion.modIdChineseName += "minecraft" to "原版"

            EnglishStorage.lang = ClientLanguage.loadFrom(mc.resourceManager, listOf("en_us"), false)
            //强制关闭模糊
            BlurHandler.sBlurEffect=false
        }
        @SubscribeEvent
        @JvmStatic
        fun onRenderGuiPre(e: RenderGuiEvent.Pre) {
            RGuiHud.onRender(e.guiGraphics)
        }
        @SubscribeEvent
        @JvmStatic
        fun onScreenKeyPressed(e: ScreenEvent.KeyPressed.Pre) {
            if (RKeyBinds.MCMOD.isActiveAndMatches(InputConstants.getKey(e.keyCode, e.scanCode))) {
                Mcmod.onKeyPressGui()
            }
        }
        @SubscribeEvent
        @JvmStatic
        fun onKeyBind(e: RegisterKeyMappingsEvent) {
            e.register(RKeyBinds.HOME)
            e.register(RKeyBinds.MCMOD)
        }
        @SubscribeEvent
        @JvmStatic
        fun onClientTick(e: ClientTickEvent.Post) {
            if (RKeyBinds.HOME.consumeClick()) {
                mc.sendCommand("ftbteambases home")
            }else if(RKeyBinds.MCMOD.consumeClick()){
                Mcmod. onKeyPressIngame()
            }
        }
    }


}