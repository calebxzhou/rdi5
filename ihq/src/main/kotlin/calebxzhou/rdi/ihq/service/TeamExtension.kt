package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.model.Host
import calebxzhou.rdi.ihq.model.Team
import calebxzhou.rdi.ihq.model.Team.Role
import calebxzhou.rdi.ihq.model.World
import org.bson.types.ObjectId

// Suspend extensions to resolve related entities for a Team
suspend fun Team.hosts(): List<Host> = HostService.listByTeam(_id)

suspend fun Team.worlds(): List<World> = WorldService.listByTeam(_id)

fun Team.hasMember(id: ObjectId): Boolean {
    return members.any { it.id == id }
}
val Team.owner
    get() =  members.find { it.role== Role.OWNER }

// Check whether a user is OWNER or ADMIN of this team
fun Team.isOwnerOrAdmin(uid: ObjectId): Boolean =
    members.any { it.id == uid && (it.role == Role.OWNER || it.role == Role.ADMIN) }
suspend fun Team.hasHost(hostId: ObjectId )= HostService.listByTeam(_id).find { it._id == hostId } != null
