package calebxzhou.rdi.ui.frag

import calebxzhou.rdi.common.json
import calebxzhou.rdi.common.model.Host
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui.FragmentSize
import calebxzhou.rdi.ui.MaterialColor
import calebxzhou.rdi.ui.button
import calebxzhou.rdi.ui.component.confirm
import calebxzhou.rdi.ui.go
import calebxzhou.rdi.ui.toast
import io.ktor.http.HttpMethod

class HostOptionsFragment(val host: Host): RFragment("主机设置") {
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