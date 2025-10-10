package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.Host
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.button
import calebxzhou.rdi.ui2.iconButton
import calebxzhou.rdi.ui2.linearLayout

class HostListFragment: RFragment("选择主机") {
    override var fragSize = FragmentSize.SMALL
    init {
        bottomOptionsConfig ={
            "＋ 创建主机" colored MaterialColor.BLUE_900
        }
    }
    fun load(){
        RServer.now.hqRequestT<List<Host>>(false,"host/my"){
        }
    }
    fun render(hosts: List<Host>){
        contentLayout.removeAllViews()
        contentLayout.apply {
            hosts.forEach {
                button("\uF233 ${it.name}")
            }
        }
    }
}