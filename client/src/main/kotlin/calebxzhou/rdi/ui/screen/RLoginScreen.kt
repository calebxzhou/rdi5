package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.auth.RAccount
import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.serdes.serdesJson
import calebxzhou.rdi.tutorial.PRIMARY
import calebxzhou.rdi.ui.component.RFormScreen
import calebxzhou.rdi.ui.component.RScreen
import calebxzhou.rdi.ui.general.confirm
import calebxzhou.rdi.ui.layout.RLinearLayout
import calebxzhou.rdi.ui.layout.linearLayout
import calebxzhou.rdi.util.body
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.mc.CenterX
import calebxzhou.rdi.util.mc.CenterY
import calebxzhou.rdi.util.mc.drawText
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import org.bson.types.ObjectId

class RLoginScreen(val server: RServer) : RScreen("登录") {
    val creds = LocalCredentials.read()
    override fun onClose() {
        mc go RTitleScreen()
    }

    lateinit var headsLayout: RLinearLayout
    override fun doInit() {
        RServer.now = server
        headsLayout = linearLayout(this) {
            startY = 50
            this.marginX = 5
            creds.idPwds.forEach { uid, pwd ->
                head(
                    uid = ObjectId(uid),
                    init = {
                        textComp =
                            textComp.withStyle(if (creds.lastLoggedId == uid) ChatFormatting.AQUA else ChatFormatting.WHITE)
                        tooltip = if (creds.lastLoggedId == uid) "上次登录".mcTooltip else "".mcTooltip
                        rightClick = {
                            contextMenu {
                                icon(
                                    icon = "error",
                                    text = "删除",
                                    click = {
                                        confirm("确定要删除账号${this@head.textComp.string}吗?") {
                                            delete(uid)
                                        }
                                    }
                                )
                                icon(
                                    icon = "pencil",
                                    text = "修改",
                                    click = {
                                        mc go addScreen(uid, pwd)
                                    }
                                )
                            }
                        }
                    },
                    click = {
                        playerLogin(uid, pwd)
                    }
                )
            }
        }
        linearLayout(this) {
            icon(
                icon = "plus",
                text = "添加",
                init = {
                    tooltip = "添加一个已经注册过的账号".mcTooltip
                },
                click = {
                    mc go addScreen("", "")
                }
            )
            icon(
                icon = "ssp",
                text = "注册",
                init = {
                    tooltip = "注册一个新账号".mcTooltip
                },
                click = {
                   /* if (!PRIMARY.isDone) {
                        confirm("要完成新手教程，才能注册账号\n现在开始吗？") {
                            PRIMARY.start()
                        }
                    } else {*/
                        mc go regScreen
                   // }
                }
            )
            icon(
                icon = "settings",
                text = "设置",
                click = {
                    mc go RSettingsScreen(mc.options)
                }
            )
            icon(icon = "hand", text = "互动教程") {
                PRIMARY.start()
            }
            icon(
                icon = "partner",
                text = "关于",
                click = {
                    mc go AboutScreen()
                }
            )

        }
    }

    override fun doRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (creds.idPwds.isEmpty()) {
            guiGraphics.drawText("没有账号", x = CenterX("没有账号"), y = CenterY)
        }
    }

    fun delete(id: String) {
        creds.idPwds.remove(id)
        creds.write()
        headsLayout.removeWidget(id)
    }

    fun playerLogin(usr: String, pwd: String) {
        server.hqSendAsync(
            true,
            true,
            "login",
            params = listOf("usr" to usr, "pwd" to pwd),
        ) {
            val account = serdesJson.decodeFromString<RAccount>(it.body)
            creds.idPwds += account._id.toHexString() to account.pwd
            creds.lastLoggedId = account._id.toHexString()
            creds.write()
            RAccount.now = account
            mc go RProfileScreen()
        }
    }

    val addScreen
        get() = { usr: String, pwd: String ->
            RFormScreen(
                title = "添加账号",
                layoutBuilder = {
                    textBox(
                        id = "usr",
                        label = "QQ号/昵称/ID",
                        length = 24,
                        init = {
                            value = usr
                        }
                    )
                    passwordBox()
                },
                submit = {
                    val usr = it["usr"]
                    val pwd = it["pwd"]
                    playerLogin(usr, pwd)
                }
            )
        }
    val regScreen
        get() = RFormScreen(
            title = "注册账号",
            layoutBuilder = {
                textBox(
                    id = "name",
                    label = "昵称(支持中文)",
                    length = 16,
                    init = {
                        invalid = {
                            if (it.value.length in 3..16)
                                null
                            else
                                "昵称长度必须3~16"
                        }
                    }
                )
                textBox(
                    id = "qq",
                    label = "QQ号",
                    length = 10,
                    init = {
                        invalid = {
                            if (it.value.length in 5..10)
                                null
                            else
                                "QQ格式错误"
                        }
                    }
                )
                passwordBox()
            },
            submit = {
                val pwd = it["pwd"]
                val qq = it["qq"]
                val name = it["name"]
                server.hqSendAsync(
                    true,
                    true,
                    "register",
                    params = listOf(
                        "name" to name,
                        "pwd" to pwd,
                        "qq" to qq
                    )
                ) {
                    playerLogin(qq, pwd)
                }
            })


}