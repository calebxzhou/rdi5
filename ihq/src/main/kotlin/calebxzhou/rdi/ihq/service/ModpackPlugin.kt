package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.exception.ParamError
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.RAccount
import calebxzhou.rdi.ihq.model.pack.Modpack
import calebxzhou.rdi.ihq.net.uid
import com.mongodb.client.model.Filters.eq
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.util.AttributeKey
import kotlinx.coroutines.flow.firstOrNull
import org.bson.types.ObjectId


class ModpackGuardConfig {
    var modpackIdExtractor: suspend ApplicationCall.() -> ObjectId = {
        val raw = parameters["modpackId"] ?: throw ParamError("缺少modpackId")
        runCatching { ObjectId(raw) }.getOrElse { throw ParamError("modpackId格式错误") }
    }

}

data class ModpackGuardContext(
    val player: RAccount,
    val modpack: Modpack,
)

val ModpackGuardContext.isAuthor: Boolean
    get() = modpack.authorId == player._id

fun ModpackGuardContext.requireAuthor() {
    if (!isAuthor) throw RequestError("不是你的整合包")
}


private val ModpackGuardKey = AttributeKey<ModpackGuardContext>("ModpackGuardContext")
private val ModpackGuardSettingsKey = AttributeKey<ModpackGuardSettings>("ModpackGuardSettings")

private data class ModpackGuardSettings(
    val modpackResolver: suspend ApplicationCall.() -> ObjectId,
)

val ModpackGuardPlugin = createRouteScopedPlugin(
    name = "ModpackGuard",
    createConfiguration = ::ModpackGuardConfig
) {
    val modpackResolver = pluginConfig.modpackIdExtractor

    onCall { call ->
        call.attributes.put(
            ModpackGuardSettingsKey,
            ModpackGuardSettings( modpackResolver)
        )
    }
}

suspend fun ApplicationCall.modpackGuardContext(): ModpackGuardContext {
    if (!attributes.contains(ModpackGuardKey)) {
        val settings = attributes[ModpackGuardSettingsKey]
        val ctx = resolveModpackGuardContext(settings)
        attributes.put(ModpackGuardKey, ctx)
    }
    return attributes[ModpackGuardKey]
}

private suspend fun ApplicationCall.resolveModpackGuardContext(
    settings: ModpackGuardSettings
): ModpackGuardContext {
    val requesterId = uid
    val player = PlayerService.getById(requesterId) ?: throw RequestError("用户不存在")
    val modpackId = settings.modpackResolver(this)
    val modpack = ModpackService.dbcl.find(eq("_id", modpackId)).firstOrNull()
        ?: throw RequestError("整合包不存在")

    return ModpackGuardContext(player, modpack)
}
