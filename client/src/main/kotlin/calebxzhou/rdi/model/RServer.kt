package calebxzhou.rdi.model

import calebxzhou.rdi.net.GameNetClient
import calebxzhou.rdi.net.httpRequest
import calebxzhou.rdi.net.success
import calebxzhou.rdi.ui2.frag.LoadingFragment
import calebxzhou.rdi.ui2.frag.SelectAccountFragment
import calebxzhou.rdi.ui2.frag.alertErr
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.uiThread
import io.ktor.client.statement.*
import io.ktor.util.*
import io.netty.channel.ChannelFuture
import kotlinx.coroutines.launch

class RServer(
    val ip: String,
    val gamePort: Int,
    val hqPort: Int
) {
    var unicomIp = ip
    var cmccIp = ip
    companion object {
        var now: RServer? = null
        val OFFICIAL_DEBUG = RServer(
            "127.0.0.1", 28506, 28507
        )
        val OFFICIAL_NNG = RServer(
            "rdi.calebxzhou.cn",
            28506, 28507,
        )

    }
    var chafu: ChannelFuture? = null
    fun connect() {
        try {
            GameNetClient.connect(this)
        } catch (e: Exception) {
            e.printStackTrace()
            alertErr("连接服务器失败: ${e.message ?: "未知错误"}")
            return
        }
        /*if (Const.DEBUG) {
            val account = RAccount.TESTS[System.getProperty("rdi.testAccount").toInt()]
            GameNetClient.send(SMeJoinPacket(account.qq, account.pwd))

        }*/
        /**/
        // Add listener to check connection status
        mc go SelectAccountFragment(this)
    }

    suspend fun prepareRequest(
        post: Boolean = false,
        path: String,
        params: List<Pair<String, Any>> = listOf(),
    ): HttpResponse {
        val fullUrl = "http://${ip}:${hqPort}/${path}"
        val headers = RAccount.now?.let {
            listOf("Authorization" to "Basic ${"${it._id}:${it.pwd}".encodeBase64()}")
        } ?: listOf()
        return httpRequest(post, fullUrl, params, headers)

    }
    fun hqRequest(
        post: Boolean = false,
        path: String,
        showLoading: Boolean = true,
        params: List<Pair<String, Any>> = listOf(),
        onOk: (HttpResponse) -> Unit
    ) {
        var frag: LoadingFragment? = null
        if (showLoading){
            frag= LoadingFragment()
            uiThread {
                mc go frag
            }
        }
        ioScope.launch {
            val req = prepareRequest(post, path, params)
            if (req.success) {
                onOk(req)
            }
            if(showLoading && frag != null) {
                uiThread {
                    frag.close()
                }
            }

        }
    }
}