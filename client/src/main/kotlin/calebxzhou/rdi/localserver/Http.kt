package calebxzhou.rdi.localserver

import calebxzhou.rdi.model.HwSpec
import calebxzhou.rdi.util.gson
import calebxzhou.rdi.util.json
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.response
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

/**
 * calebxzhou @ 2025-09-29 19:44
 */
const val LOCAL_PORT = 5523//HttpUtil.getAvailablePort()
fun Routing.mainRoutes(){
    get("/mc-user"){
        gson(mc.user)
    }
    get("/hw-info"){
        json(HwSpec.now)
    }
}