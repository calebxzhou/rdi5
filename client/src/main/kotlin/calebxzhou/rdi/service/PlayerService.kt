package calebxzhou.rdi.service

import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.net.success
import calebxzhou.rdi.ui.screen.RProfileScreen
import calebxzhou.rdi.ui2.frag.alertErr
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.serdesJson
import icyllis.modernui.R.id.background
import kotlinx.coroutines.launch

object PlayerService {
    suspend fun RServer.playerLogin(usr: String, pwd: String): RAccount? {
        val creds = LocalCredentials.read()
        val resp = prepareRequest(path = "login",
            post = true,
            params = listOf("usr" to usr, "pwd" to pwd))
        if(resp.success){
            val account = serdesJson.decodeFromString<RAccount>(resp.body)
            creds.idPwds += account._id.toHexString() to account.pwd
            creds.lastLoggedId = account._id.toHexString()
            creds.write()
            RAccount.now = account
            return account
        }else{
            alertErr(resp.body)
            return null
        }

    }
}