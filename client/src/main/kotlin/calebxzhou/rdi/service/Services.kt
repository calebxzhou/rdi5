package calebxzhou.rdi.service

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.RServer

/**
 * calebxzhou @ 2025-07-31 21:55
 */
fun logout(){
    RServer.now=null
    RAccount.now=null

}