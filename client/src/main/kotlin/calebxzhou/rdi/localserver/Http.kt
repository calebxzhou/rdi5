package calebxzhou.rdi.localserver

import calebxzhou.rdi.util.json
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.mcUserAdapter
import calebxzhou.rdi.util.serdesGson
import com.google.gson.reflect.TypeToken
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import net.minecraft.client.User
import net.minecraft.util.HttpUtil

/**
 * calebxzhou @ 2025-09-29 19:44
 */
val LOCAL_PORT = HttpUtil.getAvailablePort()
fun Routing.mainRoutes(){
    get("/mc-user"){
        json(mc.user)
    }
}