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

enum class HostPermission {
    TEAM_MEMBER,
    ADMIN_OR_OWNER,
    OWNER_ONLY,
}

class HostGuardConfig {
    var hostIdExtractor: suspend ApplicationCall.() -> ObjectId = {
        val raw = parameters["hostId"] ?: throw ParamError("缺少hostId")
        runCatching { ObjectId(raw) }.getOrElse { throw ParamError("hostId格式错误") }
    }

    var permission: HostPermission = HostPermission.TEAM_MEMBER
}

data class HostGuardContext(
    val host: Host,
    val team: Team,
    val requester: Team.Member,
)

fun HostGuardContext.requirePermission(level: HostPermission) {
    val allowed = when (level) {
        HostPermission.TEAM_MEMBER -> true
        HostPermission.ADMIN_OR_OWNER -> requester.role.level <= Team.Role.ADMIN.level
        HostPermission.OWNER_ONLY -> requester.role == Team.Role.OWNER
    }
    if (!allowed) throw RequestError("无权限")
}

private val HostGuardKey = AttributeKey<HostGuardContext>("HostGuardContext")

val HostGuardPlugin = createRouteScopedPlugin(
    name = "HostGuard",
    createConfiguration = ::HostGuardConfig
) {
    val permission = pluginConfig.permission
    val hostResolver = pluginConfig.hostIdExtractor

    onCall { call ->
        val requesterId = call.uid
        val hostId = hostResolver(call)
        val host = HostService.getById(hostId) ?: throw RequestError("无此主机")
        val team = TeamService.get(host.teamId) ?: throw RequestError("无此团队")
        val requesterMember = team.members.firstOrNull { it.id == requesterId }
            ?: throw RequestError("你不在该团队内")

        if (host.teamId != team._id) {
            throw RequestError("主机不属于该团队")
        }

        val ctx = HostGuardContext(host, team, requesterMember)
        ctx.requirePermission(permission)

        call.attributes.put(HostGuardKey, ctx)
    }
}

fun ApplicationCall.hostGuardContext(): HostGuardContext = attributes[HostGuardKey]
