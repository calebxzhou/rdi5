package calebxzhou.rdi.service

import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.mixin.AMinecraft
import calebxzhou.rdi.model.HwSpec
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.net.success
import calebxzhou.rdi.ui2.frag.alertErr
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.serdesJson
import kotlinx.serialization.encodeToString

object PlayerService  {
    suspend fun RServer.playerLogin(usr: String, pwd: String): RAccount? {
        val creds = LocalCredentials.read()
        val spec = serdesJson.encodeToString<HwSpec>(HwSpec.now)
        val resp = prepareRequest(path = "login",
            post = true,
            params = listOf("usr" to usr, "pwd" to pwd,"spec" to spec))
        if(resp.success){
            val account = serdesJson.decodeFromString<RAccount>(resp.body)
            creds.idPwds += account._id.toHexString() to account.pwd
            creds.lastLoggedId = account._id.toHexString()
            creds.write()
            RAccount.now = account
            (mc as AMinecraft).setUser(account.mcUser)

            return account
        }else{
            alertErr(resp.body)
            return null
        }

    }
    suspend fun RServer.sendLoginRecord(account: RAccount){

    }


}