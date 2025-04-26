package calebxzhou.rdi

import calebxzhou.rdi.event.BlockStateChangedEvent
import calebxzhou.rdi.event.PacketSentEvent
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.protocol.async.SPlayerMovePacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RenderFrameEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent

@EventBusSubscriber(modid = "rdi", bus = EventBusSubscriber.Bus.GAME)
object RGameEvents {
    @SubscribeEvent
    fun e1(event: BlockStateChangedEvent){
        lgr.info(event)
    }
    //todo 同步blocksState blockEntity entity
    @JvmStatic
    fun e2(event: PacketSentEvent){
        val oldPacket = event.packet
        val newPacket = when(oldPacket){
            is ServerboundMovePlayerPacket.Pos -> {
                SPlayerMovePacket.Pos(
                    oldPacket.getX(0.0).toFloat(),
                    oldPacket.getY(0.0).toFloat(),
                    oldPacket.getZ(0.0).toFloat(),
                )
            }
            is ServerboundMovePlayerPacket.Rot -> {
                SPlayerMovePacket.Rot(
                    oldPacket.getYRot(0f),
                    oldPacket.getXRot(0f),
                )
            }
            is ServerboundMovePlayerPacket.PosRot -> {
                SPlayerMovePacket(
                    oldPacket.getX(0.0).toFloat(),
                    oldPacket.getY(0.0).toFloat(),
                    oldPacket.getZ(0.0).toFloat(),
                    oldPacket.getYRot(0f),
                    oldPacket.getXRot(0f),
                )
            }
            else -> {
                TODO()
            }
        }
        RServer.now?.sendGamePacket(newPacket)

    }
    @SubscribeEvent
    fun onRenderTick(e: RenderFrameEvent.Pre) {
    }
}