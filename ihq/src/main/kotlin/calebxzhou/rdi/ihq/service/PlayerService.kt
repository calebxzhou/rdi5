package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.CRASH_REPORT_DIR
import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.exception.AuthError
import calebxzhou.rdi.ihq.exception.ParamError
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.lgr
import calebxzhou.rdi.ihq.model.AuthLog
import calebxzhou.rdi.ihq.model.HwSpec
import calebxzhou.rdi.ihq.model.RAccount
import calebxzhou.rdi.ihq.net.*
import calebxzhou.rdi.ihq.util.datetime
import calebxzhou.rdi.ihq.util.isValidHttpUrl
import calebxzhou.rdi.ihq.util.isValidObjectId
import calebxzhou.rdi.ihq.util.serdesJson
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates
import com.mongodb.client.model.Updates.combine
import io.ktor.http.ContentType
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.flow.firstOrNull
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import java.io.File

fun Route.playerRoutes() {
    get("/playerInfo") {
        val uid = ObjectId(param("uid"))
        val info = PlayerService.getInfo(uid)
        response(data=serdesJson.encodeToString(info))
    }
    get("/player-info") {
        val uid = ObjectId(param("uid"))
        val info = PlayerService.getInfo(uid)
        response(data=serdesJson.encodeToString(info))
    }
    get("/player-info-by-names") {
        val names = param("names").split("\n")
        val infos = PlayerService.getInfoByNames(names)
        response(data=serdesJson.encodeToString(infos))
    }
    get("/name") {
        val uid = ObjectId(param("uid"))
        val name = PlayerService.getName(uid) ?: "【玩家不存在】"
        response(data=name)
    }
    get("/skin") {
        val uidParam = param("uid")
        val uid = ObjectId(uidParam)
        val cloth = PlayerService.getSkin(uid)
        response(data=serdesJson.encodeToString(cloth))
    }
    post("/register") {
        val name = param("name")
        val pwd = param("pwd")
        val qq = param("qq")
        PlayerService.register(name, pwd, qq)
        ok()
    }
    post("/login") {
        val usr = param("usr")
        val pwd = param("pwd")
        val specJson = paramNull("spec")
        val account = PlayerService.login(usr, pwd, specJson, call.clientIp)
        response(data=serdesJson.encodeToString(account))
    }
    post("/crash-report") {
        val uid = ObjectId(param("uid"))
        val report = param("report")
        PlayerService.saveCrashReport(uid, report)
        ok()
    }

    authenticate("auth-basic") {
        post("/skin") {
            val skin = param("skin")
            val cape = paramNull("cape")
            val isSlim = paramNull("isSlim")?.toBoolean() ?: false
            PlayerService.changeCloth(uid, isSlim, skin, cape)
            ok()
        }
        post("/change-profile") {
            val newQq = paramNull("qq")
            val newName = paramNull("name")
            val newPwd = paramNull("pwd")
            PlayerService.changeProfile(uid, newName, newQq, newPwd)
            ok()
        }
        post("/clearSkin") {
            PlayerService.clearCloth(uid)
            ok()
        }
    }
}

object PlayerService {
    val accountCol = DB.getCollection<RAccount>("account")
    val authLogCol = DB.getCollection<AuthLog>("auth_log")
    val inGamePlayers = hashMapOf<Byte, RAccount>()

    suspend fun getByQQ(qq: String): RAccount? = accountCol.find(eq("qq", qq)).firstOrNull()
    suspend fun getByName(name: String): RAccount? = accountCol.find(eq("name", name)).firstOrNull()

    suspend fun get(usr: String): RAccount? {
        if (usr.isValidObjectId()) {
            return getById(ObjectId(usr))
        }
        return getByQQ(usr) ?: getByName(usr)
    }

    fun equalById(id: ObjectId): Bson = eq("_id", id)
    fun equalById(acc: RAccount): Bson = eq("_id", acc._id)

    suspend fun getById(id: ObjectId): RAccount? = accountCol.find(equalById(id)).firstOrNull()

    suspend fun has(id: ObjectId): Boolean = accountCol
        .find(equalById(id))
        .projection(org.bson.Document("_id", 1))
        .limit(1)
        .firstOrNull() != null

    suspend fun validate(usr: String, pwd: String): RAccount? {
        val account = get(usr)
        return if (account == null || account.pwd != pwd) null else account
    }

    suspend fun clearCloth(uid: ObjectId) {
        accountCol.updateOne(equalById(uid), Updates.unset("cloth"))
    }

    suspend fun changeCloth(uid: ObjectId, isSlim: Boolean, skin: String, cape: String?) {
        if (!skin.isValidHttpUrl()) throw ParamError("皮肤链接格式错误")
        val normalizedCape = cape?.also {
            if (!it.isValidHttpUrl()) throw ParamError("披风链接格式错误")
        }
        val cloth = RAccount.Cloth(isSlim, skin, normalizedCape)
        accountCol.updateOne(equalById(uid), Updates.set("cloth", cloth))
    }

    suspend fun getSkin(uid: ObjectId): RAccount.Cloth {
        val cloth = getById(uid)?.cloth ?: RAccount.Cloth()
        val normalizedCape = cloth.cape?.takeUnless { it == "null" }
        return cloth.copy(cape = normalizedCape)
    }

    suspend fun register(name: String, pwd: String, qq: String): RAccount {
        if (getByQQ(qq) != null || getByName(name) != null) throw RequestError("QQ或昵称被占用")
        val account = RAccount(name = name, pwd = pwd, qq = qq)
        accountCol.insertOne(account)
        return account
    }

    suspend fun login(usr: String, pwd: String, specJson: String?, clientIp: String): RAccount {
        val account = validate(usr, pwd) ?: run {
            lgr.info { "${usr}登录失败" }
            throw AuthError("密码错误")
        }
        lgr.info { "${account.name} ${account.qq}登录成功" }
        specJson?.let { json ->
            val spec = try {
                serdesJson.decodeFromString<HwSpec>(json)
            } catch (ex: Exception) {
                throw ParamError("硬件信息格式错误")
            }
            authLogCol.insertOne(
                AuthLog(
                    uid = account._id,
                    login = true,
                    ip = clientIp,
                    spec = spec
                )
            )
        }
        return account
    }

    suspend fun changeProfile(uid: ObjectId, newName: String?, newQq: String?, newPwd: String?) {
        val updates = mutableListOf<Bson>()
        newQq?.let {
            if (getByQQ(it) != null) throw RequestError("QQ用过了")
            updates += Updates.set("qq", it)
        }
        newName?.let {
            if (getByName(it) != null) throw RequestError("名字用过了")
            updates += Updates.set("name", it)
        }
        newPwd?.let { updates += Updates.set("pwd", it) }
        if (updates.isNotEmpty()) {
            accountCol.updateOne(equalById(uid), combine(updates))
        }
    }

    suspend fun getName(uid: ObjectId): String? = getById(uid)?.name

    suspend fun getInfo(uid: ObjectId): RAccount.Dto = getById(uid)?.dto ?: RAccount.Dto()

    suspend fun getInfoByNames(names: List<String>): List<RAccount.Dto> =
        names.map { getByName(it)?.dto ?: RAccount.Dto() }

    suspend fun saveCrashReport(uid: ObjectId, report: String) {
        val account = getById(uid)
        val fileName = "${account?.name ?: "未知"}-${account?.qq ?: "0"}-${datetime}.txt"
        File(CRASH_REPORT_DIR, fileName).writeText(report)
    }
}