package calebxzhou.rdi.model

import kotlinx.serialization.Contextual
import net.minecraft.nbt.CompoundTag
import org.bson.types.ObjectId

/**
 * calebxzhou @ 2025-04-16 23:01
 */
data class FirmSectionData(
    @Contextual
    val _id: ObjectId = ObjectId(),
    @Contextual
    val roomId: ObjectId,
    val blockStates: List<RBlockState> = List(4096) { RBlockState("minecraft:air") },
    val blockEntities: List<CompoundTag?> = List(4096) { null },
) {


}