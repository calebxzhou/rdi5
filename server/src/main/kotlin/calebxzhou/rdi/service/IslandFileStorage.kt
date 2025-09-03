package calebxzhou.rdi.service

import calebxzhou.rdi.model.IslandChunkPos.Companion.island
import net.minecraft.FileUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StreamTagVisitor
import net.minecraft.nbt.TagParser
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.chunk.storage.RegionFileStorage
import net.minecraft.world.level.chunk.storage.RegionStorageInfo
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.io.path.writeText

class IslandFileStorage(val info: RegionStorageInfo,val folder: Path,val sync: Boolean) : RegionFileStorage(info, folder, sync){
    override fun read(chunkPos: ChunkPos): CompoundTag? {
        if(chunkPos.x !in -8..7 || chunkPos.z !in -8..7) return null
        FileUtil.createDirectoriesSafe(this.folder)
        val icp = chunkPos.island
        val chunkPath = folder.resolve(icp.data.toHexString())
        val sectionPath = chunkPath.resolve("section")
        FileUtil.createDirectoriesSafe(chunkPath)
        FileUtil.createDirectoriesSafe(sectionPath)
        val metadataPath = chunkPath.resolve("metadata.snbt")
        if(!metadataPath.exists())
            return null
        val chunkData = TagParser.parseTag(metadataPath.readText())
        val sectionTag = ListTag()

        sectionPath.listDirectoryEntries().filter { it.isRegularFile() }.forEach { path ->
              TagParser.parseTag(path.readText()).also { sectionTag += it }
        }
        chunkData.put("sections",sectionTag)
        return chunkData
    }

    override fun write(chunkPos: ChunkPos, chunkData: CompoundTag?) {
        if(chunkPos.x !in -8..7 || chunkPos.z !in -8..7) return
        FileUtil.createDirectoriesSafe(this.folder)
        val icp = chunkPos.island
        val chunkPath = folder.resolve(icp.data.toHexString())
        val sectionPath = chunkPath.resolve("section")
        FileUtil.createDirectoriesSafe(chunkPath)
        FileUtil.createDirectoriesSafe(sectionPath)
        if(chunkData == null) return
        val metadataPath = chunkPath.resolve("metadata.snbt")
        val secTags = chunkData.getList("sections",10)
        secTags.forEachIndexed { index, tag ->
            val sectionTag = secTags.getCompound(index)
            val sectionIdx = sectionTag.getByte("Y")
            val sectionPath = sectionPath.resolve("$sectionIdx.snbt")
            sectionPath.writeText(tag.asString)
        }
        val metaTags = chunkData.copy()
        metaTags.remove("sections")
        metadataPath.writeText(metaTags.asString)
    }

    override fun flush() {

    }

    override fun close() {

    }

    override fun scanChunk(chunkPos: ChunkPos, visitor: StreamTagVisitor) {

    }
}