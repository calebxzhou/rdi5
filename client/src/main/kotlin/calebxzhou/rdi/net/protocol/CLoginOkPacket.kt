package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.lgr
import calebxzhou.rdi.net.CPacket
import calebxzhou.rdi.net.RByteBuf
import calebxzhou.rdi.service.LevelService
import calebxzhou.rdi.util.renderThread

class CLoginOkPacket(): CPacket {
    constructor(buf: RByteBuf): this(){}
    override fun handle() {
        lgr.info("登录成功")
        renderThread {

         //   LevelService.startLevel()
        }
    }
}