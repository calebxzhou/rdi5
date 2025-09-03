package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.Room
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.ui2.component.REditText
import calebxzhou.rdi.ui2.editText
import calebxzhou.rdi.ui2.textButton
import calebxzhou.rdi.util.copyToClipboard
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc

class ConfirmDeleteRoomFragment(val room: Room): RFragment("确认删除房间 ${room.name}"){
    init {
        copyToClipboard(room._id.toString())
    }
    private lateinit var idInput: REditText
    override fun initContent() {

        contentLayout.apply {
            idInput = editText("输入你的房间ID ${room._id}")
            textButton("立刻永久删除所有的存档记录"){
                RServer.default.hqRequest(true,"room/delete",){
                    confirm("删除成功", onNo = {mc go TitleFragment()}){
                        mc go TitleFragment()
                    }

                }
            }
        }
    }

}