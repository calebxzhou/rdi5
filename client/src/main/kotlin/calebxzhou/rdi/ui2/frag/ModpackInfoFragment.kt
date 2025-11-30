package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.account
import calebxzhou.rdi.model.pack.Modpack
import calebxzhou.rdi.model.pack.ModpackDetailedVo
import calebxzhou.rdi.model.pack.latest
import calebxzhou.rdi.net.humanSize
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.CurseForgeService
import calebxzhou.rdi.service.CurseForgeService.fillCurseForgeVo
import calebxzhou.rdi.service.CurseForgeService.mapMods
import calebxzhou.rdi.service.selectModpackFile
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.LoadingView
import calebxzhou.rdi.ui2.component.ModGrid
import calebxzhou.rdi.ui2.component.alertOk
import calebxzhou.rdi.ui2.component.closeLoading
import calebxzhou.rdi.ui2.component.confirm
import calebxzhou.rdi.ui2.component.showLoading
import calebxzhou.rdi.util.humanDateTime
import calebxzhou.rdi.util.ioTask
import icyllis.modernui.view.Gravity
import io.ktor.http.*
import org.bson.types.ObjectId

class ModpackInfoFragment(val modpackId: ObjectId,val changeHostId: ObjectId? = null) : RFragment("整合包信息") {
    override var fragSize = FragmentSize.FULL
    override var preserveViewStateOnDetach  = true

    init {
        contentViewInit = {
            server.request<ModpackDetailedVo>("modpack/$modpackId") {
                it.data?.run {
                    if (versions.isNotEmpty()) {
                        versions.latest.mods.fillCurseForgeVo()
                    }
                    load()
                }
            }
        }
    }

    private fun ModpackDetailedVo.load() = uiThread {

        titleView.apply {
            quickOptions {
                if (authorId == account._id) {
                    "\uF1F8 删除" colored MaterialColor.RED_900 with {
                        confirm("确定要永久删除这个整合包吗？？无法恢复！！") {
                            server.requestU("modpack/$modpackId", HttpMethod.Delete) {
                                toast("删完了")
                                close()
                            }
                        }
                    }
                }
                /* "▶ 拿最新版开服" colored MaterialColor.GREEN_900 with {
                     HostListFragment.Create(modpackId, name, versions.latest.name).go()
                 }*/
            }
        }


        contentView.apply {
            textView("$name         \uF4CA上传者：$authorName      \uF11BMC版本：$mcVer $modloader")
            textView("简介：$info")
            textView("共${versions.size}个版本：")
            versions.forEach { v ->
                linearLayout {
                    gravity = Gravity.CENTER_VERTICAL
                    padding8dp()
                    textView("V${v.name} - 上传时间：${v.time.humanDateTime} - \uF0C7${v.totalSize?.humanSize ?: ""}")
                    textView(when(v.status ){
                        Modpack.Status.OK -> "（可用）"
                        Modpack.Status.BUILDING -> "（构建中）"
                        Modpack.Status.FAIL -> "（构建失败）"
                        Modpack.Status.WAIT -> "（等待构建）"
                    })
                    quickOptions {
                        if (v.status == Modpack.Status.OK) {
                            changeHostId?.let { hostId->
                                "\uDB86\uDDD8 使用此版本作主机整合包" colored MaterialColor.TEAL_900 with {
                                    server.requestU("host/$hostId/modpack/${modpackId}/${v.name}") {
                                        close()
                                        alertOk("已更换主机整合包为$name ${v.name}")
                                    }
                                }
                            }?:let {
                                "▶ 用此版创建主机" colored MaterialColor.GREEN_900 with {
                                    HostCreateFragment(
                                        modpackId,
                                        name,
                                        v.name,
                                        v.mods.find { it.slug == "skyblock-builder" } != null
                                    ).go()
                                }
                            }
                        }
                        "\uF1F8 删除" colored MaterialColor.RED_900 with {
                            confirm("确定要永久删除这个版本吗？？无法恢复！！") {
                                server.requestU("modpack/$modpackId/version/${v.name}", HttpMethod.Delete) {
                                    toast("删完了")
                                    reloadFragment()
                                }
                            }
                        }
                        "\uF0AD 重构" with {
                            confirm("将使用最新版rdi核心重新构建此版本的整合包，确定吗？") {
                                server.requestU("modpack/$modpackId/version/${v.name}/rebuild") {
                                    toast("提交请求了 完事了发信箱告诉你")
                                    reloadFragment()
                                }
                            }
                        }
                    }
                }
            }
            if (versions.isNotEmpty()) {
                textView("mod列表：$modCount 个")
                this += ModGrid(context, mods = versions.latest.mods)
            } else {
                textView("此整合包暂无可用版本，等待作者上传....")
                button("↑ 上传新版") { btn->
                    selectModpackFile?.let {
                        contentView.textView("正在读取此整合包...")
                        btn.isEnabled = false
                        ioTask {
                            val data = CurseForgeService.loadModpack(it)
                            ModpackUploadFragment.Confirm(data, data.manifest.files.mapMods(), modpackId, name).go()
                        }
                    }
                }
            }
        }
    }
}