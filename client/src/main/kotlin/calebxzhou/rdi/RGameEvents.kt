package calebxzhou.rdi

import calebxzhou.rdi.cmd.DebugCommand
import calebxzhou.rdi.event.BlockStateChangedEvent
import calebxzhou.rdi.event.PacketSentEvent
import calebxzhou.rdi.net.GameNetClient
import calebxzhou.rdi.net.protocol.SMeBlockStateChangePacket
import calebxzhou.rdi.net.protocol.SMeMovePacket
import calebxzhou.rdi.util.asInt
import calebxzhou.rdi.util.id
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.sectionIndex
import kotlinx.coroutines.launch
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderFrameEvent
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.level.LevelEvent

@EventBusSubscriber(modid = "rdi")
class RGameEvents {
    companion object {
        @JvmStatic
        fun onBlockStateChange(event: BlockStateChangedEvent) {
            ioScope.launch {
                val cpos = event.chunk.pos.asInt
                val sy = event.blockPos.y shr 4
                val bpos = event.blockPos.sectionIndex
                val sid = event.blockState.id
                GameNetClient.send(
                    SMeBlockStateChangePacket(
                        cpos, sy.toByte(), bpos.toShort(), sid
                    )
                )
            }
        }

        //todo 同步blocksState blockEntity entity
        @JvmStatic
        fun onPacketSent(event: PacketSentEvent) {
            val oldPacket = event.packet
            val newPacket = when (oldPacket) {
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
                    null
                }
            }
            newPacket?.let { GameNetClient.send(it) }

        }

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
        fun onLevelUnload(e: LevelEvent.Unload) {
        }
    }


}