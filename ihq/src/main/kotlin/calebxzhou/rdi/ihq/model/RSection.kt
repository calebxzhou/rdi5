package calebxzhou.rdi.ihq.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlin.math.ceil
import kotlin.math.log2

/**
 * calebxzhou @ 2025-04-16 23:01
 */
@Serializable
data class RBlockState(
    val name: String,
    val properties: Map<String, String> = emptyMap()
)
@Serializable
data class RSection(
    @Contextual
    val _id: ObjectId = ObjectId(),
    val chunkPos: Int,
    val sectionY: Byte,
    val palette: List<RBlockState>,
    val data: List<Long>
) {
    val chunkX = chunkPos.toShort()
    val chunkZ = (chunkPos shr 16).toShort()

    val bitsPerBlock: Int
        get() = when (palette.size) {
            1 -> 0
            in 2..16 -> 4
            else -> ceil(log2(palette.size.toFloat())).toInt()
        }

    val expectedDataSize: Int
        get() = if (palette.size == 1) 0 else 64 * bitsPerBlock
}