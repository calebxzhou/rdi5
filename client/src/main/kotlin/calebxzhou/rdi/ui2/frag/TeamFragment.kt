package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.Team
import calebxzhou.rdi.model.account
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.isOwner
import calebxzhou.rdi.service.isOwnerOrAdmin
import calebxzhou.rdi.service.myTeam
import calebxzhou.rdi.service.owner
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.button
import calebxzhou.rdi.ui2.center
import calebxzhou.rdi.ui2.go
import calebxzhou.rdi.ui2.component.REditText
import calebxzhou.rdi.ui2.component.RTextField
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.component.alertOk
import calebxzhou.rdi.ui2.component.confirm
import calebxzhou.rdi.ui2.misc.contextMenu
import calebxzhou.rdi.ui2.textField
import calebxzhou.rdi.ui2.fctx
import calebxzhou.rdi.ui2.headButton
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.paddingDp
import calebxzhou.rdi.ui2.plusAssign
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.toast
import calebxzhou.rdi.ui2.uiThread
import calebxzhou.rdi.util.ioTask
import io.ktor.http.HttpMethod

class TeamFragment : RFragment("我的团队") {
    override var fragSize = FragmentSize.SMALL

    init {


        contentViewInit = {
            load()
        }
    }


    fun load()= ioTask {
        account.myTeam()?.let { renderTeam(it) }?: confirm(
            "你还没有加入团队，你可以：",
            yesText = "创建自己的团队",
            noText = "等朋友拉我",
            onYes = {
                server.requestU(
                    "team/",
                    HttpMethod.Post,
                    showLoading = true,
                    onOk = {
                        toast("创建成功 可以进入团队了")
                        reloadFragment()
                    }
                )
            },
            onNo = {
                close()
            }
        )

    }

    private fun renderTeam(team: Team): Unit = uiThread {
        contentView.removeAllViews()
        title = team.name
        contentView.linearLayout {
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
                        if (team.isOwnerOrAdmin(account._id)) {
                            //不允许踢出自己
                            if (account._id != member.id) {
                                "踢出" with {
                                    confirm("要踢出该成员吗？") {
                                        server.requestU(
                                            path = "team/member/${member.id.toHexString()}",
                                            method = HttpMethod.Delete,
                                            showLoading = true,
                                            onOk = {
                                                toast("已踢出")
                                                reloadFragment()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        if (team.isOwner(account)) {
                            when (member.role) {
                                Team.Role.OWNER -> "解散团队" with { Confirm().go() }
                                Team.Role.ADMIN -> "取消管理者身份" with {
                                    confirm("要取消该成员的管理者身份吗？") {
                                        server.requestU(
                                            path = "team/role/${member.id.toHexString()}/${Team.Role.MEMBER.name}",
                                            method = HttpMethod.Put,
                                            showLoading = true,
                                            onOk = {
                                                toast("已取消")
                                                load()
                                            }
                                        )
                                    }
                                }

                                Team.Role.MEMBER -> "设置为管理者" with {
                                    confirm("要设置该成员为管理者吗？") {
                                        server.requestU(
                                            path = "team/role/${member.id.toHexString()}/${Team.Role.ADMIN.name}",
                                            method = HttpMethod.Put,
                                            showLoading = true,
                                            onOk = {
                                                toast("已设置")
                                                load()
                                            }
                                        )
                                    }
                                }

                                else -> {}
                            }
                        }

                    }
                })
            }
            if (team.owner?.id == account._id) {
                button("＋", init = {
                    textSize = 24f
                    paddingDp(0, 0, 0, 4)
                }) {
                    Invite(::load).go()
                }
            }
        }
        contentView.bottomOptions {
                "▶ 游玩主机" colored MaterialColor.GREEN_900 with { HostListFragment().go() }
                "💾 管理存档" colored MaterialColor.BLUE_900 with { WorldListFragment().go() }

        }


    }

    class Confirm : RFragment("确认解散团队") {
        override var fragSize = FragmentSize.SMALL

        lateinit var t1: RTextField

        init {
            contentViewInit = {
                textView("解散团队后，地图、主机数据将被清空，且无法恢复。在下方输入 确认解散", init = { center() })
                t1 = textField("输入 确认解散")
            }
            bottomOptionsConfig = {
                "确认解散" colored MaterialColor.RED_900 with {
                    if (t1.edit.text.toString() != "确认解散") {
                        alertErr("输入的不正确")
                        return@with
                    }

                    server.requestU(
                        path = "team/",
                        method = HttpMethod.Delete,
                        showLoading = true,
                        onOk = {
                            ProfileFragment().go()
                            alertOk("团队已解散")
                        }
                    )
                }
            }
        }
    }

    class Invite(onOk: () -> Unit) : RFragment("邀请成员") {
        override var fragSize = FragmentSize.SMALL

        private lateinit var qqInput: REditText

        init {
            contentViewInit = {
                qqInput = REditText(fctx, "QQ号").also { contentView += it }
                bottomOptionsConfig = {
                    "邀请" colored MaterialColor.GREEN_900 with {
                        val qq = qqInput.text
                        server.requestU(
                            path = "team/member/$qq",
                            method = HttpMethod.Post,
                            showLoading = true,
                            onOk = {
                                uiThread {
                                    close()
                                    onOk()
                                    toast("拉人成功")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

}