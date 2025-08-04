package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.RBlockState
import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.model.Room
import calebxzhou.rdi.net.GameNetClient
import calebxzhou.rdi.net.body
import calebxzhou.rdi.net.protocol.SMeJoinPacket
import calebxzhou.rdi.service.RAccountService.getMyRoom
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.util.*
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.Toast
import kotlinx.coroutines.launch
import net.minecraft.world.level.block.Block

class ProfileFragment : RFragment("我的信息") {
    val account = RAccount.now ?: RAccount.DEFAULT
    val server = RServer.now?: RServer.OFFICIAL_DEBUG
    override fun initContent() {
        contentLayout.apply {
            orientation = LinearLayout.VERTICAL

            // Create a container for the head button
            linearLayout {
                layoutParams = linearLayoutParam(PARENT, SELF)
                gravity = Gravity.CENTER

                headButton(account._id) {
                  //  layoutParams = linearLayoutParam(SELF, SELF)
                }
            }

            textButton("修改信息", onClick = { mc go (ChangeProfileFragment()) })
            textButton("房间中心", onClick = {  ioScope.launch {
                account.getMyRoom()?.let { mc go (RoomCenterFragment(account,server, it)) }
            }} )
            fetchRoomInfo()
        }
    }
    fun start(){
        ioScope.launch {
            account.getMyRoom()?.let { room ->
                Room.now = room
                GameNetClient.connect(server)?.let {
                    GameNetClient.send(SMeJoinPacket(account.qq,account.pwd))
                }
            }?:alertErr("你还没有加入房间，请先创建或加入房间。")
        }
    }
    fun fetchRoomInfo(){
        ioScope.launch {
            account.getMyRoom()?.let { room->
                uiThread {
                    contentLayout.apply {
                        textButton("开始", onClick = ::start)
                    }
                }
            }?: uiThread {
                contentLayout.apply {
                    textButton("创建新房间", onClick = ::createNewRoom)
                    textButton("加入朋友房间", onClick = {
                        alertOk("让对方进行以下操作：\n1.打开房间中心\n2.添加成员-输入你的QQ\n3.你重新登录\n即可加入对方房间。")
                    })
                }
            }
        }
    }
    fun createNewRoom(){
        ioScope.launch {
            val bstates = Block.BLOCK_STATE_REGISTRY.mapIndexed { id, bs ->
                val name = bs.blockHolder.registeredName
                val props = bs.values.map { (prop, value) ->
                    prop.name to value.toString()
                }.toMap()
                RBlockState(
                    name = name,
                    props = props
                )
            }
            server.hqRequest(true, "room/create", params=listOf("bstates" to _root_ide_package_.calebxzhou.rdi.util.serdesJson.encodeToString(bstates))){
                 lgr.info("创建房间响应: ${it.body}")
                uiThread {
                    Toast.makeText(context, "创建完成，重新登录开始游玩",2000)
                    close()
                }
            }
        }
    }
    override fun close(){
        RServer.now=null
        RAccount.now=null
        Room.now=null
        mc go TitleFragment()
    }
}