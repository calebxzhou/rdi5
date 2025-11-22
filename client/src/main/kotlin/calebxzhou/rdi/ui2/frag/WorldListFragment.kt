package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.World
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.confirm
import calebxzhou.rdi.ui2.misc.contextMenu
import io.ktor.http.*

class WorldListFragment: RFragment("选择存档") {
    override var fragSize = FragmentSize.SMALL
    init {
        contentViewInit = {
            load()
        }
    }
    private fun load(){
        server.request<List<World>>("world"){
            render(it.data!!)
        }
    }
    private fun render(worlds: List<World>) = uiThread{
        contentView.removeAllViews()
        contentView.apply {
            textView("右键可进行删除或复制等操作。"){ padding8dp()}
            worlds.forEach { world->
                button("💾 ${world.name} ",init={
                    contextMenu {
                        "删除" with {
                            confirm("要永久删除存档”${world.name}“及其所有的回档点吗？无法恢复！"){
                                server.requestU("world/${world._id}", HttpMethod.Delete){
                                    toast("已删除")
                                    load()
                                }
                            }
                        }
                        "复制" with{
                            confirm("要给存档”${world.name}“复制一份一模一样的吗？"){
                                server.requestU("world/${world._id}/copy", HttpMethod.Post){
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
                    textView("没有存档，请在建服时选择新建存档")
                }
        }
    }
}