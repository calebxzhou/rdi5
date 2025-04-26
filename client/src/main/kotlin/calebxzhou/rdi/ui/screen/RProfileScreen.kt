package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.TOTAL_TICK_DELTA
import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.auth.RAccount
import calebxzhou.rdi.common.Carrier
import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.Island
import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.net.RKcpClient
import calebxzhou.rdi.serdes.serdesJson
import calebxzhou.rdi.tutorial.Tutorial
import calebxzhou.rdi.ui.component.RCheckbox
import calebxzhou.rdi.ui.component.RFormScreen
import calebxzhou.rdi.ui.component.RGuiEntityRenderer
import calebxzhou.rdi.ui.component.RScreen
import calebxzhou.rdi.ui.component.button.RIconButton
import calebxzhou.rdi.ui.general.alert
import calebxzhou.rdi.ui.general.alertErr
import calebxzhou.rdi.ui.layout.linearLayout
import calebxzhou.rdi.util.*
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.multiplayer.resolver.ServerAddress
import java.time.LocalTime

class RProfileScreen(
) : RScreen("我的信息") {
    val creds = LocalCredentials.read()
    val account
        get() = RAccount.now ?: RAccount.DEFAULT
    val server = RServer.now ?: RServer.OFFICIAL_DEBUG
    var hasIsland = false
    val changeProfileScreen = RFormScreen(
        title = "修改信息：",
        layoutBuilder = {
            textBox(
                id = "name",
                label = "昵称",
                length = 24,
                init = {
                    value = account.name
                }
            )
            passwordBox(init = {
                value = account.pwd
            })
            textBox(
                id = "qq",
                label = "QQ",
                length = 10,
                init = {
                    numberOnly = true
                    value = account.qq
                }
            )
        },
        submit = {
            val name = it["name"]
            val pwd = it["pwd"]
            val qq = it["qq"]
            val params = arrayListOf<Pair<String, String>>()
            if (name != account.name) params += "name" to name
            if (pwd != account.pwd) params += "pwd" to pwd
            if (qq != account.qq) params += "qq" to qq
            server.hqSendAsync(true, true, "profile", params) {
                val newAccount = RAccount(account._id, name, pwd, qq, account.score, account.cloth)
                RAccount.now = newAccount
                mc go RProfileScreen()
            }

        })


    override fun init() {
        account.cloth.register()
        server.hqSendAsync(path = "island/my") {
            if (it.body != "0") {
                hasIsland = true
            }
        }
        linearLayout(this) {
            startX = width / 2
            startY = 30
            marginX = 0
            head(
                uid = account._id,
                init = {

                }

            )
        }
        linearLayout(this) {
            icon(
                icon = "start",
                text = "开始",
            ) {
                connect()
            }
            icon(icon = "basic_info", text = "修改信息") {
                mc go changeProfileScreen
            }
                icon(icon = "island", text = "岛屿中心") {
                    if(!hasIsland){
                        alertErr("您没有岛屿 请点击开始按钮新建 或者加入")
                    }else{

                    server.hqSendAsync(true, false, "island/my") { resp ->
                        val island = serdesJson.decodeFromString<Island>(resp.body)
                        mc go IslandCenterScreen(account, server, island)
                    }
                    }
                }

            icon(icon = "clothes", text = "衣柜") {
                mc go RWardrobeScreen(account, server)
            }
            icon(icon = "city", text = creds.carrierEnum.display) {
                creds.carrier = when (creds.carrier) {
                    "t" -> "u"
                    "u" -> "m"
                    "m" -> "t"
                    else -> "t"
                }
                creds.write()
                ofWidget<RIconButton>("city").text=creds.carrierEnum.display
                lgr.info(creds.carrierEnum.display)
            }
            checkBox("kcp","双协议加速(实验)",creds.kcp){
                tooltip = "适用联通移动\n采用TCP+KCP双连接，快速+稳定\n实验功能 可能有bug".mcTooltip
            }
        }
        super.init()
    }

    override fun doTick() {

    }

    override fun onClose() {
        RServer.now?.disconnect()
        RAccount.now?.logout()

        mc go RLoginScreen(server)
    }

    private fun connect() {
        RKcpClient.enabled = ofWidget<RCheckbox>("kcp").selected()
        creds.kcp = RKcpClient.enabled
        creds.write()
        Tutorial.now = null
        //只有电信支持加速
        val ip = if(RKcpClient.enabled) server.ip[Carrier.t] else server.ip[creds.carrierEnum]
        //移动只有18~24能用
        val isEvening = LocalTime.now().isAfter(LocalTime.of(18, 0)) && LocalTime.now().isBefore(LocalTime.of(23,59))
        if(!isEvening && creds.carrierEnum== Carrier.m){
            alertErr("移动节点开放时间18:00~24:00")
            return
        }
        if (hasIsland) {
            mcMainThread {
                ConnectScreen.startConnecting(
                    this@RProfileScreen, mc,

                    ServerAddress(ip?:"localhost", server.gamePort), server.mcData, false
                )
            }
        } else {
            contextMenu {
                icon("plus", "创建新岛屿") {

                    server.hqSendAsync(true, true, path = "island/create") {
                        mc go this@RProfileScreen
                        alert("创建完成，点击开始按钮游玩")
                    }
                }
                icon("smp", "加入朋友岛屿") {
                    alert("让对方进行以下操作：\n1.打开岛屿中心\n2.成员-添加-输入你的QQ\n3.再次点击“开始”按钮")

                }
            }
        }

    }

    override fun onPressEnterKey() {
        connect()
    }

    override fun doRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        //if (modelLoaded.any { it }) {
        RGuiEntityRenderer.drawEntity(
            guiGraphics.pose(),
            width/2 ,
            260,
            120,
            TOTAL_TICK_DELTA * 2,
            mouseX.toDouble(),
            mouseY.toDouble(),
            account.cloth.isSlim == true,
            account.cloth.skinLocation,
            account.cloth.capeLocation
        )
        //}

        mc.player?.let {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics, 200, 200, 30, mouseX.toFloat(), mouseY.toFloat(),
                it
            )
        }
    }


}