package calebxzhou.rdi.service

import calebxzhou.rdi.model.Host
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.Role
import org.bson.types.ObjectId

/**
 * calebxzhou @ 2025-10-06 19:38
 */
fun Host.hasMember(id: ObjectId): Boolean {
    return members.any { it.id == id }
}
val Host.owner
    get() =  members.find { it.role== Role.OWNER }
fun Host.isOwner(acc: RAccount) = owner?.id == acc._id
// Check whether a user is OWNER or ADMIN of this team
fun Host.isAdmin(acc: RAccount): Boolean = isAdmin(acc._id)
fun Host.isAdmin(uid: ObjectId): Boolean =
    members.any { it.id == uid && (it.role == Role.OWNER || it.role == Role.ADMIN) }