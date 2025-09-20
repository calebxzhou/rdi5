package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.Room
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.RTextField
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.component.confirm

class ConfirmDeleteRoomFragment(): RFragment("确认删除房间"){
    private lateinit var idInput: RTextField
    init {
        val room = Room.now
        copyToClipboard(room._id.toString())
        contentLayoutInit = {
            idInput = editText("输入你的房间ID ${room._id}")
            button("删除", init = {center()}){
                if(idInput.txt != room._id.toString()){
                    alertErr("房间ID输入错误")
                    return@button
                }
                confirm("真的要删除吗？所有的进度 存档等数据都会被永久删除！") {
                    RServer.now.hqRequest(true, "room/delete",) {
                        uiThread {

                            toast("房间删除成功")
                        }
                        goto(TitleFragment())
                    }
                }
            }
        }
    }
}