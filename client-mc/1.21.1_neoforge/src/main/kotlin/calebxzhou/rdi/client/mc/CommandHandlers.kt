package calebxzhou.rdi.client.mc

import calebxzhou.rdi.client.mc.RDI.Companion.sendResponse
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import net.minecraft.client.gui.screens.TitleScreen

/**
 * calebxzhou @ 2025-12-15 13:06
 */

fun AddChatMsgCommand.handle() {
    mc.addChatMessage(this.msg)
}

fun ChangeHostCommand.handle()= mainThread {
    mc.level?.disconnect()
    mc.disconnect()
    mc set TitleScreen()
    RDI.HOST_PORT = this.newPort
    mc.connectServer(RDI.GAME_IP)
}

suspend fun GetVersionIdCommand.handle(session: DefaultClientWebSocketSession) {
    session.sendResponse(this,mc.launchedVersion)
}
