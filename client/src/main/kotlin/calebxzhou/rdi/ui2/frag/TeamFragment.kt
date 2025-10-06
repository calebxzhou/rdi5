package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.Team
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.service.owner
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.button
import calebxzhou.rdi.ui2.center
import calebxzhou.rdi.ui2.go
import calebxzhou.rdi.ui2.component.REditText
import calebxzhou.rdi.ui2.component.RTextField
import calebxzhou.rdi.ui2.component.alertOk
import calebxzhou.rdi.ui2.component.confirm
import calebxzhou.rdi.ui2.contextMenu
import calebxzhou.rdi.ui2.editText
import calebxzhou.rdi.ui2.fctx
import calebxzhou.rdi.ui2.headButton
import calebxzhou.rdi.ui2.iconButton
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.paddingDp
import calebxzhou.rdi.ui2.plusAssign
import calebxzhou.rdi.ui2.showOver
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.toast
import calebxzhou.rdi.ui2.uiThread
import icyllis.modernui.R.attr.button

class TeamFragment : RFragment("我的团队") {
    private val server = RServer.now
    private val account = RAccount.now ?: RAccount.DEFAULT
    override var fragSize: FragmentSize
        get() = FragmentSize.SMALL
        set(value) {}

    init {

        bottomOptionsConfig = {
            "▶ 游玩主机" colored MaterialColor.GREEN_900 with {}
        }
        contentLayoutInit = {
            showLoadingState()
            loadTeam()
        }
    }

    private fun showLoadingState(message: String = "正在加载团队信息...") {
        contentLayout.removeAllViews()
        contentLayout.textView(message) { center() }
    }

    private fun renderTeam(team: Team) {
        contentLayout.removeAllViews()
        title = team.name
        contentLayout.linearLayout {
            team.members.forEach { member ->
                headButton(member.id, init = {
                    setTextColor(
                        when (member.role) {
                            Team.Role.OWNER -> 0xFFFFD700.toInt()
                            Team.Role.ADMIN -> 0xFFC0C0C0.toInt()
                            Team.Role.MEMBER -> 0xFFCD7F32.toInt()
                            else -> -0x1
                        }
                    )
                    contextMenu {
                        if (member.role != Team.Role.OWNER)
                            "踢出" with {
                                server.hqRequest(true, "team/kick", params = listOf("uid2" to member.id.toString())) {
                                    confirm("要踢出该成员吗？"){
                                        toast("已踢出")
                                        loadTeam()
                                    }
                                }
                            }
                        when (member.role) {
                            Team.Role.OWNER -> "解散团队" with { Confirm().showOver(this@TeamFragment) }
                            Team.Role.ADMIN -> "取消管理者身份" with {}
                            Team.Role.MEMBER -> "设置为管理者" with {}
                            else -> {}
                        }
                    }
                })
            }
            if (team.owner?.id == account._id) {
                button("＋", width = 40, init = {
                    textSize = 24f
                    paddingDp(0, 0, 0, 4)
                }) {
                    Invite().go()
                }
            }
        }


    }

    class Confirm : RFragment("确认解散团队") {
        override var fragSize = FragmentSize.SMALL
        lateinit var t1: RTextField

        init {
            contentLayoutInit = {
                textView("解散团队后，地图、主机数据将被清空，且无法恢复。在下方输入 确认解散", init = { center() })
                t1 = editText("输入 确认解散")
            }
            bottomOptionsConfig = {
                "确认解散" colored MaterialColor.RED_900 with {
                    if (t1.edit.text.toString() != "确认解散") {
                        return@with
                    }

                    RServer.now.hqRequest(true, "team/delete") {
                        ProfileFragment().go()
                        alertOk("团队已解散")
                    }
                }
            }
        }
    }

    class Invite : RFragment("邀请成员") {
        override var fragSize = FragmentSize.SMALL
        private lateinit var qqInput: REditText

        init {
            contentLayoutInit = {
                qqInput = REditText(fctx, "QQ号").also { contentLayout += it }
                bottomOptionsConfig = {
                    "邀请" colored MaterialColor.GREEN_900 with {
                        val qq = qqInput.text.toString()
                        RServer.now.hqRequest(true, "team/invite", params = listOf("qq" to qq)) {
                            uiThread {
                                //todo  loadTeam
                                toast("拉人成功")
                            }
                        }
                    }
                }
            }
        }
    }

    fun loadTeam() {
        server.hqRequestT<Team>(
            false, "team/my", showLoading = true,
            onErr = {
                uiThread {
                    confirm(
                        "你还没有加入团队，你可以：",
                        yesText = "创建自己的团队",
                        noText = "等朋友拉我",
                        onYes = {
                            server.hqRequest(
                                true,
                                "team/create",
                                true,
                                params = listOf(
                                    "name" to "${account.name}的团队",
                                    "info" to ""
                                )
                            ) {
                                uiThread {
                                    toast("创建成功 可以进入团队了")
                                    loadTeam()
                                }
                            }
                        },
                        onNo = {
                            close()
                        }
                    )
                }
            }) { resp ->
            val data = resp.data
            uiThread {
                if (data != null) {
                    renderTeam(data)
                } else {
                    showLoadingState("未找到团队信息")
                }
            }
        }
    }

}