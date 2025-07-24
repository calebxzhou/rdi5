package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.net.RServer

class SelectAccountFragment(val server: RServer) : RFragment("选择账号"){
    val creds = LocalCredentials.read()
    init {
        RServer.now = server
    }
    override fun initContent() {

    }


}