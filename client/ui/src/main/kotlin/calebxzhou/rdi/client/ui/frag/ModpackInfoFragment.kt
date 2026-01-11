package calebxzhou.rdi.client.ui.frag

import calebxzhou.mykotutils.std.humanFileSize
import calebxzhou.mykotutils.std.millisToHumanDateTime
import calebxzhou.rdi.common.model.Modpack
import calebxzhou.rdi.common.model.ModpackDetailedVo
import calebxzhou.rdi.common.model.latest
import calebxzhou.rdi.common.service.CurseForgeService
import calebxzhou.rdi.common.service.CurseForgeService.fillCurseForgeVo
import calebxzhou.rdi.common.service.CurseForgeService.mapMods
import calebxzhou.rdi.common.util.ioTask
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.service.ModpackService.startInstall
import calebxzhou.rdi.client.service.selectModpackFile
import calebxzhou.rdi.client.ui.*
import calebxzhou.rdi.client.ui.component.ModGrid
import calebxzhou.rdi.client.ui.component.confirm
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

                    load()
                }
            }
        }
    }
    private val ModpackDetailedVo.isAuthorNow get() = this.authorId == loggedAccount._id
    private fun ModpackDetailedVo.load() = uiThread {

        titleView.apply {
            quickOptions {
                if (isAuthorNow) {
                    "\uF1F8 删除整个包" colored MaterialColor.RED_900 with {
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
                    textView("V${v.name} - 上传时间：${v.time.millisToHumanDateTime} - \uF0C7${v.totalSize?.humanFileSize ?: ""}")
                    val textView = textView(
                        when (v.status) {
                            Modpack.Status.OK -> "（可用）"
                            Modpack.Status.BUILDING -> "（构建中）"
                            Modpack.Status.FAIL -> "（构建失败）"
                            Modpack.Status.WAIT -> "（等待构建）"
                        }
                    )
                    quickOptions {
                        if (v.status == Modpack.Status.OK) {
                                "▶ 创建地图" colored MaterialColor.GREEN_900 with {
                                    HostCreateFragment(
                                        modpackId,
                                        name,
                                        v.name,
                                        v.mods.find { it.slug == "skyblock-builder" } != null
                                    ).go()
                                }
                        }
                        if(isAuthorNow){
                            "\uF1F8 删除此版本" colored MaterialColor.RED_900 with {
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
                        if(v.status == Modpack.Status.OK) {
                            "\uF019 下载整合包" with {
                                v.startInstall(mcVer, modloader, name)
                            }
                        }
                    }
                }
            }
            if (versions.isNotEmpty()) {
                val loadText = textView("正在载入$modCount 个Mod的详细信息...")
                ioTask {
                    if (versions.isNotEmpty()) {
                        val mods = versions.latest.mods.fillCurseForgeVo()
                        uiThread {
                            loadText.text = "载入完成，共$modCount 个Mod："
                            this += ModGrid(context, mods = mods)
                        }
                    }
                }
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