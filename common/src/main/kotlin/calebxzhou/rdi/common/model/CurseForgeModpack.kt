package calebxzhou.rdi.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.Closeable
import java.io.File
import java.util.zip.ZipFile

/**
 * calebxzhou @ 2025-12-23 13:13
 */

data class CurseForgeModpackData(
    val manifest: CurseForgePackManifest,
    val file: File,
    val zip: ZipFile,
    val overrideEntries: List<java.util.zip.ZipEntry>,
    val overridesFolder: String
): Closeable {
    override fun close() = zip.close()
}

@Serializable
data class CurseForgePackManifest(
    var name: String,
    var version: String,
    val minecraft: Mc,
    val overrides: String? = null,
    val files: List<CurseForgePackManifest.File> = arrayListOf(),
){
    @Serializable
    data class Mc(
        val version: String,
        val modLoaders: List<ModLoader> = emptyList()
    )

    @Serializable
    data class ModLoader(
        val id: String,
        val primary: Boolean = false
    )
    @Serializable
    data class File(
        @SerialName("projectID") val projectId: Int,
        @SerialName("fileID") val fileId: Int,
        val required: Boolean = true,
    )
}



data class CurseForgeLocalResult(
    val matched: List<Mod> = emptyList(),
    val unmatched: List<File> = emptyList()
)
