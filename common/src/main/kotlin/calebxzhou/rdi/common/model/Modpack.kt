package calebxzhou.rdi.common.model

import calebxzhou.mykotutils.std.displayLength
import calebxzhou.rdi.common.exception.RequestError
import calebxzhou.rdi.common.net.httpRequest
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import java.net.URI

@Serializable
class Modpack(
    @Contextual val _id: ObjectId = ObjectId(),
    val name: String,
    @Contextual
    val authorId: ObjectId,
    val iconUrl: String? = null,
    val info: String = "暂无简介",
    val modloader: ModLoader,
    val mcVer: McVersion,
    val sourceUrl: String? = null,
    val versions: List<Version> = arrayListOf(),
) {

    @Serializable
    data class Version(
        val time: Long,
        @Contextual
        val modpackId: ObjectId,
        //1.0 1.1 1.2 etc
        val name: String,
        val changelog: String,
        //构建完成状态
        val totalSize: Long? = 0L,
        val status: Status,
        val mods: MutableList<Mod> = arrayListOf(),
    ) {
    }

    @Serializable
    data class BriefVo(
        @Contextual
        val id: ObjectId = ObjectId(),
        val name: String = "未知整合包",
        @Contextual
        val authorId: ObjectId = ObjectId(),
        val authorName: String = "",
        val mcVer: McVersion = McVersion.V211,
        val modloader: ModLoader = ModLoader.neoforge,
        val modCount: Int = 0,
        val fileSize: Long = 0L,
        val icon: String? = null,
        val info: String = "暂无简介",
    )

    @Serializable
    data class DetailVo(
        @Contextual
        val _id: ObjectId,
        val name: String,
        @Contextual
        val authorId: ObjectId,
        val authorName: String = "",
        val modCount: Int,
        val sourceUrl: String? = null,
        val icon: String? = null,
        val info: String = "暂无简介",
        val modloader: ModLoader,
        val mcVer: McVersion,
        val versions: List<Version> = arrayListOf(),
    )

    @Serializable
    data class OptionsDto(
        val name: String? = null,
        val iconUrl: String? = null,
        val info: String? = null,
        val sourceUrl: String? = null
    )

    enum class Status {
        FAIL, OK, BUILDING, WAIT,
    }
}

val List<Modpack.Version>.latest get() = maxBy { it.time }

suspend fun validateModpackName(name: String): Result<Unit> {
    val trimmed = name.trim()
    val len = trimmed.displayLength
    if (len !in 3..32) throw RequestError("整合包名称长度需在3~32个字符")
    return Result.success(Unit)
}

suspend fun validateIconUrl(iconUrl: String?): Result<Unit> {
    if (iconUrl.isNullOrBlank()) return Result.success(Unit)
    val trimmed = iconUrl.trim()
    val uri = runCatching { URI(trimmed) }.getOrElse {
        throw RequestError("图标链接无效")
    }
    val scheme = uri.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") {
        throw RequestError("图标链接必须以http或https开头")
    }
    val host = uri.host?.lowercase() ?: throw RequestError("图标链接无效")
    val allowedHosts = listOf(
        "forgecdn.net",
        "curseforge.com",
        "modrinth.com",
        "xyeidc.com",
        "bbsmc.net",
        "mcmod.cn"
    )
    val allowed = allowedHosts.any { domain ->
        host == domain || host.endsWith(".$domain")
    }
    if (!allowed) {
        throw RequestError("图标链接域名不受支持")
    }

    fun isImageType(contentType: String?): Boolean {
        return contentType?.lowercase()?.startsWith("image/") == true
    }

    val contentType = runCatching {
        httpRequest {
            url(trimmed)
            method = HttpMethod.Head
            header(HttpHeaders.AcceptEncoding, "identity")
        }.contentType()?.toString()
    }.getOrNull()
        ?: runCatching {
            httpRequest {
                url(trimmed)
                method = HttpMethod.Get
                header(HttpHeaders.AcceptEncoding, "identity")
                header(HttpHeaders.Range, "bytes=0-0")
            }.contentType()?.toString()
        }.getOrNull()

    if (contentType == null) {
        throw RequestError("无法获取图标类型")
    }
    if (!isImageType(contentType)) {
        throw RequestError("图标链接不是图片")
    }
    return Result.success(Unit)
}
