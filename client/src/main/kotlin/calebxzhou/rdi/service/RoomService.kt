package calebxzhou.rdi.service

import calebxzhou.rdi.model.RBlockState
import calebxzhou.rdi.net.NetScope
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.success
import icyllis.modernui.ModernUI.props
import kotlinx.coroutines.launch
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.properties.Property

object RoomService {

    fun create() {
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
        NetScope.launch {
            val resp = RServer.now?.prepareRequest(true, "/room/create", listOf("bstates" to bstates))
            //resp.success
        }
    }
}