package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.exception.ParamError
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.Host
import calebxzhou.rdi.ihq.model.Team
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

    var permission: TeamPermission = TeamPermission.TEAM_MEMBER
}

data class HostGuardContext(
    val host: Host,
    val team: Team,
    val requester: Team.Member,
)

fun HostGuardContext.requirePermission(level: TeamPermission) {
    val allowed = when (level) {
        TeamPermission.TEAM_MEMBER -> true
        TeamPermission.ADMIN_OR_OWNER -> requester.role.level <= Team.Role.ADMIN.level
        TeamPermission.OWNER_ONLY -> requester.role == Team.Role.OWNER
        TeamPermission.ADMIN_KICK_MEMBER -> requester.role.level <= Team.Role.ADMIN.level
    }
    if (!allowed) throw RequestError("无权限")
}

private val HostGuardKey = AttributeKey<HostGuardContext>("HostGuardContext")
private val HostGuardSettingsKey = AttributeKey<HostGuardSettings>("HostGuardSettings")

private data class HostGuardSettings(
    val permission: TeamPermission,
    val hostResolver: suspend ApplicationCall.() -> ObjectId,
)

val HostGuardPlugin = createRouteScopedPlugin(
    name = "HostGuard",
    createConfiguration = ::HostGuardConfig
) {
    val permission = pluginConfig.permission
    val hostResolver = pluginConfig.hostIdExtractor

    onCall { call ->
        call.attributes.put(HostGuardSettingsKey, HostGuardSettings(permission, hostResolver))
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
    val team = TeamService.get(host.teamId) ?: throw RequestError("无此团队")
    val requesterMember = team.members.firstOrNull { it.id == requesterId }
        ?: throw RequestError("你不在该团队内")

    if (host.teamId != team._id) {
        throw RequestError("主机不属于该团队")
    }

    val ctx = HostGuardContext(host, team, requesterMember)
    ctx.requirePermission(settings.permission)
    return ctx
}
