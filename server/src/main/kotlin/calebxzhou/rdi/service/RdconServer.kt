package calebxzhou.rdi.service

import net.minecraft.server.ServerInterface


//udp simple rcon server
class RdconServer(val server: ServerInterface) {


    fun runCommand(command: String): String {
        return server.runCommand(command)
    }
}