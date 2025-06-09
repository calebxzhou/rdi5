package calebxzhou.rdi.service

object ChatService {
    //0-全局聊天 1-岛内成员聊天
    var chatMode = 0
    @JvmStatic
    fun sendMessage(content: String){
     /*   if(!mc.isPlayingServer) return
        RServer.now?.hqSendAsync(false,true,"chat", listOf(
            "mode" to chatMode.toString(), "content" to content
        )
        ){
          //  mc.addChatMessage("${if(chatMode==1)"岛内> " else ""}我: $content")
        }*/
    }
}