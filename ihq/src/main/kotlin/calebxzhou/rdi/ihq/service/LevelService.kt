package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.model.FirmSectionData
import calebxzhou.rdi.ihq.model.GameContext
import calebxzhou.rdi.ihq.net.GameNetServer.abort
import calebxzhou.rdi.ihq.net.protocol.CBlockEntityUpdatePacket
import calebxzhou.rdi.ihq.net.protocol.CBlockStateChangePacket
import calebxzhou.rdi.ihq.net.protocol.SMeBlockEntityUpdatePacket
import calebxzhou.rdi.ihq.net.protocol.SMeBlockStateChangePacket
import calebxzhou.rdi.ihq.service.PlayerService.sendPacket
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.Updates
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

object LevelService {
    val sectionDataCol = DB.getCollection<FirmSectionData>("section")
    private const val BATCH_SIZE = 500 // Process 500 blocks at a time
    private const val BATCH_DELAY_MS = 100L // 100ms delay between batches

    init {
        runBlocking {

            // Create indices for better query performance
            sectionDataCol.createIndex(org.bson.Document("roomId", 1))
            // Sparse index for blockStates since not all documents will have all positions
            sectionDataCol.createIndex(org.bson.Document("blockStates", 1).append("sparse", true))
        }
    }

    //  同步blockstate更新到全房间.
    suspend fun syncBlockState(
        packet: SMeBlockStateChangePacket,
        ctx: GameContext
    ) {
        ctx.forEachMember { _, player ->
            player.sendPacket(
                CBlockStateChangePacket(
                    packedChunkPos = packet.packedChunkPos,
                    sectionY = packet.sectionY,
                    sectionRelativeBlockPos = packet.sectionRelativeBlockPos,
                    stateID = packet.stateID
                )
            )
        }
    }

    //  同步blockentity更新到全房间.
    suspend fun syncBlockEntity(
        packet: SMeBlockEntityUpdatePacket,
        ctx: GameContext
    ) {
        ctx.forEachMember { _, player ->
            player.sendPacket(
                CBlockEntityUpdatePacket(
                    packedChunkPos = packet.packedChunkPos,
                    sectionY = packet.sectionY,
                    sectionRelativeBlockPos = packet.sectionRelativeBlockPos,
                    data = packet.data
                )
            )
        }
    }

    suspend fun changeBlockState(
        packet: SMeBlockStateChangePacket,
        ctx: GameContext
    ) {
        if (packet.sectionRelativeBlockPos !in 0..4095) {
            ctx.net.abort("sectionRelativeBlockPos out of range: ${packet.sectionRelativeBlockPos}")
            return
        }
        //房间的持久子区块列表 不包含当前子区块 则不保存 只同步
        val dimSections = RoomService.findFirmSection(ctx.dimension, packet.packedChunkPos, packet.sectionY)
        //如果当前子区块是持久的
        if (dimSections != null) {
            //保存
            val section = sectionDataCol.find(eq("_id", dimSections.id)).firstOrNull() ?: let {
                FirmSectionData(
                    dimSections.id,
                    ctx.room._id,
                ).also {
                    //没找到 新建子区块
                    sectionDataCol.insertOne(it)
                }
            }
            //从room读取状态id的具体信息
            val bstate = ctx.room.blockStates[packet.stateID]

            // Process in batches
            val updates = mutableListOf<UpdateOneModel<FirmSectionData>>()

            // Create batch update
            updates.add(
                UpdateOneModel(
                    eq("_id", section._id),
                    Updates.set(
                        "${FirmSectionData::blockStates.name}.${packet.sectionRelativeBlockPos}",
                        bstate
                    )
                )
            )

            // If we have a full batch or this is the last update
            if (updates.size >= BATCH_SIZE) {
                sectionDataCol.bulkWrite(updates)
                updates.clear()
                kotlinx.coroutines.delay(BATCH_DELAY_MS)
            }
        }

        // Send updates to clients in batches
        ctx.forEachMember { _, player ->
            val updatePacket = CBlockStateChangePacket(
                packedChunkPos = packet.packedChunkPos,
                sectionY = packet.sectionY,
                sectionRelativeBlockPos = packet.sectionRelativeBlockPos,
                stateID = packet.stateID
            )
            player.sendPacket(updatePacket)
        }
    }

    //todo 没写完
    suspend fun changeBlockEntity(
        packet: SMeBlockEntityUpdatePacket,
        ctx: GameContext
    ) {
        if (packet.sectionRelativeBlockPos !in 0..4095) {
            ctx.net.abort("sectionRelativeBlockPos out of range: ${packet.sectionRelativeBlockPos}")
            return
        }
        //房间的持久子区块列表 不包含当前子区块 则不保存 只同步
        val dimSections = RoomService.findFirmSection(ctx.dimension, packet.packedChunkPos, packet.sectionY)
        //如果当前子区块是持久的
        if (dimSections != null) {
            //保存
            val section = sectionDataCol.find(
                and(
                    eq("_id", dimSections.id),
                )
            ).firstOrNull() ?: let {
                //没找到 新建子区块
                FirmSectionData(
                    dimSections.id,
                    ctx.room._id,
                ).also {
                    sectionDataCol.insertOne(it)
                }
            }
            sectionDataCol.updateOne(
                eq("_id", section._id), Updates.set(
                    "blockStates.${packet.sectionRelativeBlockPos}",
                    packet.data
                )
            )
        }


        // 同步
        //syncBlockState(packet, ctx)
    }
}