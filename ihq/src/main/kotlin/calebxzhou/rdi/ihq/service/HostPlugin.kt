package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.exception.ParamError
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.Host
import calebxzhou.rdi.ihq.model.RAccount
import calebxzhou.rdi.ihq.model.Role
import calebxzhou.rdi.ihq.net.uid
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.util.AttributeKey
import org.bson.types.ObjectId

class HostGuardConfig {
    var hostIdExtractor: suspend ApplicationCall.() -> ObjectId = {
        val raw = parameters["hostId"] ?: throw ParamError("缺少hostId")
        runCatching { ObjectId(raw) }.getOrElse { throw ParamError("hostId格式错误") }
    }

}

data class HostGuardContext(
    val host: Host,
    val requestPlayer: RAccount,
    val requestMember: Host.Member,
)
val HostGuardContext.needAdmin  get()= requireRole(Role.ADMIN)
val HostGuardContext.needOwner  get()= requireRole(Role.OWNER)
fun HostGuardContext.requireRole(level: Role): HostGuardContext {
    requestMember.let {
        val allowed = when (level) {
            Role.MEMBER -> true
            Role.ADMIN -> requestMember.role.level <= Role.ADMIN.level
            Role.OWNER -> requestMember.role == Role.OWNER
            else -> false
        }
        if (!allowed) throw RequestError("无权限")
    }
    return this
}

private val HostGuardKey = AttributeKey<HostGuardContext>("HostGuardContext")
private val HostGuardSettingsKey = AttributeKey<HostGuardSettings>("HostGuardSettings")

private data class HostGuardSettings(
    val hostResolver: suspend ApplicationCall.() -> ObjectId,
)

val HostGuardPlugin = createRouteScopedPlugin(
    name = "HostGuard",
    createConfiguration = ::HostGuardConfig
) {
    val hostResolver = pluginConfig.hostIdExtractor

    onCall { call ->
        call.attributes.put(HostGuardSettingsKey, HostGuardSettings(hostResolver))
    }
}

suspend fun ApplicationCall.hostGuardContext(): HostGuardContext {
    if (!attributes.contains(HostGuardKey)) {
        val settings = attributes[HostGuardSettingsKey]
        val ctx = resolveHostGuardContext(settings)
        attributes.put(HostGuardKey, ctx)
    }
    return attributes[HostGuardKey]
}

private suspend fun ApplicationCall.resolveHostGuardContext(settings: HostGuardSettings): HostGuardContext {
    val requesterId = uid
    val hostId = settings.hostResolver(this)
    val host = HostService.getById(hostId) ?: throw RequestError("无此主机")
    val requesterMember = host.members.firstOrNull { it.id == requesterId } ?: throw RequestError("不是主机成员")
    val player = PlayerService.getById(requesterId)?: throw RequestError("用户不存在")
    val ctx = HostGuardContext(host,player, requesterMember)
    return ctx
}
