package calebxzhou.rdi.model

import calebxzhou.rdi.lgr
import calebxzhou.rdi.util.toUUID
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.bson.types.ObjectId

val account
    get() = RAccount.now ?: RAccount.DEFAULT.also { lgr.warn("用户未登录 使用默认账号") }
@Serializable
data class RAccount(
    @Contextual
    val _id: ObjectId,
    var name: String,
    var pwd: String,
    var qq: String,
    val score: Int = 0,
    var cloth: Cloth = Cloth(),
) {
    @Transient
    var jwt: String?=null
    @Serializable
    data class Cloth(
        var isSlim: Boolean = true,
        var skin: String = "https://littleskin.cn/textures/526fe866ed25a7ee1cf894b81a2199aaa03f139803623a25a793f6ae57e22f02",
        var cape: String? = null
    ) {

    }

    @Serializable
    data class Dto(
        @Contextual
        val id: ObjectId,
        val name: String,
        val cloth: Cloth
    ) {


    }

    companion object {
        val DEFAULT = RAccount(ObjectId(), "test", "123456", "12345", 0)
        val TESTS = listOf(
            RAccount(ObjectId("68b314bbadaf52ddab96b5ed"), "测试1", "123123", "123123", 0),
            RAccount(ObjectId("68c901f07c76a32fa7dc270a"), "测试2", "456456", "456456", 0)
        )

        @JvmField
        var now: RAccount? = null


        /*@JvmStatic
        fun processSkullProfile(
            profileCache: GameProfileCache,
            profile: GameProfile?,
            profileConsumer: Consumer<GameProfile?>
        ) {
            if(profile != null && !profile.name.isNullOrBlank() && (!profile.isComplete || !profile.properties.containsKey("textures")) && profileCache != null){
                profileCache.getAsync(profile.name) {  }
            }
        }*/

    }

    @Contextual
    val uuid = _id.toUUID()
    val dto = Dto(_id, name, cloth)

    fun logout() {
        now = null
    }

    fun updateCloth(cloth: Cloth) {
        // cloth.register()
        this.cloth = cloth
    }

}