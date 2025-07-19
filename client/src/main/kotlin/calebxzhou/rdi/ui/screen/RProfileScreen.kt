package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.model.Room
import calebxzhou.rdi.util.serdesJson
import calebxzhou.rdi.ui.component.RFormScreen
import calebxzhou.rdi.ui.general.alertErr
import calebxzhou.rdi.ui.layout.linearLayout
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import net.minecraft.client.gui.GuiGraphics

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
            if (name != account.name) params + "name" to name
            if (pwd != account.pwd) params + "pwd" to pwd
            if (qq != account.qq) params + "qq" to qq
            server.hqRequest(path = "profile", params = params) {
                val newAccount = RAccount(account._id, name, pwd, qq, account.score, account.cloth)
                RAccount.now = newAccount
                mc go RProfileScreen()
            }

        })


    override fun init() {
        //account.cloth.register()
        server.hqRequest(path = "island/my") {
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
            icon(icon = "island", text = "房间中心") {
                if (!hasIsland) {
                    alertErr("您没有房间 请点击开始按钮新建 或者加入")
                } else {
                    server.hqRequest(path = "island/my") { resp ->
                        val island = serdesJson.decodeFromString<Room>(resp.body)
                        //mc go IslandCenterScreen(account, server, island)
                    }
                }
            }
/*
            icon(icon = "clothes", text = "衣柜") {
                mc go RWardrobeScreen(account, server)
            }*/
             
        }
        super.init()
    }

    override fun doTick() {

    }

    override fun onClose() {
        RServer.now=null
        RAccount.now?.logout()

        mc go RLoginScreen(server)
    }

    private fun connect() { 
        creds.write()

       /* val ip =  server.ip
        if (hasIsland) {
            mcMainThread {
                ConnectScreen.startConnecting(
                    this@RProfileScreen, mc,

                    ServerAddress(ip ?: "localhost", server.gamePort), server.mcData, false
                )
            }
        } else {
            contextMenu {
                icon("plus", "创建新房间") {

                    server.hqSendAsync(true, true, path = "island/create") {
                        mc go this@RProfileScreen
                        alert("创建完成，点击开始按钮游玩")
                    }
                }
                icon("smp", "加入朋友房间") {
                    alert("让对方进行以下操作：\n1.打开房间中心\n2.成员-添加-输入你的QQ\n3.再次点击“开始”按钮")

                }
            }
        }*/

    }

    override fun onPressEnterKey() {
        connect()
    }

    override fun doRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        //if (modelLoaded.any { it }) {
       /* RGuiEntityRenderer.drawEntity(
            guiGraphics.pose(),
            width / 2,
            260,
            120,
            TOTAL_TICK_DELTA * 2,
            mouseX.toDouble(),
            mouseY.toDouble(),
            account.cloth.isSlim == true,
            account.cloth.skinLocation,
            account.cloth.capeLocation
        )*/
        //}

        /*mc.player?.let {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics, 200, 200, 30, mouseX.toFloat(), mouseY.toFloat(),
                it
            )
        }*/
    }


}