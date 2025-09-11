package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.Room
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.ui2.button
import calebxzhou.rdi.ui2.component.RTextField
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.component.confirm
import calebxzhou.rdi.ui2.editText
import calebxzhou.rdi.ui2.toast
import calebxzhou.rdi.ui2.uiThread
import calebxzhou.rdi.util.copyToClipboard
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc

class ConfirmDeleteRoomFragment(val room: Room,val server: RServer): RFragment("确认删除房间 ${room.name}"){
    init {
        copyToClipboard(room._id.toString())
    }
    private lateinit var idInput: RTextField
    override fun initContent() {

        contentLayout.apply {
            idInput = editText("输入你的房间ID ${room._id}")
            button("删除"){
                if(idInput.txt != room._id.toString()){
                    alertErr("房间ID输入错误")
                    return@button
                }
                confirm("真的要删除吗？所有的进度 存档等数据都会被永久删除！") {

                    server.hqRequest(true, "room/delete",) {
                        uiThread {

                            toast("房间删除成功")
                        }
                        val f = TitleFragment()
                        mc go f
                    }
                }
            }
        }
    }

}