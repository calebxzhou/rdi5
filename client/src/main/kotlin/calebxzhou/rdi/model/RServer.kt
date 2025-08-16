package calebxzhou.rdi.model

import calebxzhou.rdi.Const
import calebxzhou.rdi.net.httpStringRequest
import calebxzhou.rdi.net.success
import calebxzhou.rdi.ui2.frag.LoadingFragment
import calebxzhou.rdi.ui2.frag.SelectAccountFragment
import calebxzhou.rdi.ui2.frag.UpdateFragment
import calebxzhou.rdi.util.*
import io.netty.channel.ChannelFuture
import kotlinx.coroutines.launch
import net.minecraft.client.multiplayer.ServerData
import java.net.http.HttpResponse

class RServer(
    val ip: String,
    val hqPort: Int
) {
    var gamePorts = listOf(38510,38510)
    val gamePort = gamePorts[0]
    val noUpdate = System.getProperty("rdi.noUpdate").toBoolean()
    val mcData
        get() = {carrier: Int -> ServerData("RDI", "${ip}:${gamePorts[carrier]}", ServerData.Type.OTHER)}
    val hqUrl = "http://${ip}:${hqPort}/"

    //电信 联通 移动
    var gameCarrierIp = arrayOf(ip, ip, ip,ip)

    companion object {
        var now: RServer? = null
        val OFFICIAL_DEBUG = RServer(
            "127.0.0.1", 38511
        )
        val OFFICIAL_NNG = RServer(
            "rdi.calebxzhou.cn",
            28511,
        ).apply {
            gameCarrierIp = arrayOf(ip, "b5rdi.calebxzhou.cn")
            gamePorts = listOf(28510,58210)
        }

        val default: RServer
            get() = now ?: if(Const.DEBUG) OFFICIAL_DEBUG else OFFICIAL_NNG
    }

    var chafu: ChannelFuture? = null


    fun connect() {
        if (!noUpdate) {
            mc go UpdateFragment(this)
        } else {
            mc go SelectAccountFragment(this)
        }
    }



    suspend fun prepareRequest(
        post: Boolean = false,
        path: String,
        params: List<Pair<String, Any>> = listOf(),
    ): HttpResponse<String> {
        val fullUrl = "http://${ip}:${hqPort}/${path}"
        val headers = RAccount.now?.let {
            listOf("Authorization" to "Basic ${"${it._id}:${it.pwd}".encodeBase64}")
        } ?: listOf()
        return httpStringRequest(post, fullUrl, params, headers)

    }

    fun hqRequest(
        post: Boolean = false,
        path: String,
        showLoading: Boolean = true,
        params: List<Pair<String, Any>> = listOf(),
        onOk: (HttpResponse<String>) -> Unit
    ) {
        var frag: LoadingFragment? = null
        if (showLoading) {
            uiThread {
                frag = LoadingFragment()
                mc go frag
            }
        }
        ioScope.launch {
            val req = prepareRequest(post, path, params)
            if (showLoading && frag != null) {
                uiThread {
                    frag.close()
                }
            }
            if (req.success) {
                onOk(req)
            }


        }
    }
}