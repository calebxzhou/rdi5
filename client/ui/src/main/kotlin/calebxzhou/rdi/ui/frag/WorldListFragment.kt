package calebxzhou.rdi.ui.frag

import calebxzhou.rdi.common.model.World
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui.*
import calebxzhou.rdi.ui.component.confirm
import calebxzhou.rdi.ui.misc.contextMenu
import io.ktor.http.*

class WorldListFragment: RFragment("选择存档") {
    override var fragSize = FragmentSize.MEDIUM
    init {
        contentViewInit = {

            server.request<List<World>>("world"){
                load(it.data!!)
            }
        }
    }
    private fun load(worlds: List<World>) = uiThread{
        contentView.removeAllViews()
        contentView.apply {
            worlds.forEach { world->
                //todo
                button("💾 ${world.name} ",init={
                    contextMenu {
                        "删除" with {
                            confirm("要永久删除存档”${world.name}“及其所有的回档点吗？无法恢复！"){
                                server.requestU("world/${world._id}", HttpMethod.Delete){
                                    toast("已删除")
                                    reloadFragment()
                                }
                            }
                        }
                        "复制" with{
                            confirm("要给存档”${world.name}“复制一份一模一样的吗？"){
                                server.requestU("world/${world._id}/copy", HttpMethod.Post){
                                    toast("已复制")
                                    reloadFragment()
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