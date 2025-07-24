package calebxzhou.rdi.service

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.ui.screen.RProfileScreen
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.serdesJson
import icyllis.modernui.R.id.background
import kotlinx.coroutines.launch

object PlayerService {
    fun RServer.playerLogin(usr: String, pwd: String)    {
        hqRequest(
            path = "login",
            post = true,
            params = listOf("usr" to usr, "pwd" to pwd),
            onOk = {
                val account = serdesJson.decodeFromString<RAccount>(it.body)
                creds.idPwds += account._id.toHexString() to account.pwd
                creds.lastLoggedId = account._id.toHexString()
                creds.write()
                RAccount.now = account
                mc go RProfileScreen()
            }
        )
    }
}