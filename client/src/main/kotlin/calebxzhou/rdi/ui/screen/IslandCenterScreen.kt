package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.auth.RAccount
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.ui.component.RFormScreen
import calebxzhou.rdi.ui.component.RScreen
import calebxzhou.rdi.ui.general.alert
import calebxzhou.rdi.ui.general.confirm
import calebxzhou.rdi.ui.layout.linearLayout
import calebxzhou.rdi.util.*
import calebxzhou.rdi.util.mc.UiHeight
import calebxzhou.rdi.util.mc.drawText
import calebxzhou.rdi.util.mc.matrixOp
import calebxzhou.rdi.util.mc.tran0ScaleBack
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.Direction

//回岛 成员 信息
class IslandCenterScreen(val account: RAccount, val server: RServer, val island: Island) : RScreen("岛屿中心") {

    val inviteScreen = RFormScreen(
        title = "邀请成员",
        layoutBuilder = {
            textBox(
                id = "qq",
                label = "对方的QQ",
                length = 10,

                init = {
                    numberOnly=true
                    invalid = {
                        if (it.value.length>=5) {
                            null
                        } else {
                            "无效的QQ格式!"
                        }
                    }
                }
            )
        },
        submit = {
            val qq = it["qq"]

            server.hqSendAsync(true, true, "island/invite_qq", listOf("qq" to qq)) {
                alert("成功邀请玩家${it.body}加入了你的岛屿。")
                mc.popGuiLayer()
            }

        }
    )
    val delScreen = { quit: Boolean ->
        RFormScreen(
            title = "在下方输入 确认删除", {
                textBox("id", "...", 8)
            }) {
            if (it["id"].contains("确认删除")) {

                server.hqSendAsync(true, true, if (quit) "island/quit" else "island/delete") {
                    mcMainThread {
                        mc.level?.disconnect()
                        mc.clearLevel()
                        mc.goHome()
                        alert("成功退出你的岛屿")
                    }
                }

            }
        }
    }



    override fun doInit() {
        linearLayout(this) {
            startY = 30
            island.members.forEach { member ->
                head(member.id, init = {
                    textComp = textComp.withStyle(if (member.isOwner) ChatFormatting.GOLD else ChatFormatting.WHITE)
                    rightClick = {
                        contextMenu {
                            icon("error", "踢出") {
                                confirm("真的要踢出${this@head.textComp.string}吗？？？\n他的存档会被删除，无法恢复！") {
                                    server.hqSendAsync(true, true, "island/kick", listOf("uid2" to member.id)) {
                                        island.members.remove(member)
                                        onClose()
                                    }
                                }
                            }
                            icon("transfer", "转让") {
                                confirm("要把岛主权限转让给${this@head.textComp.string}吗？") {
                                    server.hqSendAsync(true, true, "island/transfer", listOf("uid2" to member.id)) {
                                        member.isOwner = true
                                        island.members.find { it.id == account._id }?.isOwner = false
                                        onClose()
                                    }
                                }
                            }
                        }
                    }
                })
            }
        }
        linearLayout(this) {
            startY = 120
            mc.player?.let { player->

            icon(icon = "portal", text = "设置传送点") {
                if (player.isFallFlying || !player.blockStateOn.isFaceSturdy(
                        player.level(),
                        player.onPos,
                        Direction.UP
                    )
                ) {
                    alert("必须站在实心方块上")
                } else {
                    confirm("要将脚下的位置，设置为回岛传送点吗？") {
                        //往上抬一格 不然掉下去了
                        server.hqSendAsync(true, true, "island/sethome", listOf("pos" to player.onPos.above().asLong()))
                    }
                }
            }
            }

            icon(icon = "plus", text = "邀请成员") {
                mc go inviteScreen
            }


        }
    }

    var bkspTickCount = 0
    override fun doTick() {
        if (mc pressingKey InputConstants.KEY_BACKSPACE)
            bkspTickCount++
        if (bkspTickCount == 60) {

            confirm("真的要${if (isOwner) "删除" else "退出"}这个岛屿吗?????????\n你的存档将会消失,无法恢复!!!!!!!!!!") {
                mc go delScreen(!isOwner)
            }
            bkspTickCount=0
        }

    }

    val isOwner = island.isOwner(account)
    override fun doRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.matrixOp {
            tran0ScaleBack(5,UiHeight-10,0.7)
        guiGraphics.drawText(
            text = "长按退格${(3-bkspTickCount / 20.0).toFixed(1)}秒${if (isOwner) "删除" else "退出"}岛屿",
        )
        }
    }
}