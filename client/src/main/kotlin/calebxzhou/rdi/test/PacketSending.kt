package calebxzhou.rdi.test

import calebxzhou.rdi.auth.RAccount
import calebxzhou.rdi.net.GameNetClient
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.protocol.SMeLeavePacket
import calebxzhou.rdi.net.protocol.SMeJoinPacket
import java.lang.Thread.sleep

/**
 * calebxzhou @ 2025-06-24 23:27
 */
fun main() {
    val account = RAccount.TESTS[0]
    while (true) {
        GameNetClient.connect(RServer.OFFICIAL_DEBUG)
        GameNetClient.send(SMeJoinPacket(account.qq, account.pwd))
        sleep(100)
        GameNetClient.send(SMeLeavePacket())
        sleep(100)
    }
}