package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.model.RAccount
import calebxzhou.rdi.ihq.model.RBlockPos
import calebxzhou.rdi.ihq.model.RBlockState
import calebxzhou.rdi.ihq.model.RSection
import org.bson.types.ObjectId

object LevelService {
    val sectionCol = DB.getCollection<RSection>("section")
    fun placeBlock(
        uid: ObjectId,
        chunkPos: Int,
        sectionY: Int,
        pos: RBlockPos,
        state: RBlockState
    ){

    }
    fun destroyBlock(
        uid: ObjectId,
        chunkPos: Int,
        sectionY: Int,
        pos: RBlockPos,
    ){

    }
    fun changeBlock(
        uid: ObjectId,
        chunkPos: Int,
        sectionY: Int,
        pos: RBlockPos,
        state: RBlockState
    ){

    }
}