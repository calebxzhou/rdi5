package calebxzhou.rdi.client.mc

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
        }

        @SubscribeEvent
        @JvmStatic
        fun onConnect(e: ClientPlayerNetworkEvent.LoggingIn) {
        }
        @SubscribeEvent
        @JvmStatic
        fun onDisconnect(e: ClientPlayerNetworkEvent.LoggingOut) {
        }
        @SubscribeEvent
        @JvmStatic
        fun loadComplete(e: FMLLoadCompleteEvent) {


        }
        @SubscribeEvent
        @JvmStatic
        fun onRenderGuiPre(e: RenderGuiEvent.Pre) {
        }
        @SubscribeEvent
        @JvmStatic
        fun onScreenKeyPressed(e: ScreenEvent.KeyPressed.Pre) {

        }
        @SubscribeEvent
        @JvmStatic
        fun onKeyBind(e: RegisterKeyMappingsEvent) {

        }
        @SubscribeEvent
        @JvmStatic
        fun onClientTick(e: ClientTickEvent.Post) {
        }
        @SubscribeEvent
        @JvmStatic
        fun onChat(e: ClientChatEvent) {
            /*ChatService.sendMessage(e.message)
            e.isCanceled=true*/
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