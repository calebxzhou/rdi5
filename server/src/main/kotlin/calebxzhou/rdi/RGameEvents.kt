package calebxzhou.rdi

import calebxzhou.rdi.cmd.DebugCommand
import calebxzhou.rdi.cmd.SpeakCommand
import calebxzhou.rdi.model.RGamePayload
import calebxzhou.rdi.model.RGamePayload.Companion.handleData
import calebxzhou.rdi.util.mcComp
import calebxzhou.rdi.util.mcs
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LevelEvent
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.level.LevelEvent.CreateSpawnPosition
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler
import kotlin.math.abs

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
            //e.dispatcher.register(SpecCommand.cmd)
        }

        @SubscribeEvent
        @JvmStatic
        fun onServerStarting(e: ServerStartingEvent) {
            mcs = e.server
        }
        @SubscribeEvent
        @JvmStatic
        fun onServerStarting(e: ServerStartedEvent) {
            //设置世界边界 256x256x256
            e.server.overworld().worldBorder.size = 256.0
            e.server.overworld().height
        }
        @SubscribeEvent
        @JvmStatic
        fun onEntityJoinLevel(e: EntityJoinLevelEvent){
            calebxzhou.rdi.service.EntityTicker.onCreate(e)
        }
        @SubscribeEvent
        @JvmStatic
        fun tickStart(e: ServerTickEvent.Pre){
            RDI.tickTime1 = System.currentTimeMillis()
        }
        @SubscribeEvent
        @JvmStatic
        fun tickEnd(e: ServerTickEvent.Post){
            RDI.tickTime2 = System.currentTimeMillis()
            RDI.tickDelta = RDI.tickTime2 - RDI.tickTime1
        }
        @SubscribeEvent
        @JvmStatic
        fun registerNetworkHandler(e: RegisterPayloadHandlersEvent) {
            e.registrar("1").playBidirectional(RGamePayload.TYPE, RGamePayload.CODEC, DirectionalPayloadHandler(
                RGamePayload::handleData,
                {a,b->}))
        }

        @SubscribeEvent
        @JvmStatic
        fun skyblockSpawnPos(e: CreateSpawnPosition){
            val level = e.level
            level.setBlock(Const.BASE_POS, Blocks.BEDROCK.defaultBlockState(),3)
            e.isCanceled=true
        }
        @SubscribeEvent
        @JvmStatic
        fun playerLogin( e: PlayerEvent.PlayerLoggedInEvent){
            val player = e.entity as ServerPlayer
            val data = player.persistentData
            if(!data.getBoolean(Const.OLD_PLAYER)) {
                player.sendSystemMessage("欢迎新玩家！".mcComp)
                player.teleportTo(0.5,68.0,0.5)
                player.setRespawnPosition(player.level().dimension(), BlockPos(0,65,0), 0f, true, false)
                player.inventory.add(ItemStack(Items.OAK_SAPLING,4))
                player.inventory.add(ItemStack(Items.DIRT,4))
                data.putBoolean(Const.OLD_PLAYER,true)
            }
        }
    }


}