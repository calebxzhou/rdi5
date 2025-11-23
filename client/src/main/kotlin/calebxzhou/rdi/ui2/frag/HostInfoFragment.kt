package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.Host
import calebxzhou.rdi.model.Role
import calebxzhou.rdi.model.account
import calebxzhou.rdi.model.pack.Mod
import calebxzhou.rdi.model.pack.ModpackDetailedVo
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.CurseForgeService.fillCurseForgeVo
import calebxzhou.rdi.service.isAdmin
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.component.ModGrid
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.component.confirm
import calebxzhou.rdi.ui2.go
import calebxzhou.rdi.ui2.headButton
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.misc.contextMenu
import calebxzhou.rdi.ui2.plusAssign
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.toast
import calebxzhou.rdi.ui2.uiThread
import io.ktor.http.HttpMethod
import io.ktor.http.HttpMethod.Companion.Delete
import org.bson.types.ObjectId

class HostInfoFragment(val hostId: ObjectId) : RFragment("主机详细信息") {
    override var fragSize = FragmentSize.FULL
    override var preserveViewStateOnDetach = true

    init {

        contentViewInit = {
            server.request<Host>("host/$hostId") { resp ->
                resp.data?.let { host ->
                    server.request<ModpackDetailedVo>("modpack/${host.modpackId}") { resp2 ->
                        resp2.data?.let { modpack ->
                            val mods = (modpack.versions.find { it.name == host.packVer }?.mods?:listOf()).fillCurseForgeVo()
                            host.load(modpack,mods)
                        }
                    }
                }
            }
        }
    }

    private fun Host.load(modpack: ModpackDetailedVo, mods: List<Mod>) = uiThread {
        val meOwner = ownerId == account._id
        val meAdmin = isAdmin(account)
        contentView.apply {
            textView("名称：${name}")
            textView("简介：${intro}")
            linearLayout {
                textView("成员：")
                members.forEach { member ->
                    headButton(member.id, init = {
                        setTextColor(
                            when (member.role) {
                                Role.OWNER -> 0xFFFFD700.toInt()
                                Role.ADMIN -> 0xFFC0C0C0.toInt()
                                Role.MEMBER -> 0xFFCD7F32.toInt()
                                else -> -0x1
                            }
                        )

                        contextMenu {
                            if (meOwner) {
                                when (member.role) {
                                    Role.ADMIN -> "取消管理员身份" with {
                                        confirm("要取消该成员的管理员身份吗？") {
                                            server.requestU(
                                                path = "host/${hostId}/member/${member.id}/role/${Role.MEMBER.name}",
                                                method = HttpMethod.Put,
                                                onOk = {
                                                    toast("已取消")
                                                    reloadFragment()
                                                }
                                            )
                                        }
                                    }

                                    Role.MEMBER -> "设置为管理员" with {
                                        confirm("要设置该成员为管理员吗？") {
                                            server.requestU(
                                                path = "host/${hostId}/member/${member.id}/role/${Role.ADMIN.name}",
                                                method = HttpMethod.Put,
                                                onOk = {
                                                    toast("已设置")
                                                    reloadFragment()
                                                }
                                            )
                                        }
                                    }

                                    else -> {}
                                }
                            }
                            if(meAdmin){
                                "踢出" with {
                                    confirm("要踢出该成员吗？") {
                                        server.requestU(
                                            path = "host/${hostId}/member/${member.id}",
                                            method = Delete,
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
                    })
                }
            }

            textView("mod列表：")
            this += ModGrid(context, mods = mods )
        }
        titleView.apply {
            if (meOwner) {
                quickOptions {
                    "▶ 开始游玩"
                    "\uF4FE 邀请成员" colored MaterialColor.BLUE_900 with {
                        HostInviteMemberFragment(hostId).go()
                    }
                    "\uEA81 删除主机" colored MaterialColor.RED_900 with {
                        confirm("确认删除主机吗？\n（不删存档，其余数据清空）") {
                            server.requestU("host/$hostId", Delete) { resp ->
                                close()
                                toast("成功删除主机")
                            }
                        }
                    }
                    "\uDB80\uDD8D 后台" colored MaterialColor.TEAL_900 with { HostConsoleFragment(hostId  ).go() }
                    "\uDB85\uDC5C 切换存档" colored MaterialColor.PINK_800 with { alertErr("没写完") }
                    "\uDB80\uDFD6 更新整合包" colored MaterialColor.AMBER_900 with {
                        confirm("将更新主机的整合包 ${modpack.name} 到最新版本。") {
                            server.requestU(
                                "host/${_id}/update",
                                HttpMethod.Post,
                                showLoading = true
                            ) {
                                toast("已更新到最新版 主机重启中")
                            }
                        }
                    }
                }
            }
        }
    }
}