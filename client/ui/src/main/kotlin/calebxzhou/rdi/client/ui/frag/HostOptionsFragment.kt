package calebxzhou.rdi.client.ui.frag

import calebxzhou.rdi.common.json
import calebxzhou.rdi.common.model.Host
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.ui.FragmentSize
import calebxzhou.rdi.client.ui.MaterialColor
import calebxzhou.rdi.client.ui.button
import calebxzhou.rdi.client.ui.component.confirm
import calebxzhou.rdi.client.ui.go
import calebxzhou.rdi.client.ui.toast
import io.ktor.http.HttpMethod

class HostOptionsFragment(val host: Host): RFragment("地图设置") {
    override var fragSize: FragmentSize
        get() = FragmentSize.SMALL
        set(value) {}
    init {
        titleViewInit = {
            quickOptions {
                "提交" colored MaterialColor.TEAL_900 with {
                    confirm("已修改以下内容：\n游戏规则：${host.gameRules.map { "${it.key}:${it.value}" }.toList().joinToString("\n")} \n 是否提交？") {
                        server.requestU("host/${host._id}/gamerules", HttpMethod.Put,mapOf("data" to host.gameRules.json) ){
                            toast("已提交修改")
                            close()
                        }
                    }
                }
            }
        }
        contentViewInit = {
            button("修改游戏规则"){
                GameRulesFragment(host.gameRules).go()
            }
        }
    }
}