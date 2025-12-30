package calebxzhou.rdi.master.service

import calebxzhou.mykotutils.log.Loggers
import calebxzhou.mykotutils.hwspec.HwSpec
import calebxzhou.mykotutils.std.displayLength
import calebxzhou.mykotutils.std.getDateTimeNow
import calebxzhou.mykotutils.std.isValidHttpUrl
import calebxzhou.rdi.master.CRASH_REPORT_DIR
import calebxzhou.rdi.master.DB
import calebxzhou.rdi.master.exception.AuthError
import calebxzhou.rdi.master.exception.ParamError
import calebxzhou.rdi.master.exception.RequestError
import calebxzhou.rdi.master.model.AuthLog
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.common.model.RAccount
import calebxzhou.rdi.master.net.*
import calebxzhou.rdi.master.service.PlayerService.changeCloth
import calebxzhou.rdi.master.service.PlayerService.changeProfile
import calebxzhou.rdi.master.service.PlayerService.clearCloth
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Updates
import com.mongodb.client.model.Updates.combine
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.header
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import java.io.File

fun Route.playerRoutes() {
    route("/player") {
        route("/{uid}") {
            get("/name") {
                val uid = ObjectId(param("uid"))
                val name = PlayerService.getName(uid) ?: "【玩家不存在】"
                response(data = name)
            }
            get("/skin") {
                val uidParam = param("uid")
                val uid = ObjectId(uidParam)
                val cloth = PlayerService.getSkin(uid)
                response(data = cloth)
            }
            get("/info") {
                val uid = ObjectId(param("uid"))
                val info = PlayerService.getInfo(uid)
                response(data = info)
            }
        }
        get("/infos") {
            val names = param("names").split("\n")
            val infos = PlayerService.getInfoByNames(names)
            response(data = infos)
        }
        post("/register") {
            PlayerService.register(param("name"), param("pwd"), param("qq"))
            ok()
        }
        post("/jwt") {
            PlayerService.validate(param("usr"), param("pwd"))?.let { JwtService.generateToken(it._id) }
                ?.let { response(data = it) } ?: err("账密×")
        }
        post("/login") {
            val result = PlayerService.login(param("usr"), param("pwd"), paramNull("spec"), call.clientIp)
            call.response.header("jwt", JwtService.generateToken(result._id))
            response(data = result)
        }
        post("/crash-report") {
            val uid = ObjectId(param("uid"))
            val report = param("report")
            PlayerService.saveCrashReport(uid, report)
            ok()
        }

        authenticate("auth-jwt", optional = true) {
            route("/skin") {
                post {
                    call.player().changeCloth(
                        paramNull("isSlim")?.toBoolean() ?: false,
                        param("skin"),
                        paramNull("cape")
                    )
                    ok()
                }
                delete {
                    call.player().clearCloth()
                    ok()
                }
            }
            put("/profile") {
                call.player().changeProfile(paramNull("name"), paramNull("qq"), paramNull("pwd"))
                ok()
            }
        }
    }
}

suspend fun ApplicationCall.player(): RAccount = PlayerService.getById(uid) ?: throw RequestError("用户不存在")

object PlayerService {
    val accountCol = DB.getCollection<RAccount>("account")
    val authLogCol = DB.getCollection<AuthLog>("auth_log")
    val inGamePlayers = hashMapOf<Byte, RAccount>()
    private val lgr by Loggers
    data class LoginResult(val account: RAccount, val token: String)

    suspend fun getByQQ(qq: String): RAccount? = accountCol.find(eq("qq", qq)).firstOrNull()
    suspend fun getByName(name: String): RAccount? = accountCol.find(eq("name", name)).firstOrNull()

    suspend fun get(usr: String): RAccount? {
        if (ObjectId.isValid(usr)) {
            return getById(ObjectId(usr))
        }
        return getByQQ(usr) ?: getByName(usr)
    }

    val RAccount.uidFilter
        get() = equalById(_id)

    fun equalById(id: ObjectId): Bson = eq("_id", id)
    fun equalById(acc: RAccount): Bson = eq("_id", acc._id)


    suspend fun getById(id: ObjectId): RAccount? = accountCol.find(equalById(id)).firstOrNull()

    suspend fun has(id: ObjectId): Boolean = accountCol
        .countDocuments(equalById(id)) > 0

    suspend fun validate(usr: String, pwd: String): RAccount? {
        val account = get(usr)
        return if (account == null || account.pwd != pwd) null else account
    }

    suspend fun RAccount.clearCloth() {
        accountCol.updateOne(uidFilter, Updates.unset("cloth"))
    }

    suspend fun RAccount.changeCloth(isSlim: Boolean, skin: String, cape: String?) {
        if (!skin.isValidHttpUrl()) throw ParamError("皮肤链接格式错误")
        if (!cape.isValidHttpUrl()) throw ParamError("披风链接格式错误")
        accountCol.updateOne(uidFilter, Updates.set("cloth", RAccount.Cloth(isSlim, skin, cape)))
    }

    suspend fun getSkin(uid: ObjectId): RAccount.Cloth {
        val cloth = getById(uid)?.cloth ?: RAccount.Cloth()
        val normalizedCape = cloth.cape?.takeUnless { it == "null" }
        return cloth.copy(cape = normalizedCape)
    }

    suspend fun register(name: String, pwd: String, qq: String): RAccount {
        if (getByQQ(qq) != null || getByName(name) != null) throw RequestError("QQ或昵称被占用")
        val nameSize = name.displayLength
        if (nameSize !in 3..24) {
            throw RequestError("昵称长度应在3~24，当前为${nameSize}")
        }
        if (qq.length !in 5..10 || !qq.all { it.isDigit() }) {
            throw RequestError("QQ号格式不正确")

        }
        if (pwd.length !in 6..16) {
            throw RequestError("密码长度须在6~16个字符")
        }
        val account = RAccount(_id = ObjectId(), name = name, pwd = pwd, qq = qq)
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

    suspend fun RAccount.changeProfile(newName: String?, newQq: String?, newPwd: String?) {
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
            accountCol.updateOne(uidFilter, combine(updates))
        }
    }

    suspend fun getName(uid: ObjectId): String? = getById(uid)?.name
    suspend fun List<ObjectId>.getPlayerNames(): Map<ObjectId, String> {
        if (isEmpty()) return emptyMap()
        val uniqueIds = LinkedHashSet(this)
        val names = accountCol.find(`in`("_id", uniqueIds.toList()))
            .toList()
            .associate { account -> account._id to account.name }
            .toMutableMap()

        uniqueIds.forEach { id ->
            names.putIfAbsent(id, "未知")
        }

        return names
    }

    suspend fun getInfo(uid: ObjectId): RAccount.Dto = getById(uid)?.dto ?: RAccount.DEFAULT.dto

    suspend fun getInfoByNames(names: List<String>): List<RAccount.Dto> =
        names.map { getByName(it)?.dto ?: RAccount.DEFAULT.dto }

    suspend fun saveCrashReport(uid: ObjectId, report: String) {
        val account = getById(uid)
        val fileName = "${account?.name ?: "未知"}-${account?.qq ?: "0"}-${getDateTimeNow("yyyyMMdd-HHmmss")}.txt"
        File(CRASH_REPORT_DIR, fileName).writeText(report)
    }
}