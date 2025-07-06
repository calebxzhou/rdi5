package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.model.GameContext
import calebxzhou.rdi.ihq.model.RAccount
import calebxzhou.rdi.ihq.model.RBlockPos
import calebxzhou.rdi.ihq.model.RBlockState
import calebxzhou.rdi.ihq.model.RSection
import calebxzhou.rdi.ihq.net.protocol.CBlockStateChangePacket
import calebxzhou.rdi.ihq.net.protocol.SPlayerBlockStateChangePacket
import calebxzhou.rdi.ihq.service.PlayerService.sendPacket
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import kotlinx.coroutines.flow.firstOrNull
import org.bson.types.ObjectId

object LevelService {
    val sectionCol = DB.getCollection<RSection>("section")

    suspend fun changeBlock(
        packet: SPlayerBlockStateChangePacket,
        ctx: GameContext
    ){
        //房间的持久子区块列表 不包含当前子区块 则不保存 只同步
        val dimSections = ctx.room.persistDimSections[ctx.dimension]
        if(dimSections==null){
            //todo 同步block更新到全房间.
            ctx.forEachMember { _,player ->
                player.sendPacket(CBlockStateChangePacket(
                    packedChunkPos = packet.packedChunkPos,
                    sectionY = packet.sectionY,
                    sectionRelativeBlockPos = packet.sectionRelativeBlockPos,
                    stateID = packet.stateID
                ))
            }
            return
        }
        //保存
        val section = sectionCol.find(
            and(
                eq("chunkPos",packet.packedChunkPos),
                eq("sectionY",packet.sectionY),
                )
        ).firstOrNull()?:let {
            //没找到 新建子区块
            RSection(
                chunkPos = packet.packedChunkPos,
                sectionY = packet.sectionY,
                blockStates = hashMapOf(),
                blockEntities = hashMapOf()
            ).also { newSection ->
                sectionCol.insertOne(newSection)
            }
        }
        section.blockStates[packet.sectionRelativeBlockPos] = packet.stateID
        //todo 保存完成 同步
        sectionCol
    }
}