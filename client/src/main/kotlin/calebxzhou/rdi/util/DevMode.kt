package calebxzhou.rdi.util

import calebxzhou.rdi.model.RBlockState
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import net.minecraft.world.level.block.Block

/**
 * calebxzhou @ 2025-09-29 15:02
 */
fun Routing.devRoutes() = route("/dev"){
    get("/blockstates"){
        val bss = Block.BLOCK_STATE_REGISTRY.mapIndexed { id, bs ->
            val name = bs.blockHolder.registeredName
            val props = bs.values.map { (prop, value) ->
                prop.name to value.toString()
            }.toMap()
            RBlockState(
                name = name,
                props = props
            )
        }
        gson(bss)}
}