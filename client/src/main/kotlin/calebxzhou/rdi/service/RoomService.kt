package calebxzhou.rdi.service

import calebxzhou.rdi.model.RBlockState
import calebxzhou.rdi.model.RServer
import io.ktor.client.statement.*
import net.minecraft.world.level.block.Block

object RoomService {

    suspend fun create(): HttpResponse? {
        //准备所有的方块状态
        val bstates = Block.BLOCK_STATE_REGISTRY.mapIndexed { id, bs ->
            val name = bs.blockHolder.registeredName
            val props = bs.values.map { (prop, value) ->
                prop.name to value.toString()
            }.toMap()
            RBlockState(
                name = name,
                props = props
            )
        }
        val resp = RServer.now?.prepareRequest(true, "room/create", listOf("bstates" to bstates))
            return resp
    }
}