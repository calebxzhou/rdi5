package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.Room
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.RTextField

class ConfirmDeleteRoomFragment(): RFragment("确认删除房间"){
    private lateinit var idInput: RTextField
    init {
        val room = Room.now
        copyToClipboard(room._id.toString())
        contentViewInit = {
            idInput = textField("输入你的房间ID ${room._id}")
            button("删除", init = {center()})
        }
    }
}