package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.CRASH_REPORT_DIR
import calebxzhou.rdi.ihq.DB
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
import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.coroutines.flow.firstOrNull
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import java.io.File

object PlayerService {
    val accountCol = DB.getCollection<RAccount>("account")
    val authLogCol = DB.getCollection<AuthLog>("auth_log")
    val inGamePlayers = hashMapOf<Byte, RAccount>()

    //根据qq获取
    suspend fun getByQQ(qq: String): RAccount? = accountCol.find(eq("qq", qq)).firstOrNull()
    suspend fun getByName(name: String): RAccount? = accountCol.find(eq("name", name)).firstOrNull()
    suspend fun get(usr: String): RAccount? {
        if (usr.isValidObjectId()) {
            return getById(ObjectId(usr))
        }
        return getByQQ(usr) ?: getByName(usr)

    }

    fun equalById(id: ObjectId): Bson {
        return eq("_id", id)
    }

    fun equalById(acc: RAccount): Bson {
        return eq("_id", acc._id)
    }

    //根据rid获取
    suspend fun getById(id: ObjectId): RAccount? = accountCol.find(equalById(id)).firstOrNull()


    suspend fun validate(usr: String, pwd: String): RAccount? {
        val account = get(usr)
        return if (account == null || account.pwd != pwd) {
            null
        } else {
            account
        }
    }


    suspend fun clearCloth(call: ApplicationCall) {
        accountCol.updateOne(equalById(call.uid), Updates.unset("cloth"))
        call.ok()
    }


    suspend fun changeCloth(call: ApplicationCall) {
        val params = call.receiveParameters()
        val skin = params got "skin"
        if (!skin.isValidHttpUrl()) {
            call.e400("皮肤链接格式错误")
            return
        }

        val cape = params["cape"]?.also {
            if (!it.isValidHttpUrl()) {
                call.e400("披风链接格式错误")
                return
            }
        }
        val cloth = RAccount.Cloth(params["isSlim"].toBoolean(), skin, cape)
        accountCol.updateOne(equalById(call.uid), Updates.set("cloth", cloth))
        call.ok()
    }

    suspend fun getSkin(call: ApplicationCall) {
        val params = call.initGetParams()
        val uid = params["uid"]
        getById(ObjectId(uid))?.let {
            if (it.cloth.cape == "null")
                it.cloth.cape = null
            call.ok(serdesJson.encodeToString(it.cloth))

        } ?: call.ok(serdesJson.encodeToString(RAccount.Cloth()))
    }

    suspend fun register(call: ApplicationCall) {
        val params = call.receiveParameters()
        val name = params got "name"
        val pwd = params got "pwd"
        val qq = params got "qq"
        if (getByQQ(qq) != null || getByName(name) != null) {
            call.e400("QQ或昵称被占用")
            return
        }
        val account = RAccount(
            name = name,
            pwd = pwd,
            qq = qq
        )
        accountCol.insertOne(account)
        call.ok( )
    }

    suspend fun login(call: ApplicationCall) {
        val params = call.receiveParameters()
        val usr = params got "usr"
        val pwd = params got "pwd"
        validate(usr, pwd)?.let { account ->
            lgr.info { "${account.name} ${account.qq}登录成功" }
            call.ok(serdesJson.encodeToString(account))
            params["spec"]?.let { specS->
                val spec = serdesJson.decodeFromString<HwSpec>(specS)
                authLogCol.insertOne(
                    AuthLog(
                        uid = account._id,
                        login=true,
                        ip = call.clientIp,
                        spec = spec,
                    )
                )
            }
        } ?: let {
            lgr.info { "${usr}登录失败" }
            call.e401("密码错误")
        }
    }

    suspend fun changeProfile(call: ApplicationCall) {
        val params = call.receiveParameters()
        params["qq"]?.let { qq ->
            if (getByQQ(qq) != null) {
                call.e400("QQ用过了")
                return
            }
            accountCol.updateOne(equalById(call.uid), Updates.set("qq", qq))
        }
        params["name"]?.let { name ->
            if (getByName(name) != null) {
                call.e400("名字用过了")
                return
            }
            accountCol.updateOne(equalById(call.uid), Updates.set("name", name))
        }
        params["pwd"]?.let { pwd ->
            accountCol.updateOne(equalById(call.uid), Updates.set("pwd", pwd))
        }
        call.ok()
    }


    /*suspend fun onJoinGame(call: ApplicationCall) {
        val params = call.receiveParameters()
        val uids = params got "uid"
        val ip = params got "ip"
        val uid = ObjectId(uids)
        //没岛 不允许进服
        if (IslandService.getJoinedIsland(uid) == null) {
            rconPost("kick $uids")
            return
        }
        lgrinRecordCol.insertOne(lgrinRecord(uid = uid, ip = ip))
        //自动回岛
        IslandService.goHome(uid)
        call.ok()
    }

    suspend fun onQuitGame(call: ApplicationCall) {
        val params = call.receiveParameters()
        val uid = ObjectId(params got "uid")
        lgrinRecordCol.insertOne(lgrinRecord(uid = uid, ip = null))
        call.ok()
    }*/

    suspend fun getNameFromId(call: ApplicationCall) {
        val params = call.initGetParams()
        val uid = ObjectId(params got "uid")
        getById(uid)?.let {
            call.ok(it.name)
        } ?: call.ok("【玩家不存在】")
    }

    suspend fun getInfo(call: ApplicationCall) {
        val params = call.initGetParams()
        val uid = ObjectId(params got "uid")
        val account = getById(uid)?.dto ?: RAccount.Dto()
        call.ok(serdesJson.encodeToString(account))
    }
    suspend fun getInfoByNames(call: ApplicationCall) {
        val params = call.initGetParams()
        val names = (params got "names").split("\n")
        val infos = names.map { getByName(it)?.dto ?: RAccount.Dto() }
        call.ok(serdesJson.encodeToString(infos))
    }

    suspend fun crashReport(call: ApplicationCall) {
        val params = call.initPostParams()
        val report = (params got "report")
        val account = getById(ObjectId(params got "uid"))
        val fileName = "${account?.name ?: "未知"}-${account?.qq ?: "0"}-${datetime}.txt"
        File(CRASH_REPORT_DIR,fileName).writeText(report)
        call.ok()
    }


}