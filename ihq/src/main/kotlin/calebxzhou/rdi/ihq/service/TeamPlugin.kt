package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.Team
import calebxzhou.rdi.ihq.net.uid
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.util.AttributeKey
import org.bson.types.ObjectId

/**
 * Route plugin that ensures the authenticated player has sufficient privileges on the current team.
 * Attach to a route with the required [TeamPermission], optionally provide team/target resolvers,
 * then access the resolved [TeamGuardContext] via [teamGuardContext].
 */
enum class TeamPermission {
	OWNER_ONLY,
	ADMIN_OR_OWNER,
	ADMIN_KICK_MEMBER,
}

class TeamGuardConfig {
	/** Resolve the team id involved in the request. Default: player's joined team. */
	var teamIdExtractor: suspend ApplicationCall.() -> ObjectId? = { null }

	/** Resolve an optional target member id (for kick/transfer/etc.). */
	var targetIdExtractor: suspend ApplicationCall.() -> ObjectId? = { null }

	/** Permission rule to enforce. */
	var permission: TeamPermission = TeamPermission.ADMIN_OR_OWNER

	/** Whether the resolved target must exist in the team. */
	var requireTargetMember: Boolean = false
}

data class TeamGuardContext(
	val team: Team,
	val requester: Team.Member,
	val target: Team.Member?,
)

private val TeamGuardKey = AttributeKey<TeamGuardContext>("TeamGuardContext")

val TeamGuardPlugin = createRouteScopedPlugin(
	name = "TeamGuard",
	createConfiguration = ::TeamGuardConfig
) {
	val permission = pluginConfig.permission
	val teamResolver = pluginConfig.teamIdExtractor
	val targetResolver = pluginConfig.targetIdExtractor
	val requireTarget = pluginConfig.requireTargetMember

	onCall { call ->
		val requesterId = call.uid
        val team = when (val explicitTeamId = teamResolver(call)) {
			null -> TeamService.getJoinedTeam(requesterId)
			else -> TeamService.get(explicitTeamId)
		} ?: throw RequestError("无团队")

		val requesterMember = team.members.firstOrNull { it.id == requesterId }
			?: throw RequestError("你不在团队内")

		val targetId = targetResolver(call)
		val targetMember = targetId?.let { id ->
			team.members.firstOrNull { it.id == id }
		}

		if (requireTarget && targetId != null && targetMember == null) {
			throw RequestError("对方不在团队内")
		}

		val hasPermission = when (permission) {
			TeamPermission.OWNER_ONLY -> requesterMember.role == Team.Role.OWNER

			TeamPermission.ADMIN_OR_OWNER ->
				requesterMember.role.level <= Team.Role.ADMIN.level

			TeamPermission.ADMIN_KICK_MEMBER ->
				when (requesterMember.role) {
					Team.Role.OWNER -> true
					Team.Role.ADMIN -> targetMember?.role?.level?.let { it > Team.Role.ADMIN.level } ?: false
					else -> false
				}
		}

		if (!hasPermission) {
			throw RequestError("无权限")
		}

		call.attributes.put(TeamGuardKey, TeamGuardContext(team, requesterMember, targetMember))
	}
}

fun ApplicationCall.teamGuardContext(): TeamGuardContext = attributes[TeamGuardKey]
