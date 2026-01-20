package calebxzhou.rdi.client.ui.frag

import calebxzhou.rdi.common.extension.isAdmin
import calebxzhou.rdi.common.model.Host
import calebxzhou.rdi.common.model.Mod
import calebxzhou.rdi.common.service.CurseForgeService.fillCurseForgeVo
import calebxzhou.rdi.common.util.ioTask
import calebxzhou.rdi.model.Role
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.service.ModpackService.startPlayLegacy
import calebxzhou.rdi.client.ui.*
import calebxzhou.rdi.client.ui.component.ModGrid
import calebxzhou.rdi.client.ui.component.confirm
import calebxzhou.rdi.client.ui.misc.contextMenu
import calebxzhou.rdi.common.model.Modpack
import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Delete
import org.bson.types.ObjectId
import kotlin.collections.isNotEmpty

class HostInfoFragment(val hostId: ObjectId) : RFragment("详细信息") {
    override var fragSize = FragmentSize.FULL
    override var preserveViewStateOnDetach = true

    init {

        contentViewInit = {
            server._request<Host>("host/$hostId") { resp ->
                resp.data?.let { host ->
                    val resp = server.makeRequest<Modpack.DetailVo>("modpack/${host.modpackId}")
                    val modpack = resp.data
                    val mods =
                        (modpack?.versions?.find { it.name == host.packVer }?.mods ?: listOf())
                    host.load(modpack, mods)

                }
            }
        }
    }

    private fun Host.load(modpack: Modpack.DetailVo?, mods: List<Mod>) = uiThread {
        val meAdmin = isAdmin(loggedAccount)
        val meOwner = ownerId == loggedAccount._id
        titleView.apply {
            quickOptions {
                if (modpack == null) {
                    textView("这个地图所使用的整合包被删除了，必须更换整合包才能继续游玩此地图。")
                } else {
                    "▶ 开始游玩" colored MaterialColor.GREEN_900 with {
                        this@load.startPlayLegacy()
                    }
                    "\uDB80\uDD8D 后台" colored MaterialColor.TEAL_900 with { HostConsoleFragment(hostId).go() }
                    if (meAdmin) {
                        "\uF013 设置" with {
                            HostOptionsFragment(this@load).go()
                        }
                    }

                }
                if(meOwner){
                    "\uEA81 删除地图" colored MaterialColor.RED_900 with {
                        confirm("确认删除地图吗？\n（仅删除成员列表。\n存档数据不会被删除，可导出或重复利用）") {
                            server._requestU("host/$hostId", Delete) { resp ->
                                close()
                                toast("成功删除地图")
                            }
                        }
                    }
                }

            }
        }
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
                                            server._requestU(
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
                                            server._requestU(
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
                            if (meAdmin) {
                                "踢出" with {
                                    confirm("要踢出该成员吗？") {
                                        server._requestU(
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
                if (meAdmin) {
                    button("+ 邀请成员") {
                        HostInviteMemberFragment(hostId).go()
                    }
                }
            }
            if (modpack != null) {
                linearLayout {
                    textView("整合包：${modpack.name} V${packVer}")
                    if(meAdmin){
                        button("\uDB80\uDFD6 更新") {
                            confirm("将更新地图当前的整合包《${modpack.name}》 到最新版本。") {
                                server._requestU(
                                    "host/${_id}/update",
                                    HttpMethod.Post,
                                    showLoading = true
                                ) {
                                    toast("已更新到最新版 地图重启中")
                                }
                            }
                        }
                    }
                }
            }
            if (mods.isNotEmpty()) {
                val modsText = textView("正在加载mod信息...共${mods.size}个")
                ioTask {
                    val mods = mods.fillCurseForgeVo()
                    uiThread {
                        this += ModGrid(context, mods = mods)
                        modsText.text = "Mod（共${mods.size}个）"
                    }
                }
            }
        }

    }
}
