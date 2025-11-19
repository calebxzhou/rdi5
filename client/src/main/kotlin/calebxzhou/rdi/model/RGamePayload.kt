package calebxzhou.rdi.model

import calebxzhou.rdi.util.rdiAsset
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.handling.IPayloadContext

class RGamePayload(
    val data: ByteArray
): CustomPacketPayload {
    companion object{
        val CODEC = StreamCodec.composite(ByteBufCodecs.BYTE_ARRAY, RGamePayload::data, ::RGamePayload)
        val TYPE = CustomPacketPayload.Type<RGamePayload>(rdiAsset("game_payload"))
        fun handleData(data: RGamePayload, ctx: IPayloadContext){

        }
    }
    override fun type() = TYPE
    fun send() = PacketDistributor.sendToServer(this)
}