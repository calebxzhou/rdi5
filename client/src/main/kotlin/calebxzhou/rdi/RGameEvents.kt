package calebxzhou.rdi

import calebxzhou.rdi.event.BlockStateChangedEvent
import calebxzhou.rdi.event.PacketSentEvent
import calebxzhou.rdi.net.GameNetClient
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.protocol.SMeMovePacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderFrameEvent

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
                SMeMovePacket.Pos(
                    oldPacket.getX(0.0).toFloat(),
                    oldPacket.getY(0.0).toFloat(),
                    oldPacket.getZ(0.0).toFloat(),
                )
            }
            is ServerboundMovePlayerPacket.Rot -> {
                SMeMovePacket.Rot(
                    oldPacket.getYRot(0f),
                    oldPacket.getXRot(0f),
                )
            }
            is ServerboundMovePlayerPacket.PosRot -> {
                SMeMovePacket.Pos(
                    oldPacket.getX(0.0).toFloat(),
                    oldPacket.getY(0.0).toFloat(),
                    oldPacket.getZ(0.0).toFloat(),
                )
            }
            else -> {
                TODO()
            }
        }
        GameNetClient.send(newPacket)

    }
    @SubscribeEvent
    fun onRenderTick(e: RenderFrameEvent.Pre) {
    }
}