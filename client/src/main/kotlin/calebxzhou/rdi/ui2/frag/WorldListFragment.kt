package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.World
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.button
import calebxzhou.rdi.ui2.component.confirm
import calebxzhou.rdi.ui2.misc.contextMenu
import calebxzhou.rdi.ui2.padding8dp
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.toast
import calebxzhou.rdi.ui2.uiThread
import io.ktor.http.HttpMethod

class WorldListFragment: RFragment("选择存档") {
    override var fragSize = FragmentSize.SMALL
    init {
        contentLayoutInit = {
            load()
        }
    }
    private fun load(){
        server.request<List<World>>("world/", showLoading = true){
            render(it.data!!)
        }
    }
    private fun render(worlds: List<World>) = uiThread{
        contentLayout.removeAllViews()
        contentLayout.apply {
            textView("右键可进行删除或复制等操作。"){ padding8dp()}
            worlds.forEach { world->
                button("💾 ${world.name} ",init={
                    contextMenu {
                        "删除" with {
                            confirm("要永久删除存档”${world.name}“及其所有的回档点吗？无法恢复！"){
                                server.request<Unit>("world/${world._id}", HttpMethod.Delete, showLoading = true){
                                    toast("已删除")
                                    load()
                                }
                            }
                        }
                        "复制" with{
                            confirm("要给存档”${world.name}“复制一份一模一样的吗？"){
                                server.request<Unit>("world/duplicate/${world._id}", HttpMethod.Post, showLoading = true){
                                    toast("已复制")
                                    load()
                                }
                            }
                        }
                        "回档" with {

                        }
                    }
                })
            }
                if(worlds.isEmpty()){
                    textView("没有存档，请在创建主机时选择新建存档")
                }
        }
    }
}