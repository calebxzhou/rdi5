package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.lgr
import calebxzhou.rdi.ihq.model.GameContext
import calebxzhou.rdi.ihq.model.RAccount
import calebxzhou.rdi.ihq.net.*
import calebxzhou.rdi.ihq.net.GameNetServer.abort
import calebxzhou.rdi.ihq.net.GameNetServer.sendPacket
import calebxzhou.rdi.ihq.net.protocol.CLoginOkPacket
import calebxzhou.rdi.ihq.net.protocol.CPlayerJoinPacket
import calebxzhou.rdi.ihq.net.protocol.CPlayerLeavePacket
import calebxzhou.rdi.ihq.util.isValidHttpUrl
import calebxzhou.rdi.ihq.util.isValidObjectId
import calebxzhou.rdi.ihq.util.serdesJson
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.netty.channel.ChannelHandlerContext
import kotlinx.coroutines.flow.firstOrNull
import org.bson.conversions.Bson
import org.bson.types.ObjectId

object PlayerService {
    val accountCol = DB.getCollection<RAccount>("account")
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
    suspend fun RAccount.goOnline(ctx: ChannelHandlerContext) {
        RoomService.getJoinedRoom(_id)?.let { room ->
            ctx.account = this
            gameContext = GameContext(net = ctx, room = room)
            val tmpId = inGamePlayers.size.toByte()
            if (tmpId > Byte.MAX_VALUE) {
                ctx.abort("服务器在线玩家数超过256")
                return
            }
            //告诉大家我上线了
            inGamePlayers.forEach { tmpId, acc ->
                acc.sendPacket(CPlayerJoinPacket(acc._id, tmpId, acc.name))
            }
            inGamePlayers[tmpId] = this
            gameContext?.tmpId = tmpId
            //房间在线成员 加
            room.onlineMembers += tmpId to this
            lgr.info { "${name}上线 ${inGamePlayers.size}/256" }
            ctx.sendPacket(CLoginOkPacket())
        } ?: let {
            lgr.warn { "${name}尝试上线但没有加入房间" }
            ctx.abort("请先加入房间")
            ctx.close()
            return
        }
    }

    suspend fun RAccount.goOffline() {
        RoomService.getJoinedRoom(_id)?.let { room ->
            //告诉大家我下线了
            gameContext?.let { ctx ->
                inGamePlayers.forEach { _, acc ->
                    acc.sendPacket(CPlayerLeavePacket(ctx.tmpId))
                }
                inGamePlayers.remove(ctx.tmpId)
                room.onlineMembers.remove(ctx.tmpId)
                ctx.net.close()
            }
        }
        gameContext = null
        lgr.info { "${name}下线 ${inGamePlayers.size}/256" }
    }
    fun RAccount.sendPacket(packet: CPacket){
        gameContext?.net?.sendPacket(packet)
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
        call.ok()
    }

    suspend fun login(call: ApplicationCall) {
        val params = call.receiveParameters()
        val usr = params got "usr"
        val pwd = params got "pwd"
        validate(usr, pwd)?.let { account ->
            lgr.info { "${usr}登录成功" }

            call.ok(serdesJson.encodeToString(account))
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
        val account = getById(uid)?.dto ?: RAccount.Dto(ObjectId(), "***", RAccount.Cloth())
        call.ok(serdesJson.encodeToString(account))
    }


}