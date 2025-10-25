package calebxzhou.rdi

import calebxzhou.rdi.cmd.DebugCommand
import calebxzhou.rdi.model.RGamePayload
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
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handlers.ClientPayloadHandler
import net.neoforged.neoforge.network.handlers.ServerPayloadHandler
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler

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
            e.register(RKeyBinds.MCMOD)
        }
        @SubscribeEvent
        @JvmStatic
        fun onClientTick(e: ClientTickEvent.Post) {
            if(RKeyBinds.MCMOD.consumeClick()){
                Mcmod. onKeyPressIngame()
            }
        }
        /*@SubscribeEvent
        @JvmStatic
        fun registerNetworkHandler(e: RegisterPayloadHandlersEvent) {
            e.registrar("1").playBidirectional(RGamePayload.TYPE, RGamePayload.CODEC, DirectionalPayloadHandler(
                RGamePayload::handleData,
                {a,b->}))
        }*/
    }


}