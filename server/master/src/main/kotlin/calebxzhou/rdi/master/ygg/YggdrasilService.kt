package calebxzhou.rdi.master.ygg

import calebxzhou.mykotutils.std.encodeBase64
import calebxzhou.rdi.common.json
import calebxzhou.rdi.common.model.RAccount
import calebxzhou.rdi.common.util.objectId
import calebxzhou.rdi.common.util.toUUID
import calebxzhou.rdi.master.net.param
import calebxzhou.rdi.master.service.PlayerService
import calebxzhou.rdi.master.ygg.YggdrasilService.getProfile
import calebxzhou.rdi.master.ygg.YggdrasilService.getProfiles
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.util.*

fun Route.yggdrasilRoutes() {
    post("/minecraft/profile/lookup/bulk/byname"){
        call.getProfiles(call.receive())
    }
    get("/session/minecraft/profile/{uuid}"){
        call.getProfile(param("uuid"))
    }
}
object YggdrasilService {
    val String.fromUndashedUuid: UUID
        get() = UUID.fromString(this.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})".toRegex(), "$1-$2-$3-$4-$5"))
    val UUID.undashedString: String
        get() = this.toString().replace("-","")
    val RAccount.gameProfile: GameProfile
        get() {
            val texturesPayload = MinecraftTexturesPayload(
                timestamp = System.currentTimeMillis(),
                profileId = _id.toUUID().undashedString,
                profileName = name,
                true,
                textures = mutableMapOf(
                    MinecraftProfileTexture.Type.SKIN to MinecraftProfileTexture(
                        cloth.skin,
                        metadata = if (cloth.isSlim) mapOf("model" to "slim" ) else mapOf()
                    )
                ).apply {
                    cloth.cape?.let {
                        this += MinecraftProfileTexture.Type.CAPE to MinecraftProfileTexture(it)
                    }
                }
            )
            return GameProfile(_id.toUUID().undashedString,this.name,listOf(Property(value = texturesPayload.json.encodeBase64)))
        }
    suspend fun ApplicationCall.getProfile(uuid: String){
        val uid = uuid.fromUndashedUuid
        val account = PlayerService.getById(uid.objectId) ?: RAccount.DEFAULT
        respond(account.gameProfile)
    }
    suspend fun ApplicationCall.getProfiles(names: List<String>){
        val profiles = names.map { name ->
             PlayerService.getByName(name) ?: RAccount.DEFAULT
        }.map { it.gameProfile }
        respond(profiles)
    }

}