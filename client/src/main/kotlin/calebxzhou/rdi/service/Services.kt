package calebxzhou.rdi.service

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.RServer

/**
 * calebxzhou @ 2025-07-31 21:55
 */
fun logout(){
    RServer.now=null
    RAccount.now=null

}