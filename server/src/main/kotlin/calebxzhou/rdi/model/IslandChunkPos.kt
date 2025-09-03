package calebxzhou.rdi.model

import net.minecraft.world.level.ChunkPos



data class IslandChunkPos(val data: Byte){
    constructor(x: Int, z: Int) : this(((x and 0x0F) shl 4 or (z and 0x0F)).toByte())
    companion object{
        val ChunkPos.island
            get() = IslandChunkPos(x,z)
    }
    val x: Int
        get() = (data.toInt() shr 4) and 0x0F
    val z: Int
        get() = data.toInt() and 0x0F
}
