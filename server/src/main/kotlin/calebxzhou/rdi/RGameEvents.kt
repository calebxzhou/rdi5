package calebxzhou.rdi

import calebxzhou.rdi.cmd.DebugCommand
import calebxzhou.rdi.cmd.SpeakCommand
import calebxzhou.rdi.cmd.SpecCommand
import calebxzhou.rdi.util.mcs
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent

@EventBusSubscriber(modid = "rdi")
class RGameEvents {
    companion object {


        @SubscribeEvent
        @JvmStatic
        fun onCommandRegister(e: RegisterCommandsEvent) {
            if (Const.DEBUG) {
                lgr.info("注册调试命令")
                e.dispatcher.register(DebugCommand.cmd)
            }
            e.dispatcher.register(SpeakCommand.cmd)
            e.dispatcher.register(SpecCommand.cmd)
        }

        @SubscribeEvent
        @JvmStatic
        fun onServerStarting(e: ServerStartingEvent) {
            mcs = e.server
        }
        @SubscribeEvent
        @JvmStatic
        fun onEntityJoinLevel(e: EntityJoinLevelEvent){
            calebxzhou.rdi.service.EntityTicker.onCreate(e)
        }
    }


}