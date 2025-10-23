package calebxzhou.rdi.service

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.Team
import org.bson.types.ObjectId

/**
 * calebxzhou @ 2025-10-06 19:38
 */
fun Team.hasMember(id: ObjectId): Boolean {
    return members.any { it.id == id }
}
val Team.owner
    get() =  members.find { it.role== Team.Role.OWNER }
fun Team.isOwner(acc: RAccount) = owner?.id == acc._id
// Check whether a user is OWNER or ADMIN of this team
fun Team.isOwnerOrAdmin(acc: RAccount): Boolean = isOwnerOrAdmin(acc._id)
fun Team.isOwnerOrAdmin(uid: ObjectId): Boolean =
    members.any { it.id == uid && (it.role == Team.Role.OWNER || it.role == Team.Role.ADMIN) }